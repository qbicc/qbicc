package cc.quarkus.qcc.graph.build;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class AnalysisStack {

    public AnalysisStack(ControlNode<?> control, int maxStack) {
        this.control = control;
        this.stack = new StackItem[maxStack];
        for ( int i = 0 ; i < maxStack; ++i) {
            this.stack[i] = new StackItem();
        }

    }

    public <T extends QType> void push(Node<T> node) {
        this.stack[this.sp++].store(node);
    }

    public <T extends QType> Node<T> pop(Class<T> type) {
        return this.stack[--this.sp].get(type);
    }

    public <T extends QType> Node<T> peek(Class<T> type) {
        return this.stack[this.sp - 1].get(type);
    }

    public void clear() {
        this.sp = 0;
    }

    public void addAll(AnalysisStack inbound) {
        for (int i = 0; i < inbound.sp; ++i) {
            this.stack[i].store(inbound.stack[i].get(null));
        }
        this.sp = inbound.sp;
    }

    @Override
    public String toString() {
        return this.control.toString();
    }

    @SuppressWarnings("unchecked")
    public List<PhiStackItem> ensureStackPhis(ControlNode<?> src, List<TypeDescriptor<?>> types, int offset) {
        List<PhiStackItem> stackPhis = new ArrayList<>();
        for (int i = offset, j = 0; i < types.size(); ++i, ++j) {
            StackItem stackItem = this.stack[i];
            if (!(stackItem instanceof PhiStackItem)) {
                stackItem = new PhiStackItem((RegionNode) this.control, i, types.get(j));
                this.stack[i] = stackItem;
                stackPhis.add((PhiStackItem) stackItem);
            }
            ((PhiStackItem) stackItem).addInput(src, types.get(j));
        }
        return stackPhis;
    }

    public Node<?> get(int position, Class<? extends QType> type) {
        return this.stack[position].get(type);
    }

    private final ControlNode<?> control;

    private final StackItem[] stack;

    private int sp = 0;
}
