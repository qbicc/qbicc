package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

class InvocationImpl extends MemoryStateImpl implements Invocation {
    MethodDescriptor<?> invocationTarget;
    Value[] arguments = Value.NO_VALUES;

    public MethodDescriptor<?> getInvocationTarget() {
        return invocationTarget;
    }

    public void setInvocationTarget(final MethodDescriptor<?> descriptor) {
        this.invocationTarget = descriptor;
        setArgumentCount(descriptor.getParamTypes().size());
    }

    public int getArgumentCount() {
        return arguments.length;
    }

    public void setArgumentCount(final int newCount) {
        arguments = Arrays.copyOf(arguments, newCount);
    }

    public Value getArgument(final int index) {
        return arguments[index];
    }

    public void setArgument(final int index, final Value argument) {
        arguments[index] = argument;
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        for (int i = 0; i < arguments.length; i++) {
            addEdgeTo(visited, graph, arguments[i], "argument[" + i + "]", "black", "solid", knownBlocks);
        }
    }

    public String getLabelForGraph() {
        return "invoke";
    }
}
