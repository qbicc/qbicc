package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class RegionNode extends AbstractControlNode<ControlToken> {

    public RegionNode(int maxLocals, int maxStack) {
        super(TypeDescriptor.EphemeralTypeDescriptor.CONTROL_TOKEN, maxLocals, maxStack);
    }

    public void addInput(ControlNode<?> input) {
        if( this.inputs.contains(input) ) {
            return;
        }
        this.inputs.add( input );
        input.addSuccessor(this);
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return getInputs();
    }

    public List<ControlNode<?>> getInputs() {
        return this.inputs;
    }

    @Override
    public String label() {
        return "<region:" + getId() + ">";
    }

    @Override
    public String toString() {
        return label();
    }

    @Override
    public ControlToken getValue(Context context) {
        return new ControlToken();
    }

    private final List<ControlNode<?>> inputs = new ArrayList<>();
}

