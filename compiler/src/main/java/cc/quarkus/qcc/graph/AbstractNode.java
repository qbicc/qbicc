package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

abstract class AbstractNode implements Node {
    private final Node callSite;
    private final ExecutableElement element;
    private final int line;
    private final int bci;
    private int hashCode;

    AbstractNode(final Node callSite, final ExecutableElement element, final int line, final int bci) {
        this.callSite = callSite;
        this.element = element;
        this.line = line;
        this.bci = bci;
    }

    public Node getCallSite() {
        return callSite;
    }

    public ExecutableElement getElement() {
        return element;
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
