package cc.quarkus.qcc.parse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.type.AnyType;
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
        this.id = control.getId();
    }

    public int maxLocals() {
        return this.maxLocals;
    }

    public int maxStack() {
        return this.maxStack;
    }

    public Local<?> local(int index) {
        if ( index == BytecodeParser.SLOT_RETURN ) {
            return this.returnValue;
        }
        if ( index == BytecodeParser.SLOT_IO ) {
            return this.io;
        }
        if ( index == BytecodeParser.SLOT_MEMORY ) {
            return this.memory;
        }
        return this.locals[index];
    }

    public <T extends Node<? extends ConcreteType<?>>> T push(T node) {
        this.stack.push(node);
        return node;
    }

    public <T extends ConcreteType<?>> Node<T> pop(T type) {
        Node<?> val = this.stack.pop();
        return checkType(val, type);
    }

    public <T extends ConcreteType<?>> Node<T> peek(T type) {
        Node<?> val = this.stack.peek();
        return checkType(val, type);
    }

    public void clear() {
        this.stack.clear();
    }

    public <T extends ConcreteType<?>> Node<T> load(int index, T type) {
        return this.locals[index].load(type);
    }

    public <T extends Type<?>> Node<T> get(int index, T type) {
        if ( index == BytecodeParser.SLOT_RETURN ) {
            return this.returnValue.get(type);
        }
        if (index == BytecodeParser.SLOT_IO) {
            return (Node<T>) this.io.get(IOType.INSTANCE);
        }
        if (index == BytecodeParser.SLOT_MEMORY) {
            return (Node<T>) this.memory.get(MemoryType.INSTANCE);
        }
        return this.locals[index].get(type);
    }

    public <T extends ConcreteType<?>> void store(int index, Node<T> val) {
        if (this.locals[index] == null) {
            this.locals[index] = new Local.SimpleLocal<>(this.control, index);
        }
        this.locals[index].store(val);
    }

    public void returnValue(Node<?> returnValue) {
        if ( this.returnValue == null ) {
            this.returnValue = new Local.SimpleLocal<>(this.control, BytecodeParser.SLOT_RETURN);
        }
        this.returnValue.store(returnValue);
    }

    public Node<?> returnValue() {
        return this.returnValue.load(AnyType.INSTANCE);
    }

    public void memory(Node<MemoryType> memory) {
        if (this.memory == null) {
            this.memory = new Local.SimpleLocal<>(this.control, BytecodeParser.SLOT_MEMORY);
        }
        this.memory.store(memory);
    }

    public Node<MemoryType> memory() {
        return this.memory.load(MemoryType.INSTANCE);
    }

    public void io(Node<IOType> io) {
        if (this.io == null) {
            this.io = new Local.SimpleLocal<>(this.control, BytecodeParser.SLOT_IO);
        }
        this.io.store(io);
    }

    public Node<IOType> io() {
        return this.io.load(IOType.INSTANCE);
    }

    public void mergeFrom(Frame inbound) {
        for (int i = 0; i < this.locals.length; ++i) {
            if (this.locals[i] == null) {
                if ( inbound.locals[i] != null ) {
                    this.locals[i] = inbound.locals[i].duplicate();
                }
            }
        }
        this.stack.clear();
        this.stack.addAll(inbound.stack);
        if ( this.returnValue == null && inbound.returnValue != null) {
            this.returnValue = inbound.returnValue.duplicate();
        }
        if (this.io == null) {
            this.io = inbound.io.duplicate();
        }
        if (this.memory == null) {
            this.memory = inbound.memory.duplicate();
        }
    }

    public Local.PhiLocal<?> ensurePhi(int index, ControlNode<?> input, Type type) {
        if ( index == BytecodeParser.SLOT_RETURN ) {
            if ( !(this.returnValue instanceof Local.PhiLocal<?>)) {
                this.returnValue = new Local.PhiLocal(this.control, index, type);
            }
            ((Local.PhiLocal<?>) this.returnValue).addInput(input, type);
            return (Local.PhiLocal<?>) this.returnValue;
        } else if (index == BytecodeParser.SLOT_IO) {
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

    private Local<? extends ConcreteType<?>> returnValue;
}
