package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class EndNode extends AbstractNode<EndToken> {

    public EndNode(ControlNode<?> control, TypeDescriptor returnType) {
        super(control, EndToken.class);
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return new ArrayList<>() {{
            add(getControl());
            add(io);
            add(memory);
            add(returnValue);
        }};
    }

    public String label() {
        return "<end>";
    }

    public void setIO(Node<IOToken> io) {
        this.io = io;
        io.addSuccessor(this);
    }

    public void setMemory(Node<MemoryToken> memory) {
        this.memory = memory;
        memory.addSuccessor(this);
    }

    public void setReturnValue(Node<?> returnValue) {
        this.returnValue = returnValue;
        returnValue.addSuccessor(this);
    }

    @Override
    public EndToken getValue(Context context) {
        IOToken io = context.get(this.io);
        MemoryToken memory = context.get(this.memory);
        Object returnValue = context.get(this.returnValue);

        return new EndToken(io, memory, returnValue);
    }

    private Node<IOToken> io;
    private Node<MemoryToken> memory;
    private Node<?> returnValue;

}
