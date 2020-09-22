package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

import io.smallrye.common.constraint.Assert;

abstract class NodeImpl implements Node {
    private NodeHandle myHandle;
    private int id = hashCode();
    private int sourceLine;
    private int bytecodeIndex;

    public void replaceWith(final Node other) {
        Assert.checkNotNullParam("other", other);
        // all uses of this node are replaced with other
        NodeHandle myHandle = this.myHandle;
        if (myHandle == null) {
            // no uses
            return;
        }
        myHandle.setTarget(NodeHandle.of(other));
        this.myHandle = null;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public void setSourceLine(final int sourceLine) {
        this.sourceLine = sourceLine;
    }

    public int getBytecodeIndex() {
        return bytecodeIndex;
    }

    public void setBytecodeIndex(final int bytecodeIndex) {
        this.bytecodeIndex = bytecodeIndex;
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        // write just the node
        graph.append(Integer.toString(getIdForGraph()));
        graph.append(' ');
        graph.append('[');
        graph.append("label");
        graph.append('=');
        graph.append('"');
        graph.append(getLabelForGraph());
        graph.append('"');
        graph.append(',');
        graph.append("shape");
        graph.append('=');
        graph.append(getShape());
        graph.append(',');
        graph.append("style");
        graph.append('=');
        graph.append('"');
        graph.append("solid");
        graph.append('"');
        graph.append(']');
        graph.append(System.lineSeparator());
}

    void addEdgeTo(Set<Node> visited, Appendable graph, Node target, String label, String color, String style, Set<BasicBlock> knownBlocks) throws IOException {
        if (target == null) {
            return;
        }
        graph.append(Integer.toString(getIdForGraph()));
        graph.append(" -> ");
        graph.append(Integer.toString(target.getIdForGraph()));
        boolean attrs = false;
        if (color != null) {
            graph.append(' ');
            graph.append('[');
            graph.append("color");
            graph.append('=');
            graph.append(color);
            attrs = true;
        }
        if (style != null) {
            if (attrs) {
                graph.append(',');
            } else {
                graph.append(' ');
                graph.append('[');
                attrs = true;
            }
            graph.append("style");
            graph.append('=');
            graph.append(style);
        }
        if (label != null) {
            if (attrs) {
                graph.append(',');
            } else {
                graph.append(' ');
                graph.append('[');
                attrs = true;
            }
            graph.append("label");
            graph.append('=');
            graph.append('"');
            graph.append(label);
            graph.append('"');
        }
        if (attrs) {
            graph.append(']');
        }
        graph.append(System.lineSeparator());

        if (visited.add(target)) {
            target.writeToGraph(visited, graph, knownBlocks);
        }
    }

    String getShape() {
        return "box";
    }

    public int getIdForGraph() {
        return id;
    }

    public void setIdForGraph(final int id) {
        this.id = id;
    }

    // nodes contain links to handles, not to nodes
    NodeHandle getHandle() {
        NodeHandle myHandle = this.myHandle;
        if (myHandle == null) {
            myHandle = this.myHandle = new NodeHandle();
            myHandle.setTarget(this);
        }
        return myHandle;
    }

    NodeHandle getHandleIfExists() {
        return myHandle;
    }

    void setHandle(NodeHandle newHandle) {
        assert myHandle == null;
        myHandle = newHandle;
    }
}
