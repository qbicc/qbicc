package org.qbicc.plugin.opt.ea;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.StaticField;
import org.qbicc.plugin.dot.DotContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class EscapeAnalysisDotVisitor implements NodeVisitor.Delegating<DotContext, String, String, String, String> {
    private final NodeVisitor<DotContext, String, String, String, String> delegate;
    private final EscapeAnalysisState escapeAnalysisState;
    private final Map<Invoke.ReturnValue, Node> invokeReturnValueQueue = new HashMap<>();

    public EscapeAnalysisDotVisitor(CompilationContext ctxt, NodeVisitor<DotContext, String, String, String, String> delegate) {
        this.delegate = delegate;
        this.escapeAnalysisState = EscapeAnalysisState.get(ctxt);
    }

    @Override
    public NodeVisitor<DotContext, String, String, String, String> getDelegateNodeVisitor() {
        return delegate;
    }

    @Override
    public String visit(DotContext param, New node) {
        return decorate(param, node);
    }

    @Override
    public String visit(DotContext param, StaticField node) {
        return decorate(param, node);
    }

    @Override
    public String visit(DotContext param, ParameterValue node) {
        return decorate(param, node);
    }

    @Override
    public String visit(DotContext param, InstanceFieldOf node) {
        return decorate(param, node);
    }

    @Override
    public String visit(DotContext param, PhiValue node) {
        return decorate(param, node);
    }

    @Override
    public String visit(DotContext param, Invoke node) {
        final Node pointsToFrom = invokeReturnValueQueue.get(node.getReturnValue());
        if (Objects.nonNull(pointsToFrom)) {
            // Invoke return value not shown, but instead link to parent Invoke node
            addEdge(param, pointsToFrom, node, EdgeType.POINTS_TO, "P");
        }

        return param.getName(node);
    }

    private String decorate(DotContext param, Node node) {
        final ConnectionGraph connectionGraph = getConnectionGraph(param);
        final String name = param.getName(node);
        param.appendTo(name);
        param.attr("style", "filled");
        param.attr("fillcolor", nodeType(connectionGraph.getEscapeValue(node)).fillColor);
        param.nl();
        addFieldEdges(param, node, connectionGraph);
        addPointsToEdge(param, node, connectionGraph);
        return name;
    }

    private void addPointsToEdge(DotContext param, Node node, ConnectionGraph connectionGraph) {
        final Node pointsTo = connectionGraph.getPointsToEdge(node);
        if (Objects.nonNull(pointsTo)) {
            if (pointsTo instanceof Invoke.ReturnValue ret) {
                // Delay adding edges linked to the Invoke terminator until it gets visited.
                // This is done to keep the structure of subgraphs intact.
                invokeReturnValueQueue.put(ret, node);
                return;
            }

            addEdge(param, node, pointsTo, EdgeType.POINTS_TO, "P");
        }
    }

    private void addFieldEdges(DotContext param, Node node, ConnectionGraph connectionGraph) {
        final Collection<InstanceFieldOf> fields = connectionGraph.getFieldEdges(node);
        for (InstanceFieldOf field : fields) {
            addEdge(param, node, field, EdgeType.FIELD, "F");
        }
    }

    private ConnectionGraph getConnectionGraph(DotContext dtxt) {
        return escapeAnalysisState.getConnectionGraph(dtxt.getElement());
    }

    private NodeType nodeType(EscapeValue value) {
        return switch (value) {
            case GLOBAL_ESCAPE -> NodeType.GLOBAL_ESCAPE;
            case ARG_ESCAPE -> NodeType.ARG_ESCAPE;
            case NO_ESCAPE -> NodeType.NO_ESCAPE;
            case UNKNOWN -> NodeType.UNKNOWN;
        };
    }

    private void addEdge(DotContext param, Node from, Node to, EdgeType edge, String label) {
        String fromName = param.visit(from);
        String toName = param.visit(to);
        addEdge(param, fromName, toName, edge, label);
    }

    private void addEdge(DotContext param, String fromName, String toName, EdgeType edge, String label) {
        param.appendTo(fromName);
        param.appendTo(" -> ");
        param.appendTo(toName);
        param.attr("label", label);
        param.attr("style", edge.style);
        param.attr("color", edge.color);
        param.attr("fontcolor", edge.color);
        param.nl();
    }

    private enum NodeType {
        GLOBAL_ESCAPE("lightsalmon"),
        ARG_ESCAPE("lightcyan3"),
        NO_ESCAPE("lightblue1"),
        UNKNOWN("lightpink1");

        final String fillColor;

        NodeType(String fillColor) {
            this.fillColor = fillColor;
        }
    }

    private enum EdgeType {
        POINTS_TO("gray", "solid"),
        FIELD("gray", "dashed");

        final String color;
        final String style;
        final char label;

        EdgeType(String color, String style) {
            this.color = color;
            this.style = style;
            this.label = this.toString().charAt(0);
        }
    }
}
