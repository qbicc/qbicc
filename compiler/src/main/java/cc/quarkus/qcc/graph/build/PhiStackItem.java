package cc.quarkus.qcc.graph.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class PhiStackItem extends StackItem implements PhiData {

    public PhiStackItem(RegionNode control, int position, TypeDescriptor<? extends QType> type) {
        this.control = control;
        this.position = position;
        this.type = type;
        this.phi = new PhiNode<>(this.control.getGraph(), this.control, type, this);
    }

    public PhiNode<?> getPhiNode() {
        return this.phi;
    }

    @Override
    public <V extends QType> Node<V> load(Class<V> type) {
        if (!this.killed) {
            return TypeUtil.checkType(this.phi, type);
        }
        return super.load(type);
    }

    @Override
    public <V extends QType> Node<V> get(Class<V> type) {
        return load(type);
    }

    @Override
    public void store(Node<?> val) {
        super.store(val);
        if (this.inputs.contains(val.getControl())) {
            this.killed = false;
        }
    }

    public void addInput(ControlNode<?> control, TypeDescriptor<?> type) {
        // FIXME not identity equality
        if (this.type != type) {
            throw new RuntimeException("Incompatible type " + type + " vs " + this.type);
        }
        this.inputs.add(control);
    }

    @Override
    public Node<?> getValue(ControlNode<?> discriminator) {
        return this.values.get(discriminator);
    }

    public void complete(FrameManager frameManager) {
        List<ControlNode<?>> discriminators = this.control.getInputs();
        for (ControlNode<?> discriminator : discriminators) {
            Node<?> inbound = frameManager.of(discriminator).stack().get(this.position, this.type.type());
            this.phi.addInput(inbound);
            this.values.put(discriminator, inbound);
        }
    }

    @Override
    public String toString() {
        return "PhiStackItem{" +
                "type=" + type +
                ", control=" + control +
                ", phi=" + phi +
                ", position=" + position +
                ", killed=" + killed +
                ", inputs=" + inputs +
                ", values=" + values +
                '}';
    }

    private final TypeDescriptor<? extends QType> type;

    private final RegionNode control;

    private final PhiNode<? extends QType> phi;

    private final int position;

    private boolean killed;

    private List<ControlNode<?>> inputs = new ArrayList<>();

    private Map<ControlNode<?>, Node<?>> values = new HashMap<>();
}
