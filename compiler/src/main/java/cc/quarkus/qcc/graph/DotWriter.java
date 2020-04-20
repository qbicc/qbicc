package cc.quarkus.qcc.graph;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import cc.quarkus.qcc.graph.node.AbstractControlNode;
import cc.quarkus.qcc.graph.node.AbstractNode;

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

    protected void writeNode(AbstractNode<?> node) {
        if (this.seen.contains(node)) {
            return;
        }
        this.seen.add(node);

        for (AbstractNode<?> successor : node.getSuccessors()) {
            writeNode(successor);
        }

        out.println(node.getId() + " [label=\"" + label(node) + "\",shape=" + shape(node) + ",style=\"" + style(node) + "\"];");

        for (AbstractNode<?> successor : node.getSuccessors()) {
            writeEdge(node, successor);
        }

        //if (node instanceof PhiNode) {
         //   Collection<? extends Node<?>> values = ((PhiNode<?>) node).values();
          //  for ( Node<? extends Type> each : values ) {
           //     out.print(each.getId() + " ->" + node.getId() + " [style=dotted,color=red];");
            //}
        //}

    }

    protected String label(AbstractNode<?> node) {
        return node.label();
    }

    protected String shape(AbstractNode<?> node) {
        return "box";
    }

    protected String style(AbstractNode<?> node) {
        return "normal";
    }

    protected void writeEdge(AbstractNode<?> head, AbstractNode<?> tail) {
        out.println(head.getId() + " -> " + tail.getId() + " [color=" + color(head, tail) + ",style=" + style(head, tail) + "];");
    }

    protected String color(AbstractNode<?> head, AbstractNode<?> tail) {
        if ( head instanceof AbstractControlNode) {
            return "blue";
        }
        return "black";
    }

    protected String style(AbstractNode<?> head, AbstractNode<?> tail) {
        if ( head instanceof AbstractControlNode && ! ( tail instanceof AbstractControlNode)) {
            return "dotted";
        }
        return "solid";
    }

    private Set<AbstractNode<?>> seen = new HashSet<>();

    private final PrintWriter out;
}


