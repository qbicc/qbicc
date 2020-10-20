package cc.quarkus.qcc.graph;

abstract class AbstractNode implements Node {
    private final int line;
    private final int bci;

    AbstractNode(final int line, final int bci) {
        this.line = line;
        this.bci = bci;
    }

    public int getSourceLine() {
        return line;
    }

    public int getBytecodeIndex() {
        return bci;
    }
}
