package cc.quarkus.qcc.graph.node;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.type.InvokeType;
import cc.quarkus.qcc.type.MethodDescriptor;

public class InvokeNode extends ControlNode<InvokeType> {

    public InvokeNode(ControlNode<?> control) {
        super(control, new InvokeType());
    }

    public void setMethodDescriptor(MethodDescriptor descriptor) {
        getType().setMethodDescriptor(descriptor);
    }

    public NormalControlProjection getNormalControlOut() {
        return this.normalControlOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new NormalControlProjection(this)));
    }

    public ResultProjection<?> getResultOut() {
        return this.resultOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ResultProjection<>(this, getType().getReturnType())));
    }

    public IOProjection getIOOut() {
        return this.ioOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IOProjection(this)));
    }

    public MemoryProjection getMemoryOut() {
        return this.memoryOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new MemoryProjection(this)));
    }

    @Override
    public String label() {
        return "<invoke> " + this.getType().label();
    }

    private AtomicReference<NormalControlProjection> normalControlOut = new AtomicReference<>();
    private AtomicReference<ResultProjection<?>> resultOut = new AtomicReference<>();
    private AtomicReference<IOProjection> ioOut = new AtomicReference<>();
    private AtomicReference<MemoryProjection> memoryOut = new AtomicReference<>();
    private AtomicReference<ThrowProjection> throwOut = new AtomicReference<>();


}
