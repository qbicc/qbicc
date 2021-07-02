
package org.qbicc.plugin.opt.ea;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.qbicc.graph.New;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.plugin.dot.DotGenerationContext;

public final class EscapeAnalysisDotVisitor implements NodeVisitor.Delegating<Appendable, String, String, String, String> {
    private final DotGenerationContext dtxt;
    private final NodeVisitor<Appendable, String, String, String, String> delegate;
    private final ConnectionGraph connectionGraph;
    private boolean attr;
    private boolean commaNeeded;

    public EscapeAnalysisDotVisitor(DotGenerationContext dtxt, NodeVisitor<Appendable, String, String, String, String> delegate) {
        this.dtxt = dtxt;
        this.delegate = delegate;
        this.connectionGraph = EscapeAnalysisState.get(dtxt.ctxt).getConnectionGraph(dtxt.element);
    }

    @Override
    public NodeVisitor<Appendable, String, String, String, String> getDelegateNodeVisitor() {
        return delegate;
    }

    @Override
    public String visit(Appendable param, New node) {
        final String name = dtxt.visited.get(node);
        appendTo(param, name);
        attr(param, "style", "filled");
        attr(param, "fillcolor", nodeType(connectionGraph.getEscapeValue(node)).fillColor);
        nl(param);
        return name;
    }

    private NodeType nodeType(EscapeValue value) {
        return switch (value) {
            case GLOBAL_ESCAPE -> NodeType.GLOBAL_ESCAPE;
            case ARG_ESCAPE -> NodeType.ARG_ESCAPE;
            case NO_ESCAPE -> NodeType.NO_ESCAPE;
            case UNKNOWN -> NodeType.UNKNOWN;
        };
    }

    // TODO copied from DotNodeVisitor
    private void attr(Appendable param, String name, String val) {
        if (!attr) {
            attr = true;
            appendTo(param, " [");
        }
        if (commaNeeded) {
            appendTo(param, ',');
        } else {
            commaNeeded = true;
        }
        appendTo(param, name);
        appendTo(param, '=');
        quote(param, val);
    }

    // TODO copied from DotNodeVisitor
    static void quote(Appendable output, String orig) {
        appendTo(output, '"');
        int cp;
        for (int i = 0; i < orig.length(); i += Character.charCount(cp)) {
            cp = orig.codePointAt(i);
            if (cp == '"') {
                appendTo(output, '\\');
            } else if (cp == '\\') {
                if ((i + 1) == orig.length() ||
                    "nlrGNTHE".indexOf(orig.codePointAt(i + 1)) == -1) {
                    appendTo(output, '\\');
                }
            }
            if (Character.charCount(cp) == 1) {
                appendTo(output, (char) cp);
            } else {
                appendTo(output, Character.highSurrogate(cp));
                appendTo(output, Character.lowSurrogate(cp));
            }
        }
        appendTo(output, '"');
    }
    // TODO copied from DotNodeVisitor

    private void nl(final Appendable param) {
        if (attr) {
            appendTo(param, ']');
            attr = false;
            commaNeeded = false;
        }
        appendTo(param, System.lineSeparator());
    }
    // TODO copied from DotNodeVisitor

    static void appendTo(Appendable param, Object obj) {
        try {
            param.append(obj.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
