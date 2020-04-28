package cc.quarkus.qcc.graph;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import cc.quarkus.qcc.graph.node.AbstractNode;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.node.Projection;

public class DotWriter implements AutoCloseable {

    public DotWriter(Path outputPath) throws FileNotFoundException {
        this.out = new PrintWriter(new FileOutputStream(outputPath.toFile()));
    }

    @Override
    public void close() {
        this.out.close();
    }

    public void write(Graph graph) {
        out.println("digraph thegraph {");
        this.out.println("  graph [fontname = \"helvetica\",fontsize=10,ordering=in,outputorder=depthfirst,ranksep=1];");
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

        for (Node<?> successor : node.getSuccessors()) {
            writeNode(successor);
        }

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

    }

    protected String label(Node<?> node) {
        return node.label();
    }

    protected String shape(Node<?> node) {
        if ( node instanceof PhiNode ) {
            return "house";
        }

        if ( node instanceof Projection ) {
            return "oval";
        }
        return "box";
    }

    protected String style(Node<?> node) {
        return "solid";
    }

    protected void writeEdge(Node<?> head, Node<?> tail) {
        out.println(head.getId() + " -> " + tail.getId() + " [color=" + color(head, tail) + ",style=" + style(head, tail) + ",penwidth=" + penwidth(head, tail) + "];");
    }

    protected String color(Node<?> head, Node<?> tail) {
        if ( head instanceof ControlNode) {
            return "blue";
        }
        return "black";
    }

    protected String style(Node<?> head, Node<?> tail) {
        if ( head instanceof ControlNode && ! ( tail instanceof ControlNode)) {
            return "dotted";
        }
        return "solid";
    }

    protected String penwidth(Node<?> head, Node<?> tail) {
        if ( head instanceof ControlNode && tail instanceof ControlNode) {
            return "1.2";
        }
        return "1";
    }

    private Set<Node<?>> seen = new HashSet<>();

    private final PrintWriter out;
}


