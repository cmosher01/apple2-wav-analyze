package org.cmosher01.apple2;

public record PeakTrough(
        double ts,
        long w /* negative values indicate flags:
        -1 ignore (inserted after real data to make lookahead algorithms simpler)
        */
) {
    public PeakTrough(final long w) {
        this(0.0, w);
    }
}
