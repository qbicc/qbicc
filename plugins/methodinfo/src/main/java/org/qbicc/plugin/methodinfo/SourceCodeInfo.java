package org.qbicc.plugin.methodinfo;

final class SourceCodeInfo {
    private int minfoIndex;
    private int lineNumber;
    private int bcIndex;
    private int inlinedAtIndex; // index of SourceCodeInfo in qbicc_source_code_info_table which has inlined this entry

    SourceCodeInfo(int minfoIndex, int lineNumber, int bcIndex, int inlinedAtIndex) {
        this.minfoIndex = minfoIndex;
        this.lineNumber = lineNumber;
        this.bcIndex = bcIndex;
        this.inlinedAtIndex = inlinedAtIndex;
    }

    int getMethodInfoIndex() {
        return minfoIndex;
    }

    int getLineNumber() {
        return lineNumber;
    }

    int getBcIndex() {
        return bcIndex;
    }

    int getInlinedAtIndex() {
        return inlinedAtIndex;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        SourceCodeInfo that = (SourceCodeInfo) other;
        return minfoIndex == that.minfoIndex
            && lineNumber == that.lineNumber
            && bcIndex == that.bcIndex
            && inlinedAtIndex == that.inlinedAtIndex;
    }

    @Override
    public int hashCode() {
        int result = minfoIndex;
        result = 31 * result + lineNumber;
        result = 31 * result + bcIndex;
        result = 31 * result + inlinedAtIndex;
        return result;
    }
}
