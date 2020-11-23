package cc.quarkus.qcc.graph.opt;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.Add;
import cc.quarkus.qcc.graph.And;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BinaryValue;
import cc.quarkus.qcc.graph.Catch;
import cc.quarkus.qcc.graph.CmpEq;
import cc.quarkus.qcc.graph.CmpGe;
import cc.quarkus.qcc.graph.CmpGt;
import cc.quarkus.qcc.graph.CmpLe;
import cc.quarkus.qcc.graph.CmpLt;
import cc.quarkus.qcc.graph.CmpNe;
import cc.quarkus.qcc.graph.Div;
import cc.quarkus.qcc.graph.Mod;
import cc.quarkus.qcc.graph.Multiply;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.NonCommutativeBinaryValue;
import cc.quarkus.qcc.graph.Or;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.Rol;
import cc.quarkus.qcc.graph.Ror;
import cc.quarkus.qcc.graph.Shl;
import cc.quarkus.qcc.graph.Shr;
import cc.quarkus.qcc.graph.Sub;
import cc.quarkus.qcc.graph.ThisValue;
import cc.quarkus.qcc.graph.Xor;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.FloatLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.InterfaceTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.graph.literal.ObjectLiteral;
import cc.quarkus.qcc.graph.literal.ReferenceArrayTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.StringLiteral;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.ValueArrayTypeIdLiteral;

/**
 * A visitor set that generates {@code .dot} files for each method and function in the output directory of the project.
 *
 * Note: WIP
 */
public class DotGenerator<C, V, A, T> implements NodeVisitor.Delegating<C, V, A, T> {

    private final CompilationContext context;
    private final NodeVisitor<C, V, A, T> delegate;

    private final Map<Node, String> labels = new HashMap<>();
    private final Map<BasicBlock, String> subGraphs = new HashMap<>();
    private final StringBuilder output = new StringBuilder();

    private int cnt = 1;
    private boolean comma = false;

    public DotGenerator(final CompilationContext context, final NodeVisitor<C, V, A, T> delegate) {
        this.context = context;
        this.delegate = delegate;
        output.append("digraph {\n");
        output.append("graph [");
        attr("rankdir", "BT");
        comma = false;
        output.append("]\n");
    }

    public NodeVisitor<C, V, A, T> getDelegateNodeVisitor() {
        return delegate;
    }

    public V visit(final C param, final Add node) {
        return node("+", node, delegate.visit(param, node));
    }

    public V visit(final C param, final And node) {
        return node("&", node, delegate.visit(param, node));
    }

    public V visit(final C param, final CmpEq node) {
        return node("=", node, delegate.visit(param, node));
    }

    public V visit(final C param, final CmpGe node) {
        return node("≥", node, delegate.visit(param, node));
    }

    public V visit(final C param, final CmpGt node) {
        return node(">", node, delegate.visit(param, node));
    }

    public V visit(final C param, final CmpLe node) {
        return node("≤", node, delegate.visit(param, node));
    }

    public V visit(final C param, final CmpLt node) {
        return node("<", node, delegate.visit(param, node));
    }

    public V visit(final C param, final CmpNe node) {
        return node("≠", node, delegate.visit(param, node));
    }

    public V visit(final C param, final Div node) {
        return node("/", node, delegate.visit(param, node));
    }

    public V visit(final C param, final Mod node) {
        return node("%", node, delegate.visit(param, node));
    }

    public V visit(final C param, final Multiply node) {
        return node("*", node, delegate.visit(param, node));
    }

    public V visit(final C param, final Or node) {
        return node("|", node, delegate.visit(param, node));
    }

    public V visit(final C param, final Rol node) {
        return node("|<<", node, delegate.visit(param, node));
    }

    public V visit(final C param, final Ror node) {
        return node("|>>", node, delegate.visit(param, node));
    }

    public V visit(final C param, final Shl node) {
        return node("<<", node, delegate.visit(param, node));
    }

    public V visit(final C param, final Shr node) {
        return node(">>", node, delegate.visit(param, node));
    }

    public V visit(final C param, final Sub node) {
        return node("-", node, delegate.visit(param, node));
    }

    public V visit(final C param, final Xor node) {
        return node("^", node, delegate.visit(param, node));
    }

    // constants

    public V visit(final C param, final BlockLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final BooleanLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final ClassTypeIdLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final FloatLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final IntegerLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final InterfaceTypeIdLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final NullLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final ObjectLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final ReferenceArrayTypeIdLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final StringLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final SymbolLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final ValueArrayTypeIdLiteral node) {
        return node(node, delegate.visit(param, node));
    }

    public V visit(final C param, final Catch node) {
        V res = delegate.visit(param, node);
        String name = register(node);
        startNode(name);
        attr("shape", "Msquare");
        attr("label", "<catch>");
        attr("portPos", "s");
        endNode();
        return res;
    }

    public V visit(final C param, final ParameterValue node) {
        V res = delegate.visit(param, node);
        // do not coalesce by default
        String name = nextName();
        startNode(name);
        attr("shape", "Msquare");
        attr("label", "arg" + node.getIndex());
        attr("portPos", "s");
        endNode();
        return res;
    }

    public V visit(final C param, final ThisValue node) {
        V res = delegate.visit(param, node);
        // do not coalesce by default
        String name = nextName();
        startNode(name);
        attr("shape", "Msquare");
        attr("label", "this");
        attr("portPos", "s");
        endNode();
        return res;
    }



    // helpers

    V node(String label, NonCommutativeBinaryValue node, V passThru) {
        String name = register(node);
        startNode(name);
        attr("shape", "circle");
        attr("label", label);
        attr("portPos", "s");
        endNode();
        startEdge(name, getLabel(node.getLeftInput()));
        attr("label", "lhs");
        attr("tailport", "w");
        endEdge();
        startEdge(name, getLabel(node.getRightInput()));
        attr("label", "rhs");
        attr("tailport", "e");
        endEdge();
        return passThru;
    }

    V node(String label, BinaryValue node, V passThru) {
        String name = register(node);
        startNode(name);
        attr("shape", "circle");
        attr("label", label);
        attr("portPos", "s");
        endNode();
        edge(name, getLabel(node.getLeftInput()));
        edge(name, getLabel(node.getRightInput()));
        return passThru;
    }

    V node(Literal lit, V passThru) {
        // do not coalesce by default
        String name = nextName();
        startNode(name);
        attr("shape", "Mcircle");
        attr("label", lit.getType().toString() + " " + lit.toString());
        attr("portPos", "s");
        endNode();
        return passThru;
    }

    void startNode(String name) {
        output.append(name).append(' ').append('[');
    }

    void endNode() {
        output.append(']').append('\n');
        comma = false;
    }

    void startEdge(String from, String to) {
        output.append(from).append(" -> ").append("to");
        output.append(' ');
        output.append('[');
    }

    void endEdge() {
        output.append(']').append('\n');
        comma = false;
    }

    void edge(String from, String to) {
        startEdge(from, to);
        endEdge();
    }

    void edge(String from, String to, String label) {
        startEdge(from, to);
        attr("label", label);
        endEdge();
    }

    void attr(String name, String val) {
        if (comma) {
            output.append(',');
        } else {
            comma = true;
        }
        output.append(name).append('=');
        quote(val);
    }

    void quote(String orig) {
        output.append('"');
        int cp;
        for (int i = 0; i < orig.length(); i += Character.charCount(cp)) {
            cp = orig.codePointAt(i);
            if (cp == '"' || cp == '\\') {
                output.append('\\');
            }
            output.appendCodePoint(cp);
        }
        output.append('"');
    }

    private String getLabel(final Node node) {
        String val = labels.get(node);
        if (val == null) {
            context.warning("dot Generation: Failed to find a dependency node for \"%s\" (graph may be invalid)", node);
            return "none";
        }
        return val;
    }

    private String register(final Node node) {
        String name = nextName();
        labels.put(node, name);
        return name;
    }

    private String nextName() {
        return "n" + cnt++;
    }
}