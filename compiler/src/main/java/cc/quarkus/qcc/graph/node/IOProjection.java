package cc.quarkus.qcc.graph.node;

public class IOProjection extends Node<IOType> {
    protected IOProjection(ControlNode<?> control) {
        super(control, IOType.INSTANCE);
    }
}
