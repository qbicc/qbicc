package cc.quarkus.qcc.graph.dot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import cc.quarkus.qcc.graph.Action;
import cc.quarkus.qcc.graph.ActionVisitor;
import cc.quarkus.qcc.graph.Add;
import cc.quarkus.qcc.graph.And;
import cc.quarkus.qcc.graph.ArrayElementRead;
import cc.quarkus.qcc.graph.ArrayElementWrite;
import cc.quarkus.qcc.graph.ArrayLength;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BinaryValue;
import cc.quarkus.qcc.graph.BitCast;
import cc.quarkus.qcc.graph.BlockEntry;
import cc.quarkus.qcc.graph.CastValue;
import cc.quarkus.qcc.graph.Catch;
import cc.quarkus.qcc.graph.ClassCastErrorNode;
import cc.quarkus.qcc.graph.CmpEq;
import cc.quarkus.qcc.graph.CmpGe;
import cc.quarkus.qcc.graph.CmpGt;
import cc.quarkus.qcc.graph.CmpLe;
import cc.quarkus.qcc.graph.CmpLt;
import cc.quarkus.qcc.graph.CmpNe;
import cc.quarkus.qcc.graph.ConstructorInvocation;
import cc.quarkus.qcc.graph.Convert;
import cc.quarkus.qcc.graph.Div;
import cc.quarkus.qcc.graph.Extend;
import cc.quarkus.qcc.graph.FieldRead;
import cc.quarkus.qcc.graph.FieldWrite;
import cc.quarkus.qcc.graph.Goto;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.InstanceFieldRead;
import cc.quarkus.qcc.graph.InstanceFieldWrite;
import cc.quarkus.qcc.graph.InstanceInvocation;
import cc.quarkus.qcc.graph.InstanceInvocationValue;
import cc.quarkus.qcc.graph.InstanceOperation;
import cc.quarkus.qcc.graph.Invocation;
import cc.quarkus.qcc.graph.Jsr;
import cc.quarkus.qcc.graph.Mod;
import cc.quarkus.qcc.graph.MonitorEnter;
import cc.quarkus.qcc.graph.MonitorExit;
import cc.quarkus.qcc.graph.Multiply;
import cc.quarkus.qcc.graph.Narrow;
import cc.quarkus.qcc.graph.Neg;
import cc.quarkus.qcc.graph.New;
import cc.quarkus.qcc.graph.NewArray;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.Or;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Ret;
import cc.quarkus.qcc.graph.Return;
import cc.quarkus.qcc.graph.Rol;
import cc.quarkus.qcc.graph.Ror;
import cc.quarkus.qcc.graph.Select;
import cc.quarkus.qcc.graph.Shl;
import cc.quarkus.qcc.graph.Shr;
import cc.quarkus.qcc.graph.StaticFieldRead;
import cc.quarkus.qcc.graph.StaticFieldWrite;
import cc.quarkus.qcc.graph.StaticInvocation;
import cc.quarkus.qcc.graph.StaticInvocationValue;
import cc.quarkus.qcc.graph.Sub;
import cc.quarkus.qcc.graph.Switch;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.TerminatorVisitor;
import cc.quarkus.qcc.graph.ThisValue;
import cc.quarkus.qcc.graph.Throw;
import cc.quarkus.qcc.graph.Truncate;
import cc.quarkus.qcc.graph.Try;
import cc.quarkus.qcc.graph.TypeIdOf;
import cc.quarkus.qcc.graph.UnaryValue;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueReturn;
import cc.quarkus.qcc.graph.ValueVisitor;
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
import cc.quarkus.qcc.graph.literal.ValueArrayTypeIdLiteral;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * A generator for GraphViz-style {@code dot} representations of the graph of a method, given its entry block.
 */
public class GraphDotGenerator {
    public static StringBuilder graph(final String label, BasicBlock entryBlock, StringBuilder target) {
        Set<BasicBlock> reachable = entryBlock.calculateReachableBlocks();
        target.append("digraph {\n");
        target.append("rankdir=BT;\n");
        target.append("label=\"").append("\";\n");
        entryBlock.getTerminator().accept(new OuterVisitor(target), new Visitor(target, reachable));
        target.append("}\n");
        return target;
    }

    /**
     * An entry point to print a method from the command line, using a basic environment.
     *
     * @param args the arguments
     */
    public static void main(String[] args) throws IOException {
        Iterator<String> it = Arrays.asList(args).iterator();
        List<Path> jarPath = new ArrayList<>();
        String className = null;
        int methodIdx = -1;
        while (it.hasNext()) {
            String arg = it.next();
            if (arg.startsWith("-")) {
                if (arg.equals("--classpath")) {
                    String cp = it.next();
                    String[] jars = cp.split(Pattern.quote(File.pathSeparator));
                    for (String jar : jars) {
                        jarPath.add(Path.of(jar));
                    }
                } else {
                    throw new IllegalArgumentException("Unrecognized option \"" + arg + "\"");
                }
            } else if (className == null) {
                className = arg;
            } else if (methodIdx == -1) {
                methodIdx = Integer.parseInt(arg);
            } else {
                throw new IllegalArgumentException("Extra argument \"" + arg + "\"");
            }
        }
        if (className == null) {
            throw new IllegalArgumentException("No class name given");
        }
        if (methodIdx == -1) {
            throw new IllegalArgumentException("No method index given");
        }
        try (ClassContext.Basic ctxt = ClassContext.createBasic(jarPath)) {
            DefinedTypeDefinition definedType = ctxt.findDefinedType(className);
            ValidatedTypeDefinition validated = definedType.validate();
            if (methodIdx < 0 || methodIdx >= validated.getMethodCount()) {
                System.err.println("Method index is out of range");
                System.exit(1);
            }
            MethodElement method = validated.getMethod(methodIdx);
            MethodHandle methodHandle = method.getMethodBody();
            if (methodHandle == null) {
                System.err.println("Method has no body");
                System.exit(1);
            }
            MethodBody methodBody = methodHandle.createMethodBody();
            String s = graph(method.getName() + ":" + method.getDescriptor(), methodBody.getEntryBlock(), new StringBuilder()).toString();
            System.out.println(s);
        }
    }

    static final class OuterVisitor implements TerminatorVisitor<Visitor, Void> {
        final StringBuilder target;
        final Set<Terminator> visited = new HashSet<>();

        OuterVisitor(final StringBuilder target) {
            this.target = target;
        }

        public Void visitUnknown(final Visitor param, final Terminator node) {
            if (visited.add(node)) {
                String from = param.getNodeName(node);
                int cnt = node.getSuccessorCount();
                for (int i = 0; i < cnt; i ++) {
                    Terminator next = node.getSuccessor(i).getTerminator();
                    String to = param.getNodeName(next);
                    // add edge
                    target.append(from).append(" -> ").append(to).append(" [").append("style=bold").append("]\n");
                    next.accept(this, param);
                }
            }
            return null;
        }
    }

    static final class Visitor implements TerminatorVisitor<Void, String>, ValueVisitor<Void, String>, ActionVisitor<Void, String>, NodeVisitor<Void, String> {

        final Map<Node, String> visited = new HashMap<>();
        final Set<BasicBlock> reachable;
        final StringBuilder target;
        final AtomicInteger cnt = new AtomicInteger(1);

        Visitor(final StringBuilder target, final Set<BasicBlock> reachable) {
            this.target = target;
            this.reachable = reachable;
        }

        public String visitUnknown(final Void param, final Node node) {
            throw new UnsupportedOperationException("Unsupported node type: " + node.getClass());
        }

        public String visitUnknown(final Void param, final Action node) {
            return visitUnknown(param, (Node) node);
        }

        public String visitUnknown(final Void param, final Terminator node) {
            return visitUnknown(param, (Node) node);
        }

        public String visitUnknown(final Void param, final Value node) {
            return visitUnknown(param, (Node) node);
        }

        public String visit(final Void param, final Action node) {
            return node.accept((ActionVisitor<Void, String>) this, null);
        }

        public String visit(final Void param, final Terminator node) {
            return node.accept((TerminatorVisitor<Void, String>) this, null);
        }

        public String visit(final Void param, final Value node) {
            return node.accept((ValueVisitor<Void, String>) this, null);
        }

        // terminators

        public String visit(final Void param, final Goto node) {
            return node("goto", node);
        }

        public String visit(final Void param, final If node) {
            return node("if", node);
        }

        public String visit(final Void param, final Jsr node) {
            return node("jsr", node);
        }

        public String visit(final Void param, final Ret node) {
            return node("ret", node);
        }

        public String visit(final Void param, final Return node) {
            return node("return", node);
        }

        public String visit(final Void param, final Switch node) {
            return node("switch", node);
        }

        public String visit(final Void param, final Throw node) {
            return node("throw", node);
        }

        public String visit(final Void param, final Try node) {
            return node("try", node);
        }

        public String visit(final Void param, final ValueReturn node) {
            return node("return", node);
        }

        // errors

        public String visit(final Void param, final ClassCastErrorNode node) {
            return node("cast error", node);
        }

        // binary ops

        public String visit(final Void param, final Add node) {
            return node("+", node);
        }

        public String visit(final Void param, final And node) {
            return node("&", node);
        }

        public String visit(final Void param, final CmpEq node) {
            return node("=", node);
        }

        public String visit(final Void param, final CmpGe node) {
            return node("≥", node);
        }

        public String visit(final Void param, final CmpGt node) {
            return node(">", node);
        }

        public String visit(final Void param, final CmpLe node) {
            return node("≤", node);
        }

        public String visit(final Void param, final CmpLt node) {
            return node("<", node);
        }

        public String visit(final Void param, final CmpNe node) {
            return node("≠", node);
        }

        public String visit(final Void param, final Div node) {
            return node("/", node);
        }

        public String visit(final Void param, final Mod node) {
            return node("%", node);
        }

        public String visit(final Void param, final Multiply node) {
            return node("*", node);
        }

        public String visit(final Void param, final Or node) {
            return node("|", node);
        }

        public String visit(final Void param, final Rol node) {
            return node("|<<", node);
        }

        public String visit(final Void param, final Ror node) {
            return node("|>>", node);
        }

        public String visit(final Void param, final Shl node) {
            return node("<<", node);
        }

        public String visit(final Void param, final Shr node) {
            return node(">>", node);
        }

        public String visit(final Void param, final Sub node) {
            return node("-", node);
        }

        public String visit(final Void param, final Xor node) {
            return node("^", node);
        }

        // unary ops

        public String visit(final Void param, final Neg node) {
            return node("neg", node);
        }

        // literal

        public String visit(final Void param, final BlockLiteral node) {
            return node(node);
        }

        public String visit(final Void param, final BooleanLiteral node) {
            return node(node);
        }

        public String visit(final Void param, final ClassTypeIdLiteral node) {
            return node(node);
        }

        public String visit(final Void param, final FloatLiteral node) {
            return node(node);
        }

        public String visit(final Void param, final IntegerLiteral node) {
            return node(node);
        }

        public String visit(final Void param, final InterfaceTypeIdLiteral node) {
            return node(node);
        }

        public String visit(final Void param, final NullLiteral node) {
            return node(node);
        }

        public String visit(final Void param, final ObjectLiteral node) {
            return node(node);
        }

        public String visit(final Void param, final ReferenceArrayTypeIdLiteral node) {
            return node(node);
        }

        public String visit(final Void param, final StringLiteral node) {
            return node(node);
        }

        public String visit(final Void param, final ValueArrayTypeIdLiteral node) {
            return node(node);
        }

        // ternary

        public String visit(final Void param, final Select node) {
            return null;
        }

        // parameter

        public String visit(final Void param, final ParameterValue node) {
            return node(node);
        }

        public String visit(final Void param, final ThisValue node) {
            return node(node);
        }

        public String visit(final Void param, final Catch node) {
            return node(node);
        }

        public String visit(final Void param, final PhiValue node) {
            return node(node);
        }

        // transform

        public String visit(final Void param, final BitCast node) {
            return node("cast", node);
        }

        public String visit(final Void param, final Convert node) {
            return node("conv", node);
        }

        public String visit(final Void param, final Extend node) {
            return node("ext", node);
        }

        public String visit(final Void param, final Truncate node) {
            return node("trunc", node);
        }

        public String visit(final Void param, final Narrow node) {
            return node("narrow", node);
        }

        // query

        public String visit(final Void param, final ArrayLength node) {
            return node("length", node);
        }

        public String visit(final Void param, final TypeIdOf node) {
            return node("typeOf", node);
        }

        // memory read

        public String visit(final Void param, final ArrayElementRead node) {
            return node(node);
        }

        public String visit(final Void param, final InstanceFieldRead node) {
            return node(node);
        }

        public String visit(final Void param, final StaticFieldRead node) {
            return node(node);
        }

        // invocation

        public String visit(final Void param, final ConstructorInvocation node) {
            return node("init", (Invocation) node);
        }

        public String visit(final Void param, final InstanceInvocation node) {
            return node("invoke" + node.getKind(), (Invocation) node);
        }

        public String visit(final Void param, final InstanceInvocationValue node) {
            return node("invoke" + node.getKind(), (Invocation) node);
        }

        public String visit(final Void param, final StaticInvocation node) {
            return node("invokestatic", node);
        }

        public String visit(final Void param, final StaticInvocationValue node) {
            return node("invokestatic", node);
        }

        // alloc

        public String visit(final Void param, final New node) {
            return node(node);
        }

        public String visit(final Void param, final NewArray node) {
            return node(node);
        }

        // memory write

        public String visit(final Void param, final ArrayElementWrite node) {
            return node(node);
        }

        public String visit(final Void param, final StaticFieldWrite node) {
            return node(node);
        }

        public String visit(final Void param, final InstanceFieldWrite node) {
            return node(node);
        }

        // entry

        public String visit(final Void param, final BlockEntry node) {
            return node(node);
        }

        // monitor

        public String visit(final Void param, final MonitorEnter node) {
            return node("monitorenter", node);
        }

        public String visit(final Void param, final MonitorExit node) {
            return node("monitorexit", node);
        }

        String getNodeName(Node node) {
            String name = visited.get(node);
            if (name != null) {
                return name;
            }
            return node.accept(this, null);
        }

        <N extends Action & InstanceOperation> String node(String label, N node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", label).append(',');
            appendAttr("shape", "rectangle").append(',');
            appendAttr("fixedsize", "shape").append(',');
            appendAttr("style", "filled");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(String label, Terminator node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", label).append(',');
            appendAttr("shape", "rectangle").append(',');
            appendAttr("fixedsize", "shape").append(',');
            appendAttr("style", "filled,rounded");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(String label, BinaryValue node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", label).append(',');
            appendAttr("shape", "circle").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(String label, UnaryValue node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", label).append(',');
            appendAttr("shape", "circle").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(String label, CastValue node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", label + " to " + node.getType()).append(',');
            appendAttr("shape", "circle").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        <V extends InstanceOperation & Value> String node(String label, V node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", label).append(',');
            appendAttr("shape", "circle").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        private String register(final Node node) {
            String name = "n" + cnt.getAndIncrement();
            visited.put(node, name);
            return name;
        }

        String node(String label, Invocation node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", label + "\\n" + node.getInvocationTarget()).append(',');
            appendAttr("shape", "hexagon").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(ArrayElementRead node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "read array").append(',');
            appendAttr("shape", "trapezium").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(ArrayElementWrite node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "write array").append(',');
            appendAttr("shape", "invtrapezium").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(FieldRead node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "read field \"" + node.getFieldElement().getName() + "\"\\nof " + node.getFieldElement().getEnclosingType().getInternalName()).append(',');
            appendAttr("shape", "trapezium").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(FieldWrite node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "write field \"" + node.getFieldElement().getName() + "\"\\nof " + node.getFieldElement().getEnclosingType().getInternalName()).append(',');
            appendAttr("shape", "invtrapezium").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(New node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "new " + node.getInstanceTypeId()).append(',');
            appendAttr("shape", "house").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(NewArray node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "new array " + node.getElementTypeId()).append(',');
            appendAttr("shape", "house").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            // add edges
            addBasicDeps(name, node);
            addValueDeps(name, node);
            return name;
        }

        String node(Literal node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", node.toString()).append(',');
            appendAttr("shape", "Mcircle").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            return name;
        }

        String node(ParameterValue node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "arg" + node.getIndex()).append(',');
            appendAttr("shape", "Msquare").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            return name;
        }

        String node(ThisValue node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "this").append(',');
            appendAttr("shape", "Msquare").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            return name;
        }

        String node(Catch node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "catch").append(',');
            appendAttr("shape", "trapezium").append(',');
            appendAttr("style", "diagonals").append(',');
            appendAttr("fixedsize", "shape");
            target.append(']');
            target.append('\n');
            return name;
        }

        String node(PhiValue node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "φ").append(',');
            appendAttr("fixedsize", "shape").append(',');
            appendAttr("shape", "circle");
            target.append(']');
            target.append('\n');
            for (BasicBlock incoming : reachable) {
                Value value = node.getValueForBlock(incoming);
                if (value != null) {
                    String valueName = getNodeName(value);
                    target.append(name).append(" -> ").append(valueName);
                    target.append('\n');
                }
            }
            return name;
        }

        String node(BlockEntry node) {
            String name = register(node);
            target.append(name).append(' ').append('[');
            appendAttr("label", "").append(',');
            appendAttr("shape", "point");
            target.append(']');
            target.append('\n');
            return name;
        }

        void addValueDeps(String from, Node fromNode) {
            int cnt = fromNode.getValueDependencyCount();
            for (int i = 0; i < cnt; i ++) {
                String to = getNodeName(fromNode.getValueDependency(i));
                target.append(from).append(" -> ").append(to).append('\n');
            }
        }

        void addBasicDeps(String from, Node fromNode) {
            int cnt = fromNode.getBasicDependencyCount();
            for (int i = 0; i < cnt; i ++) {
                String to = getNodeName(fromNode.getBasicDependency(i));
                target.append(from).append(" -> ").append(to);
                target.append(" [");
                appendAttr("style", "dotted");
                target.append(']');
                target.append('\n');
            }
        }

        StringBuilder appendAttr(String name, String value) {
            target.append(name).append('=').append('"');
            int cp;
            for (int i = 0; i < value.length(); i += Character.charCount(cp)) {
                cp = value.codePointAt(i);
                if (cp == '"') {
                    target.append('\\');
                }
                target.appendCodePoint(cp);
            }
            return target.append('"');
        }
    }
}
