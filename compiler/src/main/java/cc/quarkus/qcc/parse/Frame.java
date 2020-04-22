package cc.quarkus.qcc.parse;

import java.util.ArrayDeque;
import java.util.Deque;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.MemoryToken;

import static cc.quarkus.qcc.parse.TypeUtil.checkType;

public class Frame {

    public Frame(ControlNode<?> control, int maxLocals, int maxStack) {
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

    public <T> Node<T> push(Node<T> node) {
        this.stack.push(node);
        return node;
    }

    @SuppressWarnings("unchecked")
    public <T> Node<T> pop(Class<T> type) {
        Node<?> val = this.stack.pop();
        return (Node<T>) checkType(val, type);
    }

    @SuppressWarnings("unchecked")
    public <T> Node<T> peek(Class<T> type) {
        Node<?> val = this.stack.peek();
        return (Node<T>) checkType(val, type);
    }

    public void clear() {
        this.stack.clear();
    }

    @SuppressWarnings("unchecked")
    public <T> Node<T> load(int index, Class<T> type) {
        return (Node<T>) checkType(this.locals[index].load(type), type);
    }

    public <T> Node<T> get(int index, Class<T> type) {
        if ( index == BytecodeParser.SLOT_RETURN ) {
            return checkType(this.returnValue.get(type), type);
        }
        if (index == BytecodeParser.SLOT_IO) {
            return (Node<T>) checkType(this.io.get(IOToken.class), IOToken.class);
        }
        if (index == BytecodeParser.SLOT_MEMORY) {
            return (Node<T>) checkType(this.memory.get(MemoryToken.class), MemoryToken.class);
        }
        return checkType(this.locals[index].get(type), type);
    }

    public void store(int index, Node<?> val) {
        if (this.locals[index] == null) {
            this.locals[index] = new Local.SimpleLocal(this.control, index);
        }
        this.locals[index].store(val);
    }

    public void returnValue(Node<?> returnValue) {
        if ( this.returnValue == null ) {
            this.returnValue = new Local.SimpleLocal(this.control, BytecodeParser.SLOT_RETURN);
        }
        this.returnValue.store(returnValue);
    }

    public Node<?> returnValue() {
        return this.returnValue.load(null);
    }

    public void memory(Node<MemoryToken> memory) {
        if (this.memory == null) {
            this.memory = new Local.SimpleLocal(this.control, BytecodeParser.SLOT_MEMORY);
        }
        this.memory.store(memory);
    }

    public Node<MemoryToken> memory() {
        return this.memory.load(MemoryToken.class);
    }

    public void io(Node<IOToken> io) {
        if (this.io == null) {
            this.io = new Local.SimpleLocal(this.control, BytecodeParser.SLOT_IO);
        }
        this.io.store(io);
    }

    public Node<IOToken> io() {
        return this.io.load(IOToken.class);
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

    public Local.PhiLocal ensurePhi(int index, ControlNode<?> input, Class<?> type) {
        if ( !( this.control instanceof RegionNode) ) {
            throw new UnsupportedOperationException( this.control + " cannot own Phi nodes");
        }
        if ( index == BytecodeParser.SLOT_RETURN ) {
            if ( !(this.returnValue instanceof Local.PhiLocal)) {
                this.returnValue = new Local.PhiLocal((RegionNode) this.control, index, type);
            }
            ((Local.PhiLocal) this.returnValue).addInput(input, type);
            return (Local.PhiLocal) this.returnValue;
        } else if (index == BytecodeParser.SLOT_IO) {
            if (!(this.io instanceof Local.PhiLocal)) {
                this.io = new Local.PhiLocal((RegionNode) this.control, index, type);
            }
            ((Local.PhiLocal) this.io).addInput(input, type);
            return (Local.PhiLocal) this.io;
        } else if (index == BytecodeParser.SLOT_MEMORY) {
            if (!(this.memory instanceof Local.PhiLocal)) {
                this.memory = new Local.PhiLocal((RegionNode) this.control, index, type);
            }
            ((Local.PhiLocal) this.memory).addInput(input, type);
            return (Local.PhiLocal) this.memory;
        } else {
            if (!(this.locals[index] instanceof Local.PhiLocal)) {
                this.locals[index] = new Local.PhiLocal((RegionNode) this.control, index, type);
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

    private final ControlNode<?> control;

    private Local[] locals;

    private Local memory;

    private Local io;

    private Deque<Node<?>> stack;

    private Local returnValue;
}
