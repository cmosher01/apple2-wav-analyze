package org.cmosher01.apple2;

import com.github.psambit9791.jdsp.signal.*;
import com.github.psambit9791.jdsp.signal.peaks.*;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public final class Apple2WavAnalyze {
    public static void main(final String... args) throws UnsupportedAudioFileException, IOException {

        try (final var file = AudioSystem.getAudioInputStream(new File(args[0]))) {
            final var format = file.getFormat();
            showAudioFormat("original", format);
            //102_048.0f
            final var desired = new AudioFormat(
                AudioFormat.Encoding.PCM_FLOAT,
                format.getSampleRate(),
                32,
                1,
                4,
                format.getFrameRate(),
                true);
            showAudioFormat("desired", desired);
            try (final var in = AudioSystem.getAudioInputStream(desired, file)) {
                final var actual = in.getFormat();
                showAudioFormat("actual", actual);
                analyze(in, actual);
            }
        }
    }

    private static void showAudioFormat(final String name, final AudioFormat format) {
        System.out.printf("%10s audio format: frame-rate=%10.2f, sample-rate=%10.2f, %s\n", name, format.getFrameRate(), format.getSampleRate(), format);
    }

    private static final int WIN = 11;

    private static void analyze(final AudioInputStream in, final AudioFormat format) throws IOException {
        stats();
        final ArrayList<Double> r = new ArrayList<>(10_000_000);

        final var rb = new byte[4];
        while (0 <= in.read(rb)) {
            r.add(to_float(rb));
        }
        System.out.printf("total samples read from file: %,d\n", r.size());
        stats();
        System.out.println();




        double[] signal = r.stream().mapToDouble(d -> d).toArray();

        double rate = format.getSampleRate();
        signal = new Resample(400, 40, "constant").resampleSignal(signal);
        rate = rate*(400/40);
        signal = new Smooth(signal, WIN, "triangular").smoothSignal();




        final var findPeaks = new FindPeak(signal);

        final Peak outPeaks = findPeaks.detectPeaks();
//        final int[] peaks = outPeaks.getPeaks();
        final int[] peaks = outPeaks.filterByWidth(to_samples(200.0*0.75, rate), to_samples(650.0*1.25, rate));

        final Peak outTroughs = findPeaks.detectTroughs();
//        final int[] troughs = outTroughs.getPeaks();
        final int[] troughs = outTroughs.filterByWidth(to_samples(200.0*0.75, rate), to_samples(650.0*1.25, rate));

        final int[] pts = Arrays.copyOf(peaks, peaks.length + troughs.length);
        System.arraycopy(troughs, 0, pts, peaks.length, troughs.length);
        Arrays.sort(pts);



        System.out.printf("count of peaks and troughs: %,d\n", pts.length);
//        for (int i = 1; i < peaks.length; ++i) {
//            System.out.printf("%6d: %7.2f\n", peaks[i], to_micros(peaks[i]-peaks[i-1], format));
//        }
        int m = 0;
        for (int i = 1; i < pts.length; ++i) {
            final double us = to_micros(pts[i]-pts[i-1], rate);
            final long rounded = Math.round(us);
            final long discrete = discrete(rounded);
            System.out.printf("%3d ", discrete);
            if (++m % 64 == 0) {
                System.out.println();
            }
        }
    }

    private static final double M = 1_000_000;
//    private static final double M = 1_020_484;

    private static double to_micros(final int samples, final double rate) {
        return (double)samples * M / rate;
    }

    private static double to_samples(final double us, final double rate) {
        return us / M * rate;
    }

    private static void stats() {
        final Runtime rt = Runtime.getRuntime();
        System.out.println("-----------------------");
        System.out.printf("mem  tot: %,12d\n", rt.totalMemory());
        System.out.printf("mem free: %,12d\n", rt.freeMemory());
        System.out.printf("mem used: %,12d\n", rt.totalMemory()-rt.freeMemory());
        System.out.println("-----------------------");
    }

    private static void analyzeOld(final AudioInputStream in) throws IOException {
        final var rb = new byte[102048];
        final var cb = in.read(rb);
        System.out.printf("read byte count: %d\n", cb);
        final var rf = ByteBuffer.wrap(rb).order(ByteOrder.BIG_ENDIAN).asFloatBuffer();




        final var rZeroes = new ArrayList<Integer>(1024*8);

        final double M = 1.0e5d/102048.0d;
        double t = 0.0;
        long is = 0;
        float fPrev = rf.get();
//            System.out.printf("%09d: %18.8f\n", is, fCurr);
        while (rf.hasRemaining()) {
            ++is;
            float fCurr = rf.get();
//            System.out.printf("%09d: %18.8f\n", is, fCurr);
            if (Math.signum(fCurr) == 0) {
                final double p = t;
                t = M*is;
                rZeroes.add(filter(t-p));
            } else if (Math.signum(fCurr) != Math.signum(fPrev)) {
                final double p = t;
                t = zero(M*(is-1), fPrev, M*is, fCurr);
                rZeroes.add(filter(t-p));
            }
            fPrev = fCurr;
        }

//        if (false) {
        if (true) {
            int cc = 0;
            for (int i = 0; i < rZeroes.size(); ++i) {
                System.out.printf("%d ", rZeroes.get(i));
                if (++cc % 64 == 0) {
                    System.out.println();
                }
            }
            System.out.println();
        }
    }

    private static boolean crosses_zero(final double f_prev, final double f) {
        if (Math.signum(f_prev) == 0) {
            return false;
        }
        if (Math.signum(f) == 0) {
//            System.out.println("\n########################### ZERO\n");
            return true;
        }
        return
            Math.signum(f) != Math.signum(f_prev) ||
            Math.signum(f) == 0;
    }

    private static void analyze2(final AudioInputStream in) throws IOException {
        final double M = 1.0e5d/102048.0d;

        final var rb = new byte[4];

        int cc = 0;
        double t = 0.0;
        long is = 0;
        double f_prev = 0.0f;
        while (0 <= in.read(rb)) {
            ++is;
            final double f = to_float(rb);
//            System.out.printf("%09d: %18.8f\n", is, f);

//            if (crosses_zero(f_prev, f)) {
//                final double p = t;
//                t = zero(M * (is - 1), f_prev, M * is, f);
//                final long w = filter(t - p);
//                System.out.printf("%d ", w);
//                if (w==0) {
//                    System.out.printf("<@%d:%18.8f,%18.8f;%18.8f,%18.8f] ", is,M * (is - 1), f_prev, M * is, f);
//                }
//                if (++cc % 64 == 0) {
//                    System.out.println();
//                    System.out.printf("%d: ", is);
//                }
//            }

            f_prev = f;
        }

        System.out.println();
    }

    private static double to_float(final byte[] rb) {
        return ByteBuffer.wrap(rb).order(ByteOrder.BIG_ENDIAN).asFloatBuffer().get();
    }

    private static double zero(final double t0, final double v0, final double t1, final double v1) {
        final double m = (v1-v0)/(t1-t0);
        final double b = v0-m*t0;
        final double z = -(b/m);
//        System.out.printf("%18.8f <== %18.8f,%18.8f;%18.8f,%18.8f     ", z, t0, v0, t1, v1);
        return z;
    }

    private static int filter(final double x) {
        return round(x); // for testing. all results should be close to the expected discrete values
//        return discrete(round(x));
    }

    private static int round(final double x) {
        final long d = Math.round(x);
        return (int)d;
    }

    // 000 [135] 200 [225] 250 [375] 500 [575] 650 [700]
    private static long discrete(final long d) {
        if (d < 135L) {
            return 0L;
        }
        if (d < 225L) {
            return 200L;
        }
        if (d < 375L) {
            return 250L;
        }
        if (d < 575L) {
            return 500L;
        }
        if (d < 700L) {
            return 650L;
        }

        return 1000L+d;
    }
}
