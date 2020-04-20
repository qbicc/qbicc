package cc.quarkus.qcc.graph.node;

import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.EndType;
import cc.quarkus.qcc.graph.type.EndValue;
import cc.quarkus.qcc.graph.type.IOValue;
import cc.quarkus.qcc.graph.type.MemoryValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class EndNode<T extends ConcreteType<T>> extends AbstractNode<EndType<T>, EndValue<T>> {

    public EndNode(ControlNode<?,?> control, T returnType) {
        super(control, new EndType<T>(returnType));
    }

    /*
    @Override
    public Value getValue(Context context) {
        return null;
    }
     */

    @Override
    public EndValue<T> getValue(Context context) {
        return null;
    }

    @Override
    public List<? extends Node<?, ?>> getPredecessors() {
        return null;
    }

    public String label() {
        return "<end>";
    }

    /*
    @Override
    public Value<?> getValue(Context context) {
        //for (Node<?> predecessor : getPredecessors()) {
            //System.err.println( "end pred: " + predecessor + " >> " + context.get(predecessor));
        //}
        IOValue io = (IOValue) context.get(getPredecessors().get(1));
        MemoryValue memory = (MemoryValue) context.get(getPredecessors().get(2));
        Value<?> returnValue = context.get(getPredecessors().get(3));

        return getType().newInstance(io, memory, returnValue);
    }
     */
}
