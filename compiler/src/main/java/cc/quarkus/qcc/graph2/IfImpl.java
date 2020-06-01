package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
final class IfImpl extends TerminatorImpl implements If {
    NodeHandle condition;
    NodeHandle trueBranch;
    NodeHandle falseBranch;

    public Value getCondition() {
        return NodeHandle.getTargetOf(condition);
    }

    public void setCondition(final Value cond) {
        condition = NodeHandle.of(cond);
    }

    public BasicBlock getTrueBranch() {
        return NodeHandle.getTargetOf(trueBranch);
    }

    public void setTrueBranch(final BasicBlock branch) {
        trueBranch = NodeHandle.of(branch);
    }

    void setTrueBranch(final NodeHandle branch) {
        trueBranch = branch;
    }

    public BasicBlock getFalseBranch() {
        return NodeHandle.getTargetOf(falseBranch);
    }

    public void setFalseBranch(final BasicBlock branch) {
        falseBranch = NodeHandle.of(branch);
    }

    void setFalseBranch(final NodeHandle branch) {
        falseBranch = branch;
    }

    public String getLabelForGraph() {
        return "if";
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(condition), "cond", "blue", "solid", knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(trueBranch), "true", "green", "solid", knownBlocks);
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(falseBranch), "false", "red", "solid", knownBlocks);
    }
}
