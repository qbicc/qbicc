package org.qbicc.graph;

import org.qbicc.type.definition.element.ExecutableElement;

abstract class AbstractNode implements Node {
    private final Node callSite;
    private final ExecutableElement element;
    private final int line;
    private final int bci;
    private int hashCode;
    private int scheduleIndex = -1;

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

    public int getScheduleIndex() {
        return scheduleIndex;
    }

    public void setScheduleIndex(int index) {
        this.scheduleIndex = index;
    }

    abstract int calcHashCode();

    abstract String getNodeName();

    public abstract boolean equals(Object other);

    public final String toString() {
        return toString(new StringBuilder()).toString();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append(getNodeName());
        if (hasPointerValueDependency()) {
            b.append('[');
            getPointerValue().toString(b);
            b.append(']');
        }
        return b;
    }

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
