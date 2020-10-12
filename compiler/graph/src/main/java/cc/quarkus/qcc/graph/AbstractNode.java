package cc.quarkus.qcc.graph;

abstract class AbstractNode implements Node {
    private int sourceLine;
    private int bytecodeIndex;

    public int getSourceLine() {
        return sourceLine;
    }

    public void setSourceLine(final int sourceLine) {
        this.sourceLine = sourceLine;
    }

    public int getBytecodeIndex() {
        return bytecodeIndex;
    }

    public void setBytecodeIndex(final int bytecodeIndex) {
        this.bytecodeIndex = bytecodeIndex;
    }
}
