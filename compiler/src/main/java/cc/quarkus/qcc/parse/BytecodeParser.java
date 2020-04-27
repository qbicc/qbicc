package cc.quarkus.qcc.parse;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cc.quarkus.qcc.Mnemonics;
import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.AddNode;
import cc.quarkus.qcc.graph.node.BinaryIfNode;
import cc.quarkus.qcc.graph.node.CatchControlProjection;
import cc.quarkus.qcc.graph.node.CompareOp;
import cc.quarkus.qcc.graph.node.ConstantNode;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.IfNode;
import cc.quarkus.qcc.graph.node.InvokeNode;
import cc.quarkus.qcc.graph.node.NewNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.Projection;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.ReturnNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.graph.node.SubNode;
import cc.quarkus.qcc.graph.node.ThrowNode;
import cc.quarkus.qcc.graph.node.UnaryIfNode;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.graph.type.ObjectReference;
import cc.quarkus.qcc.type.MethodDefinition;
import cc.quarkus.qcc.type.MethodDescriptor;
import cc.quarkus.qcc.type.MethodDescriptorParser;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.TypeDescriptor;
import cc.quarkus.qcc.type.Universe;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static cc.quarkus.qcc.graph.node.ConstantNode.*;
import static org.objectweb.asm.Opcodes.*;

public class BytecodeParser {

    public static final int SLOT_RETURN = -1;

    public static final int SLOT_IO = -2;

    public static final int SLOT_MEMORY = -3;

    public BytecodeParser(MethodDefinition method) {
        this.method = method;
    }

    protected void buildControlFlowGraph(ControlFlowHelper links) {
        InsnList instrList = this.method.getInstructions();
        int instrLen = instrList.size();

        links.control(0, this.start);
        control(this.start);

        List<TryCatchBlockNode> tryCatchBlocks = this.method.getTryCatchBlocks();

        for (TryCatchBlockNode each : tryCatchBlocks) {
            int startIndex = instrList.indexOf(each.start);
            int endIndex = instrList.indexOf(each.end);
            int handlerIndex = instrList.indexOf(each.handler);
            TypeDefinition type = Universe.instance().findClass(each.type);
            RegionNode catchRegion = new RegionNode(this.start.frame().maxLocals(), this.start.frame().maxStack());
            //System.err.println( "catch region: " + catchRegion);
            links.catches(type, startIndex, endIndex, handlerIndex, catchRegion);
        }

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            if (instr instanceof JumpInsnNode) {
                LabelNode dest = ((JumpInsnNode) instr).label;
                int destIndex = instrList.indexOf(dest);
                links.control(destIndex, new RegionNode(this.start.frame().maxLocals(), this.start.frame().maxStack()));
            }
        }

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            int opcode = instr.getOpcode();

            //System.err.println( bci + " " + control() + " " + Mnemonics.of(opcode) + " " + instr);

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
                        links.control(bci, node);
                        links.add(bci + 1, node.getFalseOut());
                        links.add(destIndex, node.getTrueOut());
                        control(node.getFalseOut());
                        break;
                    }
                    case IF_ACMPEQ:
                    case IF_ACMPNE: {
                        BinaryIfNode<ObjectReference> node = new BinaryIfNode<>(control(), op(opcode));
                        links.control(bci, node);
                        links.add(bci + 1, node.getFalseOut());
                        links.add(destIndex, node.getTrueOut());
                        control(node.getFalseOut());
                        break;
                    }
                    case IFEQ:
                    case IFNE:
                    case IFGE:
                    case IFLE:
                    case IFGT:
                    case IFLT: {
                        UnaryIfNode node = new UnaryIfNode(control(), op(opcode));
                        links.control(bci, node);
                        links.add(bci + 1, node.getFalseOut());
                        links.add(destIndex, node.getTrueOut());
                        control(node.getFalseOut());
                        break;
                    }
                    case GOTO: {
                        links.add(destIndex, control());
                        control(null);
                        break;
                    }

                }
            } else {
                ControlNode<?> candidate = links.control(bci);
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
                        this.endRegion.addInput(control());
                        control(null);
                        break;
                    }
                    case INVOKEVIRTUAL:
                    case INVOKESPECIAL:
                    case INVOKEINTERFACE:
                    case INVOKEDYNAMIC:
                    case INVOKESTATIC: {
                        MethodDescriptorParser parser = new MethodDescriptorParser(Universe.instance(),
                                                                                   Universe.instance().findClass(((MethodInsnNode) instr).owner),
                                                                                   ((MethodInsnNode) instr).name,
                                                                                   ((MethodInsnNode) instr).desc,
                                                                                   opcode == INVOKESTATIC);
                        MethodDescriptor descriptor = parser.parseMethodDescriptor();
                        TypeDescriptor<?> returnType = descriptor.getReturnType();

                        InvokeNode<?> node = new InvokeNode(control(), descriptor.getOwner(), descriptor.getName(), returnType, descriptor.getParamTypes());
                        links.control(bci, node);
                        links.add(bci + 1, node.getNormalControlOut());
                        control(node.getNormalControlOut());
                        List<ControlFlowHelper.CatchEntry> catches = links.catchesFor(bci);
                        //for (RegionNode each : catches) {
                        for (ControlFlowHelper.CatchEntry each : catches) {
                            CatchControlProjection catchProjection = new CatchControlProjection(node.getThrowControlOut(), each.type);
                            each.handler.addInput(catchProjection);
                        }

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
        }
        return null;

    }

    protected void calculateDominanceFrontiers(ControlFlowHelper links) {

        Map<Node<?>, Set<Node<?>>> dominators = new HashMap<>();
        Set<ControlNode<?>> nodes = links.nodes();


        for (Node<?> n : nodes) {
            dominators.put(n, new HashSet<>(Collections.singleton(n)));
        }

        boolean changed = true;

        while (changed) {
            changed = false;
            for (Node<?> n : nodes) {
                //System.err.println( "df n=" + n);
                Set<Node<?>> tmp = new HashSet<>();
                //System.err.println( "df preds=" + n.getPredecessors());
                //System.err.println( "df sets=" + n.getPredecessors().stream().map(dominators::get).collect(Collectors.toList()));
                tmp.add(n);
                tmp.addAll(
                        intersect(
                                n.getPredecessors().stream().map(dominators::get).collect(Collectors.toList())
                        )
                );
                //System.err.println( "df tmp=" + tmp);

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
                    while (immediateDominators.get(n) != runner) {
                        dominanceFrontier.get(runner).add(n);
                        runner = immediateDominators.get(runner);
                    }
                }
            }
        }

        links.dominanceFrontier(dominanceFrontier);
    }

    private Collection<Node<?>> intersect(List<Set<Node<?>>> sets) {
        if (sets.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Node<?>> intersection = new HashSet<>(sets.get(0));

        for (int i = 1; i < sets.size(); ++i) {
            intersection.retainAll(sets.get(i));
        }

        return intersection;
    }

    protected Set<PhiLocal> placePhis(ControlFlowHelper links) {
        InsnList instructions = this.method.getInstructions();
        int numInstrs = instructions.size();

        PhiPlacements phiPlacements = new PhiPlacements();

        control(links.control(-1));

        for (int bci = 0; bci < numInstrs; ++bci) {
            AbstractInsnNode instr = instructions.get(bci);
            int opcode = instr.getOpcode();


            ControlNode<?> candidate = links.control(bci);
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
                    break;
                }
                case RETURN:
                case IRETURN:
                case LRETURN:
                case DRETURN:
                case FRETURN:
                case ARETURN: {
                    phiPlacements.record(control(), SLOT_RETURN, method.getReturnType());
                    break;
                }
            }
        }

        Set<PhiLocal> phis = new HashSet<>();
        phiPlacements.forEach((node, entries) -> {
            Set<ControlNode<?>> df = links.dominanceFrontier(node);

            for (PhiPlacements.Entry entry : entries) {
                for (ControlNode<?> dest : df) {
                    if (dest == this.endRegion && (entry.index != SLOT_MEMORY && entry.index != SLOT_IO && entry.index != SLOT_RETURN)) {
                        // skip
                        continue;
                    }
                    //System.err.println("place phi: " + node + " -> " + dest + " " + entry.index);
                    phis.add(dest.frame().ensurePhi(entry.index, node, entry.type));
                }
            }
        });

        return phis;
    }

    public Graph parse() {
        ControlFlowHelper links = initialize();
        buildControlFlowGraph(links);
        calculateDominanceFrontiers(links);
        Set<PhiLocal> phis = placePhis(links);

        execute(links);

        for (PhiLocal phi : phis) {
            phi.complete();
        }

        this.end.setIO(this.endRegion.frame().io());
        this.end.setMemory(this.endRegion.frame().memory());
        this.end.setReturnValue(this.endRegion.frame().returnValue());

        return new Graph(this.start, this.end);
    }

    @SuppressWarnings("unchecked")
    public Graph execute(ControlFlowHelper links) {
        Graph graph = new Graph(this.start, this.end);

        InsnList instrList = this.method.getInstructions();
        int instrLen = instrList.size();

        control(links.control(-1));

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            int opcode = instr.getOpcode();

            ControlNode<?> candidate = links.control(bci);

            if (candidate != null) {
                candidate.mergeInputs();
                control(candidate);
            }

            //System.err.println( control() + " :: " + bci + " " + Mnemonics.of(opcode));

            if (instr instanceof JumpInsnNode) {
                LabelNode dest = ((JumpInsnNode) instr).label;
                int destIndex = instrList.indexOf(dest);

                if (opcode == GOTO) {
                    // just skip this ish
                } else {
                    IfNode node = null;
                    switch (opcode) {
                        case IFEQ: {
                            Node<Integer> test = pop(Integer.class);
                            node = (IfNode) links.control(bci);
                            ((UnaryIfNode) node).setInput(test);
                            break;
                        }
                        case IFNE: {
                            Node<Integer> test = pop(Integer.class);
                            node = (IfNode) links.control(bci);
                            ((UnaryIfNode) node).setInput(test);
                            break;
                        }
                        case IF_ICMPGE: {
                            Node<Integer> rhs = pop(Integer.class);
                            Node<Integer> lhs = pop(Integer.class);
                            node = (IfNode) links.control(bci);
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
                if (links.control(bci) != null) {
                    control(links.control(bci));
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
                        push(byteConstant(control(), (byte) ((IntInsnNode) instr).operand));
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
                        NewNode<?> node = new NewNode<>(control(), type);
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
                        break;
                    }

                    case ATHROW: {
                        Node<?> thrown = pop(null);
                        clear();
                        ThrowNode<?> node = new ThrowNode(control(), thrown);
                        push(node);
                        break;
                    }

                    case IRETURN: {
                        Node<Integer> val = pop(Integer.class);
                        ReturnNode<Integer> ret = new ReturnNode<>(control(), val);
                        control().frame().returnValue(ret);
                        this.endRegion.frame().mergeFrom(control().frame());
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

        return graph;
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
        //System.err.println( control + " ALOAD " + index );
        return control().frame().load(index, ObjectReference.class);
    }

    protected void storeObject(int index, Node<ObjectReference> val) {
        //System.err.println( control() + " ASTORE " + index + " = " + val);
        control().frame().store(index, val);
    }


    protected ControlFlowHelper initialize() {
        this.start = new StartNode(this.method, this.method.getMaxLocals(), this.method.getMaxStack());
        this.endRegion = new RegionNode(this.method.getMaxLocals(), this.method.getMaxStack());
        this.end = new EndNode(this.endRegion, this.method.getReturnType());
        control(this.start);
        return new ControlFlowHelper(this.start);
    }

    protected ControlNode<?> control() {
        return this.control;
    }

    protected Node<IOToken> io() {
        return control().frame().io();
    }

    protected void io(Node<IOToken> io) {
        //System.err.println("SET IO " + io + " to " + control());
        control().frame().io(io);
    }

    protected Node<MemoryToken> memory() {
        return control().frame().memory();
    }

    protected void memory(Node<MemoryToken> memory) {
        //System.err.println("SET MEMORY " + memory + " to " + control());
        control().frame().memory(memory);
    }

    protected void control(ControlNode<?> node) {
        this.control = node;
    }

    protected <T> Node<T> push(Node<T> node) {
        return control().frame().push(node);
    }

    protected <T> Node<T> pop(Class<T> type) {
        return control().frame().pop(type);
    }

    protected <T> Node<T> peek(Class<T> type) {
        return control().frame().peek(type);
    }

    protected void clear() {
        control().frame().clear();
    }

    private StartNode start;

    private RegionNode endRegion;

    private EndNode end;

    private final MethodDefinition method;

    private ControlNode<?> control;
}
