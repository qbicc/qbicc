package cc.quarkus.qcc.graph.build;

import java.util.ArrayDeque;
import java.util.Deque;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.type.CompletionToken;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

import static cc.quarkus.qcc.graph.build.TypeUtil.checkType;

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
        if (index == GraphBuilder.SLOT_COMPLETION) {
            return this.completion;
        }
        if (index == GraphBuilder.SLOT_IO) {
            return this.io;
        }
        if (index == GraphBuilder.SLOT_MEMORY) {
            return this.memory;
        }
        return this.locals[index];
    }

    public <T extends QType> Node<T> push(Node<T> node) {
        this.stack.push(node);
        return node;
    }

    public <T extends QType> Node<T> pop(Class<T> type) {
        Node<?> val = this.stack.pop();
        return checkType(val, type);
    }

    public <T extends QType> Node<T> peek(Class<T> type) {
        Node<?> val = this.stack.peek();
        return checkType(val, type);
    }

    public void clear() {
        this.stack.clear();
    }

    public <T extends QType> Node<T> load(int index, Class<T> type) {
        return checkType(this.locals[index].load(type), type);
    }

    @SuppressWarnings("unchecked")
    public <T extends QType> Node<T> get(int index, Class<T> type) {
        if (index == GraphBuilder.SLOT_COMPLETION) {
            return checkType(this.completion.get(type), type);
        }
        if (index == GraphBuilder.SLOT_IO) {
            if (this.io instanceof PhiLocal) {
                return (Node<T>) ((PhiLocal) this.io).getPhiNode();
            }
            return (Node<T>) checkType(this.io.get(IOToken.class), IOToken.class);
        }
        if (index == GraphBuilder.SLOT_MEMORY) {
            if (this.memory instanceof PhiLocal) {
                return (Node<T>) ((PhiLocal) this.memory).getPhiNode();
            }
            return (Node<T>) checkType(this.memory.get(MemoryToken.class), MemoryToken.class);
        }
        if (this.locals[index] == null) {
            return null;
        }
        return checkType(this.locals[index].get(type), type);
    }

    public void store(int index, Node<?> val) {
        if (this.locals[index] == null) {
            this.locals[index] = new SimpleLocal(this.control, index);
        }
        this.locals[index].store(val);
    }

    public void completion(Node<CompletionToken> completion) {
        if (this.completion == null) {
            this.completion = new SimpleLocal(this.control, GraphBuilder.SLOT_COMPLETION);
        }
        this.completion.store(completion);
    }

    public Node<CompletionToken> completion() {
        if (this.completion == null) {
            return null;
        }
        return this.completion.load(null);
    }

    public void memory(Node<MemoryToken> memory) {
        if (this.memory == null) {
            this.memory = new SimpleLocal(this.control, GraphBuilder.SLOT_MEMORY);
        }
        this.memory.store(memory);
    }

    public Node<MemoryToken> memory() {
        return this.memory.load(MemoryToken.class);
    }

    public void io(Node<IOToken> io) {
        if (this.io == null) {
            this.io = new SimpleLocal(this.control, GraphBuilder.SLOT_IO);
        }
        this.io.store(io);
    }

    public Node<IOToken> io() {
        return this.io.load(IOToken.class);
    }

    public void mergeFrom(Frame inbound) {
        mergeFrom(inbound, null);
    }

    public void mergeFrom(Frame inbound, Node<?> thrown) {
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

        if (this.completion == null && inbound.completion != null) {
            this.completion = inbound.completion.duplicate();
        }

        if (this.io == null) {
            this.io = inbound.io.duplicate();
        }
        if (this.memory == null) {
            this.memory = inbound.memory.duplicate();
        }
    }

    public PhiLocal ensurePhi(int index, ControlNode<?> input, TypeDescriptor<?> type) {
        if (!(this.control instanceof RegionNode)) {
            throw new UnsupportedOperationException(this.control + " cannot own Phi nodes");
        }
        if (index == GraphBuilder.SLOT_COMPLETION) {
            if (!(this.completion instanceof PhiLocal)) {
                this.completion = new PhiLocal((RegionNode) this.control, index, type);
            }
            ((PhiLocal) this.completion).addInput(input, type);
            return (PhiLocal) this.completion;
        } else if (index == GraphBuilder.SLOT_IO) {
            if (!(this.io instanceof PhiLocal)) {
                this.io = new PhiLocal((RegionNode) this.control, index, type);
            }
            ((PhiLocal) this.io).addInput(input, type);
            return (PhiLocal) this.io;
        } else if (index == GraphBuilder.SLOT_MEMORY) {
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

    private Local completion;
}
