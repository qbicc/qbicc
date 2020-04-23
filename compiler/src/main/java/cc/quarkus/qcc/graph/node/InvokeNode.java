package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class InvokeNode<V> extends AbstractControlNode<InvokeToken> {

    public InvokeNode(ControlNode<?> control, TypeDescriptor<V> returnType, List<Class<?>> paramTypes) {
        super(control, TypeDescriptor.EphemeralTypeDescriptor.INVOKE_TOKEN);
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    public List<Class<?>> getParamTypes() {
        return this.paramTypes;
    }

    public void addArgument(Node<?> node) {
        this.arguments.add(node);
        node.addSuccessor(this);
    }

    //public void setMethodDescriptor(MethodDescriptor descriptor) {
        //this.methodDescriptor = descriptor;
    //}

    public NormalControlProjection getNormalControlOut() {
        return this.normalControlOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new NormalControlProjection(this)));
    }

    public ResultProjection<V> getResultOut() {
        return this.resultOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ResultProjection<V>(this, this.returnType)));
    }

    public IOProjection getIOOut() {
        return this.ioOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IOProjection(this)));
    }

    public MemoryProjection getMemoryOut() {
        return this.memoryOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new MemoryProjection(this)));
    }

    @Override
    public InvokeToken getValue(Context context) {
        return null;
    }

    @Override
    public List<Node<?>> getPredecessors() {
        List<Node<?>> list = new ArrayList<>();
        list.add(getControl());
        list.addAll(this.arguments);
        return list;
    }

    @Override
    public String label() {
        return "<invoke> ";
    }

    private AtomicReference<NormalControlProjection> normalControlOut = new AtomicReference<>();

    private AtomicReference<ResultProjection<V>> resultOut = new AtomicReference<>();

    private AtomicReference<IOProjection> ioOut = new AtomicReference<>();

    private AtomicReference<MemoryProjection> memoryOut = new AtomicReference<>();

    private AtomicReference<ThrowControlProjection> throwOut = new AtomicReference<>();

    private final List<Class<?>> paramTypes;

    private final List<Node<?>> arguments = new ArrayList<>();

    //private MethodDescriptor methodDescriptor;

    private final TypeDescriptor<V> returnType;

}
