package cc.quarkus.qcc.plugin.dot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.graph.Action;
import cc.quarkus.qcc.graph.Add;
import cc.quarkus.qcc.graph.And;
import cc.quarkus.qcc.graph.ArrayElementRead;
import cc.quarkus.qcc.graph.ArrayElementWrite;
import cc.quarkus.qcc.graph.ArrayLength;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BitCast;
import cc.quarkus.qcc.graph.BlockEntry;
import cc.quarkus.qcc.graph.CastValue;
import cc.quarkus.qcc.graph.Catch;
import cc.quarkus.qcc.graph.ClassCastErrorNode;
import cc.quarkus.qcc.graph.ClassNotFoundErrorNode;
import cc.quarkus.qcc.graph.Clone;
import cc.quarkus.qcc.graph.CmpEq;
import cc.quarkus.qcc.graph.CmpGe;
import cc.quarkus.qcc.graph.CmpGt;
import cc.quarkus.qcc.graph.CmpLe;
import cc.quarkus.qcc.graph.CmpLt;
import cc.quarkus.qcc.graph.CmpNe;
import cc.quarkus.qcc.graph.CommutativeBinaryValue;
import cc.quarkus.qcc.graph.ConstructorInvocation;
import cc.quarkus.qcc.graph.Convert;
import cc.quarkus.qcc.graph.Div;
import cc.quarkus.qcc.graph.DynamicInvocation;
import cc.quarkus.qcc.graph.DynamicInvocationValue;
import cc.quarkus.qcc.graph.Extend;
import cc.quarkus.qcc.graph.FunctionCall;
import cc.quarkus.qcc.graph.Goto;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.InstanceFieldRead;
import cc.quarkus.qcc.graph.InstanceFieldWrite;
import cc.quarkus.qcc.graph.InstanceInvocation;
import cc.quarkus.qcc.graph.InstanceInvocationValue;
import cc.quarkus.qcc.graph.InstanceOf;
import cc.quarkus.qcc.graph.Jsr;
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
import cc.quarkus.qcc.graph.ThisValue;
import cc.quarkus.qcc.graph.Throw;
import cc.quarkus.qcc.graph.Truncate;
import cc.quarkus.qcc.graph.Try;
import cc.quarkus.qcc.graph.TypeIdOf;
import cc.quarkus.qcc.graph.UnaryValue;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueReturn;
import cc.quarkus.qcc.graph.Xor;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.CurrentThreadLiteral;
import cc.quarkus.qcc.graph.literal.DefinedConstantLiteral;
import cc.quarkus.qcc.graph.literal.FloatLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.MethodDescriptorLiteral;
import cc.quarkus.qcc.graph.literal.MethodHandleLiteral;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.graph.literal.ObjectLiteral;
import cc.quarkus.qcc.graph.literal.StringLiteral;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.TypeLiteral;
import cc.quarkus.qcc.graph.literal.UndefinedLiteral;

/**
 * A node visitor which generates a GraphViz graph for a method or function body.
 */
public class DotNodeVisitor implements NodeVisitor<Appendable, String, String, String> {
    final Map<Node, String> visited = new HashMap<>();
    int counter;
    boolean attr;
    boolean commaNeeded;

    public String visit(final Appendable param, final ArrayElementWrite node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "array write");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInstance());
        addEdge(param, node, node.getIndex(), "idx");
        addEdge(param, node, node.getWriteValue(), "val");
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final BlockEntry node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "doublecircle");
        attr(param, "fixedsize", "shape");
        attr(param, "label", "");
        nl(param);
        return name;
    }

    public String visit(final Appendable param, final DynamicInvocation node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "invokedynamic");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getDependency());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg);
        }
        for (Value arg : node.getStaticArguments()) {
            addEdge(param, node, arg);
        }
        return name;
    }

    public String visit(final Appendable param, final InstanceFieldWrite node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "write " + node.getFieldElement().getEnclosingType().getInternalName() + "#" + node.getFieldElement().getName());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInstance());
        addEdge(param, node, node.getWriteValue(), "val");
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final InstanceInvocation node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "invoke" + node.getKind() + "\\n" + node.getInvocationTarget().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getDependency());
        addEdge(param, node, node.getInstance());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg);
        }
        return name;
    }

    public String visit(final Appendable param, final MonitorEnter node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "monitorenter");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInstance());
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final MonitorExit node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "monitorexit");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInstance());
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final StaticFieldWrite node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "write " + node.getFieldElement().getEnclosingType().getInternalName() + "#" + node.getFieldElement().getName());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getWriteValue(), "val");
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final StaticInvocation node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "invokestatic\\n" + node.getInvocationTarget().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getDependency());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg);
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
        addEdge(param, node, node.getResumeTarget());
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getCondition(), "cond");
        addEdge(param, node, node.getTrueBranch(), "true");
        addEdge(param, node, node.getFalseBranch(), "false");
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getResumeTarget(), "ret");
        addEdge(param, node, node.getJsrTarget(), "to");
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getReturnAddressValue());
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getSwitchValue(), "on");
        addEdge(param, node, node.getDependency());
        int cnt = node.getNumberOfValues();
        for (int i = 0; i < cnt; i ++) {
            addEdge(param, node, node.getTargetForIndex(i), String.valueOf(node.getValueForIndex(i)));
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
        addEdge(param, node, node.getThrownValue());
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getDelegateOperation());
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getReturnValue());
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final Add node) {
        return node(param, "+", node);
    }

    public String visit(final Appendable param, final And node) {
        return node(param, "&", node);
    }

    public String visit(final Appendable param, final ArrayElementRead node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "array read");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInstance());
        addEdge(param, node, node.getIndex(), "idx");
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final ArrayLength node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "array length");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInstance());
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
        addEdge(param, node, node.getBlock().getBlockEntry());
        return name;
    }

    public String visit(final Appendable param, final BooleanLiteral node) {
        return literal(param, String.valueOf(node.booleanValue()));
    }

    public String visit(final Appendable param, final Catch node) {
        return literal(param, "catch");
    }

    public String visit(final Appendable param, final Clone node) {
        return node(param, "clone", node);
    }

    public String visit(final Appendable param, final CmpEq node) {
        return node(param, "=", node);
    }

    public String visit(final Appendable param, final CmpGe node) {
        return node(param, "≥", node);
    }

    public String visit(final Appendable param, final CmpGt node) {
        return node(param, ">", node);
    }

    public String visit(final Appendable param, final CmpLe node) {
        return node(param, "≤", node);
    }

    public String visit(final Appendable param, final CmpLt node) {
        return node(param, "<", node);
    }

    public String visit(final Appendable param, final CmpNe node) {
        return node(param, "≠", node);
    }

    public String visit(final Appendable param, final ConstructorInvocation node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "init\\n" + node.getInvocationTarget().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getDependency());
        addEdge(param, node, node.getInstance());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg);
        }
        return name;
    }

    public String visit(final Appendable param, final Convert node) {
        return node(param, "convert", node);
    }

    public String visit(final Appendable param, final CurrentThreadLiteral node) {
        return literal(param, "thread");
    }

    public String visit(final Appendable param, final DefinedConstantLiteral node) {
        return literal(param, "constant " + node.getName());
    }

    public String visit(final Appendable param, final Div node) {
        return node(param, "/", node);
    }

    public String visit(final Appendable param, final DynamicInvocationValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "invokedynamic");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getDependency());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg);
        }
        for (Value arg : node.getStaticArguments()) {
            addEdge(param, node, arg);
        }
        return name;
    }

    public String visit(final Appendable param, final Extend node) {
        return node(param, "extend", node);
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
            addEdge(param, node, callTarget, "fn");
        }
        addEdge(param, node, node.getDependency());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg);
        }
        return name;
    }

    public String visit(final Appendable param, final InstanceFieldRead node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "read " + node.getFieldElement().getEnclosingType().getInternalName() + "#" + node.getFieldElement().getName());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInstance());
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final InstanceInvocationValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "invoke" + node.getKind() + "\\n" + node.getInvocationTarget().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getDependency());
        addEdge(param, node, node.getInstance());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg);
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
        addEdge(param, node, node.getInstance());
        return name;
    }

    public String visit(final Appendable param, final IntegerLiteral node) {
        return literal(param, String.valueOf(node.longValue()));
    }

    public String visit(final Appendable param, final MethodDescriptorLiteral node) {
        return literal(param, node.toString());
    }

    public String visit(final Appendable param, final MethodHandleLiteral node) {
        return literal(param, node.toString());
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
        for (Value dimension : node.getDimensions()) {
            addEdge(param, node, dimension, "dim");
        }
        addEdge(param, node, node.getDependency());
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
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final NewArray node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "new array\\n" + node.getArrayType().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getSize(), "size");
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final NullLiteral node) {
        return literal(param, "null");
    }

    public String visit(final Appendable param, final ObjectLiteral node) {
        return literal(param, "object");
    }

    public String visit(final Appendable param, final Or node) {
        return node(param, "|", node);
    }

    public String visit(final Appendable param, final ParameterValue node) {
        return literal(param, node.getType().toString() + " param[" + node.getIndex() + "]");
    }

    public String visit(final Appendable param, final PhiValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", "φ");
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getPinnedBlock().getBlockEntry());
        for (Map.Entry<BasicBlock, Value> entry : node.getIncomingValues()) {
            addEdge(param, node, entry.getValue());
        }
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
        addEdge(param, node, node.getDependency());
        addEdge(param, node, node.getCondition(), "cond");
        addEdge(param, node, node.getTrueValue(), "T");
        addEdge(param, node, node.getFalseValue(), "F");
        return name;
    }

    public String visit(final Appendable param, final Shl node) {
        return node(param, "<<", node);
    }

    public String visit(final Appendable param, final Shr node) {
        return node(param, ">>", node);
    }

    public String visit(final Appendable param, final StaticFieldRead node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "rectangle");
        attr(param, "label", "read " + node.getFieldElement().getEnclosingType().getInternalName() + "#" + node.getFieldElement().getName());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getDependency());
        return name;
    }

    public String visit(final Appendable param, final StaticInvocationValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "invokestatic\\n" + node.getInvocationTarget().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getDependency());
        for (Value arg : node.getArguments()) {
            addEdge(param, node, arg);
        }
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

    public String visit(final Appendable param, final ThisValue node) {
        return literal(param, "this");
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
        addEdge(param, node, node.getInstance());
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
        addEdge(param, node, node.getLeftInput());
        addEdge(param, node, node.getRightInput());
        return name;
    }

    private String node(final Appendable param, String kind, CommutativeBinaryValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", kind);
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getLeftInput());
        addEdge(param, node, node.getRightInput());
        return name;
    }

    private String node(final Appendable param, String kind, CastValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", kind + "→" + node.getType().toString());
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInput());
        return name;
    }

    private String node(final Appendable param, String kind, UnaryValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "shape", "circle");
        attr(param, "label", kind);
        attr(param, "fixedsize", "shape");
        nl(param);
        addEdge(param, node, node.getInput());
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

    private void addEdge(Appendable param, Terminator from, BasicBlock to) {
        String fromName = getNodeName(param, from);
        String toName = getNodeName(param, to.getBlockEntry());
        appendTo(param, fromName);
        appendTo(param, " -> ");
        appendTo(param, toName);
        attr(param, "style", "dotted");
        nl(param);
    }

    private void addEdge(Appendable param, Terminator from, BasicBlock to, String label) {
        String fromName = getNodeName(param, from);
        String toName = getNodeName(param, to.getBlockEntry());
        appendTo(param, fromName);
        appendTo(param, " -> ");
        appendTo(param, toName);
        attr(param, "style", "dotted");
        attr(param, "label", label);
        nl(param);
    }

    private void addEdge(Appendable param, Node from, Node to) {
        if (to instanceof Value) {
            addEdge(param, from, (Value) to, true);
        } else {
            assert to instanceof Action;
            addEdge(param, from, (Action) to);
        }
    }

    private void addEdge(Appendable param, Node from, Action to) {
        String fromName = getNodeName(param, from);
        String toName = getNodeName(param, to);
        appendTo(param, fromName);
        appendTo(param, " -> ");
        appendTo(param, toName);
        attr(param, "style", "dotted");
        nl(param);
    }

    private void addEdge(Appendable param, Node from, Value to) {
        addEdge(param, from, to, false);
    }

    private void addEdge(Appendable param, Node from, Value to, boolean dotted) {
        String fromName = getNodeName(param, from);
        String toName = getNodeName(param, to);
        appendTo(param, fromName);
        appendTo(param, " -> ");
        appendTo(param, toName);
        if (dotted) {
            attr(param, "style", "dotted");
        }
        nl(param);
    }

    private void addEdge(Appendable param, Node from, Value to, String label) {
        String fromName = getNodeName(param, from);
        String toName = getNodeName(param, to);
        appendTo(param, fromName);
        appendTo(param, " -> ");
        appendTo(param, toName);
        attr(param, "label", label);
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

    private String getNodeName(Appendable param, Terminator node) {
        String name = visited.get(node);
        if (name == null) {
            node.accept(this, param);
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

    public void process(final Appendable param, final BasicBlock block) {
        getNodeName(param, block.getTerminator());
    }
}
