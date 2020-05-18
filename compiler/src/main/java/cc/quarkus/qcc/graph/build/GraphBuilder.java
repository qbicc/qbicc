package cc.quarkus.qcc.graph.build;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.DotWriter;
import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.BinaryIfNode;
import cc.quarkus.qcc.graph.node.CatchControlProjection;
import cc.quarkus.qcc.graph.node.CompareOp;
import cc.quarkus.qcc.graph.node.ConstantNode;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.IfNode;
import cc.quarkus.qcc.graph.node.InvokeNode;
import cc.quarkus.qcc.graph.node.NarrowNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PutFieldNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.ReturnNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.graph.node.ThrowNode;
import cc.quarkus.qcc.graph.node.UnaryIfNode;
import cc.quarkus.qcc.graph.node.WidenNode;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.type.QDouble;
import cc.quarkus.qcc.type.QFloat;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QInt64;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.QVoid;
import cc.quarkus.qcc.type.definition.FieldDefinition;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;
import cc.quarkus.qcc.type.definition.MethodDefinition;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptorParser;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.universe.Universe;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static cc.quarkus.qcc.graph.node.ConstantNode.*;
import static org.objectweb.asm.Opcodes.*;

public class GraphBuilder<V extends QType> {

    public static final int SLOT_COMPLETION = -1;

    public static final int SLOT_IO = -2;

    public static final int SLOT_MEMORY = -3;

    public GraphBuilder(MethodDefinition<V> method) {
        this.method = method;
        initialize();
    }

    protected StartNode getStart() {
        return this.graph.getStart();
    }

    protected EndNode<?> getEnd() {
        return this.graph.getEnd();
    }

    protected RegionNode getEndRegion() {
        return this.graph.getEndRegion();
    }

    protected int getMaxLocals() {
        return this.method.getMaxLocals();
    }

    protected int getMaxStack() {
        return this.method.getMaxStack();
    }


    protected NodeManager nodeManager() {
        return this.nodeManager;
    }

    protected NodeFactory nodeFactory() {
        return nodeManager().nodeFactory();
    }

    protected FrameManager frameManager() {
        return nodeManager().frameManager();
    }

    @SuppressWarnings("ConstantConditions")
    protected DefUse buildStructure() {
        InsnList instrList = this.method.getInstructions();
        int instrLen = instrList.size();

        DefUse defUse = new DefUse(nodeManager());

        //controlManager().recordControlForBci(0, getStart());
        nodeManager().initializeControlForStructure(-1);
        //setControl(getStart());

        List<TryCatchBlockNode> tryCatchBlocks = this.method.getTryCatchBlocks();

        for (TryCatchBlockNode each : tryCatchBlocks) {
            int startIndex = instrList.indexOf(each.start);
            int endIndex = instrList.indexOf(each.end);
            int handlerIndex = instrList.indexOf(each.handler);
            TypeDefinition type = null;
            if (each.type != null) {
                type = Universe.instance().findClass(each.type);
            }
            nodeManager().addCatch(type, startIndex, endIndex, handlerIndex);
        }

        int line = 0;

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            if (instr instanceof LineNumberNode) {
                line = ((LineNumberNode) instr).line;
            }
            if (instr instanceof JumpInsnNode) {
                LabelNode dest = ((JumpInsnNode) instr).label;
                int destIndex = instrList.indexOf(dest);
                nodeManager().recordControlForBci(destIndex, nodeFactory().regionNode().setLine(line));
            }
        }

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            int opcode = instr.getOpcode();

            if (instr instanceof LineNumberNode) {
                line = ((LineNumberNode) instr).line;
            }

            if (instr instanceof JumpInsnNode) {
                LabelNode dest = ((JumpInsnNode) instr).label;
                int destIndex = instrList.indexOf(dest);
                switch (opcode) {
                    case IF_ICMPEQ:
                    case IF_ICMPNE:
                    case IF_ICMPGE:
                    case IF_ICMPLE:
                    case IF_ICMPGT:
                    case IF_ICMPLT:
                    case IF_ACMPEQ:
                    case IF_ACMPNE: {
                        BinaryIfNode<?> node = nodeFactory().binaryIfNode(op(opcode));
                        nodeManager().recordControlForBci(bci, node);
                        nodeManager().addControlInputForBci(bci + 1, node.getFalseOut());
                        nodeManager().addControlInputForBci(destIndex, node.getTrueOut());
                        nodeManager().setControl(node.getFalseOut());
                        break;
                    }
                    case IFEQ:
                    case IFNE:
                    case IFGE:
                    case IFLE:
                    case IFGT:
                    case IFLT:
                    case IFNULL:
                    case IFNONNULL: {
                        UnaryIfNode node = nodeFactory().unaryIfNode(op(opcode));
                        nodeManager().recordControlForBci(bci, node);
                        nodeManager().addControlInputForBci(bci + 1, node.getFalseOut());
                        nodeManager().addControlInputForBci(destIndex, node.getTrueOut());
                        nodeManager().setControl(node.getFalseOut());
                        break;
                    }
                    case GOTO: {
                        nodeManager().addControlInputForBci(destIndex, currentControl());
                        nodeManager().clearControl();
                        break;
                    }
                }
            } else {
                nodeManager().initializeControlForStructure(bci);
                switch (opcode) {
                    case ASTORE: {
                        defUse.def(currentControl(), ((VarInsnNode) instr).var, TypeDescriptor.OBJECT);
                        break;
                    }
                    case ISTORE: {
                        defUse.def(currentControl(), ((VarInsnNode) instr).var, TypeDescriptor.INT32);
                        break;
                    }
                    case LSTORE: {
                        defUse.def(currentControl(), ((VarInsnNode) instr).var, TypeDescriptor.INT64);
                        break;
                    }
                    case FSTORE: {
                        defUse.def(currentControl(), ((VarInsnNode) instr).var, TypeDescriptor.FLOAT);
                        break;
                    }
                    case DSTORE: {
                        defUse.def(currentControl(), ((VarInsnNode) instr).var, TypeDescriptor.DOUBLE);
                        break;
                    }
                    case ALOAD: {
                        defUse.use(currentControl(), ((VarInsnNode) instr).var, TypeDescriptor.OBJECT);
                        break;
                    }
                    case ILOAD: {
                        defUse.use(currentControl(), ((VarInsnNode) instr).var, TypeDescriptor.INT32);
                        break;
                    }
                    case LLOAD: {
                        defUse.use(currentControl(), ((VarInsnNode) instr).var, TypeDescriptor.INT64);
                        break;
                    }
                    case FLOAD: {
                        defUse.use(currentControl(), ((VarInsnNode) instr).var, TypeDescriptor.FLOAT);
                        break;
                    }
                    case DLOAD: {
                        defUse.use(currentControl(), ((VarInsnNode) instr).var, TypeDescriptor.DOUBLE);
                        break;
                    }
                    case RETURN:
                    case IRETURN:
                    case DRETURN:
                    case FRETURN:
                    case LRETURN:
                    case ARETURN: {
                        defUse.def(currentControl(), SLOT_COMPLETION, EphemeralTypeDescriptor.COMPLETION_TOKEN);
                        getEndRegion().addInput(currentControl());
                        nodeManager().clearControl();
                        break;
                    }
                    case INVOKEVIRTUAL:
                    case INVOKESPECIAL:
                    case INVOKEINTERFACE:
                    case INVOKEDYNAMIC:
                    case INVOKESTATIC: {
                        assert instr instanceof MethodInsnNode;
                        MethodDescriptorParser parser = new MethodDescriptorParser(Universe.instance(),
                                                                                   Universe.instance().findClass(((MethodInsnNode) instr).owner),
                                                                                   ((MethodInsnNode) instr).name,
                                                                                   ((MethodInsnNode) instr).desc,
                                                                                   opcode == INVOKESTATIC);
                        MethodDescriptor<?> descriptor = parser.parseMethodDescriptor();

                        InvokeNode<?> node = new InvokeNode<>(graph(), currentControl(), descriptor, invocationType(opcode)).setLine(line);
                        nodeManager().recordControlForBci(bci, node);
                        nodeManager().addControlInputForBci(bci + 1, node.getNormalControlOut());
                        defUse.def(node, SLOT_IO, EphemeralTypeDescriptor.IO_TOKEN);
                        defUse.def(node, SLOT_MEMORY, EphemeralTypeDescriptor.MEMORY_TOKEN);
                        nodeManager().setControl(node.getNormalControlOut());
                        List<TryRange> ranges = nodeManager().getCatchesForBci(bci);
                        for (TryRange range : ranges) {
                            for (CatchEntry entry : range.getCatches()) {
                                CatchControlProjection catchProjection = new CatchControlProjection(graph(), node.getThrowControlOut(), node.getExceptionOut(), entry.getMatcher());
                                entry.getRegion().addInput(catchProjection);
                            }
                        }
                        // TODO check for possibility of uncaught avoidance.
                        CatchControlProjection notCaughtProjection = new CatchControlProjection(graph(), node.getThrowControlOut(), node.getExceptionOut(), new CatchMatcher());
                        getEndRegion().addInput(notCaughtProjection);
                        defUse.def(notCaughtProjection, SLOT_COMPLETION, EphemeralTypeDescriptor.COMPLETION_TOKEN);
                        break;
                    }

                    case ATHROW: {
                        ThrowNode node = new ThrowNode(graph(), currentControl());
                        nodeManager().recordControlForBci(bci, node);

                        List<TryRange> ranges = nodeManager().getCatchesForBci(bci);
                        for (TryRange range : ranges) {
                            for (CatchEntry entry : range.getCatches()) {
                                CatchControlProjection catchProjection = new CatchControlProjection(graph(), node.getThrowControlOut(), node.getExceptionOut(), entry.getMatcher());
                                entry.getRegion().addInput(catchProjection);
                            }
                        }
                        // TODO check for possibility of uncaught avoidance.
                        CatchControlProjection notCaughtProjection = new CatchControlProjection(graph(), node.getThrowControlOut(), node.getExceptionOut(), new CatchMatcher());
                        getEndRegion().addInput(notCaughtProjection);
                        defUse.def(notCaughtProjection, SLOT_COMPLETION, EphemeralTypeDescriptor.COMPLETION_TOKEN);
                        break;
                    }
                }
            }
        }

        defUse.use(getEndRegion(), SLOT_COMPLETION, EphemeralTypeDescriptor.COMPLETION_TOKEN);
        defUse.use(getEndRegion(), SLOT_IO, EphemeralTypeDescriptor.IO_TOKEN);
        defUse.use(getEndRegion(), SLOT_MEMORY, EphemeralTypeDescriptor.MEMORY_TOKEN);

        return defUse;
    }

    protected InvokeNode.InvocationType invocationType(int opcode) {
        switch ( opcode ) {
            case INVOKESTATIC: {
                return InvokeNode.InvocationType.STATIC;
            }
            case INVOKESPECIAL: {
                return InvokeNode.InvocationType.SPECIAL;
            }
            case INVOKEVIRTUAL:
            case INVOKEINTERFACE: {
                return InvokeNode.InvocationType.VIRTUAL;
            }
        }

        return null;
    }

    protected CompareOp op(int opcode) {
        switch (opcode) {
            case IFEQ:
            case IF_ACMPEQ:
            case IF_ICMPEQ: {
                return CompareOp.EQUAL;
            }
            case IFNE:
            case IF_ACMPNE:
            case IF_ICMPNE: {
                return CompareOp.NOT_EQUAL;
            }
            case IF_ICMPGE: {
                return CompareOp.GREATER_THAN_OR_EQUAL;
            }
            case IF_ICMPLE: {
                return CompareOp.LESS_THAN_OR_EQUAL;
            }
            case IF_ICMPGT: {
                return CompareOp.GREATER_THAN;
            }
            case IF_ICMPLT: {
                return CompareOp.LESS_THAN;
            }
            case IFNULL: {
                return CompareOp.NULL;
            }
            case IFNONNULL: {
                return CompareOp.NONNULL;
            }
        }
        return null;

    }

    @SuppressWarnings("SuspiciousMethodCalls")
    protected void calculateDominanceFrontiers() {

        Map<ControlNode<?>, Set<ControlNode<?>>> dominators = new HashMap<>();
        Set<ControlNode<?>> nodes = nodeManager().getAllControlNodes();

        for (ControlNode<?> n : nodes) {
            dominators.put(n, new HashSet<>(Collections.singleton(n)));
        }

        boolean changed = true;

        while (changed) {
            changed = false;
            for (ControlNode<?> n : nodes) {
                //System.err.println( "n=" + n);
                //System.err.println( "n.pred=" + n.getPredecessors());
                Set<ControlNode<?>> tmp = new HashSet<>();
                tmp.add(n);
                tmp.addAll(
                        intersect(
                                n.getControlPredecessors().stream().map(dominators::get).collect(Collectors.toList())
                        )
                );

                if (!dominators.get(n).equals(tmp)) {
                    dominators.put(n, tmp);
                    changed = true;
                }
            }
        }

        Map<ControlNode<?>, ControlNode<?>> immediateDominators = new HashMap<>();

        for (ControlNode<?> n : nodes) {
            Set<ControlNode<?>> strictDominators = new HashSet<>(dominators.get(n));
            strictDominators.remove(n);
            ControlNode<?> idom = strictDominators.stream()
                    .filter(e -> dominators.get(e).containsAll(strictDominators))
                    .findFirst().orElse(null);

            immediateDominators.put(n, idom);
        }

        Map<ControlNode<?>, Set<ControlNode<?>>> dominanceFrontier = new HashMap<>();

        for (ControlNode<?> n : nodes) {
            dominanceFrontier.put(n, new HashSet<>());
        }

        for (ControlNode<?> n : nodes) {
            if (n.getPredecessors().size() > 1) {
                for (ControlNode<?> p : n.getControlPredecessors()) {
                    ControlNode<?> runner = p;
                    while (immediateDominators.get(n) != runner) {
                        dominanceFrontier.get(runner).add(n);
                        runner = immediateDominators.get(runner);
                    }
                }
            }
        }

        nodeManager().dominanceFrontier(dominanceFrontier);
    }

    private Collection<ControlNode<?>> intersect(List<Set<ControlNode<?>>> sets) {
        if (sets.isEmpty()) {
            return Collections.emptySet();
        }

        //System.err.println( " intersect: "+ sets);
        // remove any unconnected?
        sets = sets.stream().filter(Objects::nonNull).collect(Collectors.toList());

        Set<ControlNode<?>> intersection = new HashSet<>(sets.get(0));

        for (int i = 1; i < sets.size(); ++i) {
            intersection.retainAll(sets.get(i));
        }

        return intersection;
    }

    public Graph<V> build() {
        DefUse defUse = buildStructure();

        try (DotWriter writer = new DotWriter(Paths.get("target/cfg.dot"))) {
            //writer.write(new Graph(this.start, this.end));
            writer.write(graph());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        calculateDominanceFrontiers();

        defUse.placePhis();
        buildInstructions();
        defUse.completePhis();

        nodeManager().mergeInputs(getEndRegion());

        getEnd().setIO(frameOf(getEndRegion()).io());
        getEnd().setMemory(frameOf(getEndRegion()).memory());
        getEnd().setCompletion(frameOf(getEndRegion()).completion());


        return this.graph;
    }

    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public void buildInstructions() {
        InsnList instrList = this.method.getInstructions();
        int instrLen = instrList.size();

        //setControl(nodeManager().getControlChainForBci(-1));
        nodeManager().initializeControlForInstructions(-1);

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            int opcode = instr.getOpcode();

            nodeManager().initializeControlForInstructions(bci);

            //if (!this.reachable.contains(currentControl())) {
            //   continue;
            //}

            //System.err.println( currentControl() + " :: " + bci + " :: " + Mnemonics.of(opcode));
            if (instr instanceof JumpInsnNode) {
                if (opcode == GOTO) {
                    // just skip this ish
                } else {
                    IfNode node = null;
                    switch (opcode) {
                        case IFEQ:
                        case IFNE: {
                            Node<QInt32> test = pop(QInt32.class);
                            node = (IfNode) nodeManager().getControlForBci(bci);
                            ((UnaryIfNode) node).setTest(test);
                            break;
                        }
                        case IFNULL:
                        case IFNONNULL: {
                            Node<ObjectReference> test = pop(ObjectReference.class);
                            node = (IfNode) nodeManager().getControlForBci(bci);
                            ((UnaryIfNode) node).setTest(test);
                            break;
                        }
                        case IF_ICMPEQ:
                        case IF_ICMPNE:
                        case IF_ICMPGT:
                        case IF_ICMPLT:
                        case IF_ICMPGE:
                        case IF_ICMPLE: {
                            Node<QInt32> rhs = pop(QInt32.class);
                            Node<QInt32> lhs = pop(QInt32.class);
                            node = (IfNode) nodeManager().getControlForBci(bci);
                            ((BinaryIfNode<QInt32>) node).setLHS(lhs);
                            ((BinaryIfNode<QInt32>) node).setRHS(rhs);
                            break;
                        }
                        default: {
                            if (opcode >= 0) {
                                System.err.println("unhandled: " + Mnemonics.of(opcode) + " " + opcode);
                            }
                        }
                    }

                    assert node != null;
                }
            } else {
                //if (nodeManager().getControlChainForBci(bci) != null) {
                //setControl(nodeManager().getControlChainForBci(bci));
                //}
                switch (opcode) {
                    case NOP: {
                        break;
                    }
                    case ACONST_NULL: {
                        push(ConstantNode.nullConstant(currentControl()));
                        break;
                    }
                    case ICONST_M1: {
                        push(intConstant(currentControl(), -1));
                        break;
                    }
                    case ICONST_0: {
                        push(intConstant(currentControl(), 0));
                        break;
                    }
                    case ICONST_1: {
                        push(intConstant(currentControl(), 1));
                        break;
                    }
                    case ICONST_2: {
                        push(intConstant(currentControl(), 2));
                        break;
                    }
                    case ICONST_3: {
                        push(intConstant(currentControl(), 3));
                        break;
                    }
                    case ICONST_4: {
                        push(intConstant(currentControl(), 4));
                        break;
                    }
                    case ICONST_5: {
                        push(intConstant(currentControl(), 5));
                        break;
                    }
                    case LCONST_0: {
                        push(longConstant(currentControl(), 0L));
                        break;
                    }
                    case LCONST_1: {
                        push(longConstant(currentControl(), 1L));
                        break;
                    }
                    case FCONST_0: {
                        push(floatConstant(currentControl(), 0F));
                        break;
                    }
                    case FCONST_1: {
                        push(floatConstant(currentControl(), 1F));
                        break;
                    }
                    case FCONST_2: {
                        push(floatConstant(currentControl(), 2F));
                        break;
                    }
                    case DCONST_0: {
                        push(doubleConstant(currentControl(), 0D));
                        break;
                    }
                    case DCONST_1: {
                        push(doubleConstant(currentControl(), 1D));
                        break;
                    }
                    case BIPUSH: {
                        push(intConstant(currentControl(), ((IntInsnNode) instr).operand));
                        break;
                    }
                    case SIPUSH: {
                        push(intConstant(currentControl(), ((IntInsnNode) instr).operand));
                        break;
                    }
                    case LDC: {
                        Object val = ((LdcInsnNode) instr).cst;
                        push(constant(currentControl(), val));
                        break;
                    }
                    case ASTORE: {
                        storeObject(((VarInsnNode) instr).var, pop(null));
                        break;
                    }
                    case ILOAD: {
                        push(loadInt(((VarInsnNode) instr).var));
                        break;
                    }
                    case ISTORE: {
                        storeInt(((VarInsnNode) instr).var, pop(QInt32.class));
                        break;
                    }
                    case LLOAD: {
                        push(loadLong(((VarInsnNode) instr).var));
                        break;
                    }
                    case FLOAD: {
                        push(loadFloat(((VarInsnNode) instr).var));
                        break;
                    }
                    case DLOAD: {
                        push(loadDouble(((VarInsnNode) instr).var));
                        break;
                    }
                    case ALOAD: {
                        push(loadObject(((VarInsnNode) instr).var));
                        break;
                    }
                    case I2B: {
                        push(NarrowNode.i2b(graph(), currentControl(), pop(QInt32.class)));
                        break;
                    }
                    case I2C: {
                        push(NarrowNode.i2c(graph(), currentControl(), pop(QInt32.class)));
                        break;
                    }
                    case I2S: {
                        push(NarrowNode.i2s(graph(), currentControl(), pop(QInt32.class)));
                        break;
                    }
                    case I2L: {
                        push(WidenNode.i2l(graph(), currentControl(), pop(QInt32.class)));
                        break;
                    }
                    case L2I: {
                        push(NarrowNode.l2i(graph(), currentControl(), pop(QInt64.class)));
                        break;
                    }
                    case DUP: {
                        Node<?> val = peek(null);
                        push(val);
                        break;
                    }
                    case POP: {
                        pop(null);
                        break;
                    }
                    case POP2: {
                        Node<?> v = pop(null);
                        if (v.getTypeDescriptor() != TypeDescriptor.INT64 && v.getTypeDescriptor() != TypeDescriptor.DOUBLE) {
                            // only pop 1 if double or long.
                            pop(null);
                        }
                    }

                    // ----------------------------------------

                    case IADD: {
                        Node<QInt32> val1 = pop(QInt32.class);
                        Node<QInt32> val2 = pop(QInt32.class);
                        push(nodeFactory().addNode(TypeDescriptor.INT32, val1, val2, (l, r) -> QInt32.of(l.value() + r.value())));
                        break;
                    }
                    case ISUB: {
                        Node<QInt32> val1 = pop(QInt32.class);
                        Node<QInt32> val2 = pop(QInt32.class);
                        push(nodeFactory().subNode(TypeDescriptor.INT32, val1, val2, (l, r) -> QInt32.of(l.value() - r.value())));
                        break;
                    }
                    // ----------------------------------------

                    case NEW: {
                        TypeDescriptor<?> type = Universe.instance().findType(((TypeInsnNode) instr).desc);
                        push(nodeFactory().newNode((TypeDescriptor<ObjectReference>) type));
                        break;
                    }

                    // ----------------------------------------
                    case INVOKEDYNAMIC:
                    case INVOKEINTERFACE:
                    case INVOKESPECIAL:
                    case INVOKESTATIC:
                    case INVOKEVIRTUAL: {
                        InvokeNode<?> node = (InvokeNode<?>) currentControl();

                        Class<?> returnType = node.getType();

                        Node<?>[] args = new Node<?>[node.getParamTypes().size()];

                        for (int i = args.length - 1; i >= 0; --i) {
                            args[i] = pop(null);
                        }

                        for (Node<?> arg : args) {
                            node.addArgument(arg);
                        }

                        node.setIO(io());
                        node.setMemory(memory());

                        io(node.getIO());
                        memory(node.getMemory());

                        if (returnType != QVoid.class) {
                            push(node.getResultOut());
                        }

                        frameOf(node.getThrowControlOut()).io(io());
                        frameOf(node.getThrowControlOut()).memory(memory());

                        for (ControlNode<?> each : node.getThrowControlOut().getControlSuccessors()) {
                            if (each instanceof CatchControlProjection && each.getControlSuccessors().contains(getEndRegion())) {
                                frameOf(each).completion(((CatchControlProjection) each).getCompletionOut());
                                frameOf(each).io(io());
                                frameOf(each).memory(memory());
                            }
                        }

                        break;
                    }
                    case ATHROW: {
                        Node<ObjectReference> thrown = pop(ObjectReference.class);
                        ThrowNode node = (ThrowNode) currentControl();
                        node.setThrown(thrown);
                        clear();
                        push(thrown);

                        frameOf(node.getThrowControlOut()).io(io());
                        frameOf(node.getThrowControlOut()).memory(memory());

                        for (ControlNode<?> each : node.getThrowControlOut().getControlSuccessors()) {
                            if (each instanceof CatchControlProjection && each.getControlSuccessors().contains(getEndRegion())) {
                                frameOf(each).completion(((CatchControlProjection) each).getCompletionOut());
                                frameOf(each).io(io());
                                frameOf(each).memory(memory());
                            }
                        }
                        break;
                    }
                    case PUTFIELD: {
                        String name = ((FieldInsnNode) instr).name;
                        String owner = ((FieldInsnNode) instr).owner;
                        TypeDefinition type = Universe.instance().findClass(owner);
                        FieldDefinition<?> field = type.findField(name);
                        PutFieldNode<?> node = newPutField(field);
                        memory(node);
                        break;
                    }

                    case GETFIELD: {
                        String name = ((FieldInsnNode) instr).name;
                        String owner = ((FieldInsnNode) instr).owner;

                        TypeDefinition type = Universe.instance().findClass(owner);
                        FieldDefinition<?> field = type.findField(name);

                        Node<ObjectReference> objectRef = pop(ObjectReference.class);
                        push(nodeFactory().getFieldNode(objectRef, field));
                        break;
                    }
                    case GETSTATIC: {
                        String name = ((FieldInsnNode) instr).name;
                        String owner = ((FieldInsnNode) instr).owner;
                        TypeDefinition type = Universe.instance().findClass(owner);
                        FieldDefinition<?> field = type.findField(name);
                        push(nodeFactory().getStaticNode(field));
                        break;
                    }


                    case RETURN: {
                        ConstantNode<QVoid> val = voidConstant(currentControl());
                        ReturnNode<QVoid> ret = nodeFactory().returnNode(val);
                        currentFrame().completion(ret);
                        frameOf(getEndRegion()).mergeFrom(currentFrame());
                        nodeManager().clearControl();
                        break;
                    }

                    case IRETURN: {
                        Node<QInt32> val = pop(QInt32.class);
                        ReturnNode<QInt32> ret = nodeFactory().returnNode(val);
                        currentFrame().completion(ret);
                        frameOf(getEndRegion()).mergeFrom(currentFrame());
                        nodeManager().clearControl();
                        break;
                    }
                    case ARETURN: {
                        Node<ObjectReference> val = pop(ObjectReference.class);
                        ReturnNode<ObjectReference> ret = nodeFactory().returnNode(val);
                        currentFrame().completion(ret);
                        frameOf(getEndRegion()).mergeFrom(currentFrame());
                        nodeManager().clearControl();
                        break;
                    }
                    default: {
                        if (opcode >= 0) {
                            System.err.println("unhandled: " + Mnemonics.of(opcode) + " " + opcode);
                        }
                    }
                }
            }
        }
    }

    protected <V extends QType> PutFieldNode<V> newPutField(FieldDescriptor<V> field) {
        Node<V> value = pop(field.getTypeDescriptor().type());
        Node<ObjectReference> objectRef = pop(ObjectReference.class);
        return nodeFactory().putFieldNode(objectRef, value, field, memory());
    }

    protected Node<QInt32> loadInt(int index) {
        return currentFrame().load(index, QInt32.class);
    }

    protected void storeInt(int index, Node<QInt32> val) {
        currentFrame().store(index, val);
    }

    protected Node<QInt64> loadLong(int index) {
        return currentFrame().load(index, QInt64.class);
    }

    protected Node<QFloat> loadFloat(int index) {
        return currentFrame().load(index, QFloat.class);
    }

    protected Node<QDouble> loadDouble(int index) {
        return currentFrame().load(index, QDouble.class);
    }

    protected Node<ObjectReference> loadObject(int index) {
        return currentFrame().load(index, ObjectReference.class);
    }

    protected void storeObject(int index, Node<ObjectReference> val) {
        currentFrame().store(index, val);
    }


    protected void initialize() {
        this.graph = new Graph<>(this.method);
        this.nodeManager = new NodeManager(graph());
    }

    protected Node<IOToken> io() {
        return currentFrame().io();
    }

    protected void io(Node<IOToken> io) {
        currentFrame().io(io);
    }

    protected Node<MemoryToken> memory() {
        return currentFrame().memory();
    }

    protected void memory(Node<MemoryToken> memory) {
        currentFrame().memory(memory);
    }

    protected ControlNode<?> currentControl() {
        return nodeManager().currentControl();
    }

    protected Frame currentFrame() {
        return frameManager().of(currentControl());
    }

    protected Frame frameOf(ControlNode<?> control) {
        return frameManager().of(control);
    }

    @SuppressWarnings("UnusedReturnValue")
    protected <T extends QType> Node<T> push(Node<T> node) {
        return currentFrame().push(node);
    }

    protected <T extends QType> Node<T> pop(Class<T> type) {
        return currentFrame().pop(type);
    }

    @SuppressWarnings("SameParameterValue")
    protected <T extends QType> Node<T> peek(Class<T> type) {
        return currentFrame().peek(type);
    }

    protected void clear() {
        currentFrame().clear();
    }

    protected Graph<V> graph() {
        return this.graph;
    }

    private Graph<V> graph;

    private NodeManager nodeManager;

    private final MethodDefinition<V> method;

    //private ControlNode<?> control;

}
