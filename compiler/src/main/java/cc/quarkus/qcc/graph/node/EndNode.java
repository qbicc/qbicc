package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.CompletionToken;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

@SuppressWarnings("rawtypes")
public class EndNode<T> extends AbstractNode<EndToken> {

    public EndNode(Graph<?> graph, ControlNode<?> control, TypeDescriptor<T> returnType) {
        super(graph, control, TypeDescriptor.EphemeralTypeDescriptor.END_TOKEN);
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return new ArrayList<>() {{
            add(getControl());
            add(io);
            add(memory);
            add(completion);
        }};
    }

    public String label() {
        return "<end>";
    }

    public void setIO(Node<IOToken> io) {
        this.io = io;
        io.addSuccessor(this);
    }

    public Node<IOToken> getIO() {
        return this.io;
    }

    public void setMemory(Node<MemoryToken> memory) {
        this.memory = memory;
        memory.addSuccessor(this);
    }

    public Node<MemoryToken> getMemory() {
        return this.memory;
    }

    public void setCompletion(Node<CompletionToken> completion) {
        this.completion = completion;
        this.completion.addSuccessor(this);
    }

    public Node<CompletionToken> getCompletion() {
        return this.completion;
    }


    @SuppressWarnings("unchecked")
    @Override
    public EndToken<T> getValue(Context context) {
        IOToken io = context.get(this.io);
        MemoryToken memory = context.get(this.memory);
        CompletionToken<T> completion = context.get(this.completion);

        return new EndToken<>(io, memory, completion.returnValue(), completion.throwValue());
    }

    private Node<IOToken> io;
    private Node<MemoryToken> memory;
    private Node<CompletionToken> completion;

}
