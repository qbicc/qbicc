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
import cc.quarkus.qcc.graph.node.CompareOp;
import cc.quarkus.qcc.graph.node.ConstantNode;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.IOType;
import cc.quarkus.qcc.graph.node.IfNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.ReturnNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.graph.node.SubNode;
import cc.quarkus.qcc.graph.node.UnaryIfNode;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.DoubleType;
import cc.quarkus.qcc.graph.type.FloatType;
import cc.quarkus.qcc.graph.type.IntType;
import cc.quarkus.qcc.graph.type.LongType;
import cc.quarkus.qcc.graph.type.MemoryType;
import cc.quarkus.qcc.graph.type.ObjectType;
import cc.quarkus.qcc.graph.type.StartType;
import cc.quarkus.qcc.type.MethodDefinition;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static cc.quarkus.qcc.graph.node.ConstantNode.byteConstant;
import static cc.quarkus.qcc.graph.node.ConstantNode.constant;
import static cc.quarkus.qcc.graph.node.ConstantNode.doubleConstant;
import static cc.quarkus.qcc.graph.node.ConstantNode.floatConstant;
import static cc.quarkus.qcc.graph.node.ConstantNode.intConstant;
import static cc.quarkus.qcc.graph.node.ConstantNode.longConstant;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ACMPEQ;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEDYNAMIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;

public class BytecodeParser {

    public static final int SLOT_IO = -1;
    public static final int SLOT_MEMORY = -2;

    public BytecodeParser(MethodDefinition method) {
        this.method = method;
    }

    protected void buildControlFlowGraph(ControlFlowHelper links) {
        InsnList instrList = this.method.getInstructions();
        int instrLen = instrList.size();

        RegionNode root = new RegionNode(this.start.frame().maxLocals(), this.start.frame().maxStack());
        links.control(0, root);
        root.addInput(this.start);
        control(root);

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            int opcode = instr.getOpcode();

            if (instr instanceof JumpInsnNode) {
                LabelNode dest = ((JumpInsnNode) instr).label;
                int destIndex = instrList.indexOf(dest);
                links.control(destIndex, new RegionNode(this.start.frame().maxLocals(), this.start.frame().maxStack()));
            }
        }

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            int opcode = instr.getOpcode();

            System.err.println("CFG: " + bci + " // " + Mnemonics.of(opcode));
            if (instr instanceof JumpInsnNode) {
                LabelNode dest = ((JumpInsnNode) instr).label;
                int destIndex = instrList.indexOf(dest);
                /*
                this.leaders.put(destIndex, new RegionNode(this.method.getMaxLocals(), this.method.getMaxStack()));
                if (opcode != GOTO) {
                    // some flavor of IF
                    // mark a leader for the next instruction, but not ourself
                    this.leaders.put(i + 1, new RegionNode(this.method.getMaxLocals(), this.method.getMaxStack()));
                }
                 */
                switch (opcode) {
                    case IF_ICMPEQ:
                    case IF_ICMPNE:
                    case IF_ICMPGE:
                    case IF_ICMPLE:
                    case IF_ICMPGT:
                    case IF_ICMPLT: {
                        BinaryIfNode<IntType> node = new BinaryIfNode<>(control());
                        links.control(bci, node);
                        //links.control(bci+1, node.getFalseOut());
                        links.add(bci + 1, node.getFalseOut());
                        links.add(destIndex, node.getTrueOut());
                        control(node.getFalseOut());
                        break;
                    }
                    case IF_ACMPEQ:
                    case IF_ACMPNE: {
                        BinaryIfNode<ObjectType> node = new BinaryIfNode<>(control());
                        links.control(bci, node);
                        //links.control(bci+1, node.getFalseOut());
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
                        UnaryIfNode node = new UnaryIfNode(control());
                        links.control(bci, node);
                        //links.control(bci+1, node.getFalseOut());
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
                        candidate.addInput(control());
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
                        System.err.println("add END");
                        this.end.addInput(control());
                        break;
                    }
                }
            }
        }
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
                Set<Node<?>> temp = new HashSet<>();
                System.err.println( "n=" + n);
                System.err.println( "n.predecessors=" + n.getPredecessors());
                System.err.println( "n.predecessors.dom=" + n.getPredecessors().stream().map(dominators::get).collect(Collectors.toList()));
                temp.add(n);
                temp.addAll(
                        intersect(
                                n.getPredecessors().stream().map(dominators::get).collect(Collectors.toList())
                        )
                );

                if ( ! dominators.get(n).equals(temp)) {
                    dominators.put(n, temp);
                    changed = true;
                }
            }
        }

        Map<Node<?>, Node<?>> immediateDominators = new HashMap<>();

        for (Node<?> n : nodes) {
            Set<Node<?>> strictDominators = new HashSet<>(dominators.get(n));
            System.err.println( "A sdom for " + n + " >> " + strictDominators);
            strictDominators.remove(n);
            System.err.println( "B sdom for " + n + " >> " + strictDominators);
            Node idom = strictDominators.stream()
                    .filter(e -> dominators.get(e).containsAll(strictDominators))
                    .findFirst().orElse(null);

            immediateDominators.put(n, idom);
        }

        Map<ControlNode<?>, Set<ControlNode<?>>> dominanceFrontier = new HashMap<>();

        for (ControlNode<?> n : nodes) {
            dominanceFrontier.put(n, new HashSet<>());
        }

        for (ControlNode<?> n : nodes) {
            if ( n.getPredecessors().size() > 1 ) {
                System.err.println( "calc DF for " + n);
                for (ControlNode<?> p : n.getControlPredecessors()) {
                    Node<?> runner = p;
                    System.err.println( "runner: " + runner + " vs " + immediateDominators.get(n));
                    while ( immediateDominators.get(n) != runner ) {
                        System.err.println( "add " + n + " to DF for " + runner);
                        dominanceFrontier.get(runner).add(n);
                        runner = immediateDominators.get(runner);
                        System.err.println( "runner now: " + runner);
                    }
                }
            }
        }

        System.err.println("********************");
        for (Node<?> n : nodes) {
            System.err.println( "** " + n);
            System.err.println( "dom: " + dominators.get(n));
            System.err.println( "idom: " + immediateDominators.get(n));
            System.err.println( "df: " + dominanceFrontier.get(n));
            System.err.println( "--");
        }
        System.err.println("********************");

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

    protected Set<Local.PhiLocal<?>> placePhis(ControlFlowHelper links) {
        InsnList instructions = this.method.getInstructions();
        int numInstrs = instructions.size();

        //Map<ControlNode<?>, Set<Integer>> phiPlacements = new HashMap<>();
        PhiPlacements phiPlacements = new PhiPlacements();

        control(links.control(-1));

        for (int bci = 0; bci < numInstrs ; ++bci) {
            AbstractInsnNode instr = instructions.get(bci);
            int opcode = instr.getOpcode();

            System.err.println( "pp: " + bci + ": " + Mnemonics.of(opcode));

            ControlNode<?> candidate = links.control(bci);
            if ( candidate != null ) {
                control(candidate);
            }

            switch (opcode) {
                case ISTORE: {
                    phiPlacements.record(control(), ((VarInsnNode)instr).var, IntType.INSTANCE);
                    break;
                }
                case LSTORE: {
                    phiPlacements.record(control(), ((VarInsnNode)instr).var, LongType.INSTANCE);
                    break;
                }
                case FSTORE: {
                    phiPlacements.record(control(), ((VarInsnNode)instr).var, FloatType.INSTANCE);
                    break;
                }
                case DSTORE: {
                    phiPlacements.record(control(), ((VarInsnNode)instr).var, DoubleType.INSTANCE);
                    break;
                }
                case ASTORE: {
                    phiPlacements.record(control(), ((VarInsnNode) instr).var, ObjectType.java.lang.Object);
                    break;
                }
                    //int index = ((VarInsnNode)instr).var;
                    //Set<Integer> set = phiPlacements.get(control());
                    //if ( set == null ) {
                        //set = new HashSet<>();
                        //phiPlacements.put(control(), set);
                    //}
                    //set.add(index);
                    //break;
                //}
                case INVOKEDYNAMIC:
                case INVOKEINTERFACE:
                case INVOKESPECIAL:
                case INVOKESTATIC:
                case INVOKEVIRTUAL: {
                    //Set<Integer> set = phiPlacements.get(control());
                    //if ( set == null ) {
                        //set = new HashSet<>();
                        //phiPlacements.put(control(), set);
                    //}
                    //set.add(SLOT_IO);
                    //set.add(SLOT_MEMORY);
                    phiPlacements.record(control(), SLOT_IO, IOType.INSTANCE);
                    phiPlacements.record(control(), SLOT_MEMORY, MemoryType.INSTANCE);
                    break;
                }
            }
        }

        Set<Local.PhiLocal<?>> phis = new HashSet<>();
        phiPlacements.forEach((node, entries)->{
            Set<ControlNode<?>> df = links.dominanceFrontier(node);

            System.err.println( "DF "+ node + " >> " + df);

            for (PhiPlacements.Entry entry : entries) {
                for (ControlNode<?> dest : df) {
                    System.err.println( " place " + node + "@" + entry.index + " in " + dest);
                    phis.add( dest.frame().ensurePhi(entry.index, node, entry.type) );
                }
            }
        });

        return phis;
    }

    public Graph parse() {
        ControlFlowHelper links = initialize();
        buildControlFlowGraph(links);
        calculateDominanceFrontiers(links);
        Set<Local.PhiLocal<?>> phis = placePhis(links);

        execute(links);

        for (Local.PhiLocal<?> phi : phis) {
            phi.complete();
        }

        return new Graph(this.start, this.end);
    }

    public Graph execute(ControlFlowHelper links) {

        InsnList instrList = this.method.getInstructions();
        int instrLen = instrList.size();

        control(links.control(-1));

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            int opcode = instr.getOpcode();
            System.err.println("** " + bci + " " + Mnemonics.of(opcode));

            ControlNode<?> candidate = links.control(bci);
            if ( candidate != null ) {
                if (control() != null) {
                    candidate.frame().mergeFrom(control().frame());
                }
                control(candidate);
            }

            if (instr instanceof JumpInsnNode) {
                LabelNode dest = ((JumpInsnNode) instr).label;
                int destIndex = instrList.indexOf(dest);

                if (opcode == GOTO) {
                    //control(destIndex, control());
                    //control(this.leaders.get(bci + 1));
                } else {
                    IfNode node = null;
                    switch (opcode) {
                        case IFEQ: {
                            Node<IntType> test = pop(IntType.INSTANCE);
                            //node = new UnaryIfNode(control(), test, CompareOp.EQUAL);
                            node = (IfNode) links.control(bci);
                            node.setOp(CompareOp.EQUAL);
                            node.addPredecessor(test);
                            break;
                        }
                        case IFNE: {
                            Node<IntType> test = pop(IntType.INSTANCE);
                            //node = new UnaryIfNode(control(), test, CompareOp.NOT_EQUAL);
                            node = (IfNode) links.control(bci);
                            node.setOp(CompareOp.NOT_EQUAL);
                            node.addPredecessor(test);
                            break;
                        }
                        case IF_ICMPGE: {
                            Node<IntType> lhs = pop(IntType.INSTANCE);
                            Node<IntType> rhs = pop(IntType.INSTANCE);
                            //node = new BinaryIfNode<>(control(), lhs, rhs, CompareOp.GREATER_THAN_OR_EQUAL);
                            node = (IfNode) links.control(bci);
                            node.setOp(CompareOp.GREATER_THAN_OR_EQUAL);
                            node.addPredecessor(lhs);
                            node.addPredecessor(rhs);
                            break;
                        }
                        default: {
                            System.err.println("unhandled: " + Mnemonics.of(opcode));
                        }
                    }

                    assert node != null;
                    //node.addInput(control());
                    //node.getTrueOut().frame().merge(control().frame());
                    //node.getFalseOut().frame().merge(control().frame());

                    //control(destIndex, node.getTrueOut());
                    //control(bci + 1, node.getFalseOut());

                    //node.getFalseOut().frame().merge(control().frame());
                    //node.getFalseOut().addInput(control());
                    //control(node.getFalseOut());
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
                    case ILOAD: {
                        push(loadInt(((VarInsnNode) instr).var));
                        break;
                    }
                    case ISTORE: {
                        storeInt(((VarInsnNode) instr).var, pop(IntType.INSTANCE));
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

                    // ----------------------------------------

                    case IADD: {
                        Node<IntType> val1 = pop(IntType.INSTANCE);
                        Node<IntType> val2 = pop(IntType.INSTANCE);
                        push(new AddNode<>(control(), IntType.INSTANCE, val1, val2));
                        break;
                    }
                    case ISUB: {
                        Node<IntType> val1 = pop(IntType.INSTANCE);
                        Node<IntType> val2 = pop(IntType.INSTANCE);
                        push(new SubNode<>(control(), IntType.INSTANCE, val1, val2));
                        break;
                    }

                    case IRETURN: {
                        Node<IntType> val = pop(IntType.INSTANCE);
                        ReturnNode<IntType> ret = new ReturnNode<>(control(), IntType.INSTANCE, val);
                        this.end.addInput(control());
                        this.end.addPredecessor(io());
                        this.end.addPredecessor(memory());
                        this.end.returnValue(ret);
                        break;
                    }

                    default: {
                        System.err.println("unknown bytecode: " + Mnemonics.of(opcode));
                    }
                }
            }
        }

        //possiblySimplify();
        //return this.start;
        Graph graph = new Graph(this.start, this.end);
        //return possiblySimplify(graph);
        return graph;
    }

    protected Node<IntType> loadInt(int index) {
        return control().frame().load(index, IntType.INSTANCE);
        //return null;
    }

    protected void storeInt(int index, Node<IntType> val) {
        control().frame().store(index, val);
    }

    protected Node<LongType> loadLong(int index) {
        return control().frame().load(index, LongType.INSTANCE);
    }

    protected Node<FloatType> loadFloat(int index) {
        return control().frame().load(index, FloatType.INSTANCE);
    }

    protected Node<DoubleType> loadDouble(int index) {
        return control().frame().load(index, DoubleType.INSTANCE);
    }

    protected Node<ObjectType> loadObject(int index) {
        return control().frame().load(index, ObjectType.java.lang.Object);
    }

    protected ControlFlowHelper initialize() {
        this.start = new StartNode(startType());
        this.end = new EndNode<>(this.method.getReturnType());
        control(this.start);
        return new ControlFlowHelper(this.start);
    }

    protected StartType startType() {
        return new StartType(this.method.getMaxLocals(),
                             this.method.getMaxStack(),
                             this.method.getParamTypes());
    }

    protected ControlNode<?> control() {
        return this.control;
    }

    protected Node<IOType> io() {
        return control().frame().io();
    }

    protected Node<MemoryType> memory() {
        return control().frame().memory();
    }

    protected void control(ControlNode<?> node) {
        System.err.println("set control:: " + node);
        this.control = node;
    }

    protected <T extends Node<? extends ConcreteType<?>>> T push(T node) {
        return control().frame().push(node);
        //return null;
    }

    protected <T extends ConcreteType<?>> Node<T> pop(T type) {
        return control().frame().pop(type);
        //return null;
    }

    private StartNode start;

    private EndNode<?> end;

    private final MethodDefinition method;

    private ControlNode control;
}
