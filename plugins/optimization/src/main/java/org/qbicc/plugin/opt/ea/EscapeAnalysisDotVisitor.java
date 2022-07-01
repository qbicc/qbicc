package org.qbicc.plugin.opt.ea;

import java.util.Collection;
import java.util.Objects;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Store;
import org.qbicc.graph.ValueHandle;
import org.qbicc.plugin.dot.Disassembler;
import org.qbicc.plugin.dot.DotAttributes;

public final class EscapeAnalysisDotVisitor implements NodeVisitor.Delegating<Disassembler, Void, Void, Void, Void> {
    private final NodeVisitor<Disassembler, Void, Void, Void, Void> delegate;
    private final EscapeAnalysisState escapeAnalysisState;

    public EscapeAnalysisDotVisitor(CompilationContext ctxt, NodeVisitor<Disassembler, Void, Void, Void, Void> delegate) {
        this.delegate = delegate;
        this.escapeAnalysisState = EscapeAnalysisState.get(ctxt);
    }

    @Override
    public NodeVisitor<Disassembler, Void, Void, Void, Void> getDelegateNodeVisitor() {
        return delegate;
    }

    @Override
    public Void visit(Disassembler param, New node) {
        decorate(param, node);
        return delegate.visit(param, node);
    }

    @Override
    public Void visit(Disassembler param, Store node) {
        final ValueHandle valueHandle = node.getValueHandle();
        if (valueHandle instanceof StaticField || valueHandle instanceof InstanceFieldOf) {
            decorate(param, valueHandle);
        }

        return delegate.visit(param, node);
    }

    @Override
    public Void visit(Disassembler param, ParameterValue node) {
        decorate(param, node);
        return delegate.visit(param, node);
    }

    @Override
    public Void visit(Disassembler param, PhiValue node) {
        decorate(param, node);
        return delegate.visit(param, node);
    }

    private void decorate(Disassembler param, Node node) {
        final ConnectionGraph connectionGraph = getConnectionGraph(param);
        param.setLineColor(nodeType(connectionGraph.getEscapeValue(node)).fillColor);
        addFieldEdges(param, node, connectionGraph);
        addPointsToEdge(param, node, connectionGraph);
    }

    private void addPointsToEdge(Disassembler param, Node node, ConnectionGraph connectionGraph) {
        final Node pointsTo = connectionGraph.getPointsToEdge(node);
        if (Objects.nonNull(pointsTo)) {
            param.addCellEdge(node, pointsTo, "P", EdgeType.POINTS_TO);
        }
    }

    private void addFieldEdges(Disassembler param, Node node, ConnectionGraph connectionGraph) {
        final Collection<InstanceFieldOf> fields = connectionGraph.getFieldEdges(node);
        for (InstanceFieldOf field : fields) {
            param.addCellEdge(node, field, "F", EdgeType.FIELD);
        }
    }

    private ConnectionGraph getConnectionGraph(Disassembler disassembler) {
        return escapeAnalysisState.getConnectionGraph(disassembler.getElement());
    }

    private NodeType nodeType(EscapeValue value) {
        return switch (value) {
            case GLOBAL_ESCAPE -> NodeType.GLOBAL_ESCAPE;
            case ARG_ESCAPE -> NodeType.ARG_ESCAPE;
            case NO_ESCAPE -> NodeType.NO_ESCAPE;
            case UNKNOWN -> NodeType.UNKNOWN;
        };
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

    private enum EdgeType implements DotAttributes {
        POINTS_TO("gray", "solid"),
        FIELD("gray", "dashed");

        final String color;
        final String style;

        EdgeType(String color, String style) {
            this.color = color;
            this.style = style;
        }

        @Override
        public String color() {
            return color;
        }

        @Override
        public String style() {
            return style;
        }

        @Override
        public String portPos() {
            return this == FIELD ? "w" : "e";
        }
    }
}
