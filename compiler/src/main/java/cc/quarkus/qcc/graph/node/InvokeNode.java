package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.IOProvider;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.graph.type.MemoryProvider;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.CallResult;
import cc.quarkus.qcc.type.MethodDefinition;
import cc.quarkus.qcc.type.MethodDescriptor;
import cc.quarkus.qcc.type.TypeDescriptor;

public class InvokeNode<V> extends AbstractControlNode<InvokeToken> implements IOProvider, MemoryProvider {

    public InvokeNode(Graph<?> graph, ControlNode<?> control, MethodDescriptor<V> methodDescriptor) {
        super(graph, control, TypeDescriptor.EphemeralTypeDescriptor.INVOKE_TOKEN);
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public InvokeNode<V> setLine(int line) {
        super.setLine(line);
        return this;
    }

    public List<Class<?>> getParamTypes() {
        return this.methodDescriptor.getParamTypes().stream().map(TypeDescriptor::valueType).collect(Collectors.toList());
    }

    public void addArgument(Node<?> node) {
        this.arguments.add(node);
        node.addSuccessor(this);
    }

    public void setIO(Node<IOToken> io) {
        this.io = io;
        io.addSuccessor(this);
    }

    public void setMemory(Node<MemoryToken> memory) {
        this.memory = memory;
        memory.addSuccessor(this);
    }

    public NormalControlProjection getNormalControlOut() {
        return this.normalControlOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new NormalControlProjection(getGraph(), this)));
    }

    public ThrowControlProjection getThrowControlOut() {
        return this.throwOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ThrowControlProjection(getGraph(), this)));
    }

    public ResultProjection<V> getResultOut() {
        return this.resultOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ResultProjection<V>(getGraph(), this, this.methodDescriptor.getReturnType())));
    }

    public ExceptionProjection getExceptionOut() {
        return this.exceptionOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ExceptionProjection(getGraph(), this)));
    }

    @Override
    public IOProjection getIO() {
        return this.ioOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IOProjection(getGraph(), this)));
    }

    @Override
    public MemoryProjection getMemory() {
        return this.memoryOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new MemoryProjection(getGraph(), this)));
    }

    @Override
    public InvokeToken getValue(Context context) {
        MethodDefinition<V> m = this.methodDescriptor.getOwner().findMethod(this.methodDescriptor);
        CallResult<V> result = m.call(context.heap(), this.arguments.stream().map(e -> e.getValue(context)).collect(Collectors.toList()));
        return new InvokeToken(result.getReturnValue(), result.getThrowValue());
    }

    @Override
    public List<Node<?>> getPredecessors() {
        List<Node<?>> list = new ArrayList<>();
        list.add(getControl());
        list.addAll(this.arguments);
        if (this.io != null) {
            list.add(this.io);
        }
        if (this.memory != null) {
            list.add(this.memory);
        }
        return list;
    }

    /*
    @Override
    public void mergeInputs() {
        getThrowControlOut().frame().io(getIOOut());
        getThrowControlOut().frame().memory(getMemoryOut());

        getNormalControlOut().frame().io(getIOOut());
        getNormalControlOut().frame().memory(getMemoryOut());
        super.mergeInputs();
    }
     */

    @Override
    public String label() {
        return "<invoke:" + getId() + " (" + getLine() + ")> "
                + (this.methodDescriptor == null ? "" :
                (this.methodDescriptor.getOwner().getName() + "#" + this.methodDescriptor.getName()));
    }

    @Override
    public String toString() {
        return label();
    }

    private AtomicReference<NormalControlProjection> normalControlOut = new AtomicReference<>();

    private AtomicReference<ResultProjection<V>> resultOut = new AtomicReference<>();

    private AtomicReference<ExceptionProjection> exceptionOut = new AtomicReference<>();

    private AtomicReference<IOProjection> ioOut = new AtomicReference<>();

    private AtomicReference<MemoryProjection> memoryOut = new AtomicReference<>();

    private AtomicReference<ThrowControlProjection> throwOut = new AtomicReference<>();

    private final MethodDescriptor<V> methodDescriptor;

    //private final TypeDefinition owner;

    //private final String name;

    //private final List<Class<?>> paramTypes;

    //private final TypeDescriptor<V> returnType;

    private final List<Node<?>> arguments = new ArrayList<>();

    private Node<IOToken> io;

    private Node<MemoryToken> memory;

}

