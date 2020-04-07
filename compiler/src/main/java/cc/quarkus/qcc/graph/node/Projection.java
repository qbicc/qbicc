package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.Type;

public class Projection<INPUT extends ControlNode<?>, OUTPUT extends Type> extends Node<OUTPUT> {

    protected Projection(INPUT in, OUTPUT outType) {
        super(in, outType);
    }

}
