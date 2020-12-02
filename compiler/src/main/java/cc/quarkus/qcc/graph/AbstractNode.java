package cc.quarkus.qcc.graph;

abstract class AbstractNode implements Node {
    private final int line;
    private final int bci;
    private int hashCode;

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

    abstract int calcHashCode();

    public abstract boolean equals(Object other);

    public final int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = calcHashCode();
            if (hashCode == 0) {
                hashCode = 1 << 31;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }
}
