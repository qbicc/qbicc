package cc.quarkus.qcc.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.graph.type.ConcreteType;

public class Graph<T extends ConcreteType<?>> {

    public Graph(StartNode start, EndNode<T> end) {
        this.start = start;
        this.end = end;
    }

    public List<Node<?>> reversePostOrder() {
        List<Node<?>> order = postOrder();
        Collections.reverse(order);
        return order;
    }

    public List<Node<?>> postOrder() {
        List<Node<?>> order = new ArrayList<>();
        Set<Node<?>> seen = new HashSet<>();
        walk(order, seen, this.start);
        return order;
    }

    public StartNode getStart() {
        return this.start;
    }

    private void walk(List<Node<?>> order, Set<Node<?>> seen, Node<?> node) {
        if ( seen.contains(node)) {
            return;
        }
        seen.add(node);
        for (Node<?> successor : node.getSuccessors()) {
            walk(order, seen, successor);
        }
        order.add(node);
    }

    private final StartNode start;

    private final EndNode<T> end;
}
