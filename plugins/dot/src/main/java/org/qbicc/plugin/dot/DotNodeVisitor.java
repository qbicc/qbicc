package org.qbicc.plugin.dot;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Add;
import org.qbicc.graph.AddressOf;
import org.qbicc.graph.And;
import org.qbicc.graph.AsmHandle;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BitReverse;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.ByteSwap;
import org.qbicc.graph.Call;
import org.qbicc.graph.CallNoReturn;
import org.qbicc.graph.CallNoSideEffects;
import org.qbicc.graph.CastValue;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.Cmp;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.CmpG;
import org.qbicc.graph.CmpL;
import org.qbicc.graph.CommutativeBinaryValue;
import org.qbicc.graph.Comp;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.Convert;
import org.qbicc.graph.CountLeadingZeros;
import org.qbicc.graph.CountTrailingZeros;
import org.qbicc.graph.CurrentThread;
import org.qbicc.graph.DebugAddressDeclaration;
import org.qbicc.graph.DebugValueDeclaration;
import org.qbicc.graph.Div;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.Extend;
import org.qbicc.graph.ExtractElement;
import org.qbicc.graph.ExtractInstanceField;
import org.qbicc.graph.ExtractMember;
import org.qbicc.graph.Fence;
import org.qbicc.graph.FunctionElementHandle;
import org.qbicc.graph.GetAndAdd;
import org.qbicc.graph.GetAndBitwiseAnd;
import org.qbicc.graph.GetAndBitwiseNand;
import org.qbicc.graph.GetAndBitwiseOr;
import org.qbicc.graph.GetAndBitwiseXor;
import org.qbicc.graph.GetAndSet;
import org.qbicc.graph.GetAndSetMax;
import org.qbicc.graph.GetAndSetMin;
import org.qbicc.graph.GetAndSub;
import org.qbicc.graph.GlobalVariable;
import org.qbicc.graph.Goto;
import org.qbicc.graph.If;
import org.qbicc.graph.InitCheck;
import org.qbicc.graph.InsertElement;
import org.qbicc.graph.InsertMember;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.InstanceOf;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.InvokeNoReturn;
import org.qbicc.graph.IsEq;
import org.qbicc.graph.IsGe;
import org.qbicc.graph.IsGt;
import org.qbicc.graph.IsLe;
import org.qbicc.graph.IsLt;
import org.qbicc.graph.IsNe;
import org.qbicc.graph.Jsr;
import org.qbicc.graph.Load;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.Max;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.MemberSelector;
import org.qbicc.graph.Min;
import org.qbicc.graph.Mod;
import org.qbicc.graph.MonitorEnter;
import org.qbicc.graph.MonitorExit;
import org.qbicc.graph.MultiNewArray;
import org.qbicc.graph.Multiply;
import org.qbicc.graph.Neg;
import org.qbicc.graph.New;
import org.qbicc.graph.NewArray;
import org.qbicc.graph.NewReferenceArray;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.NonCommutativeBinaryValue;
import org.qbicc.graph.NotNull;
import org.qbicc.graph.OffsetOfField;
import org.qbicc.graph.Or;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.PopCount;
import org.qbicc.graph.ReadModifyWriteValue;
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
import org.qbicc.graph.Throw;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.UnaryValue;
import org.qbicc.graph.Unreachable;
import org.qbicc.graph.UnsafeHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueReturn;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.Xor;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.ConstantLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.graph.literal.ValueConvertLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;

/**
 * A node visitor which generates a GraphViz graph for a method or function body.
 */
public class DotNodeVisitor implements NodeVisitor.Delegating<DotContext, String, String, String, String> {
    private final NodeVisitor<DotContext, String, String, String, String> delegate;

    @Override
    public NodeVisitor<DotContext, String, String, String, String> getDelegateNodeVisitor() {
        return delegate;
    }

    enum EdgeType implements DotAttributes {
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

        private final String color;
        private final String style;

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

    DotNodeVisitor(CompilationContext ctxt, NodeVisitor<DotContext, String, String, String, String> delegate) {
        this.delegate = delegate;
    }

    public String visitUnknown(final DotContext param, Value node) {
        throw new IllegalStateException("Visitor for node " + node.getClass() + " is not implemented");
    }

    public String visit(final DotContext param, final AsmHandle node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "asm(" + node.getInstruction() + "," + node.getConstraints() + ")");
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final BlockEntry node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "doublecircle");
        param.attr("fixedsize", "shape");
        String label = "";
        if (node.getPinnedBlock() == param.getEntryBlock()) {
            label = "start";
            param.attr("style", "filled");
            param.attr("fillcolor", NodeType.START_NODE.color);
        }
        param.attr("label", label);
        param.nl();
        param.addDependency(name);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Cmp node) {
        node(param, "cmp", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final CmpAndSwap node) {
        node(param, "cmpAndSwap", node);
        param.addEdge(node, node.getExpectedValue(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getUpdateValue(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final CmpL node) {
        node(param, "cmpl", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final CmpG node) {
        node(param, "cmpg", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ElementOf node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "elementOf");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getIndex(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final GlobalVariable node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "global\n\n"+node.getVariableElement().getName());
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final InstanceFieldOf node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "field access\\n"+node.getVariableElement().getName());
        param.nl();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final MemberOf node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "memberOf");
        param.nl();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final MonitorEnter node) {
        node(param, "monitorenter", node);
        param.addEdge(node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final MonitorExit node) {
        node(param, "monitorexit", node);
        param.addEdge(node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final PointerHandle node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "ptr");
        param.nl();
        param.addEdge(node, node.getPointerValue(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ReferenceHandle node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "ref");
        param.nl();
        param.addEdge(node, node.getReferenceValue(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final StaticField node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.nl();
        param.attr("label", "static field\\n" + node.getVariableElement().toString());
        param.nl();
        return delegate.visit(param, node);
    }

    // value handles

    public String visit(DotContext param, ConstructorElementHandle node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.nl();
        param.attr("label", "constructor\\n" + node.getExecutable());
        param.nl();
        param.addEdge(node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final CurrentThread node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.nl();
        param.attr("label", "currentThread");
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, ExactMethodElementHandle node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.nl();
        param.attr("label", "method (exact)\\n" + node.getExecutable());
        param.nl();
        param.addEdge(node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, FunctionElementHandle node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.nl();
        param.attr("label", "function\\n" + node.getExecutable());
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, InterfaceMethodElementHandle node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.nl();
        param.attr("label", "method (interface)\\n" + node.getExecutable());
        param.nl();
        param.addEdge(node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, VirtualMethodElementHandle node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.nl();
        param.attr("label", "method (virtual)\\n" + node.getExecutable());
        param.nl();
        param.addEdge(node, node.getInstance(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, LocalVariable node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "local\\n\\n"+node.getVariableElement().getName());
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, StaticMethodElementHandle node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.nl();
        param.attr("label", "method (static)\\n" + node.getExecutable());
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final UnsafeHandle node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "unsafeHandle");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getOffset(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    // terminators

    public String visit(final DotContext param, final CallNoReturn node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "call (no return)\\n" + node.getValueHandle().toString());
        param.attr("fixedsize", "shape");
        param.attr("fillcolor", NodeType.CALL_NO_RETURN.color);
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            param.addEdge(node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        param.appendTo("}");
        param.nl();
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, Invoke node) {
        String name = param.getName(node);
        param.addVisited(node.getReturnValue(), name);
        param.appendTo(name);
        param.attr("label", "invoke\\n" + node.getValueHandle().toString());
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            param.addEdge(node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        param.appendTo("}");
        param.nl();
        param.addBBConnection(node, node.getCatchBlock(), "catch");
        param.addBBConnection(node, node.getResumeTarget(), "resume");
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, InvokeNoReturn node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "invoke (no return)\\n" + node.getValueHandle().toString());
        param.attr("fixedsize", "shape");
        param.attr("fillcolor", NodeType.CALL_NO_RETURN.color);
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            param.addEdge(node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        param.appendTo("}");
        param.nl();
        param.addBBConnection(node, node.getCatchBlock(), "catch");
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, TailCall node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "tail call\\n" + node.getValueHandle().toString());
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            param.addEdge(node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        param.appendTo("}");
        param.nl();
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, TailInvoke node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "tail invoke\\n" + node.getValueHandle().toString());
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            param.addEdge(node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        param.appendTo("}");
        param.nl();
        param.addBBConnection(node, node.getCatchBlock(), "catch");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Goto node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "goto");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.appendTo("}");
        param.nl();
        param.addBBConnection(node, node.getResumeTarget());
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final If node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "diamond");
        param.attr("style", "diagonals, filled");
        param.attr("label", "if");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.addEdge(node, node.getCondition(), EdgeType.COND_DEPENDENCY, "cond");
        param.appendTo("}");
        param.nl();
        param.addBBConnection(node, node.getTrueBranch(), "true", EdgeType.COND_TRUE_FLOW);
        param.addBBConnection(node, node.getFalseBranch(), "false", EdgeType.COND_FALSE_FLOW);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Jsr node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "jsr");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.appendTo("}");
        param.nl();
        param.addBBConnection(node, node.getResumeTarget(), "ret", EdgeType.RET_RESUME_FLOW);
        param.addBBConnection(node, node.getJsrTarget(), "to");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Ret node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "ret");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.appendTo("}");
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Return node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("fillcolor", NodeType.RETURN_NODE.color);
        param.attr("label", "return");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.appendTo("}");
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Invoke.ReturnValue node) {
        param.processDependency(node.getInvoke());
        delegate.visit(param, node);
        return param.getName(node.getInvoke());
    }

    public String visit(final DotContext param, final Unreachable node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("fillcolor", NodeType.EXCEPTION_EXIT.color);
        param.attr("label", "unreachable");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.appendTo("}");
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Switch node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "diamond");
        param.attr("style", "diagonals, filled");
        param.attr("label", "switch");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.addEdge(node, node.getSwitchValue(), EdgeType.COND_DEPENDENCY, "on");
        param.appendTo("}");
        param.nl();
        int cnt = node.getNumberOfValues();
        for (int i = 0; i < cnt; i++) {
            param.addBBConnection(node, node.getTargetForIndex(i), String.valueOf(node.getValueForIndex(i)),
                            EdgeType.COND_TRUE_FLOW);
        }
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Throw node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "throw");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.addEdge(node, node.getThrownValue(), EdgeType.VALUE_DEPENDENCY);
        param.appendTo("}");
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ValueReturn node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("fillcolor", NodeType.RETURN_NODE.color);
        param.attr("label", "return");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.processDependencyList();
        param.addEdge(node, node.getReturnValue(), EdgeType.VALUE_DEPENDENCY);
        param.appendTo("}");
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, InitCheck node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "check init " + node.getInitializerElement());
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.addEdge(node, node.getInitThunk(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, DebugAddressDeclaration node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "declare " + node.getVariable());
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.addEdge(node, node.getAddress(), EdgeType.VALUE_DEPENDENCY);
        param.processDependency(node.getDependency());
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final DebugValueDeclaration node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("style", "diagonals, filled");
        param.attr("label", "declare " + node.getVariable());
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.addEdge(node, node.getValue(), EdgeType.VALUE_DEPENDENCY);
        param.processDependency(node.getDependency());
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Add node) {
        node(param, "+", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final AddressOf node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "addr of");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final And node) {
        node(param, "&", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final BitCast node) {
        node(param, "bit cast", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final BitCastLiteral node) {
        literal(param, "bit cast →" + node.getType().toString());
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final BitReverse node) {
        node(param, "bit reverse", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final BlockLiteral node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "block");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getBlock().getBlockEntry(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final BooleanLiteral node) {
        literal(param, String.valueOf(node.booleanValue()));
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ByteArrayLiteral node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "byte array [" + node.getValues().length + "]");
        param.attr("fixedsize", "shape");
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ByteSwap node) {
        node(param, "byte swap", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Call node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "call" + "\\n" + node.getValueHandle().toString());
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            param.addEdge(node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final CallNoSideEffects node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "call (NSE)" + "\\n" + node.getValueHandle().toString());
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        for (Value arg : node.getArguments()) {
            param.addEdge(node, arg, EdgeType.VALUE_DEPENDENCY);
        }
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ClassOf node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "classOf");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getInput(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getDimensions(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Comp node) {
        node(param, "~", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final CountLeadingZeros node) {
        node(param, "clz", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final CountTrailingZeros node) {
        node(param, "ctz", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Convert node) {
        node(param, "convert", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Div node) {
        node(param, "/", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Extend node) {
        node(param, "extend", node);
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, ExtractElement node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "extracted element");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getIndex(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getArrayValue(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, ExtractInstanceField node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "extracted field \"" + node.getFieldElement().getName() + "\"");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getObjectValue(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, ExtractMember node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "extracted member \"" + node.getMember().getName() + "\"");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getCompoundValue(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, Fence node) {
        node(param, "fence", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final FloatLiteral node) {
        literal(param, String.valueOf(node.doubleValue()));
        return delegate.visit(param, node);
    }

    private String node(DotContext param, String label, ReadModifyWriteValue node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", label);
        param.nl();
        param.addDependency(name);
        if (node instanceof OrderedNode) {
            param.processDependency(((OrderedNode) node).getDependency());
        }
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getUpdateValue(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    public String visit(DotContext param, GetAndAdd node) {
        node(param, "get-and-add", (ReadModifyWriteValue) node);
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, GetAndSet node) {
        node(param, "get-and-set", (ReadModifyWriteValue) node);
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, GetAndBitwiseAnd node) {
        node(param, "get-and-and", (ReadModifyWriteValue) node);
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, GetAndBitwiseNand node) {
        node(param, "get-and-nand", (ReadModifyWriteValue) node);
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, GetAndBitwiseOr node) {
        node(param, "get-and-or", (ReadModifyWriteValue) node);
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, GetAndBitwiseXor node) {
        node(param, "get-and-xor", (ReadModifyWriteValue) node);
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, GetAndSetMax node) {
        node(param, "get-and-set-max", (ReadModifyWriteValue) node);
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, GetAndSetMin node) {
        node(param, "get-and-set-min", (ReadModifyWriteValue) node);
        return delegate.visit(param, node);
    }

    @Override
    public String visit(DotContext param, GetAndSub node) {
        node(param, "get-and-sub", (ReadModifyWriteValue) node);
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, InsertElement node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "inserted element");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getIndex(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getInsertedValue(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getArrayValue(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, InsertMember node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "inserted member \"" + node.getMember().getName() + "\"");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getInsertedValue(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getCompoundValue(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final InstanceOf node) {
        node(param, "instanceof " + node.getCheckType().toString(), node);
        param.addEdge(node, node.getInstance(), EdgeType.VALUE_DEPENDENCY, "value");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final IntegerLiteral node) {
        literal(param, String.valueOf(node.longValue()));
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final IsEq node) {
        node(param, "eq", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final IsGe node) {
        node(param, "≥", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final IsGt node) {
        node(param, ">", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final IsLe node) {
        node(param, "≤", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final IsLt node) {
        node(param, "<", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final IsNe node) {
        node(param, "neq", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Load node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "load");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final MethodHandleLiteral node) {
        literal(param, node.toString());
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Max node) {
        node(param, "max", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final MemberSelector node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "sel");
        param.nl();
        param.addDependency(name);
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Min node) {
        node(param, "min", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Mod node) {
        node(param, "%", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final MultiNewArray node) {
        node(param, "new multi array\\n" + node.getArrayType().toString(), node);
        for (Value dimension : node.getDimensions()) {
            param.addEdge(node, dimension, EdgeType.VALUE_DEPENDENCY, "dim");
        }
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Multiply node) {
        node(param, "*", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final OffsetOfField node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "offset-of " + node.getFieldElement().toString());
        param.attr("fixedsize", "shape");
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final CheckCast node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", node.getKind() + "→" + node.getType().toString());
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.addEdge(node, node.getInput(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getToType(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getToDimensions(), EdgeType.VALUE_DEPENDENCY);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ConstantLiteral node) {
        literal(param, "constant");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Neg node) {
        node(param, "neg", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final New node) {
        node(param, "new\\n" + node.getType().getUpperBound().toString(), node);
        param.addEdge(node, node.getTypeId(), EdgeType.VALUE_DEPENDENCY, "typeId");
        param.addEdge(node, node.getSize(), EdgeType.VALUE_DEPENDENCY, "size");
        param.addEdge(node, node.getAlign(), EdgeType.VALUE_DEPENDENCY, "align");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final NewArray node) {
        node(param, "new array\\n" + node.getArrayType().toString(), node);
        param.addEdge(node, node.getSize(), EdgeType.VALUE_DEPENDENCY, "size");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final NewReferenceArray node) {
        node(param, "new reference array\\n" + node.getArrayType().toString(), node);
        param.addEdge(node, node.getElemTypeId(), EdgeType.VALUE_DEPENDENCY, "elemTypeId");
        param.addEdge(node, node.getDimensions(), EdgeType.VALUE_DEPENDENCY, "dimensions");
        param.addEdge(node, node.getSize(), EdgeType.VALUE_DEPENDENCY, "size");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final NotNull node) {
        node(param, "not-null", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final NullLiteral node) {
        literal(param, "null");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ZeroInitializerLiteral node) {
        literal(param, "zero");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ObjectLiteral node) {
        literal(param, "object");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Or node) {
        node(param, "|", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ParameterValue node) {
        int index = node.getIndex();
        StringBuilder b = new StringBuilder();
        b.append(node.getType()).append(' ').append("param").append('[').append(node.getLabel());
        if (index > 0) {
            b.append(index);
        }
        b.append(']');
        literal(param, b.toString());
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final PhiValue node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", "phi");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addToPhiQueue(node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final PointerLiteral node) {
        literal(param, "pointer");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final PopCount node) {
        node(param, "pop count", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Rol node) {
        node(param, "|<<", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Ror node) {
        node(param, "|>>", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Select node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "diamond");
        param.attr("label", "select");
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getCondition(), EdgeType.COND_DEPENDENCY,"cond");
        param.addEdge(node, node.getTrueValue(), EdgeType.COND_TRUE_FLOW, "T");
        param.addEdge(node, node.getFalseValue(), EdgeType.COND_FALSE_FLOW, "F");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Shl node) {
        node(param, "<<", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Shr node) {
        node(param, ">>", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final StackAllocation node) {
        node(param, "alloca " + node.getType(), node);
        param.addEdge(node, node.getCount(), EdgeType.VALUE_DEPENDENCY, "count");
        param.nl();
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Store node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("label", "store");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        param.addEdge(node, node.getValueHandle(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getValue(), EdgeType.VALUE_DEPENDENCY, "value");
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final StringLiteral node) {
        literal(param, '"' + node.getValue() + '"');
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final ProgramObjectLiteral node) {
        literal(param, "@" + node.getName());
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Sub node) {
        node(param, "-", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Truncate node) {
        node(param, "trunc", node);
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final TypeLiteral node) {
        literal(param, node.getType().getUpperBound().toString());
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final UndefinedLiteral node) {
        literal(param, "undef");
        return delegate.visit(param, node);
    }

    public String visit(DotContext param, ValueConvertLiteral node) {
        literal(param, "convert →" + node.getType().toString());
        return delegate.visit(param, node);
    }

    public String visit(final DotContext param, final Xor node) {
        node(param, "^", node);
        return delegate.visit(param, node);
    }

    private String literal(final DotContext param, final String label) {
        String name = param.getName();
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", label);
        param.attr("fixedsize", "shape");
        param.nl();
        return name;
    }

    private String node(final DotContext param, String kind, NonCommutativeBinaryValue node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", kind);
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getLeftInput(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getRightInput(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    private String node(final DotContext param, String kind, CommutativeBinaryValue node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", kind);
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getLeftInput(), EdgeType.VALUE_DEPENDENCY);
        param.addEdge(node, node.getRightInput(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    private String node(final DotContext param, String kind, CastValue node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", kind + "→" + node.getType().toString());
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getInput(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    private String node(final DotContext param, String kind, UnaryValue node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "circle");
        param.attr("label", kind);
        param.attr("fixedsize", "shape");
        param.nl();
        param.addEdge(node, node.getInput(), EdgeType.VALUE_DEPENDENCY);
        return name;
    }

    private String node(DotContext param, String kind, OrderedNode node) {
        String name = param.getName(node);
        param.appendTo(name);
        param.attr("shape", "rectangle");
        param.attr("label", kind);
        param.attr("fixedsize", "shape");
        param.nl();
        param.addDependency(name);
        param.processDependency(node.getDependency());
        return name;
    }
}
