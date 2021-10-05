package org.qbicc.plugin.dot;

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

import io.smallrye.common.constraint.Assert;
import org.qbicc.graph.Action;
import org.qbicc.graph.Add;
import org.qbicc.graph.AddressOf;
import org.qbicc.graph.And;
import org.qbicc.graph.ArrayLength;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.ByteSwap;
import org.qbicc.graph.Call;
import org.qbicc.graph.CallNoReturn;
import org.qbicc.graph.CallNoSideEffects;
import org.qbicc.graph.CastValue;
import org.qbicc.graph.ClassCastErrorNode;
import org.qbicc.graph.ClassInitCheck;
import org.qbicc.graph.ClassNotFoundErrorNode;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.Clone;
import org.qbicc.graph.Cmp;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.CmpG;
import org.qbicc.graph.CmpL;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.DataDeclarationHandle;
import org.qbicc.graph.DataHandle;
import org.qbicc.graph.DebugAddressDeclaration;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.ExtractElement;
import org.qbicc.graph.ExtractInstanceField;
import org.qbicc.graph.ExtractMember;
import org.qbicc.graph.Fence;
import org.qbicc.graph.FunctionDeclarationHandle;
import org.qbicc.graph.FunctionElementHandle;
import org.qbicc.graph.FunctionHandle;
import org.qbicc.graph.GetAndAdd;
import org.qbicc.graph.InsertElement;
import org.qbicc.graph.InsertMember;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.InvokeNoReturn;
import org.qbicc.graph.IsEq;
import org.qbicc.graph.IsGe;
import org.qbicc.graph.IsGt;
import org.qbicc.graph.IsLe;
import org.qbicc.graph.IsLt;
import org.qbicc.graph.IsNe;
import org.qbicc.graph.CommutativeBinaryValue;
import org.qbicc.graph.Convert;
import org.qbicc.graph.CurrentThreadRead;
import org.qbicc.graph.Div;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.Extend;
import org.qbicc.graph.GlobalVariable;
import org.qbicc.graph.Goto;
import org.qbicc.graph.If;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.InstanceOf;
import org.qbicc.graph.Jsr;
import org.qbicc.graph.Load;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.Max;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.Min;
import org.qbicc.graph.Mod;
import org.qbicc.graph.MonitorEnter;
import org.qbicc.graph.MonitorExit;
import org.qbicc.graph.MultiNewArray;
import org.qbicc.graph.Multiply;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.Neg;
import org.qbicc.graph.New;
import org.qbicc.graph.NewArray;
import org.qbicc.graph.NoSuchMethodErrorNode;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.NonCommutativeBinaryValue;
import org.qbicc.graph.NotNull;
import org.qbicc.graph.OffsetOfField;
import org.qbicc.graph.Or;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.Ret;
import org.qbicc.graph.Return;
import org.qbicc.graph.Rol;
import org.qbicc.graph.Ror;
import org.qbicc.graph.Select;
import org.qbicc.graph.Shl;
import org.qbicc.graph.Shr;
import org.qbicc.graph.StackAllocation;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Store;
import org.qbicc.graph.Sub;
import org.qbicc.graph.Switch;
import org.qbicc.graph.TailCall;
import org.qbicc.graph.TailInvoke;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Throw;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.TypeIdOf;
import org.qbicc.graph.UnaryValue;
import org.qbicc.graph.Unreachable;
import org.qbicc.graph.UnsafeHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueReturn;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.Xor;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ConstantLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.MethodDescriptorLiteral;
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;

/**
 * A node visitor which generates a GraphViz graph for a method or function body.
 */
public class DotNodeVisitor implements NodeVisitor<Appendable, String, String, String, String> {
    final BasicBlock entryBlock;
    final Map<Node, String> visited = new HashMap<>();
    private final Set<BasicBlock> blockQueued = ConcurrentHashMap.newKeySet();
    private final Queue<BasicBlock> blockQueue = new ArrayDeque<>();
    int depth;
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
        VALUE_DEPENDENCY ("blue", "solid"),
        COND_DEPENDENCY ("blueviolet", "solid"),
        ORDER_DEPENDENCY ("black", "dotted"),
        CONTROL_FLOW ("black", "bold"),
        COND_TRUE_FLOW ("brown", "bold"),
        COND_FALSE_FLOW ("darkgreen", "bold"),
        RET_RESUME_FLOW ("darkgreen", "dashed, bold");

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

    private enum NodeType {
        START_NODE ("orange"),
        RETURN_NODE ("orange"),
        CALL_NO_RETURN ("wheat"),
        EXCEPTION_EXIT ("wheat");

        NodeType(String color) {
            this.color = color;
        }
        private String color;
    }

    private static class NodePair {
        private Node n1;
        private Node n2;
        private String label;
        private EdgeType edgeType;

        NodePair(Node n1, Node n2) {
            this(n1, n2, "");
        }

        NodePair(Node n1, Node n2, String label) { this(n1, n2, label, EdgeType.CONTROL_FLOW); }

        NodePair(Node n1, Node n2, String label, EdgeType edgeType) {
            this.n1 = n1;
            this.n2 = n2;
            this.label = label;
            this.edgeType = edgeType;
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
            attr(param, "style", "filled");
            attr(param, "fillcolor", NodeType.START_NODE.color);
        }
        attr(param, "label", label);
        nl(param);
        dependencyList.add(name);
        return name;
    }

    public String visit(final Appendable param, final Cmp node) {
        return node(param, "cmp", node);
    }

    public String visit(final Appendable param, final CmpAndSwap node) {
        String name = node(param, "cmpAndSwap", node);
        addEdge(param, node, node.getExpectedValue(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getUpdateValue(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final CmpL node) {
        return node(param, "cmpl", node);
    }

    public String visit(final Appendable param, final CmpG node) {
        return node(param, "cmpg", node);
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

    public String visit(final Appendable param, final MemberOf node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "memberOf");
        nl(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final MonitorEnter node) {
        String name = node(param, "monitorenter", node);
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final MonitorExit node) {
        String name = node(param, "monitorexit", node);
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

    // value handles

    public String visit(Appendable param, ConstructorElementHandle node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "constructor\\n" + node.getExecutable());
        nl(param);
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(Appendable param, DataDeclarationHandle node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "data\\n" + node.getProgramObject());
        nl(param);
        return name;
    }

    public String visit(Appendable param, DataHandle node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "data\\n" + node.getProgramObject());
        nl(param);
        return name;
    }

    public String visit(Appendable param, ExactMethodElementHandle node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "method (exact)\\n" + node.getExecutable());
        nl(param);
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(Appendable param, FunctionDeclarationHandle node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "function\\n" + node.getProgramObject());
        nl(param);
        return name;
    }

    public String visit(Appendable param, FunctionElementHandle node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "function\\n" + node.getExecutable());
        nl(param);
        return name;
    }

    public String visit(Appendable param, FunctionHandle node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "function\\n" + node.getProgramObject());
        nl(param);
        return name;
    }

    public String visit(Appendable param, InterfaceMethodElementHandle node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "method (interface)\\n" + node.getExecutable());
        nl(param);
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(Appendable param, VirtualMethodElementHandle node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "method (virtual)\\n" + node.getExecutable());
        nl(param);
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(Appendable param, LocalVariable node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "local\n\n"+node.getVariableElement().getName());
        nl(param);
        return name;
    }

    public String visit(Appendable param, StaticMethodElementHandle node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "method (static)\\n" + node.getExecutable());
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final UnsafeHandle node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "unsafeHandle");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getOffset(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    // terminators

    public String visit(final Appendable param, final CallNoReturn node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "call (no return)\\n" + node.getValueHandle().toString());
        attr(param, "fixedsize", "shape");
        attr(param, "fillcolor", NodeType.CALL_NO_RETURN.color);
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        appendTo(param, "}");
        nl(param);
        return name;
    }

    @Override
    public String visit(Appendable param, Invoke node) {
        String name = register(node);
        visited.put(node.getReturnValue(), name);
        appendTo(param, name);
        attr(param, "label", "invoke\\n" + node.getValueHandle().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        appendTo(param, "}");
        nl(param);
        addBBConnection(param, node, node.getCatchBlock(), "catch");
        addBBConnection(param, node, node.getResumeTarget(), "resume");
        return name;
    }

    @Override
    public String visit(Appendable param, InvokeNoReturn node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "invoke (no return)\\n" + node.getValueHandle().toString());
        attr(param, "fixedsize", "shape");
        attr(param, "fillcolor", NodeType.CALL_NO_RETURN.color);
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        appendTo(param, "}");
        nl(param);
        addBBConnection(param, node, node.getCatchBlock(), "catch");
        return name;
    }

    @Override
    public String visit(Appendable param, TailCall node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "tail call\\n" + node.getValueHandle().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        appendTo(param, "}");
        nl(param);
        return name;
    }

    @Override
    public String visit(Appendable param, TailInvoke node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "tail invoke\\n" + node.getValueHandle().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        processDependencyList(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        appendTo(param, "}");
        nl(param);
        addBBConnection(param, node, node.getCatchBlock(), "catch");
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
        addEdge(param, node, node.getCondition(), EdgeType.COND_DEPENDENCY, "cond");
        appendTo(param, "}");
        nl(param);
        addBBConnection(param, node, node.getTrueBranch(), "true", EdgeType.COND_TRUE_FLOW);
        addBBConnection(param, node, node.getFalseBranch(), "false", EdgeType.COND_FALSE_FLOW);
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
        addBBConnection(param, node, node.getResumeTarget(), "ret", EdgeType.RET_RESUME_FLOW);
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
        attr(param, "fillcolor", NodeType.RETURN_NODE.color);
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

    public String visit(final Appendable param, final Invoke.ReturnValue node) {
        processDependency(param, node.getInvoke());
        return visited.get(node.getInvoke());
    }

    public String visit(final Appendable param, final Unreachable node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "fillcolor", NodeType.EXCEPTION_EXIT.color);
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
        addEdge(param, node, node.getSwitchValue(), EdgeType.COND_DEPENDENCY, "on");
        appendTo(param, "}");
        nl(param);
        int cnt = node.getNumberOfValues();
        for (int i = 0; i < cnt; i++) {
            addBBConnection(param, node, node.getTargetForIndex(i), String.valueOf(node.getValueForIndex(i)),
                            EdgeType.COND_TRUE_FLOW);
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

    public String visit(final Appendable param, final ValueReturn node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "fillcolor", NodeType.RETURN_NODE.color);
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

    public String visit(Appendable param, ClassInitCheck node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "check init " + node.getObjectType());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        return name;
    }

    @Override
    public String visit(Appendable param, DebugAddressDeclaration node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "style", "diagonals, filled");
        attr(param, "label", "declare " + node.getVariable());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        addEdge(param, node, node.getAddress(), EdgeType.VALUE_DEPENDENCY);
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
        String name = node(param, "array length", node);
        addEdge(param, node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final BitCast node) {
        return node(param, "bit cast", node);
    }

    public String visit(final Appendable param, final BitCastLiteral node) {
        return literal(param, "bit cast →" + node.getType().toString());
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

    public String visit(final Appendable param, final ByteSwap node) {
        return node(param, "byte swap", node);
    }

    public String visit(final Appendable param, final Call node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "call" + "\\n" + node.getValueHandle().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        return name;
    }

    public String visit(final Appendable param, final CallNoSideEffects node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "call (NSE)" + "\\n" + node.getValueHandle().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        return name;
    }

    public String visit(final Appendable param, final ClassOf node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "classOf");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInput(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getDimensions(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final Clone node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "clone");
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final Convert node) {
        return node(param, "convert", node);
    }

    public String visit(final Appendable param, final CurrentThreadRead node) {
        return node(param, "read thread", node);
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

    public String visit(Appendable param, Fence node) {
        return node(param, "fence", node);
    }

    public String visit(final Appendable param, final FloatLiteral node) {
        return literal(param, String.valueOf(node.doubleValue()));
    }

    public String visit(Appendable param, GetAndAdd node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "get-and-add");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getUpdateValue(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(Appendable param, InsertElement node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "inserted element");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getIndex(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getInsertedValue(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getArrayValue(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(Appendable param, InsertMember node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "inserted member \"" + node.getMember().getName() + "\"");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInsertedValue(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getCompoundValue(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final InstanceOf node) {
        String name = node(param, "instanceof " + node.getCheckType().toString(), node);
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
        String name = node(param, "new multi array\\n" + node.getArrayType().toString(), node);
        for (Value dimension : node.getDimensions()) {
            addEdge(param, node, dimension, EdgeType.VALUE_DEPENDENCY, "dim");
        }
        return name;
    }

    public String visit(final Appendable param, final Multiply node) {
        return node(param, "*", node);
    }

    public String visit(final Appendable param, final OffsetOfField node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "offset-of " + node.getFieldElement().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final CheckCast node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", node.getKind() + "→" + node.getType().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
        addEdge(param, node, node.getInput(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getToType(), EdgeType.VALUE_DEPENDENCY);
        addEdge(param, node, node.getToDimensions(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(final Appendable param, final ConstantLiteral node) {
        return literal(param, "constant");
    }

    public String visit(final Appendable param, final Neg node) {
        return node(param, "neg", node);
    }

    public String visit(final Appendable param, final New node) {
        return node(param, "new\\n" + node.getType().getUpperBound().toString(), node);
    }

    public String visit(final Appendable param, final NewArray node) {
        String name = node(param, "new array\\n" + node.getArrayType().toString(), node);
        addEdge(param, node, node.getSize(), EdgeType.VALUE_DEPENDENCY, "size");
        return name;
    }

    public String visit(final Appendable param, final NotNull node) {
        return node(param, "not-null", node);
    }

    public String visit(final Appendable param, final NullLiteral node) {
        return literal(param, "null");
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
        addEdge(param, node, node.getCondition(), EdgeType.COND_DEPENDENCY,"cond");
        addEdge(param, node, node.getTrueValue(), EdgeType.COND_TRUE_FLOW, "T");
        addEdge(param, node, node.getFalseValue(), EdgeType.COND_FALSE_FLOW, "F");
        return name;
    }

    public String visit(final Appendable param, final Shl node) {
        return node(param, "<<", node);
    }

    public String visit(final Appendable param, final Shr node) {
        return node(param, ">>", node);
    }

    public String visit(final Appendable param, final StackAllocation node) {
        String name = node(param, "alloca " + node.getType(), node);
        addEdge(param, node, node.getCount(), EdgeType.VALUE_DEPENDENCY, "count");
        nl(param);
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

    private String node(Appendable param, String kind, OrderedNode node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", kind);
        attr(param, "fixedsize", "shape");
        nl(param);
        dependencyList.add(name);
        processDependency(param, node.getDependency());
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
            if (cp == '"') {
                appendTo(output, '\\');
            } else if (cp == '\\') {
                if((i + 1) == orig.length() ||
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

    void processDependency(Appendable param, Node node) {
        if (depth++ > 500) {
            throw new TooBigException();
        }
        try {
            getNodeName(param, node);
        } finally {
            depth--;
        }
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
        attr(param, "color", EdgeType.ORDER_DEPENDENCY.color());
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

    private void addBBConnection(Appendable param, Terminator from, BasicBlock to, String label, EdgeType style) {
        bbConnections.add(new NodePair(from, to.getBlockEntry(), label, style));
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
        PhiValue phi;
        while ((phi = phiQueue.poll()) != null) {
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
            attr(param, "style", pair.edgeType.style());
            attr(param, "color", pair.edgeType.color());
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
