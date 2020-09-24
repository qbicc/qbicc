package cc.quarkus.qcc.type.definition.classfile;

import java.io.IOException;
import java.util.Set;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Type;

/**
 * A special type representing a return address for bytecode {@code jsr}/{@code ret} processing.
 */
public final class ReturnAddressType implements Type {
    static final ReturnAddressType INSTANCE = new ReturnAddressType();

    private ReturnAddressType() {}

    public boolean isAssignableFrom(final Type otherType) {
        return otherType == this;
    }

    public int getParameterCount() {
        return 0;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public void replaceWith(final Node node) {
        throw new UnsupportedOperationException();
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        throw new UnsupportedOperationException();
    }

    public String getLabelForGraph() {
        throw new UnsupportedOperationException();
    }

    public int getIdForGraph() {
        throw new UnsupportedOperationException();
    }

    public void setIdForGraph(final int id) {
        throw new UnsupportedOperationException();
    }

    public int getSourceLine() {
        throw new UnsupportedOperationException();
    }

    public void setSourceLine(final int sourceLine) {
        throw new UnsupportedOperationException();
    }

    public int getBytecodeIndex() {
        throw new UnsupportedOperationException();
    }

    public void setBytecodeIndex(final int bytecodeIndex) {
        throw new UnsupportedOperationException();
    }
}
