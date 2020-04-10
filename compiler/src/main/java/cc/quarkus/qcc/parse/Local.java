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
            return TypeUtil.checkType(this.val, type);
        }

        public <J extends Type> Node<J> get(J type) {
            return load(type);
        }

        private Node<?> val;
    }

    public static class PhiLocal<T extends Type> extends SimpleLocal<T> {
        public PhiLocal(ControlNode<?> control, int index, T type) {
            super(control, index);
            this.phi = new PhiNode<>(this.control, type, this);
            this.type = type;
        }

        @Override
        public <J extends Type> Node<J> load(J type) {
            if ( ! this.killed ) {
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
            //System.err.println( "storing: " + val + " > " + this.inputs + " vs " + val.getControlPredecessors().iterator().next());
            //if ( ! this.inputs.contains(val.getControlPredecessors().iterator().next())) {
              //  System.err.println( "killing with " + val);
               // super.store(val);
            //}
            super.store(val);
            if ( this.inputs.contains(val.getControlPredecessors().iterator().next())) {
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
            List<ControlNode<?>> discriminators = this.control.getControlPredecessors();

            for (ControlNode<?> discriminator : discriminators) {
                this.phi.addPredecessor( discriminator.frame().get(this.index, AnyType.INSTANCE));
            }
        }

        private final PhiNode<?> phi;
        private Type type;
        private List<ControlNode<?>> inputs = new ArrayList<>();

    }
}
