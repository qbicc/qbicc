package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.TypeDescriptor;

public class InvokeNode<V> extends AbstractControlNode<InvokeToken> {

    public InvokeNode(ControlNode<?> control, TypeDefinition owner, String name, TypeDescriptor<V> returnType, List<Class<?>> paramTypes) {
        super(control, TypeDescriptor.EphemeralTypeDescriptor.INVOKE_TOKEN);
        this.owner = owner;
        this.name = name;
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

    public void setIO(Node<IOToken> io) {
        this.io = io;
        io.addSuccessor(this);
    }

    public void setMemory(Node<MemoryToken> memory) {
        this.memory = memory;
        memory.addSuccessor(this);
    }

    public NormalControlProjection getNormalControlOut() {
        return this.normalControlOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new NormalControlProjection(this)));
    }

    public ThrowControlProjection getThrowControlOut() {
        return this.throwOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ThrowControlProjection(this)));
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
        if (this.io != null) {
            list.add(this.io);
        }
        if (this.memory != null) {
            list.add(this.memory);
        }
        return list;
    }

    @Override
    public void mergeInputs() {
        //frame().mergeInputs();
        //for (ControlNode<?> each : getControlSuccessors()) {
            //if (each instanceof Projection) {
                //System.err.println( "** merge from " + this + " to " + each);
                //each.mergeInputs();
            //}
        //}
        getThrowControlOut().frame().io(getIOOut());
        getThrowControlOut().frame().memory(getMemoryOut());

        getNormalControlOut().frame().io(getIOOut());
        getNormalControlOut().frame().memory(getMemoryOut());
        super.mergeInputs();
    }

    @Override
    public String label() {
        return getId() + " <invoke> " + this.owner.getName() + "#" + this.name;
    }

    @Override
    public String toString() {
        return label();
    }

    private AtomicReference<NormalControlProjection> normalControlOut = new AtomicReference<>();

    private AtomicReference<ResultProjection<V>> resultOut = new AtomicReference<>();

    private AtomicReference<IOProjection> ioOut = new AtomicReference<>();

    private AtomicReference<MemoryProjection> memoryOut = new AtomicReference<>();

    private AtomicReference<ThrowControlProjection> throwOut = new AtomicReference<>();

    private final TypeDefinition owner;

    private final String name;

    private final List<Class<?>> paramTypes;

    private final TypeDescriptor<V> returnType;

    private final List<Node<?>> arguments = new ArrayList<>();

    private Node<IOToken> io;

    private Node<MemoryToken> memory;


}
