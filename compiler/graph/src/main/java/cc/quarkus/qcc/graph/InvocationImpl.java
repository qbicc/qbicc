package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

class InvocationImpl extends DependentNodeImpl implements Invocation {
    ClassType owner;
    MethodIdentifier invocationTarget;
    Value[] arguments = Value.NO_VALUES;

    public ClassType getMethodOwner() {
        return owner;
    }

    public void setMethodOwner(final ClassType owner) {
        this.owner = owner;
    }

    public MethodIdentifier getInvocationTarget() {
        return invocationTarget;
    }

    public void setInvocationTarget(final MethodIdentifier descriptor) {
        this.invocationTarget = descriptor;
        setArgumentCount(descriptor.getParameterCount());
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

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
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
