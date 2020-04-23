package cc.quarkus.qcc.parse;

import java.util.ArrayDeque;
import java.util.Deque;

import cc.quarkus.qcc.graph.node.CatchControlProjection;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.type.TypeDescriptor;

import static cc.quarkus.qcc.parse.TypeUtil.checkType;

public class Frame {

    public Frame(ControlNode<?> control, int maxLocals, int maxStack) {
        this.control = control;
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
        this.locals = new SimpleLocal[maxLocals];
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
        if (index == BytecodeParser.SLOT_RETURN) {
            return this.returnValue;
        }
        if (index == BytecodeParser.SLOT_IO) {
            return this.io;
        }
        if (index == BytecodeParser.SLOT_MEMORY) {
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
        //System.err.println("frame: " + this.control + " get " + index + " / " + type);
        if (index == BytecodeParser.SLOT_RETURN) {
            return checkType(this.returnValue.get(type), type);
        }
        //System.err.println("GET 1");
        if (index == BytecodeParser.SLOT_IO) {
            //System.err.println( this.control + " get io: " + this.io + " >> " + this.io.get(IOToken.class));
            if (this.io instanceof PhiLocal) {
                //System.err.println("return io phi");
                return (Node<T>) ((PhiLocal) this.io).getPhiNode();
            }
            //System.err.println("return io local");
            return (Node<T>) checkType(this.io.get(IOToken.class), IOToken.class);
        }
        //System.err.println("GET 2");
        if (index == BytecodeParser.SLOT_MEMORY) {
            //System.err.println( this.control +  " get memory: " + this.memory + " >> " + this.memory.get(MemoryToken.class));
            if (this.memory instanceof PhiLocal) {
                //System.err.println("return memory phi");
                return (Node<T>) ((PhiLocal) this.memory).getPhiNode();
            }
            //System.err.println("return memory local");
            return (Node<T>) checkType(this.memory.get(MemoryToken.class), MemoryToken.class);
        }
        //System.err.println("GET 3 @ " + index + " on " + this.control + " >> " + this.locals[index] );
        return checkType(this.locals[index].get(type), type);
    }

    public void store(int index, Node<?> val) {
        //System.err.println( "STORE " + index + " = " + val + " on " + this.control);
        if (this.locals[index] == null) {
            this.locals[index] = new SimpleLocal(this.control, index);
        }
        //System.err.println( "  perform store on " + this.locals)
        this.locals[index].store(val);
    }

    public void returnValue(Node<?> returnValue) {
        if (this.returnValue == null) {
            this.returnValue = new SimpleLocal(this.control, BytecodeParser.SLOT_RETURN);
        }
        this.returnValue.store(returnValue);
    }

    public Node<?> returnValue() {
        return this.returnValue.load(null);
    }

    public void memory(Node<MemoryToken> memory) {
        if (this.memory == null) {
            this.memory = new SimpleLocal(this.control, BytecodeParser.SLOT_MEMORY);
        }
        this.memory.store(memory);
    }

    public Node<MemoryToken> memory() {
        return this.memory.load(MemoryToken.class);
    }

    public void io(Node<IOToken> io) {
        if (this.io == null) {
            this.io = new SimpleLocal(this.control, BytecodeParser.SLOT_IO);
        }
        this.io.store(io);
    }

    public Node<IOToken> io() {
        return this.io.load(IOToken.class);
    }

    public void mergeInputs() {
        //System.err.println( this.control + " merge inputs");
        for (ControlNode<?> each : this.control.getControlPredecessors()) {
            //System.err.println( this.control + " << " + each );
            if ( this.control instanceof CatchControlProjection ) {
                mergeFrom(each.frame(), this.control);
            } else {
                mergeFrom(each.frame());
            }
        }
    }

    public void mergeFrom(Frame inbound) {
        mergeFrom(inbound, null);
    }

    public void mergeFrom(Frame inbound, Node<?> thrown) {
        //System.err.println(this.control + " merge from " + inbound.control + " thrown=" + thrown);
        //new Exception().printStackTrace();
        if (thrown == null) {
            for (int i = 0; i < this.locals.length; ++i) {
                if (this.locals[i] == null) {
                    if (inbound.locals[i] != null) {
                        this.locals[i] = inbound.locals[i].duplicate();
                    }
                }
            }
            this.stack.clear();
            this.stack.addAll(inbound.stack);
        } else {
            this.stack.clear();
            this.stack.add(thrown);
        }

        if (this.returnValue == null && inbound.returnValue != null) {
            this.returnValue = inbound.returnValue.duplicate();
        }
        if (this.io == null) {
            //System.err.println("dupe io " + inbound.io);
            this.io = inbound.io.duplicate();
        } else {
            //System.err.println("have io " + this.io);
        }
        if (this.memory == null) {
            //System.err.println("dupe memory " + inbound.memory);
            this.memory = inbound.memory.duplicate();
        } else {
            //System.err.println("have memory " + this.memory);
        }
    }

    public PhiLocal ensurePhi(int index, ControlNode<?> input, TypeDescriptor<?> type) {
        if (!(this.control instanceof RegionNode)) {
            throw new UnsupportedOperationException(this.control + " cannot own Phi nodes");
        }
        if (index == BytecodeParser.SLOT_RETURN) {
            if (!(this.returnValue instanceof PhiLocal)) {
                this.returnValue = new PhiLocal((RegionNode) this.control, index, type);
            }
            ((PhiLocal) this.returnValue).addInput(input, type);
            return (PhiLocal) this.returnValue;
        } else if (index == BytecodeParser.SLOT_IO) {
            if (!(this.io instanceof PhiLocal)) {
                this.io = new PhiLocal((RegionNode) this.control, index, type);
            }
            ((PhiLocal) this.io).addInput(input, type);
            return (PhiLocal) this.io;
        } else if (index == BytecodeParser.SLOT_MEMORY) {
            if (!(this.memory instanceof PhiLocal)) {
                this.memory = new PhiLocal((RegionNode) this.control, index, type);
            }
            ((PhiLocal) this.memory).addInput(input, type);
            return (PhiLocal) this.memory;
        } else {
            if (!(this.locals[index] instanceof PhiLocal)) {
                this.locals[index] = new PhiLocal((RegionNode) this.control, index, type);
            }
            ((PhiLocal) this.locals[index]).addInput(input, type);
            return (PhiLocal) this.locals[index];
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
