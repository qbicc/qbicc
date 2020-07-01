package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
public interface Node {
    void replaceWith(Node node);

    void writeToGraph(final Set<Node> visited, Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException;

    String getLabelForGraph();

    int getIdForGraph();

    void setIdForGraph(int id);

    default int getValueDependencyCount() {
        return 0;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

}
