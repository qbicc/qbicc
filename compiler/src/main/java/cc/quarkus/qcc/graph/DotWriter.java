package cc.quarkus.qcc.graph;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.Type;

public class DotWriter implements AutoCloseable {

    public DotWriter(Path outputPath) throws FileNotFoundException {
        this.out = new PrintWriter(new FileOutputStream(outputPath.toFile()));
    }

    @Override
    public void close() {
        this.out.close();
    }

    public void write(Graph<?> graph) {
        out.println("digraph thegraph {");
        this.out.println("  graph [fontname = \"helvetica\",fontsize=10,ordering=in,outputorder=depthfirst];");
        this.out.println("  node [fontname = \"helvetica\",fontsize=10,ordering=in];");
        this.out.println("  edge [fontname = \"helvetica\",fontsize=10];");

        writeNode(graph.getStart());

        out.println("}");
    }

    protected void writeNode(Node<?> node) {
        if (this.seen.contains(node)) {
            return;
        }
        this.seen.add(node);

        out.println(node.getId() + " [label=\"" + label(node) + "\",shape=" + shape(node) + ",style=\"" + style(node) + "\"];");

        for (Node<?> successor : node.getSuccessors()) {
            writeEdge(node, successor);
        }

        //if (node instanceof PhiNode) {
         //   Collection<? extends Node<?>> values = ((PhiNode<?>) node).values();
          //  for ( Node<? extends Type> each : values ) {
           //     out.print(each.getId() + " ->" + node.getId() + " [style=dotted,color=red];");
            //}
        //}

        for (Node<?> successor : node.getSuccessors()) {
            writeNode(successor);
        }
    }

    protected String label(Node<?> node) {
        return node.label();
    }

    protected String shape(Node<?> node) {
        return "box";
    }

    protected String style(Node<?> node) {
        return "normal";
    }

    protected void writeEdge(Node<?> head, Node<?> tail) {
        out.println(head.getId() + " -> " + tail.getId() + " [color=" + color(head, tail) + ",style=" + style(head, tail) + "];");
    }

    protected String color(Node<?> head, Node<?> tail) {
        if ( head instanceof ControlNode ) {
            return "blue";
        }
        return "black";
    }

    protected String style(Node<?> head, Node<?> tail) {
        if ( head instanceof ControlNode && ! ( tail instanceof ControlNode )) {
            return "dotted";
        }
        return "solid";
    }

    private Set<Node<?>> seen = new HashSet<>();

    private final PrintWriter out;
}


