package org.cmosher01.apple2;

import java.io.PrintStream;

public class Printer {
    private final int width;
    private final PrintStream out;

    private int pos = 0;

    public Printer(final int width, final PrintStream out) {
        this.width = width;
        this.out = out;
    }

    public void newLine() {
        this.pos = 0;
        this.out.println();
    }

    public void print(final PeakTrough pt) {
        if (pt.w() == -1) {
            return;
        }

        if (this.pos == 0) {
            this.out.printf("%10.6f: ", pt.ts());
        }
        this.out.printf("%3d ", pt.w());
        if (this.width <= ++this.pos) {
            newLine();
        }
    }

    public void dot() {
        this.out.print(".");
        if (this.width <= ++this.pos) {
            newLine();
        }
    }

    public void bit(final int bit, final PeakTrough pt) {
        if (this.pos == 0) {
            this.out.printf("%10.6f: ", pt.ts());
        }
        this.out.printf("%c ", (bit!=0) ? '1' : '0');
        if (this.width <= ++this.pos) {
            newLine();
        }
    }

    public void tag(final String tag, final PeakTrough pt) {
        if (0 < this.pos) {
            newLine();
        }
        this.out.printf("%10.6f: [%s]\n", pt.ts(), tag);
    }
}
