package cc.quarkus.qcc.graph2;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.machine.llvm.Function;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Types;
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
                HashSet<Node> visited = new HashSet<>();
                visited.add(firstBlock);
                firstBlock.writeToGraph(visited, System.out, firstBlock.getReachableBlocks());
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
                Set<BasicBlock> knownBlocks = firstBlock.getReachableBlocks();
                Ctxt ctxt = new Ctxt(module, knownBlocks);
                FunctionDefinition def = module.define(name);
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
                // FIRST PASS: add all basic blocks with their phi instructions
                ctxt.blocks.put(firstBlock, def);
                for (BasicBlock bb : knownBlocks) {
                    if (bb != firstBlock) {
                        cc.quarkus.qcc.machine.llvm.BasicBlock block = def.createBlock();
                        ctxt.blocks.put(bb, block);
                        // process phi instructions
                        processPhis(ctxt, bb.getTerminalInstruction(), bb, block);
                    }
                }
                // now add the instructions for each block
                for (BasicBlock bb : knownBlocks) {
                    addInst(ctxt, bb.getTerminalInstruction(), bb, ctxt.blocks.get(bb));
                }
                // now finish the phis
                for (BasicBlock bb : knownBlocks) {
                    if (bb != firstBlock) {
                        finishPhis(ctxt, bb.getTerminalInstruction(), ctxt.blocks.get(bb));
                    }
                }
                // now write the terminal instructions
                for (BasicBlock bb : knownBlocks) {
                    addTermInst(ctxt, bb.getTerminalInstruction(), bb, ctxt.blocks.get(bb));
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

    private static void finishPhis(final Ctxt ctxt, final Instruction inst, final cc.quarkus.qcc.machine.llvm.BasicBlock target) {
        Instruction dep = inst.getDependency();
        if (dep != null) {
            finishPhis(ctxt, dep, target);
        }
        if (inst instanceof PhiInstruction) {
            PhiValue phiValue = ((PhiInstruction) inst).getValue();
            Phi phi = ctxt.phis.get(inst);
            for (BasicBlock bb : ctxt.knownBlocks) {
                Value inVal = phiValue.getValueForBlock(bb);
                if (inVal != null) {
                    cc.quarkus.qcc.machine.llvm.BasicBlock innerTarget = ctxt.blocks.get(bb);
                    phi.item(getValue(ctxt, inVal, bb, innerTarget), innerTarget);
                }
            }
        }
    }

    private static void processPhis(final Ctxt ctxt, final Instruction inst, final BasicBlock bb, final cc.quarkus.qcc.machine.llvm.BasicBlock target) {
        Instruction dep = inst.getDependency();
        if (dep != null) {
            processPhis(ctxt, dep, bb, target);
        }
        if (inst instanceof PhiInstruction) {
            Phi phi = target.phi(Types.i32);
            PhiInstruction phiInst = (PhiInstruction) inst;
            ctxt.phis.put(phiInst, phi);
            ctxt.values.put(phiInst.getValue(), phi.asLocal());
        }
    }

    private static void addInst(final Ctxt ctxt, final Instruction inst, final BasicBlock block, final cc.quarkus.qcc.machine.llvm.BasicBlock target) {
        Instruction dep = inst.getDependency();
        if (dep != null) {
            addInst(ctxt, dep, block, target);
        }
        if (inst instanceof TerminalInstruction) {
            // ignore
        } else if (inst instanceof PhiInstruction) {
            // ignore
        } else {
            throw new IllegalStateException();
        }
    }

    private static void addTermInst(final Ctxt ctxt, final TerminalInstruction inst, final BasicBlock block, final cc.quarkus.qcc.machine.llvm.BasicBlock target) {
        if (inst instanceof ReturnValueInstruction) {
            target.ret(Types.i32, getValue(ctxt, ((ReturnValueInstruction) inst).getReturnValue(), block, target));
        } else if (inst instanceof ReturnInstruction) {
            target.ret();
        } else if (inst instanceof GotoInstruction) {
            BasicBlock jmpTarget = ((GotoInstruction) inst).getTarget();
            cc.quarkus.qcc.machine.llvm.BasicBlock bbTarget = getBlock(ctxt, target, jmpTarget);
            target.br(bbTarget);
        } else if (inst instanceof IfInstruction) {
            IfInstruction ifInst = (IfInstruction) inst;
            Value cond = ifInst.getCondition();
            BasicBlock tb = ifInst.getTrueBranch();
            BasicBlock fb = ifInst.getFalseBranch();
            cc.quarkus.qcc.machine.llvm.BasicBlock tTarget = getBlock(ctxt, target, tb);
            cc.quarkus.qcc.machine.llvm.BasicBlock fTarget = getBlock(ctxt, target, fb);
            cc.quarkus.qcc.machine.llvm.Value condVal = getValue(ctxt, cond, block, target);
            target.br(condVal, tTarget, fTarget);
        } else {
            throw new IllegalStateException();
        }
    }

    private static cc.quarkus.qcc.machine.llvm.Value getValue(final Ctxt ctxt, final Value value, final BasicBlock block, final cc.quarkus.qcc.machine.llvm.BasicBlock target) {
        cc.quarkus.qcc.machine.llvm.Value val = ctxt.values.get(value);
        if (val != null) {
            return val;
        }
        if (value instanceof CommutativeBinaryOp) {
            CommutativeBinaryOp op = (CommutativeBinaryOp) value;
            switch (op.getKind()) {
                case ADD: val = target.add(Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case AND: val = target.and(Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case OR: val = target.or(Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case XOR: val = target.xor(Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case MULTIPLY: val = target.mul(Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case CMP_EQ: val = target.icmp(IntCondition.eq, Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case CMP_NE: val = target.icmp(IntCondition.ne, Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                default: throw new IllegalStateException();
            }
            ctxt.values.put(value, val);
        } else if (value instanceof NonCommutativeBinaryOp) {
            NonCommutativeBinaryOp op = (NonCommutativeBinaryOp) value;
            switch (op.getKind()) {
                case SUB: val = target.sub(Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case DIV: val = target.sdiv(Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case MOD: val = target.srem(Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case CMP_LT: val = target.icmp(IntCondition.slt, Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case CMP_LE: val = target.icmp(IntCondition.sle, Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case CMP_GT: val = target.icmp(IntCondition.sgt, Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                case CMP_GE: val = target.icmp(IntCondition.sge, Types.i32, getValue(ctxt, op.getLeft(), block, target), getValue(ctxt, op.getRight(), block, target)).asLocal(); break;
                default: throw new IllegalStateException();
            }
            ctxt.values.put(value, val);
        } else if (value instanceof IntConstantValueImpl) {
            ctxt.values.put(value, val = LLVM.intConstant(((IntConstantValueImpl) value).getValue()));
        } else {
            throw new IllegalStateException();
        }
        return val;
    }

    private static cc.quarkus.qcc.machine.llvm.BasicBlock getBlock(final Ctxt ctxt, final cc.quarkus.qcc.machine.llvm.BasicBlock target, final BasicBlock toAdd) {
        return ctxt.blocks.get(toAdd);
    }

    static final class Ctxt {
        final Map<Value, cc.quarkus.qcc.machine.llvm.Value> values = new HashMap<>();
        final Map<BasicBlock, cc.quarkus.qcc.machine.llvm.BasicBlock> blocks = new HashMap<>();
        final Set<BasicBlock> processed = new HashSet<>();
        final Module module;
        final Set<BasicBlock> knownBlocks;
        final Map<PhiInstruction, Phi> phis = new HashMap<>();
        final Map<PhiValue, BasicBlock> phiOwners = new HashMap<>();

        Ctxt(final Module module, final Set<BasicBlock> knownBlocks) {
            this.module = module;
            this.knownBlocks = knownBlocks;
        }
    }
}
