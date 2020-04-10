package cc.quarkus.qcc.graph.node;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.type.IOType;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.ControlType;
import cc.quarkus.qcc.graph.type.FunctionType;
import cc.quarkus.qcc.graph.type.MemoryType;
import cc.quarkus.qcc.graph.type.ThrowType;

public class CallNode<T extends Type> extends ControlNode<FunctionType<T>> {

    public static <T extends Type> CallNode make(ControlNode<?> control,
                                                 Node<IOType> io,
                                                 Node<MemoryType> memory,
                                                 T returnType,
                                                 Node<?>... parameters) {
        return new CallNode<>(
                control,
                io,
                memory,
                new FunctionType<>(returnType),
                parameters
        );
    }

    public CallNode(ControlNode<?> control,
                    Node<IOType> io,
                    Node<MemoryType> memory,
                    FunctionType<T> outType,
                    Node<?>... parameters) {
        super(control, outType);
        addPredecessor(io);
        addPredecessor(memory);
        for (Node<?> parameter : parameters) {
            addPredecessor(parameter);
        }
    }

    @SuppressWarnings("unchecked")
    public <J extends Type> Node<J> getOut(J type) {
        if (type == getType().getReturnType()) {
            return (Node<J>) this;
        }
        if (type instanceof ControlType) {
            return (Node<J>) this;
        }
        if (type instanceof IOType) {
            return (Node<J>) getIOOut();
        }
        if (type instanceof MemoryType) {
            return (Node<J>) getMemoryOut();
        }
        if (type instanceof ThrowType) {
            return (Node<J>) this.throwOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ThrowProjection(this)));
        }
        return null;
    }

    public IOProjection getIOOut() {
        return  this.ioOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IOProjection(this)));
    }

    public MemoryProjection getMemoryOut() {
        return this.memoryOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new MemoryProjection(this)));
    }

    private AtomicReference<IOProjection> ioOut = new AtomicReference<>();
    private AtomicReference<MemoryProjection> memoryOut = new AtomicReference<>();
    private AtomicReference<ThrowProjection> throwOut = new AtomicReference<>();


}
