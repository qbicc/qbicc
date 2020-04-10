package cc.quarkus.qcc.graph.node;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.parse.Local;

public class PhiNode<T extends Type> extends Node<T> {

    public PhiNode(ControlNode<?> control, T outType, Local.PhiLocal local) {
        super(control, outType);
        this.local = local;
        this.id = COUNTER.incrementAndGet();
    }

    //public Collection<? extends Node<T>> values() {
    //return local.values();
    //return null;
    //}


    @Override
    public String label() {
        return "phi: " + this.id;
    }

    @Override
    public String toString() {
        return label();
    }

    private final Local.PhiLocal<T> local;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final int id;
}
