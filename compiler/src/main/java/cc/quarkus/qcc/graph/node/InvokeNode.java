package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.InvokeType;
import cc.quarkus.qcc.graph.type.InvokeValue;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.MethodDescriptor;

public class InvokeNode extends AbstractControlNode<InvokeType, InvokeValue> {

    public InvokeNode(ControlNode<?,?> control) {
        super(control, new InvokeType());
    }

    public void addArgument(Node<?,?> node) {
        this.arguments.add(node);
        node.addSuccessor(this);
    }

    public void setMethodDescriptor(MethodDescriptor descriptor) {
        getType().setMethodDescriptor(descriptor);
    }

    public NormalControlProjection getNormalControlOut() {
        return this.normalControlOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new NormalControlProjection(this)));
    }

    public ResultProjection<? extends ConcreteType<?>,?> getResultOut() {
        return this.resultOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ResultProjection<>(this, getType().getReturnType())));
    }

    public IOProjection getIOOut() {
        return this.ioOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IOProjection(this)));
    }

    public MemoryProjection getMemoryOut() {
        return this.memoryOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new MemoryProjection(this)));
    }

    @Override
    public InvokeValue getValue(Context context) {
        return null;
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        List<Node<?,?>> list = new ArrayList<>();
        list.add( getControl() );
        list.addAll( this.arguments );
        return list;
    }

    @Override
    public String label() {
        return "<invoke> " + this.getType().label();
    }

    private AtomicReference<NormalControlProjection> normalControlOut = new AtomicReference<>();
    private AtomicReference<ResultProjection<? extends ConcreteType<?>>> resultOut = new AtomicReference<>();
    private AtomicReference<IOProjection> ioOut = new AtomicReference<>();
    private AtomicReference<MemoryProjection> memoryOut = new AtomicReference<>();
    private AtomicReference<ThrowControlProject> throwOut = new AtomicReference<>();

    private final List<Node<?,?>> arguments = new ArrayList<>();


}
