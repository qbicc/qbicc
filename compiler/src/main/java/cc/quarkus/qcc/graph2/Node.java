package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
public interface Node {
    void replaceWith(Node node);

    Appendable writeToGraph(final Set<Node> visited, Appendable graph) throws IOException;

    String getLabelForGraph();

    int getIdForGraph();

    void setIdForGraph(int id);
}
