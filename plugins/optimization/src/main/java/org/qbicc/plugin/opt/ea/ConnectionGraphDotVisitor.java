package org.qbicc.plugin.opt.ea;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.graph.Action;
import org.qbicc.graph.Add;
import org.qbicc.graph.AddressOf;
import org.qbicc.graph.And;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BinaryValue;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEntry;
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
import org.qbicc.graph.Comp;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.Convert;
import org.qbicc.graph.CountLeadingZeros;
import org.qbicc.graph.CountTrailingZeros;
import org.qbicc.graph.CurrentThreadRead;
import org.qbicc.graph.DebugAddressDeclaration;
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
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.NotNull;
import org.qbicc.graph.OffsetOfField;
import org.qbicc.graph.Or;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.PointerHandle;
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
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Throw;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.UnaryValue;
import org.qbicc.graph.Unreachable;
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
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;

public class ConnectionGraphDotVisitor implements NodeVisitor<Appendable, String, String, String, String> {

    private final ConnectionGraph connectionGraph;

    // TODO copied from DotNodeVisitor
    final BasicBlock entryBlock;
    final Map<Node, String> visited = new HashMap<>();
    private final Set<BasicBlock> blockQueued = ConcurrentHashMap.newKeySet();
    private final Queue<BasicBlock> blockQueue = new ArrayDeque<>();
    int depth;
    int counter;
    int bbCounter;
    boolean attr;
    boolean commaNeeded;
    // Queue<String> dependencyList = new ArrayDeque();
    // List<NodePair> bbConnections = new ArrayList<>(); // stores pair of Terminator, BlockEntry
    // private final Queue<PhiValue> phiQueue = new ArrayDeque<>();

    public ConnectionGraphDotVisitor(BasicBlock entryBlock, ConnectionGraph connectionGraph) {
        this.entryBlock = entryBlock;
        this.connectionGraph = connectionGraph;
    }

    @Override
    public String visitUnknown(final Appendable param, Value node) {
        throw new IllegalStateException("Visitor for node " + node.getClass() + " is not implemented");
    }

    @Override
    public String visit(Appendable param, BlockEntry node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final Cmp node) {
        return bypass(param, node);
    }

    @Override
    public String visit(Appendable param, CmpAndSwap node) {
        String name = register(node);
        processDependency(param, node.getExpectedValue());
        processDependency(param, node.getUpdateValue());
        return name;
    }

    @Override
    public String visit(final Appendable param, final CmpL node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final CmpG node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final ElementOf node) {
        String name = register(node);
        processDependency(param, node.getValueHandle());
        processDependency(param, node.getIndex());
        return name;
    }

    @Override
    public String visit(final Appendable param, final GlobalVariable node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final InstanceFieldOf node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "field access\\n"+node.getVariableElement().getName()); // TODO copied from DotNodeVisitor
        attr(param, "style", "filled");
        attr(param, "fillcolor", String.valueOf(nodeType(connectionGraph.getEscapeValue(node)).fillColor));
        nl(param);
        final Collection<Node> pointsTo = connectionGraph.getPointsTo(node, false);
        for (Node pointsToNode : pointsTo) {
            String valueName = getNodeName(param, pointsToNode);
            addEdge(param, name, valueName, EdgeType.POINTS_TO);
        }

        processDependency(param, node.getValueHandle());
        return name;
    }

    @Override
    public String visit(final Appendable param, final MemberOf node) {
        String name = register(node);
        processDependency(param, node.getValueHandle());
        return name;
    }

    @Override
    public String visit(final Appendable param, final MonitorEnter node) {
        String name = bypass(param, node);
        processDependency(param, node.getInstance());
        return name;
    }

    @Override
    public String visit(final Appendable param, final MonitorExit node) {
        String name = bypass(param, node);
        processDependency(param, node.getInstance());
        return name;
    }

    @Override
    public String visit(final Appendable param, final PointerHandle node) {
        String name = register(node);
        processDependency(param, node.getPointerValue());
        return name;
    }


    @Override
    public String visit(final Appendable param, final ReferenceHandle node) {
        String name = register(node);
        processDependency(param, node.getReferenceValue());
        return name;
    }

    @Override
    public String visit(final Appendable param, final StaticField node) {
        String name = register(node);
        appendTo(param, name);
        nl(param);
        attr(param, "label", "static field\\n" + node.getVariableElement().toString());
        attr(param, "style", "filled");
        attr(param, "fillcolor", String.valueOf(nodeType(connectionGraph.getEscapeValue(node)).fillColor));
        nl(param);
        final Collection<Node> pointsTo = connectionGraph.getPointsTo(node, false);
        for (Node pointsToNode : pointsTo) {
            String valueName = getNodeName(param, pointsToNode);
            addEdge(param, name, valueName, EdgeType.POINTS_TO);
        }
        return name;
    }

    // value handles

    @Override
    public String visit(Appendable param, ConstructorElementHandle node) {
        String name = register(node);
        processDependency(param, node.getInstance());
        return name;
    }

    @Override
    public String visit(Appendable param, ExactMethodElementHandle node) {
        String name = register(node);
        processDependency(param, node.getInstance());
        return name;
    }

    @Override
    public String visit(Appendable param, FunctionElementHandle node) {
        return register(node);
    }

    @Override
    public String visit(Appendable param, InterfaceMethodElementHandle node) {
        String name = register(node);
        processDependency(param, node.getInstance());
        return name;
    }

    @Override
    public String visit(Appendable param, VirtualMethodElementHandle node) {
        String name = register(node);
        processDependency(param, node.getInstance());
        return name;
    }

    @Override
    public String visit(Appendable param, LocalVariable node) {
        return register(node);
    }

    @Override
    public String visit(Appendable param, StaticMethodElementHandle node) {
        return register(node);
    }

    // terminators

    // terminator
    public String visit(final Appendable param, final CallNoReturn node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        processDependency(param, node.getValueHandle());
        for (Value arg : node.getArguments()) {
            processDependency(param, arg);
        }
        appendTo(param, "}");
        nl(param);
        return name;
    }

    // terminator
    public String visit(Appendable param, Invoke node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        processDependency(param, node.getValueHandle());
        for (Value arg : node.getArguments()) {
            processDependency(param, arg);
        }
        appendTo(param, "}");
        nl(param);
        addToQueue(node.getCatchBlock());
        addToQueue(node.getResumeTarget());
        return name;
    }

    // terminator

    public String visit(Appendable param, InvokeNoReturn node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        processDependency(param, node.getValueHandle());
        for (Value arg : node.getArguments()) {
            processDependency(param, arg);
        }
        appendTo(param, "}");
        nl(param);
        addToQueue(node.getCatchBlock());
        return name;
    }
    // terminator

    public String visit(Appendable param, TailCall node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        processDependency(param, node.getValueHandle());
        for (Value arg : node.getArguments()) {
            processDependency(param, arg);
        }
        appendTo(param, "}");
        nl(param);
        return name;
    }
    // terminator

    public String visit(Appendable param, TailInvoke node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        processDependency(param, node.getValueHandle());
        for (Value arg : node.getArguments()) {
            processDependency(param, arg);
        }
        appendTo(param, "}");
        nl(param);
        addToQueue(node.getCatchBlock());
        return name;
    }
    // terminator

    public String visit(final Appendable param, final Goto node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        appendTo(param, "}");
        nl(param);
        addToQueue(node.getResumeTarget());
        return name;
    }
    // terminator

    public String visit(final Appendable param, final If node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        processDependency(param, node.getCondition());
        appendTo(param, "}");
        nl(param);
        addToQueue(node.getTrueBranch());
        addToQueue(node.getFalseBranch());
        return name;
    }
    // terminator

    public String visit(final Appendable param, final Jsr node) {
        return bypassTerminator(param, node);
    }
    // terminator

    public String visit(final Appendable param, final Ret node) {
        return bypassTerminator(param, node);
    }
    // terminator

    public String visit(final Appendable param, final Return node) {
        return bypassTerminator(param, node);
    }
    // terminator

    public String visit(final Appendable param, final Invoke.ReturnValue node) {
        processDependency(param, node.getInvoke());
        return visited.get(node.getInvoke());
    }
    // terminator

    public String visit(final Appendable param, final Unreachable node) {
        return bypassTerminator(param, node);
    }
    // terminator

    public String visit(final Appendable param, final Switch node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        appendTo(param, "}");
        nl(param);
        int cnt = node.getNumberOfValues();
        for (int i = 0; i < cnt; i++) {
            addToQueue(node.getTargetForIndex(i));
        }
        return name;
    }
    // terminator

    public String visit(final Appendable param, final Throw node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        processDependency(param, node.getThrownValue());
        appendTo(param, "}");
        nl(param);
        return name;
    }
    // terminator

    public String visit(final Appendable param, final ValueReturn node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        processDependency(param, node.getReturnValue());
        appendTo(param, "}");
        nl(param);
        return name;
    }

    // others

    @Override
    public String visit(Appendable param, InitCheck node) {
        return bypass(param, node);
    }

    @Override
    public String visit(Appendable param, DebugAddressDeclaration node) {
        String name = register(node);
        processDependency(param, node.getAddress());
        processDependency(param, node.getDependency());
        return name;
    }

    @Override
    public String visit(final Appendable param, final Add node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final AddressOf node) {
        String name = register(node);
        processDependency(param, node.getValueHandle());
        return name;
    }

    @Override
    public String visit(final Appendable param, final And node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final BitCast node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final BitCastLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final BlockLiteral node) {
        String name = register(node);
        processDependency(param, node.getBlock().getBlockEntry());
        return name;
    }

    @Override
    public String visit(final Appendable param, final BooleanLiteral node) {
        return register(node);
    }

    @Override
    public String visit(Appendable param, Call node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "call" + "\\n" + node.getValueHandle().toString());
        attr(param, "style", "filled");
        attr(param, "fillcolor", String.valueOf(nodeType(connectionGraph.getEscapeValue(node)).fillColor));
        nl(param);
        processDependency(param, node.getDependency());
        processDependency(param, node.getValueHandle());
        for (Value arg : node.getArguments()) {
            processDependency(param, arg);
        }
        return name;
    }

    @Override
    public String visit(final Appendable param, final CallNoSideEffects node) {
        String name = register(node);
        processDependency(param, node.getValueHandle());
        for (Value arg : node.getArguments()) {
            processDependency(param, arg);
        }
        return name;
    }

    @Override
    public String visit(final Appendable param, final ClassOf node) {
        String name = register(node);
        processDependency(param, node.getInput());
        return name;
    }

    @Override
    public String visit(final Appendable param, final Comp node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final CountLeadingZeros node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final CountTrailingZeros node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final Convert node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final CurrentThreadRead node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final Div node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final Extend node) {
        return bypass(param, node);
    }

    @Override
    public String visit(Appendable param, ExtractElement node) {
        String name = register(node);
        processDependency(param, node.getIndex());
        processDependency(param, node.getArrayValue());
        return name;
    }

    @Override
    public String visit(Appendable param, ExtractInstanceField node) {
        String name = register(node);
        processDependency(param, node.getObjectValue());
        return name;
    }

    @Override
    public String visit(Appendable param, ExtractMember node) {
        String name = register(node);
        processDependency(param, node.getCompoundValue());
        return name;
    }

    @Override
    public String visit(Appendable param, Fence node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final FloatLiteral node) {
        return register(node);
    }

    private String node(Appendable param, ReadModifyWriteValue node) {
        String name = register(node);
        if (node instanceof OrderedNode on) {
            processDependency(param, on.getDependency());
        }
        processDependency(param, node.getValueHandle());
        processDependency(param, node.getUpdateValue());
        return name;
    }

    @Override
    public String visit(Appendable param, GetAndAdd node) {
        return node(param, node);
    }

    @Override
    public String visit(Appendable param, GetAndSet node) {
        return node(param, node);
    }

    @Override
    public String visit(Appendable param, GetAndBitwiseAnd node) {
        return node(param, node);
    }

    @Override
    public String visit(Appendable param, GetAndBitwiseNand node) {
        return node(param, node);
    }

    @Override
    public String visit(Appendable param, GetAndBitwiseOr node) {
        return node(param, node);
    }

    @Override
    public String visit(Appendable param, GetAndBitwiseXor node) {
        return node(param, node);
    }

    @Override
    public String visit(Appendable param, GetAndSetMax node) {
        return node(param, node);
    }

    @Override
    public String visit(Appendable param, GetAndSetMin node) {
        return node(param, node);
    }

    @Override
    public String visit(Appendable param, GetAndSub node) {
        return node(param, node);
    }

    @Override
    public String visit(Appendable param, InsertElement node) {
        String name = register(node);
        processDependency(param, node.getIndex());
        processDependency(param, node.getInsertedValue());
        processDependency(param, node.getArrayValue());
        return name;
    }

    @Override
    public String visit(Appendable param, InsertMember node) {
        String name = register(node);
        processDependency(param, node.getInsertedValue());
        processDependency(param, node.getCompoundValue());
        return name;
    }

    @Override
    public String visit(final Appendable param, final InstanceOf node) {
        String name = bypass(param, node);
        processDependency(param, node.getInstance());
        return name;
    }

    @Override
    public String visit(final Appendable param, final IntegerLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final IsEq node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final IsGe node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final IsGt node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final IsLe node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final IsLt node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final IsNe node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final Load node) {
        String name = bypass(param, node);
        processDependency(param, node.getValueHandle());
        return name;
    }

    @Override
    public String visit(final Appendable param, final MethodHandleLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final Max node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final Min node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final Mod node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final MultiNewArray node) {
        String name = bypass(param, node);
        for (Value dimension : node.getDimensions()) {
            processDependency(param, dimension);
        }
        return name;
    }

    @Override
    public String visit(final Appendable param, final Multiply node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final OffsetOfField node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final CheckCast node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", node.getKind() + "→" + node.getType().toString());
        attr(param, "style", "filled");
        attr(param, "fillcolor", String.valueOf(nodeType(connectionGraph.getEscapeValue(node)).fillColor));
        nl(param);
        final Collection<Node> pointsTo = connectionGraph.getPointsTo(node, false);
        for (Node pointedTo : pointsTo) {
            String pointedToName = getNodeName(param, pointedTo);
            addEdge(param, name, pointedToName, EdgeType.POINTS_TO);
        }
        processDependency(param, node.getDependency());
        processDependency(param, node.getInput());
        processDependency(param, node.getToType());
        processDependency(param, node.getToDimensions());
        return name;
    }

    @Override
    public String visit(final Appendable param, final ConstantLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final Neg node) {
        return bypass(param, node);
    }

    @Override
    public String visit(Appendable param, New node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "new\\n" + show(node));
        attr(param, "style", "filled");
        attr(param, "fillcolor", String.valueOf(nodeType(connectionGraph.getEscapeValue(node)).fillColor));
        nl(param);
        final Collection<InstanceFieldOf> fields = connectionGraph.getFields(node);
        for (InstanceFieldOf field : fields) {
            String fieldName = getNodeName(param, field);
            addEdge(param, name, fieldName, EdgeType.FIELD);
        }
        return name;
    }

    private String show(New node) {
        return node.getType().getUpperBound().toString();
    }

    @Override
    public String visit(final Appendable param, final NewArray node) {
        String name = bypass(param, node);
        processDependency(param, node.getSize());
        return name;
    }

    @Override
    public String visit(final Appendable param, final NewReferenceArray node) {
        String name = bypass(param, node);
        processDependency(param, node.getElemTypeId());
        processDependency(param, node.getDimensions());
        processDependency(param, node.getSize());
        return name;
    }

    @Override
    public String visit(final Appendable param, final NotNull node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final NullLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final ZeroInitializerLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final ObjectLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final Or node) {
        return bypass(param, node);
    }

    @Override
    public String visit(Appendable param, ParameterValue node) {
        String name = register(node);
        appendTo(param, name);

        int index = node.getIndex();
        StringBuilder b = new StringBuilder();
        b.append(node.getType()).append(' ').append("param").append('[').append(node.getLabel());
        if (index > 0) {
            b.append(index);
        }
        b.append(']');

        attr(param, "label", b.toString());
        attr(param, "style", "filled");
        attr(param, "fillcolor", String.valueOf(nodeType(connectionGraph.getEscapeValue(node)).fillColor));
        nl(param);

        final ValueHandle deferred = connectionGraph.getDeferred(node);
        if (deferred != null) {
            String deferredName = getNodeName(param, deferred);
            addEdge(param, name, deferredName, EdgeType.DEFERRED);
        } else {
            final Collection<Node> pointsTo = connectionGraph.getPointsTo(node, false);
            for (Node pointedTo : pointsTo) {
                String pointedToName = getNodeName(param, pointedTo);
                addEdge(param, name, pointedToName, EdgeType.POINTS_TO);
            }
        }

        return name;
    }

    @Override
    public String visit(final Appendable param, final PhiValue node) {
        String name = register(node);
        appendTo(param, name);
        attr(param, "label", "phi");
        attr(param, "style", "filled");
        attr(param, "fillcolor", String.valueOf(nodeType(connectionGraph.getEscapeValue(node)).fillColor));
        nl(param);
        return name;
    }

    @Override
    public String visit(final Appendable param, final Rol node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final Ror node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final Select node) {
        String name = register(node);
        processDependency(param, node.getCondition());
        processDependency(param, node.getTrueValue());
        processDependency(param, node.getFalseValue());
        return name;
    }

    @Override
    public String visit(final Appendable param, final Shl node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final Shr node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final StackAllocation node) {
        String name = bypass(param, node);
        processDependency(param, node.getCount());
        return name;
    }

    @Override
    public String visit(final Appendable param, final Store node) {
        String name = bypass(param, node);
        processDependency(param, node.getValueHandle());
        processDependency(param, node.getValue());
        return name;
    }

    @Override
    public String visit(final Appendable param, final StringLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final ProgramObjectLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final Sub node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final Truncate node) {
        return bypass(param, node);
    }

    @Override
    public String visit(final Appendable param, final TypeLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final UndefinedLiteral node) {
        return register(node);
    }

    @Override
    public String visit(final Appendable param, final Xor node) {
        return bypass(param, node);
    }

    private void addEdge(Appendable param, String fromName, String toName, EdgeType edge) {
        appendTo(param, fromName);
        appendTo(param, " -> ");
        appendTo(param, toName);
        attr(param, "style", edge.style);
        attr(param, "color", edge.color);
        attr(param, "label", " " + edge.label);
        nl(param);
    }

    private String bypass(final Appendable param, BinaryValue node) {
        String name = register(node);
        processDependency(param, node.getLeftInput());
        processDependency(param, node.getRightInput());
        return name;
    }

    private String bypass(Appendable param, OrderedNode node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        return name;
    }

    private String bypass(final Appendable param, CastValue node) {
        String name = register(node);
        processDependency(param, node.getInput());
        return name;
    }

    private String bypass(final Appendable param, UnaryValue node) {
        String name = register(node);
        processDependency(param, node.getInput());
        return name;
    }

    private String bypassTerminator(Appendable param, OrderedNode node) {
        String name = register(node);
        processDependency(param, node.getDependency());
        appendTo(param, "}");
        nl(param);
        return name;
    }

    // TODO copied from DotNodeVisitor
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

    // TODO copied from DotNodeVisitor
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

    // TODO copied from DotNodeVisitor
    private String getNodeName(Appendable param, Action node) {
        String name = visited.get(node);
        if (name == null) {
            name = node.accept(this, param);
        }
        return name;
    }

    // TODO copied from DotNodeVisitor
    private String getNodeName(Appendable param, Value node) {
        String name = visited.get(node);
        if (name == null) {
            name = node.accept(this, param);
        }
        return name;
    }

    // TODO copied from DotNodeVisitor
    private String getNodeName(Appendable param, ValueHandle node) {
        String name = visited.get(node);
        if (name == null) {
            name = node.accept(this, param);
        }
        return name;
    }

    // TODO copied from DotNodeVisitor
    private String getNodeName(Appendable param, Terminator node) {
        String name = visited.get(node);
        if (name == null) {
            name = node.accept(this, param);
        }
        return name;
    }

    // TODO copied from DotNodeVisitor
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

    // TODO copied from DotNodeVisitor
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

    // TODO copied from DotNodeVisitor
    private String register(final Node node) {
        String name = nextName();
        visited.put(node, name);
        return name;
    }

    // TODO copied from DotNodeVisitor
    private String nextName() {
        return "n" + counter++;
    }

    // TODO copied from DotNodeVisitor
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
        // connectBasicBlocks(param);
        // processPhiQueue(param);
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


    // TODO copied from DotNodeVisitor
    void addToQueue(final BasicBlock block) {
        if (blockQueued.add(block)) {
            blockQueue.add(block);
        }
    }

    // TODO copied from DotNodeVisitor
    private String nextBBName() {
        return "b" + bbCounter++;
    }

    private enum EdgeType {
        DEFERRED("black", "dashed"),
        POINTS_TO("black", "solid"),
        FIELD("black", "solid");

        final String color;
        final String style;
        final char label;

        EdgeType(String color, String style) {
            this.color = color;
            this.style = style;
            this.label = this.toString().charAt(0);
        }
    }

    private NodeType nodeType(EscapeValue value) {
        switch (value) {
            case GLOBAL_ESCAPE:
                return NodeType.GLOBAL_ESCAPE;
            case ARG_ESCAPE:
                return NodeType.ARG_ESCAPE;
            case NO_ESCAPE:
                return NodeType.NO_ESCAPE;
            case UNKNOWN:
                return NodeType.UNKNOWN;
            default:
                throw new IllegalStateException("Unknown escape value: " + value);
        }
    }

    private enum NodeType {
        GLOBAL_ESCAPE(2),
        ARG_ESCAPE(3),
        NO_ESCAPE(1),
        UNKNOWN(4);

        final int fillColor;

        NodeType(int fillColor) {
            this.fillColor = fillColor;
        }
    }
    
}
