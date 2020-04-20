package cc.quarkus.qcc.parse;

import java.util.ArrayDeque;
import java.util.Deque;

import cc.quarkus.qcc.graph.node.AbstractControlNode;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiOwner;
import cc.quarkus.qcc.graph.node.VariableProjection;
import cc.quarkus.qcc.graph.type.AnyType;
import cc.quarkus.qcc.graph.type.IOType;
import cc.quarkus.qcc.graph.node.AbstractNode;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.IOValue;
import cc.quarkus.qcc.graph.type.MemoryType;
import cc.quarkus.qcc.graph.type.MemoryValue;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;

import static cc.quarkus.qcc.parse.TypeUtil.checkType;

public class Frame {

    public Frame(ControlNode<?,?> control, int maxLocals, int maxStack) {
        this.control = control;
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
        this.locals = new Local.SimpleLocal[maxLocals];
        for (int i = 0; i < locals.length; ++i) {
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

    public Local local(int index) {
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

    public <T extends Node<? extends ConcreteType<?>,?>> T push(T node) {
        this.stack.push(node);
        return node;
    }

    @SuppressWarnings("unchecked")
    public <T extends ConcreteType<T>> Node<T,?> pop(T type) {
        Node<?,?> val = this.stack.pop();
        return (Node<T, ?>) checkType(val, type);
    }

    @SuppressWarnings("unchecked")
    public <T extends ConcreteType<T>> Node<T,?> peek(T type) {
        Node<?,?> val = this.stack.peek();
        return (Node<T, ?>) checkType(val, type);
    }

    public void clear() {
        this.stack.clear();
    }

    @SuppressWarnings("unchecked")
    public <T extends Type<T>, V extends Value<T,V>> Node<T,V> load(int index, T type) {
        return (Node<T, V>) checkType(this.locals[index].load(type), type);
    }

    public <T extends Type<T>, V extends Value<T,V>> Node<T,V> get(int index, T type) {
        if ( index == BytecodeParser.SLOT_RETURN ) {
            return (Node<T, V>) checkType(this.returnValue.get(type), type);
        }
        if (index == BytecodeParser.SLOT_IO) {
            return (Node<T, V>) checkType(this.io.get(IOType.INSTANCE), IOType.INSTANCE);
        }
        if (index == BytecodeParser.SLOT_MEMORY) {
            return (Node<T,V>) checkType(this.memory.get(MemoryType.INSTANCE), MemoryType.INSTANCE);
        }
        return (Node<T, V>) checkType(this.locals[index].get(type), type);
    }

    public <T extends ConcreteType<T>, V extends Value<T,V>> void store(int index, Node<T,V> val) {
        if (this.locals[index] == null) {
            this.locals[index] = new Local.SimpleLocal(this.control, index);
        }
        this.locals[index].store(val);
    }

    public void returnValue(Node<?,?> returnValue) {
        if ( this.returnValue == null ) {
            this.returnValue = new Local.SimpleLocal(this.control, BytecodeParser.SLOT_RETURN);
        }
        this.returnValue.store(returnValue);
    }

    public Node<?,?> returnValue() {
        return this.returnValue.load(AnyType.INSTANCE);
    }

    public void memory(Node<MemoryType, MemoryValue> memory) {
        if (this.memory == null) {
            this.memory = new Local.SimpleLocal(this.control, BytecodeParser.SLOT_MEMORY);
        }
        this.memory.store(memory);
    }

    public Node<MemoryType,MemoryValue> memory() {
        return (Node<MemoryType, MemoryValue>) this.memory.load(MemoryType.INSTANCE);
    }

    public void io(Node<IOType, IOValue> io) {
        if (this.io == null) {
            this.io = new Local.SimpleLocal(this.control, BytecodeParser.SLOT_IO);
        }
        this.io.store(io);
    }

    public Node<IOType, IOValue> io() {
        return (Node<IOType, IOValue>) this.io.load(IOType.INSTANCE);
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

    public Local.PhiLocal ensurePhi(int index, ControlNode<?,?> input, Type type) {
        if ( !( this.control instanceof PhiOwner ) ) {
            throw new UnsupportedOperationException( this.control + " cannot own Phi nodes");
        }
        if ( index == BytecodeParser.SLOT_RETURN ) {
            if ( !(this.returnValue instanceof Local.PhiLocal)) {
                this.returnValue = new Local.PhiLocal((ControlNode<?,?> & PhiOwner) this.control, index, type);
            }
            ((Local.PhiLocal) this.returnValue).addInput(input, type);
            return (Local.PhiLocal) this.returnValue;
        } else if (index == BytecodeParser.SLOT_IO) {
            if (!(this.io instanceof Local.PhiLocal)) {
                this.io = new Local.PhiLocal((ControlNode<?,?> & PhiOwner) this.control, index, type);
            }
            ((Local.PhiLocal) this.io).addInput(input, type);
            return (Local.PhiLocal) this.io;
        } else if (index == BytecodeParser.SLOT_MEMORY) {
            if (!(this.memory instanceof Local.PhiLocal)) {
                this.memory = new Local.PhiLocal((ControlNode<?,?> & PhiOwner) this.control, index, type);
            }
            ((Local.PhiLocal) this.memory).addInput(input, type);
            return (Local.PhiLocal) this.memory;
        } else {
            if (!(this.locals[index] instanceof Local.PhiLocal)) {
                this.locals[index] = new Local.PhiLocal((ControlNode<?,?> & PhiOwner) this.control, index, type);
            }
            ((Local.PhiLocal) this.locals[index]).addInput(input, type);
            return (Local.PhiLocal) this.locals[index];
        }
    }

    public String toString() {
        return "frame-" + this.id;
    }

    private final int maxLocals;

    private final int maxStack;

    private final int id;

    private final ControlNode<?,?> control;

    private Local[] locals;

    private Local memory;

    private Local io;

    private Deque<Node<?,?>> stack;

    private Local returnValue;
}
