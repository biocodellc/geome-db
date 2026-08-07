package org.dhatim.fastexcel;

/**
 * Range variant for formulas that must not shift when Excel applies them to
 * multiple cells, such as data validation list sources.
 */
public class AbsoluteRange extends Range {

    public AbsoluteRange(Worksheet worksheet, int top, int left, int bottom, int right) {
        super(worksheet, top, left, bottom, right);
    }

    @Override
    public String toString() {
        return absoluteAddress(getLeft(), getTop()) + ":" + absoluteAddress(getRight(), getBottom());
    }

    private static String absoluteAddress(int column, int row) {
        return "$" + colToString(column) + "$" + (row + 1);
    }
}
