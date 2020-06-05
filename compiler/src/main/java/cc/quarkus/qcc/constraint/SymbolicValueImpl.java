package cc.quarkus.qcc.constraint;

import java.io.IOException;
import java.util.Set;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Node;

public class SymbolicValueImpl implements SymbolicValue {

    SymbolicValueImpl(String label) {
        this.label = label;
    }

    @Override
    public Constraint getConstraint() {
        return null;
    }

    public void setConstraint(final Constraint constraint) {

    }

    @Override
    public String toString() {
        return this.label;
    }

    private final String label;

    public void replaceWith(final Node node) {

    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {

    }

    public String getLabelForGraph() {
        return null;
    }

    public int getIdForGraph() {
        return 0;
    }

    public void setIdForGraph(final int id) {

    }
}
