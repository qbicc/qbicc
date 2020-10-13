package cc.quarkus.qcc.graph;

/**
 *
 */
public interface Node {
    int getSourceLine();

    void setSourceLine(int sourceLine);

    int getBytecodeIndex();

    void setBytecodeIndex(int bytecodeIndex);

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

    default Node getSingleDependency(BasicBlockBuilder graphFactory, Node defaultValue) {
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
