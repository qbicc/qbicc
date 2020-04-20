package cc.quarkus.qcc.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.AbstractNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.StartValue;

public class Graph<T extends ConcreteType<?>> {

    public Graph(StartNode start, EndNode<T> end) {
        this.start = start;
        this.end = end;
    }

    public List<AbstractNode<?>> reversePostOrder() {
        List<AbstractNode<?>> order = postOrder();
        Collections.reverse(order);
        return order;
    }

    public List<AbstractNode<?>> postOrder() {
        List<AbstractNode<?>> order = new ArrayList<>();
        Set<AbstractNode<?>> seen = new HashSet<>();
        walk(order, seen, this.start);
        return order;
    }

    public StartNode getStart() {
        return this.start;
    }

    public void execute(StartValue context) {
        //this.start.receive(context);
    }

    private void walk(List<AbstractNode<?>> order, Set<AbstractNode<?>> seen, AbstractNode<?> node) {
        if ( seen.contains(node)) {
            return;
        }
        seen.add(node);
        for (AbstractNode<?> successor : node.getSuccessors()) {
            walk(order, seen, successor);
        }
        order.add(node);
    }

    private final StartNode start;

    private final EndNode<T> end;
}
