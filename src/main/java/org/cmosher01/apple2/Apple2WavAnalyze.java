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
            final var desired = desiredFormat(format);
            showAudioFormat("desired", desired);
            try (final var in = AudioSystem.getAudioInputStream(desired, file)) {
                final var actual = in.getFormat();
                showAudioFormat("actual", actual);

                final var signal = readSignalFile(in);
                analyze(signal, actual);
            }
        }
    }

    private static AudioFormat desiredFormat(final AudioFormat format) {
        return new AudioFormat(
            AudioFormat.Encoding.PCM_FLOAT,
            format.getSampleRate(),
            32,
            1,
            4,
            format.getFrameRate(),
            true);
    }

    private static void showAudioFormat(final String name, final AudioFormat format) {
        System.out.printf("%10s audio format: frame-rate=%.1f, sample-rate=%.1f, %s\n", name, format.getFrameRate(), format.getSampleRate(), format);
    }

    private static final int SMOOTHING_WINDOW = 11;

    private record PeakTrough (
        double ts,
        long w
    ) {
    }

    private static void analyze(double[] signal, final AudioFormat format) {
        double rate = format.getSampleRate();
        signal = new Resample(400, 40, "constant").resampleSignal(signal);
        rate *= 400/40;
        signal = new Smooth(signal, SMOOTHING_WINDOW, "triangular").smoothSignal();

        final var pts = findPeaksAndTroughs(signal, rate);
        System.out.printf("count of peaks and troughs: %,d\n", pts.length);
        System.out.println();

        final var rPT = findDiscreteWidths(pts, rate);
        for (int i = 0; i < rPT.size(); ++i) {
            final var pt = rPT.get(i);
            if (i % 64 == 0) {
                System.out.printf("%10.6f: %010d: ", pt.ts, i);
            }
            System.out.printf("%3d ", pt.w);
            if ((i+1) % 64 == 0) {
                System.out.println();
            }
        }

    }

    private static ArrayList<PeakTrough> findDiscreteWidths(int[] pts, double rate) {
        final var rPT = new ArrayList<PeakTrough>();

        for (int i = 1; i < pts.length; ++i) {
            final double us = to_micros(pts[i]- pts[i-1], rate);
            long w = 0;

            w = Math.round(us);
            w = discrete(w);

            rPT.add(new PeakTrough(to_micros(pts[i], rate)/1e6, w));
        }
        return rPT;
    }

    private static double[] readSignalFile(final AudioInputStream in) throws IOException {
        stats();

        final var rd = new ArrayList<Double>(10_000_000);

        final var rb = new byte[4];
        while (0 <= in.read(rb)) {
            rd.add(to_float(rb));
        }

        System.out.printf("total samples read from file: %,d\n", rd.size());
        stats();
        System.out.println();

        return rd.stream().mapToDouble(d -> d).toArray();
    }

    public static final double MIN_WIDTH = 200.0 * 0.75;
    public static final double MAX_WIDTH = 650.0*1.25;

    private static int[] findPeaksAndTroughs(final double[] signal, final double rate) {
        final var findPeaks = new FindPeak(signal);

        final var peaks = findPeaks.detectPeaks().filterByWidth(to_samples(MIN_WIDTH, rate), to_samples(MAX_WIDTH, rate));
        final var troughs = findPeaks.detectTroughs().filterByWidth(to_samples(MIN_WIDTH, rate), to_samples(MAX_WIDTH, rate));

        return merge(peaks, troughs);
    }

    private static int[] merge(final int[] ra, final int[] rb) {
        final int[] rc = new int[ra.length + rb.length];

        int ia = 0;
        int ib = 0;
        int ic = 0;

        while (ia < ra.length && ib < rb.length) {
            if (ra[ia] < rb[ib]) {
                rc[ic++] = ra[ia++];
            } else {
                rc[ic++] = rb[ib++];
            }
        }

        while (ia < ra.length) {
            rc[ic++] = ra[ia++];
        }

        while (ib < rb.length) {
            rc[ic++] = rb[ib++];
        }

        return rc;
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

    private static double to_float(final byte[] rb) {
        return ByteBuffer.wrap(rb).order(ByteOrder.BIG_ENDIAN).asFloatBuffer().get();
    }

    // d <135] 200 <225] 250 <375] 500 <575] 650 <700> d
    // 0-134, 200, 250, 500, 650, 700-n
    private static long discrete(final long d) {
        if (d < 135L) {
            return d;
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
        return d;
    }
}
