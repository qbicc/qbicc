package cc.quarkus.qcc.graph.build;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.DotWriter;
import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.AddNode;
import cc.quarkus.qcc.graph.node.BinaryIfNode;
import cc.quarkus.qcc.graph.node.CatchControlProjection;
import cc.quarkus.qcc.graph.node.CompareOp;
import cc.quarkus.qcc.graph.node.ConstantNode;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.GetFieldNode;
import cc.quarkus.qcc.graph.node.GetStaticNode;
import cc.quarkus.qcc.graph.node.IfNode;
import cc.quarkus.qcc.graph.node.InvokeNode;
import cc.quarkus.qcc.graph.node.NewNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PutFieldNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.ReturnNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.graph.node.SubNode;
import cc.quarkus.qcc.graph.node.ThrowNode;
import cc.quarkus.qcc.graph.node.UnaryIfNode;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.type.FieldDescriptor;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.FieldDefinition;
import cc.quarkus.qcc.type.MethodDefinition;
import cc.quarkus.qcc.type.MethodDescriptor;
import cc.quarkus.qcc.type.MethodDescriptorParser;
import cc.quarkus.qcc.type.Sentinel;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.TypeDescriptor;
import cc.quarkus.qcc.type.Universe;
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

public class GraphBuilder<V> {

    public static final int SLOT_COMPLETION = -1;

    //public static final int SLOT_THROW = -2;

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

    protected ControlManager controlManager() {
        return this.controlManager;
    }

    protected void buildControlFlowGraph() {
        InsnList instrList = this.method.getInstructions();
        int instrLen = instrList.size();

        controlManager().recordControlForBci(0, getStart());
        control(getStart());

        List<TryCatchBlockNode> tryCatchBlocks = this.method.getTryCatchBlocks();

        for (TryCatchBlockNode each : tryCatchBlocks) {
            int startIndex = instrList.indexOf(each.start);
            int endIndex = instrList.indexOf(each.end);
            int handlerIndex = instrList.indexOf(each.handler);
            TypeDefinition type = null;
            if (each.type != null) {
                type = Universe.instance().findClass(each.type);
            }
            controlManager().addCatch(type, startIndex, endIndex, handlerIndex);
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
                controlManager().recordControlForBci(destIndex, new RegionNode(getMaxLocals(), getMaxStack()).setLine(line));
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
                    case IF_ICMPLT: {
                        BinaryIfNode<Integer> node = new BinaryIfNode<>(control(), op(opcode));
                        controlManager().recordControlForBci(bci, node);
                        controlManager().addControlInputForBci(bci + 1, node.getFalseOut());
                        controlManager().addControlInputForBci(destIndex, node.getTrueOut());
                        control(node.getFalseOut());
                        break;
                    }
                    case IF_ACMPEQ:
                    case IF_ACMPNE: {
                        BinaryIfNode<ObjectReference> node = new BinaryIfNode<>(control(), op(opcode));
                        controlManager().recordControlForBci(bci, node);
                        controlManager().addControlInputForBci(bci + 1, node.getFalseOut());
                        controlManager().addControlInputForBci(destIndex, node.getTrueOut());
                        control(node.getFalseOut());
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
                        UnaryIfNode node = new UnaryIfNode(control(), op(opcode));
                        controlManager().recordControlForBci(bci, node);
                        controlManager().addControlInputForBci(bci + 1, node.getFalseOut());
                        controlManager().addControlInputForBci(destIndex, node.getTrueOut());
                        control(node.getFalseOut());
                        break;
                    }
                    case GOTO: {
                        controlManager().addControlInputForBci(destIndex, control());
                        control(null);
                        break;
                    }

                }
            } else {
                ControlNode<?> candidate = controlManager().getControlForBci(bci);
                if (candidate != null) {
                    if (control() != null) {
                        if (candidate instanceof RegionNode) {
                            ((RegionNode) candidate).addInput(control());
                        } else {
                            if (candidate.getControl() == null) {
                                candidate.setControl(control());
                            }
                        }
                    }
                    control(candidate);
                }
                switch (opcode) {
                    case RETURN:
                    case IRETURN:
                    case DRETURN:
                    case FRETURN:
                    case LRETURN:
                    case ARETURN: {
                        getEndRegion().addInput(control());
                        control(null);
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

                        InvokeNode<?> node = new InvokeNode<>(control(), descriptor).setLine(line);
                        controlManager().recordControlForBci(bci, node);
                        controlManager().addControlInputForBci(bci + 1, node.getNormalControlOut());
                        control(node.getNormalControlOut());
                        List<TryRange> ranges = controlManager().getCatchesForBci(bci);
                        for (TryRange range : ranges) {
                            for (CatchEntry entry : range.getCatches()) {
                                CatchControlProjection catchProjection = new CatchControlProjection(node.getThrowControlOut(), node.getExceptionOut(), entry.getMatcher());
                                entry.getRegion().addInput(catchProjection);
                            }

                        }
                        CatchControlProjection notCaughtProjection = new CatchControlProjection(node.getThrowControlOut(), node.getExceptionOut(), new CatchMatcher());
                        getEndRegion().addInput(notCaughtProjection);
                        break;
                    }

                }
            }
        }
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

        Map<ControlNode<?>, Set<Node<?>>> dominators = new HashMap<>();
        Set<ControlNode<?>> nodes = controlManager().getAllControlNodes();

        for (ControlNode<?> n : nodes) {
            dominators.put(n, new HashSet<>(Collections.singleton(n)));
        }

        boolean changed = true;

        while (changed) {
            changed = false;
            for (ControlNode<?> n : nodes) {
                //System.err.println( "n=" + n);
                //System.err.println( "n.pred=" + n.getPredecessors());
                Set<Node<?>> tmp = new HashSet<>();
                tmp.add(n);
                tmp.addAll(
                        intersect(
                                n.getPredecessors().stream().map(dominators::get).collect(Collectors.toList())
                        )
                );

                if (!dominators.get(n).equals(tmp)) {
                    dominators.put(n, tmp);
                    changed = true;
                }
            }
        }

        Map<Node<?>, Node<?>> immediateDominators = new HashMap<>();

        for (Node<?> n : nodes) {
            Set<Node<?>> strictDominators = new HashSet<>(dominators.get(n));
            strictDominators.remove(n);
            Node<?> idom = strictDominators.stream()
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
                    Node<?> runner = p;
                    if (this.reachable.contains(runner)) {
                        while (immediateDominators.get(n) != runner) {
                            dominanceFrontier.get(runner).add(n);
                            runner = immediateDominators.get(runner);
                        }
                    }
                }
            }
        }

        controlManager().dominanceFrontier(dominanceFrontier);
    }

    private Collection<Node<?>> intersect(List<Set<Node<?>>> sets) {
        if (sets.isEmpty()) {
            return Collections.emptySet();
        }

        //System.err.println( " intersect: "+ sets);
        // remove any unconnected?
        sets = sets.stream().filter(Objects::nonNull).collect(Collectors.toList());

        Set<Node<?>> intersection = new HashSet<>(sets.get(0));

        for (int i = 1; i < sets.size(); ++i) {
            intersection.retainAll(sets.get(i));
        }

        return intersection;
    }

    protected Set<PhiLocal> placePhis() {
        InsnList instructions = this.method.getInstructions();
        int numInstrs = instructions.size();

        PhiPlacements phiPlacements = new PhiPlacements();

        control(controlManager().getControlForBci(-1));

        for (int bci = 0; bci < numInstrs; ++bci) {
            AbstractInsnNode instr = instructions.get(bci);
            int opcode = instr.getOpcode();

            ControlNode<?> candidate = controlManager().getControlForBci(bci);
            if (candidate != null) {
                control(candidate);
            }

            switch (opcode) {
                case ISTORE: {
                    phiPlacements.record(control(), ((VarInsnNode) instr).var, TypeDescriptor.INT);
                    break;
                }
                case LSTORE: {
                    phiPlacements.record(control(), ((VarInsnNode) instr).var, TypeDescriptor.LONG);
                    break;
                }
                case FSTORE: {
                    phiPlacements.record(control(), ((VarInsnNode) instr).var, TypeDescriptor.FLOAT);
                    break;
                }
                case DSTORE: {
                    phiPlacements.record(control(), ((VarInsnNode) instr).var, TypeDescriptor.DOUBLE);
                    break;
                }
                case ASTORE: {
                    phiPlacements.record(control(), ((VarInsnNode) instr).var, TypeDescriptor.OBJECT);
                    break;
                }
                case INVOKEDYNAMIC:
                case INVOKEINTERFACE:
                case INVOKESPECIAL:
                case INVOKESTATIC:
                case INVOKEVIRTUAL: {
                    phiPlacements.record(control(), SLOT_IO, TypeDescriptor.EphemeralTypeDescriptor.IO_TOKEN);
                    phiPlacements.record(control(), SLOT_MEMORY, TypeDescriptor.EphemeralTypeDescriptor.MEMORY_TOKEN);
                    //phiPlacements.record(control(), SLOT_THROW, TypeDescriptor.OBJECT);
                    break;
                }
                case RETURN:
                case IRETURN:
                case LRETURN:
                case DRETURN:
                case FRETURN:
                case ARETURN: {
                    phiPlacements.record(control(), SLOT_COMPLETION, TypeDescriptor.EphemeralTypeDescriptor.COMPLETION_TOKEN);
                    break;
                }
                case ATHROW: {
                    //phiPlacements.record(control(), SLOT_THROW, null);
                    break;

                }
            }
        }

        Set<PhiLocal> phis = new HashSet<>();
        phiPlacements.forEach((node, entries) -> {
            if (!this.reachable.contains(node)) {
                return;
            }

            Set<ControlNode<?>> df = controlManager().dominanceFrontier(node);

            for (PhiPlacements.Entry entry : entries) {
                for (ControlNode<?> dest : df) {
                    if (dest == getEndRegion() && (entry.index != SLOT_MEMORY && entry.index != SLOT_IO && entry.index != SLOT_COMPLETION)) {
                        // skip
                        continue;
                    }
                    phis.add(dest.frame().ensurePhi(entry.index, node, entry.type));
                }
            }
        });

        return phis;
    }

    protected void determineReachability() {
        Deque<ControlNode<?>> worklist = new ArrayDeque<>();
        worklist.add(getStart());

        while (!worklist.isEmpty()) {
            ControlNode<?> cur = worklist.pop();
            if (this.reachable.contains(cur)) {
                continue;
            }
            this.reachable.add(cur);
            worklist.addAll(cur.getControlSuccessors());
        }
    }

    public Graph<V> build() {
        //ControlManager links = initialize();
        buildControlFlowGraph();

        try (DotWriter writer = new DotWriter(Paths.get("target/cfg.dot"))) {
            //writer.write(new Graph(this.start, this.end));
            writer.write(this.graph);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        determineReachability();

        calculateDominanceFrontiers();
        Set<PhiLocal> phis = placePhis();

        execute();

        for (PhiLocal phi : phis) {
            phi.complete();
        }

        getEndRegion().mergeInputs();

        getEnd().setIO(getEndRegion().frame().io());
        getEnd().setMemory(getEndRegion().frame().memory());
        getEnd().setCompletion(getEndRegion().frame().completion());
        //this.end.setThrowValue(this.endRegion.frame().throwValue());

        tidyGraph();

        //return new Graph(this.start, this.end);
        return this.graph;
    }

    protected void tidyGraph() {
        removeUnreachableControls();
        removeUnreachableDefinitions();
    }

    protected void removeUnreachableControls() {
        Deque<ControlNode<?>> worklist = new ArrayDeque<>();
        worklist.add(getEndRegion());

        Set<ControlNode<?>> processed = new HashSet<>();

        while (!worklist.isEmpty()) {
            ControlNode<?> cur = worklist.pop();
            cur.removeUnreachable(this.reachable);
            processed.add(cur);

            for (ControlNode<?> each : cur.getControlPredecessors()) {
                if (this.reachable.contains(each) && !processed.contains(each)) {
                    worklist.add(each);
                }
            }
        }
    }

    protected void removeUnreachableDefinitions() {
        Deque<Node<?>> worklist = new ArrayDeque<>();
        Set<Node<?>> processed = new HashSet<>();
        worklist.add(getStart());

        while ( ! worklist.isEmpty() ) {
            Node<?> cur = worklist.pop();
            if ( processed.contains(cur)) {
                continue;
            }
            if ( cur.removeUnreachableSuccessors() ) {
                worklist.addAll(cur.getPredecessors());
            } else {
                processed.add(cur);
            }
            worklist.addAll(cur.getSuccessors());
        }
    }

    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public void execute() {
        InsnList instrList = this.method.getInstructions();
        int instrLen = instrList.size();

        control(controlManager().getControlForBci(-1));

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            int opcode = instr.getOpcode();

            ControlNode<?> candidate = controlManager().getControlForBci(bci);

            if (candidate != null) {
                if (this.reachable.contains(candidate)) {
                    candidate.mergeInputs();
                }
                control(candidate);
            }

            if (!this.reachable.contains(control())) {
                continue;
            }

            //System.err.println( control() + " :: " + bci + " :: " + Mnemonics.of(opcode));
            if (instr instanceof JumpInsnNode) {
                if (opcode == GOTO) {
                    // just skip this ish
                } else {
                    IfNode node = null;
                    switch (opcode) {
                        case IFEQ:
                        case IFNE: {
                            Node<Integer> test = pop(Integer.class);
                            node = (IfNode) controlManager().getControlForBci(bci);
                            ((UnaryIfNode) node).setInput(test);
                            break;
                        }
                        case IFNULL:
                        case IFNONNULL: {
                            Node<ObjectReference> test = pop(ObjectReference.class);
                            node = (IfNode) controlManager().getControlForBci(bci);
                            ((UnaryIfNode) node).setInput(test);
                            break;
                        }
                        case IF_ICMPGE: {
                            Node<Integer> rhs = pop(Integer.class);
                            Node<Integer> lhs = pop(Integer.class);
                            node = (IfNode) controlManager().getControlForBci(bci);
                            ((BinaryIfNode<Integer>) node).setLHS(lhs);
                            ((BinaryIfNode<Integer>) node).setRHS(rhs);
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
                if (controlManager().getControlForBci(bci) != null) {
                    control(controlManager().getControlForBci(bci));
                }
                switch (opcode) {
                    case NOP: {
                        break;
                    }
                    case ACONST_NULL: {
                        push(ConstantNode.nullConstant(control()));
                        break;
                    }
                    case ICONST_M1: {
                        push(intConstant(control(), -1));
                        break;
                    }
                    case ICONST_0: {
                        push(intConstant(control(), 0));
                        break;
                    }
                    case ICONST_1: {
                        push(intConstant(control(), 1));
                        break;
                    }
                    case ICONST_2: {
                        push(intConstant(control(), 2));
                        break;
                    }
                    case ICONST_3: {
                        push(intConstant(control(), 3));
                        break;
                    }
                    case ICONST_4: {
                        push(intConstant(control(), 4));
                        break;
                    }
                    case ICONST_5: {
                        push(intConstant(control(), 5));
                        break;
                    }
                    case LCONST_0: {
                        push(longConstant(control(), 0L));
                        break;
                    }
                    case LCONST_1: {
                        push(longConstant(control(), 1L));
                        break;
                    }
                    case FCONST_0: {
                        push(floatConstant(control(), 0F));
                        break;
                    }
                    case FCONST_1: {
                        push(floatConstant(control(), 1F));
                        break;
                    }
                    case FCONST_2: {
                        push(floatConstant(control(), 2F));
                        break;
                    }
                    case DCONST_0: {
                        push(doubleConstant(control(), 0D));
                        break;
                    }
                    case DCONST_1: {
                        push(doubleConstant(control(), 1D));
                        break;
                    }
                    case BIPUSH: {
                        push(byteConstant(control(), (byte) ((IntInsnNode) instr).operand));
                        break;
                    }
                    case SIPUSH: {
                        push(shortConstant(control(), (byte) ((IntInsnNode) instr).operand));
                        break;
                    }
                    case LDC: {
                        Object val = ((LdcInsnNode) instr).cst;
                        push(constant(control(), val));
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
                        storeInt(((VarInsnNode) instr).var, pop(Integer.class));
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

                    case DUP: {
                        Node<?> val = peek(null);
                        push(val);
                        break;
                    }

                    case POP: {
                        pop(null);
                        break;
                    }

                    // ----------------------------------------

                    case IADD: {
                        Node<Integer> val1 = pop(Integer.class);
                        Node<Integer> val2 = pop(Integer.class);
                        push(new AddNode<>(control(), TypeDescriptor.INT, val1, val2, Integer::sum));
                        break;
                    }
                    case ISUB: {
                        Node<Integer> val1 = pop(Integer.class);
                        Node<Integer> val2 = pop(Integer.class);
                        push(new SubNode<>(control(), TypeDescriptor.INT, val1, val2, (l, r) -> l - r));
                        break;
                    }
                    // ----------------------------------------

                    case NEW: {
                        TypeDescriptor<?> type = Universe.instance().findType(((TypeInsnNode) instr).desc);
                        NewNode node = new NewNode(control(), (TypeDescriptor<ObjectReference>) type);
                        push(node);
                        break;
                    }

                    // ----------------------------------------
                    case INVOKEDYNAMIC:
                    case INVOKEINTERFACE:
                    case INVOKESPECIAL:
                    case INVOKESTATIC:
                    case INVOKEVIRTUAL: {
                        InvokeNode<?> node = (InvokeNode<?>) control();

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

                        io(node.getIOOut());
                        memory(node.getMemoryOut());

                        if (returnType != Void.class) {
                            push(node.getResultOut());
                        }

                        node.getThrowControlOut().frame().io(io());
                        node.getThrowControlOut().frame().memory(memory());

                        for (ControlNode<?> each : node.getThrowControlOut().getControlSuccessors()) {
                            if (each instanceof CatchControlProjection && each.getControlSuccessors().contains(getEndRegion())) {
                                each.frame().completion(((CatchControlProjection) each).getCompletionOut());
                                each.frame().io(io());
                                each.frame().memory(memory());
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
                        GetFieldNode<?> node = new GetFieldNode<>(control(), objectRef, field);
                        push(node);
                        break;
                    }
                    case GETSTATIC: {
                        String name = ((FieldInsnNode) instr).name;
                        String owner = ((FieldInsnNode) instr).owner;
                        TypeDefinition type = Universe.instance().findClass(owner);
                        FieldDefinition<?> field = type.findField(name);
                        GetStaticNode<?> node = new GetStaticNode<>(control(), field);
                        push(node);
                        break;
                    }
                    case ATHROW: {
                        Node<? extends ObjectReference> thrown = pop(ObjectReference.class);
                        clear();
                        ThrowNode<? extends ObjectReference> node = new ThrowNode<>(control(), thrown);
                        /*
                        List<ControlFlowHelper.CatchEntry> catches = links.catchesFor(bci);
                        for (ControlFlowHelper.CatchEntry each : catches) {
                            CatchControlProjection catchProjection = new CatchControlProjection(control(), each.type);
                            each.handler.addInput(catchProjection);
                        }
                         */
                        getEndRegion().frame().mergeFrom(control().frame());
                        break;
                    }

                    case RETURN: {
                        ConstantNode<Sentinel.Void> val = voidConstant(control());
                        ReturnNode<Sentinel.Void> ret = new ReturnNode<>(control(), val);
                        control().frame().completion(ret);
                        getEndRegion().frame().mergeFrom(control().frame());
                        control(null);
                        break;
                    }

                    case IRETURN: {
                        Node<Integer> val = pop(Integer.class);
                        ReturnNode<Integer> ret = new ReturnNode<>(control(), val);
                        control().frame().completion(ret);
                        getEndRegion().frame().mergeFrom(control().frame());
                        control(null);
                        break;
                    }
                    case ARETURN: {
                        Node<ObjectReference> val = pop(ObjectReference.class);
                        ReturnNode<ObjectReference> ret = new ReturnNode<>(control(), val);
                        control().frame().completion(ret);
                        getEndRegion().frame().mergeFrom(control().frame());
                        control(null);
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

    protected <V> PutFieldNode<V> newPutField(FieldDescriptor<V> field) {
        Node<V> value = pop(field.getTypeDescriptor().valueType() );
        Node<ObjectReference> objRef = pop(ObjectReference.class);
        return new PutFieldNode<>(control(), objRef, value, field, memory());
    }

    protected Node<Integer> loadInt(int index) {
        return control().frame().load(index, Integer.class);
        //return null;
    }

    protected void storeInt(int index, Node<Integer> val) {
        control().frame().store(index, val);
    }

    protected Node<Long> loadLong(int index) {
        return control().frame().load(index, Long.class);
    }

    protected Node<Float> loadFloat(int index) {
        return control().frame().load(index, Float.class);
    }

    protected Node<Double> loadDouble(int index) {
        return control().frame().load(index, Double.class);
    }

    protected Node<ObjectReference> loadObject(int index) {
        return control().frame().load(index, ObjectReference.class);
    }

    protected void storeObject(int index, Node<ObjectReference> val) {
        control().frame().store(index, val);
    }


    protected void initialize() {
        this.graph = new Graph<>(this.method);
        control(getStart());
        this.controlManager = new ControlManager(getStart());
    }

    protected ControlNode<?> control() {
        return this.control;
    }

    protected Node<IOToken> io() {
        return control().frame().io();
    }

    protected void io(Node<IOToken> io) {
        control().frame().io(io);
    }

    protected Node<MemoryToken> memory() {
        return control().frame().memory();
    }

    protected void memory(Node<MemoryToken> memory) {
        control().frame().memory(memory);
    }

    protected void control(ControlNode<?> node) {
        this.control = node;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected <T> Node<T> push(Node<T> node) {
        return control().frame().push(node);
    }

    protected <T> Node<T> pop(Class<T> type) {
        return control().frame().pop(type);
    }

    @SuppressWarnings("SameParameterValue")
    protected <T> Node<T> peek(Class<T> type) {
        return control().frame().peek(type);
    }

    protected void clear() {
        control().frame().clear();
    }

    private Graph<V> graph;

    private ControlManager controlManager;

    private final MethodDefinition<V> method;

    private ControlNode<?> control;

    private Set<ControlNode<?>> reachable = new HashSet<>();

}
