package cc.quarkus.qcc.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.node.PhiOwner;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.type.AnyType;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;

public abstract class Local {

    public Local(ControlNode<?,?> control, int index) {
        this.control = control;
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    public abstract void store(Node<?,?> val);

    public abstract Node<?,?> load(Type<?> type);

    public abstract Node<?, ?> get(Type<?> type);

    public abstract Local duplicate();

    protected final int index;

    protected boolean killed;

    protected ControlNode<?,?> control;

    public static class SimpleLocal extends Local {

        public SimpleLocal(ControlNode<?,?> control, int index) {
            super(control, index);
        }

        @Override
        public  void store(Node<?,?> val) {
            this.val = val;
            this.killed = true;
        }

        @Override
        public Node<?,?> load(Type<?> type) {
            return TypeUtil.checkType(this.val, type);
        }

        public Node<?,?> get(Type<?> type) {
            return load(type);
        }

        public String toString() {
            return "Local: val=" + val;
        }

        @Override
        public Local duplicate() {
            SimpleLocal dupe = new SimpleLocal(this.control, this.index);
            dupe.val = this.val;
            return dupe;
        }

        protected Node<?,?> val;
    }

    public static class PhiLocal extends SimpleLocal {
        public <T extends ControlNode<?,?> & PhiOwner> PhiLocal(T control, int index, Type<?> type) {
            super(control, index);
            this.phi = new PhiNode(this.control, type, this);
            this.type = type;
        }

        PhiOwner getPhiOwner() {
            return (PhiOwner) this.control;
        }

        public Node<?,?> load(Type<?> type) {
            if (!this.killed) {
                return TypeUtil.checkType(this.phi, type);
            }
            return super.load(type);
        }

        public Node<?,?> get(Type<?> type) {
            return super.load(type);
        }

        @Override
        public void store(Node<?,?> val) {
            super.store(val);
            if ( this.inputs.contains(val.getControl())) {
                this.killed = false;
            }
        }

        public void addInput(ControlNode<?,?> control, Type<?> type) {
            this.type = this.type.join(type);
            this.inputs.add(control);
        }

        public void complete() {
            List<ControlNode<?,?>> discriminators = getPhiOwner().getInputs();

            for (ControlNode<?,?> discriminator : discriminators) {
                Node<?,?> inbound = discriminator.frame().get(this.index, AnyType.INSTANCE);
                this.phi.addInput(inbound);
                this.values.put(discriminator, inbound);
            }
        }

        public Node<?,?> getValue(ControlNode<?,?> discriminator) {
            return this.values.get(discriminator);
        }

        public String toString() {
            return "PhiLocal: val=" + val + " kill=" + this.killed + " phi=" + this.phi + " inputs=" + this.inputs;
        }

        @Override
        public Local duplicate() {
            // don't really duplicate.
            return this;
        }

        private PhiNode<?,?> phi;
        private Type<?> type;
        private List<ControlNode<?,?>> inputs = new ArrayList<>();
        private Map<ControlNode<?,?>, Node<?,?>> values = new HashMap<>();

    }
}
