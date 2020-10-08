package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

import cc.quarkus.qcc.constraint.Constraint;

final class ArrayTypeImpl extends NodeImpl implements ArrayType {
    private final Type elementType;

    ArrayTypeImpl(final Type elementType) {
        this.elementType = elementType;
    }

    public Type getElementType() {
        return elementType;
    }

    public ArrayClassType getArrayClassType() {
        throw new UnsupportedOperationException();
    }

    public int getParameterCount() {
        return 0;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        return null;
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        return null;
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, elementType, "of", "black", "solid", knownBlocks);
    }

    public String getLabelForGraph() {
        return "array";
    }
}
