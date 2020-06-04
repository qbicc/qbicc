package cc.quarkus.qcc.graph;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.machine.llvm.Function;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Types;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.impl.LLVM;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.type.universe.Universe;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 *
 */
@SuppressWarnings("SpellCheckingInspection")
public class Main {
    public static void main(String[] args) throws IOException {
        Map<String, Map<String, GraphBuilder>> gbs = new HashMap<>();
        try (InputStream is = Test.class.getResourceAsStream("Test.class")) {
            ClassReader cr = new ClassReader(is);
            cr.accept(new ClassVisitor(Universe.ASM_VERSION) {
                public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
                    GraphBuilder gb = new GraphBuilder(access, name, descriptor, signature, exceptions);
                    Map<String, GraphBuilder> subMap = gbs.get(name);
                    if (subMap == null) {
                        gbs.put(name, subMap = new HashMap<>());
                    }
                    subMap.put(descriptor, gb);
                    return gb;
                }
            }, 0);
        }
        for (String name : gbs.keySet()) {
            Map<String, GraphBuilder> subMap = gbs.get(name);
            for (String desc : subMap.keySet()) {
                GraphBuilder gb = subMap.get(desc);
                System.out.println("--- " + name + " " + desc);
                System.out.println("digraph gr {");
                System.out.println("  graph [fontname = \"helvetica\",fontsize=10,ordering=in,outputorder=depthfirst,ranksep=1];");
                System.out.println("  node [fontname = \"helvetica\",fontsize=10,ordering=in];");
                System.out.println("  edge [fontname = \"helvetica\",fontsize=10];");
                BasicBlockImpl firstBlock = gb.firstBlock;
//                new CleanUpPhisPass().apply(firstBlock);
                HashSet<Node> visited = new HashSet<>();
                visited.add(firstBlock);
                firstBlock.writeToGraph(visited, System.out, firstBlock.calculateReachableBlocks());
                System.out.println("}");
                System.out.println("---");
            }
        }
        // now print out the program. why not
        final Module module = LLVM.newModule();
        for (String name : gbs.keySet()) {
            Map<String, GraphBuilder> subMap = gbs.get(name);
            name.replaceAll("[^a-zA-Z0-9_.]", ".");
            for (String desc : subMap.keySet()) {
                GraphBuilder gb = subMap.get(desc);
                BasicBlock firstBlock = gb.firstBlock;
                Set<BasicBlock> knownBlocks = firstBlock.calculateReachableBlocks();
                FunctionDefinition def = module.define(name);
                Ctxt ctxt = new Ctxt(def, module, knownBlocks);
                int i = 0;
                if (gb.thisValue != null) {
                    Function.Parameter fp = def.param(Types.i32);
                    fp.name(".this");
                    ctxt.values.put(gb.thisValue, fp.asValue());
                }
                for (ParameterValue param : gb.originalParams) {
                    Function.Parameter fp = def.param(Types.i32);
                    fp.name("param" + i++);
                    ctxt.values.put(param, fp.asValue());
                }
                def.returns(Types.i32);
                ctxt.blocks.put(firstBlock, def);
                // write the terminal instructions
                for (BasicBlock bb : knownBlocks) {
                    addTermInst(ctxt, bb.getTerminator(), ctxt.blocks.get(bb));
                }
            }
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(System.out)) {
            try (OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
                try (BufferedWriter bw = new BufferedWriter(osw)) {
                    module.writeTo(bw);
                }
            }
        }
    }

    private static void addTermInst(final Ctxt ctxt, final Terminator inst, final cc.quarkus.qcc.machine.llvm.BasicBlock target) {
        if (inst instanceof ValueReturn) {
            target.ret(Types.i32, getValue(ctxt, ((ValueReturn) inst).getReturnValue()));
        } else if (inst instanceof Return) {
            target.ret();
        } else if (inst instanceof TryInvocationValue) {
            TryInvocationValue tiv = (TryInvocationValue) inst;

        } else if (inst instanceof TryInvocation) {
            TryInvocation ti = (TryInvocation) inst;

        } else if (inst instanceof TryInstanceFieldWrite) {
            TryInstanceFieldWrite w = (TryInstanceFieldWrite) inst;
            BasicBlock jmpTarget = w.getNextBlock();
            cc.quarkus.qcc.machine.llvm.BasicBlock bbTarget = getBlock(ctxt, jmpTarget);
            cc.quarkus.qcc.machine.llvm.BasicBlock unwindTarget = null; // getCatchBlock(ctxt, w.getCatchHandler());
            cc.quarkus.qcc.machine.llvm.Value setFieldFn = ctxt.module.declare(".trySetInstanceField.fieldDeclHere").asGlobal();
            target.invoke(Types.i32, setFieldFn, bbTarget, unwindTarget).arg(Types.i32, getValue(ctxt, w.getInstance())).arg(Types.i32, getValue(ctxt, w.getWriteValue()));
        } else if (inst instanceof TryInstanceFieldReadValue) {
            TryInstanceFieldReadValue rv = (TryInstanceFieldReadValue) inst;


            BasicBlock jmpTarget = rv.getNextBlock();
            cc.quarkus.qcc.machine.llvm.BasicBlock bbTarget = getBlock(ctxt, jmpTarget);
            cc.quarkus.qcc.machine.llvm.BasicBlock unwindTarget = null; // getCatchBlock(ctxt, rv.getCatchHandler());
            cc.quarkus.qcc.machine.llvm.Value getFieldFn = ctxt.module.declare(".tryGetInstanceField.fieldDeclHere").asGlobal();
            target.invoke(Types.i32, getFieldFn, bbTarget, unwindTarget).arg(Types.i32, getValue(ctxt, rv.getInstance()));
        } else if (inst instanceof TryThrow) {
            TryThrow t = (TryThrow) inst;
            target.br(getBlock(ctxt, t.getCatchHandler()));
        } else if (inst instanceof Throw) {
            Throw t = (Throw) inst;
            cc.quarkus.qcc.machine.llvm.Value doThrowFn = ctxt.module.declare(".throw").asGlobal();
            target.call(Types.i32, doThrowFn).arg(Types.i32, getValue(ctxt, t.getThrownValue()));
        } else if (inst instanceof Goto) {
            BasicBlock jmpTarget = ((Goto) inst).getNextBlock();
            target.br(getBlock(ctxt, jmpTarget));
        } else if (inst instanceof If) {
            If ifInst = (If) inst;
            Value cond = ifInst.getCondition();
            BasicBlock tb = ifInst.getTrueBranch();
            BasicBlock fb = ifInst.getFalseBranch();
            cc.quarkus.qcc.machine.llvm.BasicBlock tTarget = getBlock(ctxt, tb);
            cc.quarkus.qcc.machine.llvm.BasicBlock fTarget = getBlock(ctxt, fb);
            cc.quarkus.qcc.machine.llvm.Value condVal = getValue(ctxt, cond);
            target.br(condVal, tTarget, fTarget);
        } else {
            throw new IllegalStateException();
        }
    }

    private static void process(final Ctxt ctxt, final MemoryState memoryState) {
        if (memoryState instanceof Value) {
            getValue(ctxt, (Value) memoryState);
        } else if (memoryState instanceof Terminator) {
            throw new IllegalStateException();
        } else if (memoryState instanceof InstanceFieldWrite) {
            InstanceFieldWrite ifw = (InstanceFieldWrite) memoryState;
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(ctxt, ifw.getOwner());
            target.store(Types.i32, getValue(ctxt, ifw.getWriteValue()), Types.i32, Values.ZERO /* todo: get address of static field */);
        } else if (memoryState instanceof FieldWrite) {
            // static
            FieldWrite sfw = (FieldWrite) memoryState;
            cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(ctxt, sfw.getOwner());
            target.store(Types.i32, getValue(ctxt, sfw.getWriteValue()), Types.i32, Values.ZERO /* todo: get address of static field */);
        } else {
            throw new IllegalStateException();
        }
    }

    private static cc.quarkus.qcc.machine.llvm.Value getValue(final Ctxt ctxt, final Value value) {
        cc.quarkus.qcc.machine.llvm.Value val = ctxt.values.get(value);
        if (val != null) {
            return val;
        }
        if (value instanceof ProgramNode) {
            BasicBlock owner = ((ProgramNode) value).getOwner();
            final cc.quarkus.qcc.machine.llvm.BasicBlock target = getBlock(ctxt, owner);
            if (value instanceof CommutativeBinaryValue) {
                CommutativeBinaryValue op = (CommutativeBinaryValue) value;
                switch (op.getKind()) {
                    case ADD: val = target.add(Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case AND: val = target.and(Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case OR: val = target.or(Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case XOR: val = target.xor(Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case MULTIPLY: val = target.mul(Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case CMP_EQ: val = target.icmp(IntCondition.eq, Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case CMP_NE: val = target.icmp(IntCondition.ne, Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    default: throw new IllegalStateException();
                }
                ctxt.values.put(value, val);
            } else if (value instanceof NonCommutativeBinaryValue) {
                NonCommutativeBinaryValue op = (NonCommutativeBinaryValue) value;
                switch (op.getKind()) {
                    case SUB: val = target.sub(Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case DIV: val = target.sdiv(Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case MOD: val = target.srem(Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case CMP_LT: val = target.icmp(IntCondition.slt, Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case CMP_LE: val = target.icmp(IntCondition.sle, Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case CMP_GT: val = target.icmp(IntCondition.sgt, Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    case CMP_GE: val = target.icmp(IntCondition.sge, Types.i32, getValue(ctxt, op.getLeftInput()), getValue(ctxt, op.getRightInput())).asLocal(); break;
                    default: throw new IllegalStateException();
                }
                ctxt.values.put(value, val);
            } else if (value instanceof FieldReadValue) {

            } else if (value instanceof IfValue) {
                IfValue op = (IfValue) value;
                val = target.select(Types.i1, getValue(ctxt, op.getCond()), Types.i32, getValue(ctxt, op.getTrueValue()), getValue(ctxt, op.getFalseValue())).asLocal();
                ctxt.values.put(value, val);
            } else if (value instanceof PhiValue) {
                PhiValue phiValue = (PhiValue) value;
                if (true) {
                    final Iterator<BasicBlock> iterator = ctxt.knownBlocks.iterator();
                    while (iterator.hasNext()) {
                        BasicBlock b1 = iterator.next();
                        Value v1 = phiValue.getValueForBlock(b1);
                        if (v1 != null) {
                            // got first value
                            while (iterator.hasNext()) {
                                BasicBlock b2 = iterator.next();
                                Value v2 = phiValue.getValueForBlock(b2);
                                if (v2 != null && v2 != v1) {
                                    // it's a phi, so we'll just live with it
                                    Phi phi = target.phi(Types.i32);
                                    ctxt.values.put(value, val = phi.asLocal());
                                    phi.item(getValue(ctxt, v1), getBlock(ctxt, b1));
                                    phi.item(getValue(ctxt, v2), getBlock(ctxt, b2));
                                    while (iterator.hasNext()) {
                                        b2 = iterator.next();
                                        v2 = phiValue.getValueForBlock(b2);
                                        if (v2 != null) {
                                            phi.item(getValue(ctxt, v2), getBlock(ctxt, b2));
                                        }
                                    }
                                    return val;
                                }
                            }
                            // only one value for phi!
                            phiValue.replaceWith(v1);
                            return getValue(ctxt, v1);
                        }
                    }
                } else {
                    Phi phi = target.phi(Types.i32);
                    ctxt.values.put(value, val = phi.asLocal());
                    for (BasicBlock knownBlock : ctxt.knownBlocks) {
                        Value v = phiValue.getValueForBlock(knownBlock);
                        if (v != null) {
                            phi.item(getValue(ctxt, v), getBlock(ctxt, knownBlock));
                        }
                    }
                    return val;
                }
                // no branches!
                throw new IllegalStateException();
            } else {
                throw new IllegalStateException();
            }
        } else if (value instanceof IntConstantValueImpl) {
            ctxt.values.put(value, val = LLVM.intConstant(((IntConstantValueImpl) value).getValue()));
        } else {
            throw new IllegalStateException();
        }
        return val;
    }

    private static cc.quarkus.qcc.machine.llvm.BasicBlock getBlock(final Ctxt ctxt, final BasicBlock bb) {
        cc.quarkus.qcc.machine.llvm.BasicBlock target = ctxt.blocks.get(bb);
        if (target == null) {
            target = ctxt.def.createBlock();
            ctxt.blocks.put(bb, target);
        }
        return target;
    }

    static final class Ctxt {
        final FunctionDefinition def;
        final Map<Value, cc.quarkus.qcc.machine.llvm.Value> values = new HashMap<>();
        final Map<BasicBlock, cc.quarkus.qcc.machine.llvm.BasicBlock> blocks = new HashMap<>();
        final Set<BasicBlock> processed = new HashSet<>();
        final Module module;
        final Set<BasicBlock> knownBlocks;

        Ctxt(final FunctionDefinition def, final Module module, final Set<BasicBlock> knownBlocks) {
            this.def = def;
            this.module = module;
            this.knownBlocks = knownBlocks;
        }
    }
}
