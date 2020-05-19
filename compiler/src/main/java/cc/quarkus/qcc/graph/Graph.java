package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.definition.MethodDefinition;

public class Graph<V extends QType> {

    public Graph(MethodDefinition<V> method) {
        this.method = method;
        this.start = new StartNode(this);
        this.endRegion = new RegionNode(this);
        this.end = new EndNode<>(this, this.endRegion, this.method.getReturnType());
    }

    public void write(String path) throws IOException {
        write(Paths.get(path));
    }

    public void write(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            path = path.resolve(defaultGraphName());
            path = path.getParent().resolve( path.getFileName() + ".dot");
        }

        Files.createDirectories(path.getParent());

        try ( DotWriter writer = new DotWriter(path) ) {
            writer.write(this);
        }
    }

    protected String defaultGraphName() {
        return this.method.getTypeDefinition().getName() + "-" + this.method.getName() + this.method.getDescriptor();
    }

    public MethodDefinition<V> getMethod() {
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

    public int consumeNextId() {
        return ++this.nodeCounter;
    }

    private final MethodDefinition<V> method;
    private final RegionNode endRegion;
    private final StartNode start;
    private final EndNode<V> end;

    private int nodeCounter = 0;

}
