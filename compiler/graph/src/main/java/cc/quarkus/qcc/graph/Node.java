package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
public interface Node {
    void replaceWith(Node node);

    void writeToGraph(Set<Node> visited, Appendable graph, Set<BasicBlock> knownBlocks) throws IOException;

    String getLabelForGraph();

    int getIdForGraph();

    void setIdForGraph(int id);

    int getSourceLine();

    void setSourceLine(int sourceLine);

    int getBytecodeIndex();

    void setBytecodeIndex(int bytecodeIndex);

    <P> void accept(GraphVisitor<P> visitor, P param);

    default int getValueDependencyCount() {
        return 0;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    default int getBasicDependencyCount() {
        return 0;
    }

    default Node getBasicDependency(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    default Node getSingleDependency(GraphFactory graphFactory, Node defaultValue) {
        int cnt = getValueDependencyCount();
        if (cnt == 0) {
            return defaultValue;
        } else if (cnt == 1) {
            return getBasicDependency(0);
        } else {
            throw new IllegalStateException();
        }
    }
}
