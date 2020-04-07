package cc.quarkus.qcc.parse;

import java.lang.reflect.Member;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.IOType;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.MemoryType;

import static cc.quarkus.qcc.parse.TypeUtil.checkType;

public class Frame {

    public Frame(ControlNode<?> control, int maxLocals, int maxStack) {
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
        this.locals = new MultiData[maxLocals];
        for ( int i = 0 ; i < locals.length ; ++i) {
            this.locals[i] = new MultiData(control);
        }
        this.stack = new ArrayDeque<>(maxStack);
        this.id = COUNTER.incrementAndGet();

        this.io = new MultiData(control);
        this.memory = new MultiData(control);
    }

    public int maxLocals() {
        return this.maxLocals;
    }

    public int maxStack() {
        return this.maxStack;
    }

    public <T extends Node<? extends ConcreteType<?>>> T push(T node) {
        //System.err.println( this.id + " push " + node);
        this.stack.push(node);
        return node;
    }

    public <T extends ConcreteType<?>> Node<T> pop(T type) {
        //System.err.println( this.id + " pop " + type);
        return checkType(this.stack.pop(), type);
    }

    public <T extends ConcreteType<?>> Node<T> load(int index, T type) {
        //System.err.println( this.id + " load " + type + " @ " + index);
        return (Node<T>) this.locals[index].load(type);
    }

    public  <T extends ConcreteType<?>> void store(int index, Node<T> val) {
        //System.err.println( this.id + " store " + val + " @ " + index);
        this.locals[index].store((Node<ConcreteType<?>>) val);
    }

    public void memory(Node<MemoryType> memory) {
        this.memory.store(memory);
    }

    public Node<MemoryType> memory() {
        return this.memory.load(MemoryType.INSTANCE);
    }

    public void io(Node<IOType> io) {
        this.io.store(io);
    }

    public Node<IOType> io() {
        return this.io.load(IOType.INSTANCE);
    }

    public void merge(Frame frame) {
        System.err.println( "merge " + frame.id + " into " + this.id);
        for ( int i = 0 ; i < this.locals.length ; ++i) {
            //System.err.println( "inbound: " +i + " > " + frame.local(i));
            this.locals[i].merge(frame.local(i));
        }

        this.memory.merge(frame.memory);
        this.io.merge(frame.io);
    }

    public void returnValue(Node<? extends ConcreteType<?>> val) {
        this.returnValue.add(val);

    }

    public void possiblySimplify() {
        for (MultiData local : this.locals) {
            local.possiblySimplify();
        }
        this.io.possiblySimplify();
        this.memory.possiblySimplify();
    }

    private MultiData local(int i) {
        return this.locals[i];
    }

    public String toString() {
        return "frame-"+ this.id;
    }

    private final int maxLocals;

    private final int maxStack;

    private final int id;

    private MultiData<ConcreteType<?>>[] locals;

    private MultiData<MemoryType> memory;
    private MultiData<IOType> io;

    private Deque<Node<?>> stack;
    private List<Node<? extends ConcreteType<?>>> returnValue = new ArrayList<>();

    private static final AtomicInteger COUNTER = new AtomicInteger(0);
}
