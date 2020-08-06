package cc.quarkus.qcc.constraint;

import java.io.IOException;
import java.util.Set;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.GraphVisitor;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.graph.Value;

public class ValueImpl implements Value {

    public ValueImpl(String label) {
        this.label = label;
    }


    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    @Override
    public Constraint getConstraint() {
        return this.constraint;
    }

    @Override
    public String toString() {
        return "ValueImpl{" +
                "label=" + label +
                //", constraint=" + constraint +
                '}';
    }

    private final String label;

    private Constraint constraint;

    public Type getType() {
        return null;
    }

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

    public int getSourceLine() {
        return 0;
    }

    public void setSourceLine(final int sourceLine) {

    }

    public <P> void accept(final GraphVisitor<P> visitor, final P param) {

    }
}
