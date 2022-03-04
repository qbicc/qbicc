
package org.qbicc.plugin.opt.ea;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.New;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.plugin.dot.DotContext;

public final class EscapeAnalysisDotVisitor implements NodeVisitor.Delegating<DotContext, String, String, String, String> {
    private final NodeVisitor<DotContext, String, String, String, String> delegate;
    private final EscapeAnalysisState escapeAnalysisState;

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
        final String name = param.getName(node);
        param.appendTo(name);
        param.attr("style", "filled");
        param.attr("fillcolor", nodeType(getConnectionGraph(param).getEscapeValue(node)).fillColor);
        param.nl();
        return name;
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
}
