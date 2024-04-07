package org.cmosher01.apple2;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class BitsToHex {
    public static void convert(final BufferedReader in, final int address) throws IOException {
//        final var toks = new StreamTokenizer(in);
//        for (var tok = toks.nextToken(); tok != StreamTokenizer.TT_EOF; tok = toks.nextToken()) {
//            switch (toks.ttype) {
//                case StreamTokenizer.TT_NUMBER: {
//                    System.out.println(toks.nval);
//                }
//                break;
//            }
//        }

        final var skipped = new ArrayList<String>();
        final var rb = new ArrayList<Integer>();
        {
            final var scanner = new Scanner(in);
            int b = 0; // unsigned byte 0-255
            int i = 8;
            while (scanner.hasNext()) {
                var tok = scanner.next();
                if (tok.equals("0") || tok.equals("1")) {
                    final var bit = tok.codePointAt(0) - '0';
                    b <<= 1;
                    if (bit != 0) {
                        b |= 1;
                    }
                    if (--i <= 0) {
                        rb.add(b);
                        b = 0;
                        i = 8;
                    }
                } else {
                    skipped.add(tok);
                }
            }
            if (i < 8) {
                b <<= i;
                rb.add(b);
            }
        }

        for (int ib = 0; ib < rb.size(); ++ib) {
            if (ib == 0 || (ib+address) % 8 == 0) {
                System.out.printf("%04X: ", ib+address);
            }
            var b = rb.get(ib);
            System.out.printf("%02X ", b);
            if ((ib+address+1) % 8 == 0) {
                System.out.println();
            }
        }
        System.out.println();
    }
}
