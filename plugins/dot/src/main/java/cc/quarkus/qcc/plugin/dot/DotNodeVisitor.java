package cc.quarkus.qcc.plugin.dot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.graph.Action;
import cc.quarkus.qcc.graph.Add;
import cc.quarkus.qcc.graph.AddressOf;
import cc.quarkus.qcc.graph.And;
import cc.quarkus.qcc.graph.ArrayLength;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BitCast;
import cc.quarkus.qcc.graph.BlockEntry;
import cc.quarkus.qcc.graph.CastValue;
import cc.quarkus.qcc.graph.ClassCastErrorNode;
import cc.quarkus.qcc.graph.ClassNotFoundErrorNode;
import cc.quarkus.qcc.graph.ClassOf;
import cc.quarkus.qcc.graph.Clone;
import cc.quarkus.qcc.graph.Cmp;
import cc.quarkus.qcc.graph.ExtractElement;
import cc.quarkus.qcc.graph.ExtractInstanceField;
import cc.quarkus.qcc.graph.ExtractMember;
import cc.quarkus.qcc.graph.IsEq;
import cc.quarkus.qcc.graph.IsGe;
import cc.quarkus.qcc.graph.IsGt;
import cc.quarkus.qcc.graph.IsLe;
import cc.quarkus.qcc.graph.IsLt;
import cc.quarkus.qcc.graph.IsNe;
import cc.quarkus.qcc.graph.CommutativeBinaryValue;
import cc.quarkus.qcc.graph.ConstructorInvocation;
import cc.quarkus.qcc.graph.Convert;
import cc.quarkus.qcc.graph.CurrentThreadRead;
import cc.quarkus.qcc.graph.Div;
import cc.quarkus.qcc.graph.ElementOf;
import cc.quarkus.qcc.graph.Extend;
import cc.quarkus.qcc.graph.FunctionCall;
import cc.quarkus.qcc.graph.GlobalVariable;
import cc.quarkus.qcc.graph.Goto;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.InstanceFieldOf;
import cc.quarkus.qcc.graph.InstanceInvocation;
import cc.quarkus.qcc.graph.InstanceInvocationValue;
import cc.quarkus.qcc.graph.InstanceOf;
import cc.quarkus.qcc.graph.Jsr;
import cc.quarkus.qcc.graph.Load;
import cc.quarkus.qcc.graph.Max;
import cc.quarkus.qcc.graph.MemberOf;
import cc.quarkus.qcc.graph.Min;
import cc.quarkus.qcc.graph.Mod;
import cc.quarkus.qcc.graph.MonitorEnter;
import cc.quarkus.qcc.graph.MonitorExit;
import cc.quarkus.qcc.graph.MultiNewArray;
import cc.quarkus.qcc.graph.Multiply;
import cc.quarkus.qcc.graph.Narrow;
import cc.quarkus.qcc.graph.Neg;
import cc.quarkus.qcc.graph.New;
import cc.quarkus.qcc.graph.NewArray;
import cc.quarkus.qcc.graph.NoSuchMethodErrorNode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.NonCommutativeBinaryValue;
import cc.quarkus.qcc.graph.Or;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.PointerHandle;
import cc.quarkus.qcc.graph.ReferenceHandle;
import cc.quarkus.qcc.graph.Ret;
import cc.quarkus.qcc.graph.Return;
import cc.quarkus.qcc.graph.Rol;
import cc.quarkus.qcc.graph.Ror;
import cc.quarkus.qcc.graph.Select;
import cc.quarkus.qcc.graph.Shl;
import cc.quarkus.qcc.graph.Shr;
import cc.quarkus.qcc.graph.StackAllocation;
import cc.quarkus.qcc.graph.StaticField;
import cc.quarkus.qcc.graph.StaticInvocation;
import cc.quarkus.qcc.graph.StaticInvocationValue;
import cc.quarkus.qcc.graph.Store;
import cc.quarkus.qcc.graph.Sub;
import cc.quarkus.qcc.graph.Switch;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.Throw;
import cc.quarkus.qcc.graph.Truncate;
import cc.quarkus.qcc.graph.Try;
import cc.quarkus.qcc.graph.TypeIdOf;
import cc.quarkus.qcc.graph.UnaryValue;
import cc.quarkus.qcc.graph.Unreachable;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.ValueReturn;
import cc.quarkus.qcc.graph.Xor;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.DefinedConstantLiteral;
import cc.quarkus.qcc.graph.literal.FloatLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.MethodDescriptorLiteral;
import cc.quarkus.qcc.graph.literal.MethodHandleLiteral;
import cc.quarkus.qcc.graph.literal.ObjectLiteral;
import cc.quarkus.qcc.graph.literal.StringLiteral;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.TypeLiteral;
import cc.quarkus.qcc.graph.literal.UndefinedLiteral;
import cc.quarkus.qcc.graph.literal.ZeroInitializerLiteral;

/**
 * A node visitor which generates a GraphViz graph for a method or function body.
 */
public class DotNodeVisitor implements NodeVisitor<Appendable, String, String, String, String> {
    final BasicBlock entryBlock;
    final Map<Node, String> visited = new HashMap<>();
    private final Set<BasicBlock> blockQueued = ConcurrentHashMap.newKeySet();
    private final Queue<BasicBlock> blockQueue = new ArrayDeque<>();
    int counter;
    int bbCounter;
    boolean attr;
    boolean commaNeeded;
    Queue<String> dependencyList = new ArrayDeque();
    List<NodePair> bbConnections = new ArrayList<>(); // stores pair of Terminator, BlockEntry
    private final Queue<PhiValue> phiQueue = new ArrayDeque<>();

    private enum EdgeType {
        PHI_INCOMING ("green", "dashed"),
        PHI_INCOMING_UNREACHABLE ("brown", "dashed"),
        PHI_PINNED_NODE ("red", "dashed"),
        VALUE_DEPENDENCY ("blue", "dashed"),
        ORDER_DEPENDENCY ("black", "solid"),
        CONTROL_FLOW ("black", "bold");

        private String color;
        private String style;

        EdgeType(String color, String style) {
            this.color = color;
            this.style = style;
        }
        public String color() {
            return this.color;
        }
        public String style() {
            return this.style;
        }
    }

    private static class NodePair {
        private Node n1;
        private Node n2;
        private String label;

        NodePair(Node n1, Node n2) {
            this(n1, n2, "");
        }

        NodePair(Node n1, Node n2, String label) {
            this.n1 = n1;
            this.n2 = n2;
            this.label = label;
        }
    }

    DotNodeVisitor(final BasicBlock entryBlock) {
        this.entryBlock = entryBlock;
    }

    public String visitUnknown(final Appendable param, Value node) {
        throw new IllegalStateException("Visitor for node " + node.getClass() + " is not implemented");
    }

    public String visit(final Appendable param, final BlockEntry node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "doublecircle");
        attr(param, "fixedsize", "shape");
        String label = "";
        if (node.getPinnedBlock() == entryBlock) {
            label = "start";
        }
        attr(param, "label", label);
        nl(param);
        dependencyList.add(name);
        return name;
    }

    public String visit(final Appendable param, final Cmp node) {
        return node(param, "cmp", node);
    }

    public String visit(final Appendable param, final ElementOf node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "elementOf");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getIndex(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final GlobalVariable node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "global\n\n"+node.getVariableElement().getName());
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final InstanceFieldOf node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "field access\\n"+node.getVariableElement().getName());
        nl(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final InstanceInvocation node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "invoke" + node.getKind() + "\\n" + node.getInvocationTarget().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        return name;
    }

    public String visit(final Appendable param, final MemberOf node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "memberOf");
        nl(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final MonitorEnter node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "monitorenter");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final MonitorExit node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "monitorexit");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final PointerHandle node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "ptr");
        nl(param);
        addEdge(param, node, node.getPointerValue(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final ReferenceHandle node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "ref");
        nl(param);
        addEdge(param, node, node.getReferenceValue(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final StaticField node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "static field\\n" + node.getVariableElement().toString());
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final StaticInvocation node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "invokestatic\\n" + node.getInvocationTarget().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        return name;
    }

    public String visit(final Appendable param, final Goto node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "goto");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        appendTo(param, "}");
        nl(param);
        addBBConnection(param, node, node.getResumeTarget());
        return name;
    }

    public String visit(final Appendable param, final If node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "diamond");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "if");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        addEdge(param, node, node.getCondition(), EdgeType.VALUE_DEPENDENCY, "cond");
        appendTo(param, "}");
        nl(param);
        addBBConnection(param, node, node.getTrueBranch(), "true");
        addBBConnection(param, node, node.getFalseBranch(), "false");
        return name;
    }

    public String visit(final Appendable param, final Jsr node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "jsr");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        appendTo(param, "}");
        nl(param);
        addBBConnection(param, node, node.getResumeTarget(), "ret");
        addBBConnection(param, node, node.getJsrTarget(), "to");
        return name;
    }

    public String visit(final Appendable param, final Ret node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "ret");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        appendTo(param, "}");
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final Return node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "return");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        appendTo(param, "}");
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final Unreachable node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "unreachable");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        appendTo(param, "}");
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final Switch node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "diamond");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "switch");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        addEdge(param, node, node.getSwitchValue(), EdgeType.VALUE_DEPENDENCY, "on");
        appendTo(param, "}");
        nl(param);
        int cnt = node.getNumberOfValues();
        for (int i = 0; i < cnt; i++) {
            addBBConnection(param, node, node.getTargetForIndex(i), String.valueOf(node.getValueForIndex(i)));
        }
        return name;
    }

    public String visit(final Appendable param, final Throw node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "throw");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        addEdge(param, node, node.getThrownValue(), EdgeType.VALUE_DEPENDENCY);
        appendTo(param, "}");
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final Try node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "try");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDelegateOperation());
        processDependencyList(param);
        appendTo(param, "}");
        nl(param);
        addBBConnection(param, node, node.getResumeBranch(), "resume");
        addBBConnection(param, node, node.getExceptionHandlerBranch(), "exception");
        return name;
    }

    public String visit(final Appendable param, final ValueReturn node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "return");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        addEdge(param, node, node.getReturnValue(), EdgeType.VALUE_DEPENDENCY);
        appendTo(param, "}");
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final ClassCastErrorNode node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "class cast exception");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final NoSuchMethodErrorNode node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "no such method exception");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final ClassNotFoundErrorNode node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "class not found exception");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final Add node) {
        return node(param, "+", node);
    }

    public String visit(final Appendable param, final AddressOf node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "addr of");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final And node) {
        return node(param, "&", node);
    }

    public String visit(final Appendable param, final ArrayLength node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "array length");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final BitCast node) {
        return node(param, "bit cast", node);
    }

    public String visit(final Appendable param, final BlockLiteral node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "block");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getBlock().getBlockEntry(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final BooleanLiteral node) {
        return literal(param, String.valueOf(node.booleanValue()));
    }

    public String visit(final Appendable param, final ClassOf node) {
        return node(param, "classOf", node);
    }

    public String visit(final Appendable param, final Clone node) {
        return node(param, "clone", node);
    }

    public String visit(final Appendable param, final ConstructorInvocation node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "init\\n" + node.getInvocationTarget().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        return name;
    }

    public String visit(final Appendable param, final Convert node) {
        return node(param, "convert", node);
    }

    public String visit(final Appendable param, final CurrentThreadRead node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "read thread");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final DefinedConstantLiteral node) {
        return literal(param, "constant " + node.getName());
    }

    public String visit(final Appendable param, final Div node) {
        return node(param, "/", node);
    }

    public String visit(final Appendable param, final Extend node) {
        return node(param, "extend", node);
    }

    public String visit(Appendable param, ExtractElement node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "extracted element");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getIndex(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getArrayValue(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(Appendable param, ExtractInstanceField node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "extracted field \"" + node.getFieldElement().getName() + "\"");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getObjectValue(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(Appendable param, ExtractMember node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "extracted member \"" + node.getMember().getName() + "\"");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getCompoundValue(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final FloatLiteral node) {
        return literal(param, String.valueOf(node.doubleValue()));
    }

    public String visit(final Appendable param, final FunctionCall node) {
        String name = register(node);
        appendTo(param, name);
        Value callTarget = node.getCallTarget();
        if (callTarget instanceof SymbolLiteral) {
            attr(param, "label", "call @" + ((SymbolLiteral) callTarget).getName());
            attr(param, "fixedsize", "shape");
            nl(param);
        } else {
            attr(param, "label", "call");
            attr(param, "fixedsize", "shape");
            nl(param);
            addEdge(param, node, callTarget, EdgeType.VALUE_DEPENDENCY, "fn");
        }
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        return name;
    }

    public String visit(final Appendable param, final InstanceInvocationValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "invoke" + node.getKind() + "\\n" + node.getInvocationTarget().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        return name;
    }

    public String visit(final Appendable param, final InstanceOf node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "instanceof " + node.getCheckType().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY, "value");
        return name;
    }

    public String visit(final Appendable param, final IntegerLiteral node) {
        return literal(param, String.valueOf(node.longValue()));
    }

    public String visit(final Appendable param, final IsEq node) {
        return node(param, "eq", node);
    }

    public String visit(final Appendable param, final IsGe node) {
        return node(param, "≥", node);
    }

    public String visit(final Appendable param, final IsGt node) {
        return node(param, ">", node);
    }

    public String visit(final Appendable param, final IsLe node) {
        return node(param, "≤", node);
    }

    public String visit(final Appendable param, final IsLt node) {
        return node(param, "<", node);
    }

    public String visit(final Appendable param, final IsNe node) {
        return node(param, "neq", node);
    }

    public String visit(final Appendable param, final Load node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "load");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final MethodDescriptorLiteral node) {
        return literal(param, node.toString());
    }

    public String visit(final Appendable param, final MethodHandleLiteral node) {
        return literal(param, node.toString());
    }

    public String visit(final Appendable param, final Max node) {
        return node(param, "max", node);
    }

    public String visit(final Appendable param, final Min node) {
        return node(param, "min", node);
    }

    public String visit(final Appendable param, final Mod node) {
        return node(param, "%", node);
    }

    public String visit(final Appendable param, final MultiNewArray node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "new multi array\\n" + node.getArrayType().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        for (Value dimension : node.getDimensions()) {
            addEdge(param, node, dimension, EdgeType.VALUE_DEPENDENCY, "dim");
        }
        return name;
    }

    public String visit(final Appendable param, final Multiply node) {
        return node(param, "*", node);
    }

    public String visit(final Appendable param, final Narrow node) {
        return node(param, "narrow", node);
    }

    public String visit(final Appendable param, final Neg node) {
        return node(param, "neg", node);
    }

    public String visit(final Appendable param, final New node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "new\\n" + node.getType().getUpperBound().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final NewArray node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "new array\\n" + node.getArrayType().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getSize(), EdgeType.VALUE_DEPENDENCY, "size");
        return name;
    }

    public String visit(final Appendable param, final ZeroInitializerLiteral node) {
        return literal(param, "zero");
    }

    public String visit(final Appendable param, final ObjectLiteral node) {
        return literal(param, "object");
    }

    public String visit(final Appendable param, final Or node) {
        return node(param, "|", node);
    }

    public String visit(final Appendable param, final ParameterValue node) {
        int index = node.getIndex();
        StringBuilder b = new StringBuilder();
        b.append(node.getType()).append(' ').append("param").append('[').append(node.getLabel());
        if (index > 0) {
            b.append(index);
        }
        b.append(']');
        return literal(param, b.toString());
    }

    public String visit(final Appendable param, final PhiValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "phi");
        attr(param, "fixedsize", "shape");
        nl(param);
        phiQueue.add(node);
        return name;
    }

    public String visit(final Appendable param, final Rol node) {
        return node(param, "|<<", node);
    }

    public String visit(final Appendable param, final Ror node) {
        return node(param, "|>>", node);
    }

    public String visit(final Appendable param, final Select node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "diamond");
        attr(param, "label", "select");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getCondition(), EdgeType.VALUE_DEPENDENCY,"cond");
        addEdge(param, node, node.getTrueValue(), EdgeType.CONTROL_FLOW, "T");
        addEdge(param, node, node.getFalseValue(), EdgeType.CONTROL_FLOW, "F");
        return name;
    }

    public String visit(final Appendable param, final Shl node) {
        return node(param, "<<", node);
    }

    public String visit(final Appendable param, final Shr node) {
        return node(param, ">>", node);
    }

    public String visit(final Appendable param, final StackAllocation node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "alloca " + node.getType());
        attr(param, "fixedsize", "shape");
        addEdge(param, node, node.getCount(), EdgeType.VALUE_DEPENDENCY, "count");
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final StaticInvocationValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "invokestatic\\n" + node.getInvocationTarget().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        return name;
    }

    public String visit(final Appendable param, final Store node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "store");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getValue(), EdgeType.VALUE_DEPENDENCY, "value");
        return name;
    }

    public String visit(final Appendable param, final StringLiteral node) {
        return literal(param, '"' + node.getValue() + '"');
    }

    public String visit(final Appendable param, final SymbolLiteral node) {
        return literal(param, "@" + node.getName());
    }

    public String visit(final Appendable param, final Sub node) {
        return node(param, "-", node);
    }

    public String visit(final Appendable param, final Truncate node) {
        return node(param, "trunc", node);
    }

    public String visit(final Appendable param, final TypeIdOf node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "type of");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final TypeLiteral node) {
        return literal(param, node.getType().getUpperBound().toString());
    }

    public String visit(final Appendable param, final UndefinedLiteral node) {
        return literal(param, "undef");
    }

    public String visit(final Appendable param, final Xor node) {
        return node(param, "^", node);
    }

    private String literal(final Appendable param, final String label) {
        String name = nextName();
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", label);
        attr(param, "fixedsize", "shape");
        nl(param);
        return name;
    }

    private String node(final Appendable param, String kind, NonCommutativeBinaryValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", kind);
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getLeftInput(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getRightInput(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    private String node(final Appendable param, String kind, CommutativeBinaryValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", kind);
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getLeftInput(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getRightInput(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    private String node(final Appendable param, String kind, CastValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", kind + "→" + node.getType().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInput(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    private String node(final Appendable param, String kind, UnaryValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", kind);
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInput(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    private void attr(Appendable param, String name, String val) {
        if (! attr) {
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

    void quote(Appendable output, String orig) {
        appendTo(output, '"');
        int cp;
        for (int i = 0; i < orig.length(); i += Character.charCount(cp)) {
            cp = orig.codePointAt(i);
            if (cp == '"' || cp == '\\') {
                appendTo(output, '\\');
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

    void processDependency(Appendable param, Node node) {
        getNodeName(param, node);
    }

    void processDependencyList(Appendable param) {
        String name;
        while ((name = dependencyList.poll()) != null) {
            appendTo(param, name);
            if (!dependencyList.isEmpty()) {
                appendTo(param, " -> ");
            }
        }
        attr(param, "style", EdgeType.ORDER_DEPENDENCY.style());
        attr(param, "style", EdgeType.ORDER_DEPENDENCY.color());
        nl(param);
    }

    private void addBBConnection(Appendable param, Terminator from, BasicBlock to) {
        bbConnections.add(new NodePair(from, to.getBlockEntry()));
        addToQueue(to);
    }

    private void addBBConnection(Appendable param, Terminator from, BasicBlock to, String label) {
        bbConnections.add(new NodePair(from, to.getBlockEntry(), label));
        addToQueue(to);
    }

    private void addEdge(Appendable param, Node from, Node to, EdgeType edge) {
        if (to instanceof Value) {
            addEdge(param, from, (Value) to, edge);
        } else if (to instanceof ValueHandle) {
            addEdge(param, from, (ValueHandle)to, edge);
        } else {
            assert to instanceof Action;
            addEdge(param, from, (Action) to, edge);
        }
    }

    private void addEdge(Appendable param, Node from, Action to, EdgeType edge) {
        String fromName = getNodeName(param, from);
        String toName = getNodeName(param, to);
        appendTo(param, fromName);
        appendTo(param, " -> ");
        appendTo(param, toName);
        attr(param, "style", edge.style());
        attr(param, "color", edge.color());
        nl(param);
    }

    private void addEdge(Appendable param, Node from, ValueHandle to, EdgeType edge) {
        String fromName = getNodeName(param, from);
        String toName = getNodeName(param, to);
        appendTo(param, fromName);
        appendTo(param, " -> ");
        appendTo(param, toName);
        attr(param, "style", edge.style());
        attr(param, "color", edge.color());
        nl(param);
    }

    private void addEdge(Appendable param, Node from, Value to, EdgeType edge) {
        String fromName = getNodeName(param, from);
        String toName = getNodeName(param, to);
        appendTo(param, fromName);
        appendTo(param, " -> ");
        appendTo(param, toName);
        attr(param, "style", edge.style());
        attr(param, "color", edge.color());
        nl(param);
    }


    private void addEdge(Appendable param, Node from, Value to, EdgeType edge, String label) {
        String fromName = getNodeName(param, from);
        String toName = getNodeName(param, to);
        appendTo(param, fromName);
        appendTo(param, " -> ");
        appendTo(param, toName);
        attr(param, "label", label);
        attr(param, "style", edge.style());
        attr(param, "color", edge.color());
        nl(param);
    }

    private void nl(final Appendable param) {
        if (attr) {
            appendTo(param, ']');
            attr = false;
            commaNeeded = false;
        }
        appendTo(param, System.lineSeparator());
    }

    private String getNodeName(Appendable param, Node node) {
        if (node instanceof Value) {
            return getNodeName(param, (Value) node);
        } else if (node instanceof ValueHandle) {
            return getNodeName(param, (ValueHandle)node);
        } else if (node instanceof Action) {
            return getNodeName(param, (Action) node);
        } else {
            assert node instanceof Terminator;
            return getNodeName(param, (Terminator) node);
        }
    }

    private String getNodeName(Appendable param, Action node) {
        String name = visited.get(node);
        if (name == null) {
            name = node.accept(this, param);
        }
        return name;
    }

    private String getNodeName(Appendable param, Value node) {
        String name = visited.get(node);
        if (name == null) {
            name = node.accept(this, param);
        }
        return name;
    }

    private String getNodeName(Appendable param, ValueHandle node) {
        String name = visited.get(node);
        if (name == null) {
            name = node.accept(this, param);
        }
        return name;
    }

    private String getNodeName(Appendable param, Terminator node) {
        String name = visited.get(node);
        if (name == null) {
            name = node.accept(this, param);
        }
        return name;
    }

    private String register(final Node node) {
        String name = nextName();
        visited.put(node, name);
        return name;
    }

    static void appendTo(Appendable param, Object obj) {
        try {
            param.append(obj.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void appendTo(Appendable param, char c) {
        try {
            param.append(c);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String nextName() {
        return "n" + counter++;
    }

    private String nextBBName() {
        return "b" + bbCounter++;
    }

    void processPhiQueue(Appendable param) {
        for (PhiValue phi : phiQueue) {
            for (BasicBlock block : phi.getPinnedBlock().getIncoming()) {
                Value value = phi.getValueForInput(block.getTerminator());
                if (block.isReachable()) {
                    addEdge(param, phi, value, EdgeType.PHI_INCOMING);
                } else {
                    addEdge(param, phi, value, EdgeType.PHI_INCOMING_UNREACHABLE);
                }
            }
            addEdge(param, phi, phi.getPinnedBlock().getBlockEntry(), EdgeType.PHI_PINNED_NODE);
        }
    }

    void addToQueue(final BasicBlock block) {
        if (blockQueued.add(block)) {
            blockQueue.add(block);
        }
    }

    void connectBasicBlocks(Appendable param) {
        for (NodePair pair: bbConnections) {
            assert(visited.get(pair.n1) != null);
            assert(visited.get(pair.n2) != null);
            appendTo(param, visited.get(pair.n1));
            appendTo(param, " -> ");
            appendTo(param, visited.get(pair.n2));
            attr(param, "label", pair.label);
            attr(param, "style", EdgeType.CONTROL_FLOW.style());
            attr(param, "color", EdgeType.CONTROL_FLOW.color());
            nl(param);
        }
    }

    public void process(final Appendable param) {
        addToQueue(entryBlock);
        BasicBlock block;
        while ((block = blockQueue.poll()) != null) {
            String bbName = nextBBName();
            appendTo(param, "subgraph cluster_" + bbName + " {");
            nl(param);
            appendTo(param, "label = \"" + bbName + "\";");
            nl(param);
            getNodeName(param, block.getTerminator());
        }
        connectBasicBlocks(param);
        processPhiQueue(param);
    }
}
