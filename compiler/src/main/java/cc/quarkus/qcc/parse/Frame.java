package cc.quarkus.qcc.parse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.type.IOType;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.MemoryType;
import cc.quarkus.qcc.graph.type.Type;

import static cc.quarkus.qcc.parse.TypeUtil.checkType;

public class Frame {

    public Frame(ControlNode<?> control, int maxLocals, int maxStack) {
        this.control = control;
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
        this.locals = new Local.SimpleLocal[maxLocals];
        for (int i = 0; i < locals.length; ++i) {
            //this.locals[i] = new Local.SimpleLocal<>(control, i);
            this.locals[i] = null;
        }
        this.stack = new ArrayDeque<>(maxStack);
        //this.io = new Local.SimpleLocal<>(control, BytecodeParser.SLOT_IO);
        //this.memory = new Local.SimpleLocal<>(control, BytecodeParser.SLOT_MEMORY);
        this.id = control.getId();
    }

    public int maxLocals() {
        return this.maxLocals;
    }

    public int maxStack() {
        return this.maxStack;
    }

    public Local<?> local(int index) {
        return this.locals[index];
    }

    public <T extends Node<? extends ConcreteType<?>>> T push(T node) {
        System.err.println(this.id + " push " + node);
        this.stack.push(node);
        return node;
    }

    public <T extends ConcreteType<?>> Node<T> pop(T type) {
        Node<?> val = this.stack.pop();
        System.err.println(this.id + " pop " + type + " > " + val);
        return checkType(val, type);
    }

    public <T extends ConcreteType<?>> Node<T> load(int index, T type) {
        System.err.println(this.id + " load " + type + " @ " + index);
        return this.locals[index].load(type);
    }

    public <T extends Type> Node<T> get(int index, T type) {
        System.err.println(this.id + " get " + type + " @ " + index);
        if (index == BytecodeParser.SLOT_IO) {
            return (Node<T>) this.io.get(IOType.INSTANCE);
        }
        if (index == BytecodeParser.SLOT_MEMORY) {
            return (Node<T>) this.memory.get(MemoryType.INSTANCE);
        }
        return this.locals[index].get(type);
    }

    public <T extends ConcreteType<?>> void store(int index, Node<T> val) {
        System.err.println(this.id + " store " + val + " @ " + index);
        if (this.locals[index] == null) {
            this.locals[index] = new Local.SimpleLocal<>(this.control, index);
        }
        this.locals[index].store(val);
    }

    public void memory(Node<MemoryType> memory) {
        System.err.println(this.control + " store memory() " + memory);
        if (this.memory == null) {
            this.memory = new Local.SimpleLocal<>(this.control, BytecodeParser.SLOT_MEMORY);
        }
        this.memory.store(memory);
    }

    public Node<MemoryType> memory() {
        return this.memory.load(MemoryType.INSTANCE);
    }

    public void io(Node<IOType> io) {
        System.err.println(this.control + " store io() " + io);
        if (this.io == null) {
            this.io = new Local.SimpleLocal<>(this.control, BytecodeParser.SLOT_IO);
        }
        this.io.store(io);
    }

    public Node<IOType> io() {
        System.err.println(control + " get io() " + this.io);
        return this.io.load(IOType.INSTANCE);
    }

    //public void merge(Frame frame) {
    //System.err.println("merge " + frame.id + " into " + this.id);
    //for (int i = 0; i < this.locals.length; ++i) {
    ////System.err.println( "inbound: " +i + " > " + frame.local(i));
    //this.locals[i].merge(frame.local(i));
    //}

    //this.memory.merge(frame.memory);
    //this.io.merge(frame.io);
    //}

    public void mergeFrom(Frame inbound) {
        System.err.println( "merge frame=" + this.id + " from " + inbound.id);
        for (int i = 0; i < this.locals.length; ++i) {
            if (this.locals[i] == null) {
                System.err.println( " actual merge: " + i + " = " + inbound.locals[i]);
                if ( inbound.locals[i] != null ) {
                    this.locals[i] = inbound.locals[i].duplicate();
                }
            }
        }
        this.stack.clear();
        this.stack.addAll(inbound.stack);
        //if ( ! ( this.io instanceof Local.PhiLocal<?>)) {
        if (this.io == null) {
            System.err.println("merge io " + this.control + " from " + inbound + " // " + inbound.io);
            this.io = inbound.io.duplicate();
        } else {
            System.err.println("NOT merge io " + this.control + " from " + inbound + " // " + this.io);
        }
        //if ( ! ( this.memory instanceof Local.PhiLocal<?>)) {
        if (this.memory == null) {
            System.err.println("merge memory " + this.control + " from " + inbound + " // " + inbound.memory);
            this.memory = inbound.memory.duplicate();
        } else {
            System.err.println("NOT merge memory " + this.control + " from " + inbound + " // " + this.memory);
        }
    }

    public void returnValue(Node<? extends ConcreteType<?>> val) {
        this.returnValue.add(val);

    }

    public Local.PhiLocal<?> ensurePhi(int index, ControlNode<?> input, Type type) {
        if (index == BytecodeParser.SLOT_IO) {
            if (!(this.io instanceof Local.PhiLocal<?>)) {
                this.io = new Local.PhiLocal(this.control, index, type);
            }
            ((Local.PhiLocal<IOType>) this.io).addInput(input, type);
            return (Local.PhiLocal<?>) this.io;
        } else if (index == BytecodeParser.SLOT_MEMORY) {
            if (!(this.memory instanceof Local.PhiLocal<?>)) {
                this.memory = new Local.PhiLocal(this.control, index, type);
            }
            ((Local.PhiLocal<MemoryType>) this.memory).addInput(input, type);
            return (Local.PhiLocal<?>) this.memory;
        } else {
            if (!(this.locals[index] instanceof Local.PhiLocal)) {
                this.locals[index] = new Local.PhiLocal(this.control, index, type);
            }
            ((Local.PhiLocal<?>) this.locals[index]).addInput(input, type);
            return (Local.PhiLocal<?>) this.locals[index];
        }
    }

    public String toString() {
        return "frame-" + this.id;
    }


    private final int maxLocals;

    private final int maxStack;

    private final int id;

    private final ControlNode<?> control;

    private Local<?>[] locals;

    private Local<MemoryType> memory;

    private Local<IOType> io;

    private Deque<Node<?>> stack;

    private List<Node<? extends ConcreteType<?>>> returnValue = new ArrayList<>();
}
