package cc.quarkus.qcc.graph.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.type.TypeDescriptor;

public class PhiLocal extends SimpleLocal {
    public PhiLocal(RegionNode control, int index, TypeDescriptor<?> type) {
        super(control, index);
        this.phi = new PhiNode(this.control, type, this);
        this.type = type;
    }

    public Node<?> getPhiNode() {
        return this.phi;
    }

    RegionNode getRegion() {
        return (RegionNode) this.control;
    }

    public <V> Node<V> load(Class<V> type) {
        if (!this.killed) {
            return TypeUtil.checkType(this.phi, type);
        }
        return super.load(type);
    }

    public <V> Node<V> get(Class<V> type) {
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

    public void complete() {
        List<ControlNode<?>> discriminators = getRegion().getInputs();
        for (ControlNode<?> discriminator : discriminators) {
            Node<?> inbound = discriminator.frame().get(this.index, this.type.valueType());
            this.phi.addInput(inbound);
            this.values.put(discriminator, inbound);
        }
    }

    public Node<?> getValue(ControlNode<?> discriminator) {
        return this.values.get(discriminator);
    }

    public String toString() {
        return "PhiLocal: val=" + val + " kill=" + this.killed + " phi=" + this.phi + " inputs=" + this.inputs;
    }

    @Override
    public Local duplicate() {
        return this;
    }

    private PhiNode<?> phi;

    private TypeDescriptor<?> type;

    private List<ControlNode<?>> inputs = new ArrayList<>();

    private Map<ControlNode<?>, Node<?>> values = new HashMap<>();

}
