package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
final class IfInstructionImpl extends InstructionImpl implements IfInstruction {
    NodeHandle condition;
    NodeHandle trueBranch;
    NodeHandle falseBranch;

    public Value getCondition() {
        return condition.getTarget();
    }

    public void setCondition(final Value cond) {
        condition = NodeHandle.of(cond);
    }

    public BasicBlock getTrueBranch() {
        return trueBranch.getTarget();
    }

    public void setTrueBranch(final BasicBlock branch) {
        trueBranch = NodeHandle.of(branch);
    }

    void setTrueBranch(final NodeHandle branch) {
        trueBranch = branch;
    }

    public BasicBlock getFalseBranch() {
        return falseBranch.getTarget();
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

    public Appendable writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        addEdgeTo(visited, graph, condition.getTarget(), "cond", "blue", "solid", knownBlocks);
        addEdgeTo(visited, graph, trueBranch.getTarget(), "true", "green", "solid", knownBlocks);
        addEdgeTo(visited, graph, falseBranch.getTarget(), "false", "red", "solid", knownBlocks);
        return graph;
    }
}
