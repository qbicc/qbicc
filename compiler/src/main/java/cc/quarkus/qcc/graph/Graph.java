package cc.quarkus.qcc.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.type.MethodDefinition;

public class Graph {

    /*
    public Graph(StartNode start, EndNode<?> end) {
        this.start = start;
        this.end = end;
    }
     */
    public Graph(MethodDefinition method) {
        this.method = method;
        this.start = new StartNode(method, method.getMaxLocals(), method.getMaxStack());
        this.endRegion = new RegionNode(this.method.getMaxLocals(), this.method.getMaxStack());
        this.end = new EndNode<>(this.endRegion, this.method.getReturnType());
    }

    public MethodDefinition getMethod() {
        return this.method;
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

    public EndNode<?> getEnd() {
        return this.end;
    }

    public RegionNode getEndRegion() {
        return this.endRegion;
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

    private final MethodDefinition method;
    private final RegionNode endRegion;
    private final StartNode start;
    private final EndNode<?> end;

}
