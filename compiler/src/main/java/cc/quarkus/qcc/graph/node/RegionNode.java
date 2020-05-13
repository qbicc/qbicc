package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class RegionNode extends AbstractControlNode<ControlToken> {

    public RegionNode(Graph<?> graph) {
        super(graph, TypeDescriptor.EphemeralTypeDescriptor.CONTROL_TOKEN);
    }

    public void removeUnreachable(Set<ControlNode<?>> reachable) {
        this.inputs.retainAll(reachable);
        super.removeUnreachable(reachable);
    }

    public void addInput(ControlNode<?> input) {
        if ( input == null ) {
            return;
        }
        if( this.inputs.contains(input) ) {
            return;
        }
        this.inputs.add( input );
        input.addSuccessor(this);
    }

    @Override
    public RegionNode setLine(int line) {
        return (RegionNode) super.setLine(line);
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
        return "<region:" + getId() + ( getLine() > 0 ? " (" + getLine() + ")>" : ">" );
    }

    @Override
    public String toString() {
        return label();
    }

    @Override
    public ControlToken getValue(Context context) {
        return ControlToken.CONTROL;
    }

    private final List<ControlNode<?>> inputs = new ArrayList<>();
}

