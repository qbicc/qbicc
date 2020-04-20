package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.ControlType;
import cc.quarkus.qcc.graph.type.ControlValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class RegionNode extends AbstractControlNode<ControlType, ControlValue> implements PhiOwner {

    public RegionNode(int maxLocals, int maxStack) {
        super(ControlType.INSTANCE, maxLocals, maxStack);
    }

    public void addInput(ControlNode<?,?> input) {
        this.inputs.add( input );
    }

    @Override
    public List<? extends Node<?, ?>> getPredecessors() {
        return getInputs();
    }

    @Override
    public List<ControlNode<?, ?>> getInputs() {
        return this.inputs;
    }

    @Override
    public String label() {
        return getId() + ": <region>";
    }

    @Override
    public ControlValue getValue(Context context) {
        return new ControlValue();
    }

    private final List<ControlNode<?,?>> inputs = new ArrayList<>();
}

