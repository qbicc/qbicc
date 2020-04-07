package cc.quarkus.qcc.parse;

import java.util.HashMap;
import java.util.Map;

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
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.FLOAD;
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
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.SIPUSH;

public class BytecodeParser {

    public BytecodeParser(MethodDefinition method) {
        this.method = method;
    }

    protected void analyzeLeaders() {
        InsnList instrList = this.method.getInstructions();
        int instrLen = instrList.size();

        for (int i = 0; i < instrLen; ++i) {
            AbstractInsnNode instr = instrList.get(i);
            int opcode = instr.getOpcode();

            if (instr instanceof JumpInsnNode) {
                LabelNode dest = ((JumpInsnNode) instr).label;
                int destIndex = instrList.indexOf(dest);
                this.leaders.put(destIndex, new RegionNode(this.method.getMaxLocals(), this.method.getMaxStack()));
                if (opcode != GOTO) {
                    // some flavor of IF
                    // mark a leader for the next instruction, but not ourself
                    this.leaders.put(i + 1, new RegionNode(this.method.getMaxLocals(), this.method.getMaxStack()));
                }
            }
        }
    }

    public Graph parse() {
        analyzeLeaders();
        initialize();

        InsnList instrList = this.method.getInstructions();
        int instrLen = instrList.size();

        for (int bci = 0; bci < instrLen; ++bci) {
            AbstractInsnNode instr = instrList.get(bci);
            int opcode = instr.getOpcode();
            System.err.println("** " + Mnemonics.of(opcode));

            RegionNode candidate = this.leaders.get(bci);
            if (candidate != null) {
                if ( control() != null ) {
                    candidate.addInput(control());
                }
                control(candidate);
            }

            if (instr instanceof JumpInsnNode) {
                LabelNode dest = ((JumpInsnNode) instr).label;
                int destIndex = instrList.indexOf(dest);

                if (opcode == GOTO) {
                    control(destIndex, control());
                    control(this.leaders.get(bci + 1));
                } else {
                    IfNode node = null;
                    switch (opcode) {
                        case IFEQ: {
                            Node<IntType> test = pop(IntType.INSTANCE);
                            node = new UnaryIfNode(control(), test, CompareOp.EQUAL);
                            break;
                        }
                        case IFNE: {
                            Node<IntType> test = pop(IntType.INSTANCE);
                            node = new UnaryIfNode(control(), test, CompareOp.NOT_EQUAL);
                            break;
                        }
                        case IF_ICMPGE: {
                            Node<IntType> lhs = pop(IntType.INSTANCE);
                            Node<IntType> rhs = pop(IntType.INSTANCE);
                            node = new BinaryIfNode<>(control(), lhs, rhs, CompareOp.GREATER_THAN_OR_EQUAL);
                            break;
                        }
                        default: {
                            System.err.println("unhandled: " + Mnemonics.of(opcode));
                        }
                    }

                    assert node != null;
                    //node.addInput(control());
                    node.getTrueOut().frame().merge(control().frame());
                    node.getFalseOut().frame().merge(control().frame());

                    control(destIndex, node.getTrueOut());
                    control(bci + 1, node.getFalseOut());

                    //node.getFalseOut().frame().merge(control().frame());
                    //node.getFalseOut().addInput(control());
                    control(node.getFalseOut());
                }
            } else {
                if (control() == null) {
                    control(this.leaders.get(bci));
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
                        //this.end.frame().io(io());
                        //this.end.frame().memory(memory());
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
        return possiblySimplify(graph);
        //return graph;
    }

    protected Graph<?> possiblySimplify(Graph<?> graph) {
        for (Node<?> node : graph.postOrder()) {
            if ( node instanceof ControlNode ) {
                ((ControlNode) node).possiblySimplify();
            }
        }

        return graph;


        //Deque<Node<?>> worklist = new ArrayDeque<>();
        //Set<Node<?>> seen = new HashSet<>();
//
        //worklist.push(this.start);
//
        //while (!worklist.isEmpty()) {
            //Node<?> each = worklist.pop();
            //if (!seen.contains(each)) {
                //seen.add(each);
                //if ( each instanceof ControlNode ) {
                    //((ControlNode) each).possiblySimplify();
                //}
                //worklist.addAll(each.getSuccessors());
            //}
        //}
    }

    protected Node<IntType> loadInt(int index) {
        return control().frame().load(index, IntType.INSTANCE);
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

    protected void initialize() {
        this.start = new StartNode(startType());
        this.end = new EndNode<>(this.method.getReturnType());
        control(this.start);
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
        this.control = node;
    }

    protected void control(int bci, ControlNode<?> node) {
        RegionNode region = this.leaders.get(bci);
        region.addInput(node);
    }

    protected <T extends Node<? extends ConcreteType<?>>> T push(T node) {
        return control().frame().push(node);
    }

    protected <T extends ConcreteType<?>> Node<T> pop(T type) {
        return control().frame().pop(type);
    }

    private StartNode start;

    private EndNode<?> end;

    private Map<Integer, RegionNode> leaders = new HashMap<>();

    private final MethodDefinition method;

    private ControlNode control;
}
