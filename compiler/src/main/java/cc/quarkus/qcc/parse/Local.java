package cc.quarkus.qcc.parse;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.type.AnyType;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.Type;

public abstract class Local<T extends Type> {

    public Local(ControlNode<?> control, int index) {
        this.control = control;
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    public abstract <J extends Type> void store(Node<J> val);

    public abstract <J extends Type> Node<J> load(J type);

    public abstract <J extends Type> Node<J> get(J type);

    public abstract Local<T> duplicate();

    protected final int index;

    protected boolean killed;

    protected ControlNode<?> control;

    public static class SimpleLocal<T extends Type> extends Local<T> {

        public SimpleLocal(ControlNode<?> control, int index) {
            super(control, index);
        }

        @Override
        public <J extends Type> void store(Node<J> val) {
            this.val = val;
            this.killed = true;
        }

        @Override
        public <J extends Type> Node<J> load(J type) {
            System.err.println("load: " + type + " > " + this.val);
            return TypeUtil.checkType(this.val, type);
        }

        public <J extends Type> Node<J> get(J type) {
            return load(type);
        }

        public String toString() {
            return "Local: val=" + val;
        }

        @Override
        public Local<T> duplicate() {
            SimpleLocal<T> dupe = new SimpleLocal<>(this.control, this.index);
            dupe.val = this.val;
            return dupe;
        }

        protected Node<?> val;
    }

    public static class PhiLocal<T extends Type> extends SimpleLocal<T> {
        public PhiLocal(ControlNode<?> control, int index, T type) {
            super(control, index);
            this.phi = new PhiNode<>(this.control, type, this);
            this.type = type;
        }

        @Override
        public <J extends Type> Node<J> load(J type) {
            if (!this.killed) {
                //return TypeUtil.checkType(new PhiNode<>(this.control, type, this), type);
                return TypeUtil.checkType(this.phi, type);
            }
            return super.load(type);
        }

        public <J extends Type> Node<J> get(J type) {
            return super.load(type);
        }

        @Override
        public <J extends Type> void store(Node<J> val) {
            super.store(val);
            if (this.inputs.contains(val.getControlPredecessors().iterator().next())) {
                this.killed = false;
            }
        }

        public void addInput(ControlNode<?> control, Type type) {
            this.type = this.type.join(type);
            this.inputs.add(control);
        }

        public List<ControlNode<?>> getInputs() {
            return this.inputs;
        }

        public void complete() {
            System.err.println("**************************");
            System.err.println("complete: " + this.type + " in " + this.control);
            List<ControlNode<?>> discriminators = this.control.getControlPredecessors();

            for (ControlNode<?> discriminator : discriminators) {
                System.err.println("discriminator: " + discriminator + " for " + this.index + discriminator.frame().local(this.index));
                Node<Type> inbound = discriminator.frame().get(this.index, AnyType.INSTANCE);
                System.err.println( "---> " + inbound + " // " + inbound.getId() );
                this.phi.addPredecessor(inbound);
            }
        }

        public String toString() {
            return "PhiLocal: val=" + val + " kill=" + this.killed + " phi=" + this.phi + " inputs=" + this.inputs;
        }

        @Override
        public Local<T> duplicate() {
            // don't really duplicate.
            return this;
        }

        private PhiNode<?> phi;

        private Type type;

        private List<ControlNode<?>> inputs = new ArrayList<>();

    }
}
