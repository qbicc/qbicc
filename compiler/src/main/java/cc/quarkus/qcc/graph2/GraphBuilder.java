package cc.quarkus.qcc.graph2;

import static java.lang.Math.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.type.universe.Universe;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 */
public final class GraphBuilder extends MethodVisitor {
    private static final Value[] NO_VALUES = new Value[0];
    final BasicBlock firstBlock;
    final ArrayDeque<Value> stack = new ArrayDeque<>();
    final ArrayList<Value> locals = new ArrayList<>();
    final List<ParameterValue> originalParams;
    int pni = 0;
    BasicBlock currentBlock; // set in initial and gotInst
    Instruction prevInst;
    final Map<Label, NodeHandle> allBlocks = new IdentityHashMap<>();
    final List<Label> pendingLabels = new ArrayList<>();
    final State initial = new Initial();
    final State gotInst = new GotInst();
    final State noBlock = new NoBlock();
    final State possibleBlock = new PossibleBlock();
    // basic block enterer state
    final Map<BasicBlock, Map<NodeHandle, Capture>> enterers = new IdentityHashMap<>();
    // basic block phis
    final Map<BasicBlock, Capture> capturedPhis = new IdentityHashMap<>();

    public GraphBuilder(int paramCount, int mods) {
        super(Universe.ASM_VERSION);
        boolean isStatic = (mods & Opcodes.ACC_STATIC) != 0;
        if (! isStatic) {
            // there's a receiver
            paramCount += 1;
        }
        if (paramCount == 0) {
            originalParams = List.of();
        } else {
            List<ParameterValue> params = Arrays.asList(new ParameterValue[paramCount]);
            for (int i = 0; i < paramCount; i ++) {
                ParameterValue pv = new ParameterValueImpl();
                pv.setIndex(i);
                params.set(i, pv);
            }
            originalParams = params;
            locals.addAll(params);
        }
        firstBlock = new BasicBlockImpl();
    }

    public void visitParameter(final String name, final int access) {
        ParameterValue pv = originalParams.get(pni++);
        pv.setName(name);
    }

    public void visitCode() {
        // TODO: set locals to this & method parameter values
        currentBlock = firstBlock;
        enter(initial);
    }

    Value pop2() {
        Value value = pop();
        pop();
        return value;
    }

    Value pop() {
        return stack.removeLast();
    }

    void dup() {
        push(peek());
    }

    void dup2() {
        Value v2 = pop();
        Value v1 = pop();
        push(v1);
        push(v2);
        push(v1);
        push(v2);
    }

    void swap() {
        Value v2 = pop();
        Value v1 = pop();
        push(v2);
        push(v1);
    }

    void push2(Value value) {
        push(Value.ICONST_0);
        push(value);
    }

    void push(Value value) {
        stack.addLast(value);
    }

    Value peek() {
        return stack.peekLast();
    }

    Value peek2() {
        return peek();
    }

    Capture capture() {
        Value[] captureStack = stack.toArray(NO_VALUES);
        Value[] captureLocals = locals.toArray(NO_VALUES);
        return new Capture(captureStack, captureLocals);
    }

    void registerEntererCapture(final BasicBlock currentBlock, final NodeHandle jumpTarget, final Capture capture) {
        Map<NodeHandle, Capture> inner = enterers.get(currentBlock);
        if (inner == null) {
            enterers.put(currentBlock, Map.of(jumpTarget, capture));
        } else {
            enterers.put(currentBlock, Util.copyMap(inner, jumpTarget, capture));
        }
    }

    void phiAndCapture(final BasicBlock currentBlock) {
        // create phis for every value on the stack or local table.
        // do NOT coalesce same values since they might only be the same from certain entry points.
        int size = stack.size();
        for (int i = 0; i < size; i ++) {
            Value item = stack.removeFirst();
            // this will create some useless phis for long/double, but that's OK, we can delete them later
            PhiValueImpl phiValue = new PhiValueImpl();
            phiValue.setValueForBlock(currentBlock, item);
            stack.addLast(phiValue);
        }
        size = locals.size();
        for (int i = 0; i < size; i ++) {
            Value value = locals.get(i);
            if (value != null) {
                PhiValue phiValue = new PhiValueImpl();
                phiValue.setValueForBlock(currentBlock, value);
                locals.set(i, phiValue);
            }
        }
        capturedPhis.put(currentBlock, capture());
    }

    NodeHandle getOrMakeBlockHandle(Label label) {
        NodeHandle nodeHandle = allBlocks.get(label);
        if (nodeHandle == null) {
            nodeHandle = new NodeHandle();
            allBlocks.put(label, nodeHandle);
        }
        return nodeHandle;
    }

    NodeHandle getBlockHandleIfExists(Label label) {
        return allBlocks.get(label);
    }

    public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
        // todo
    }

    public void visitLocalVariable(final String name, final String descriptor, final String signature, final Label start, final Label end, final int index) {
        // todo
    }

    public void visitLineNumber(final int line, final Label start) {
        // todo
    }

    public void visitEnd() {
        // fail if the state is invalid
        super.visitEnd();
        // now wrap up the phis
        for (BasicBlock basicBlock : capturedPhis.keySet()) {
            Capture phiSet = capturedPhis.get(basicBlock);
            Map<NodeHandle, Capture> captures = enterers.getOrDefault(basicBlock, Map.of());
            for (Map.Entry<NodeHandle, Capture> entry : captures.entrySet()) {
                NodeHandle enteringHandle = entry.getKey();
                BasicBlock enterer = enteringHandle.getTarget();
                Capture capture = entry.getValue();
                Value[] phiLocals = phiSet.locals;
                Value[] captureLocals = capture.locals;
                for (int i = 0; i < min(phiLocals.length, captureLocals.length); i ++) {
                    PhiValue phiValue = (PhiValue) phiLocals[i];
                    phiValue.setValueForBlock(enterer, captureLocals[i]);
                }
                Value[] phiStack = phiSet.stack;
                Value[] captureStack = capture.stack;
                for (int i = 0; i < min(phiStack.length, captureStack.length); i ++) {
                    PhiValue phiValue = (PhiValue) phiStack[i];
                    phiValue.setValueForBlock(enterer, captureStack[i]);
                }
            }
        }
    }

    void enter(State state) {
        State old = (State) this.mv;
        if (old != null) {
            old.handleExit(state);
        }
        this.mv = state;
        state.handleEntry(old);
    }

    MethodVisitor outer() {
        return this;
    }

    abstract static class State extends MethodVisitor {
        State() {
            super(Universe.ASM_VERSION);
        }

        void handleEntry(State previous) {
        }

        void handleExit(State next) {
        }

        public void visitInsn(final int opcode) {
            throw new IllegalStateException();
        }

        public void visitIntInsn(final int opcode, final int operand) {
            throw new IllegalStateException();
        }

        public void visitVarInsn(final int opcode, final int var) {
            throw new IllegalStateException();
        }

        public void visitTypeInsn(final int opcode, final String type) {
            throw new IllegalStateException();
        }

        public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
            throw new IllegalStateException();
        }

        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            throw new IllegalStateException();
        }

        public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
            throw new IllegalStateException();
        }

        public void visitJumpInsn(final int opcode, final Label label) {
            throw new IllegalStateException();
        }

        public void visitLdcInsn(final Object value) {
            throw new IllegalStateException();
        }

        public void visitIincInsn(final int var, final int increment) {
            throw new IllegalStateException();
        }

        public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
            throw new IllegalStateException();
        }

        public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
            throw new IllegalStateException();
        }

        public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
            throw new IllegalStateException();
        }

        public void visitEnd() {
            throw new IllegalStateException("Unterminated block");
        }
    }

    final class Initial extends State {
        Initial() {
        }

        public void visitInsn(final int opcode) {
            enter(gotInst);
            outer().visitInsn(opcode);
        }

        public void visitIntInsn(final int opcode, final int operand) {
            enter(gotInst);
            outer().visitIntInsn(opcode, operand);
        }

        public void visitVarInsn(final int opcode, final int var) {
            enter(gotInst);
            outer().visitVarInsn(opcode, var);
        }

        public void visitTypeInsn(final int opcode, final String type) {
            enter(gotInst);
            outer().visitTypeInsn(opcode, type);
        }

        public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
            enter(gotInst);
            outer().visitFieldInsn(opcode, owner, name, descriptor);
        }

        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            enter(gotInst);
            outer().visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
            enter(gotInst);
            outer().visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        public void visitJumpInsn(final int opcode, final Label label) {
            enter(gotInst);
            outer().visitJumpInsn(opcode, label);
        }

        public void visitLdcInsn(final Object value) {
            enter(gotInst);
            outer().visitLdcInsn(value);
        }

        public void visitIincInsn(final int var, final int increment) {
            enter(gotInst);
            outer().visitIincInsn(var, increment);
        }

        public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
            enter(gotInst);
            outer().visitTableSwitchInsn(min, max, dflt, labels);
        }

        public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
            enter(gotInst);
            outer().visitLookupSwitchInsn(dflt, keys, labels);
        }

        public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
            enter(gotInst);
            outer().visitMultiANewArrayInsn(descriptor, numDimensions);
        }

        public void visitLabel(final Label label) {
            NodeHandle handle = allBlocks.get(label);
            if (handle == null) {
                allBlocks.put(label, NodeHandle.of(currentBlock));
            } else {
                handle.setTarget(currentBlock);
            }
        }
    }
    final class GotInst extends State {
        GotInst() {
        }

        public void visitInsn(final int opcode) {
            switch (opcode) {
                case Opcodes.NOP: {
                    return;
                }
                case Opcodes.ICONST_0: {
                    push(Value.ICONST_0);
                    return;
                }
                case Opcodes.ICONST_1:
                case Opcodes.ICONST_2:
                case Opcodes.ICONST_3:
                case Opcodes.ICONST_4:
                case Opcodes.ICONST_5: {
                    push(Value.iconst(opcode - Opcodes.ICONST_0));
                    return;
                }
                case Opcodes.POP: {
                    pop();
                    return;
                }
                case Opcodes.POP2: {
                    pop2();
                    return;
                }
                case Opcodes.DUP: {
                    dup();
                    return;
                }
                case Opcodes.DUP2: {
                    dup2();
                    return;
                }
                case Opcodes.SWAP: {
                    swap();
                    return;
                }
                case Opcodes.INEG: {
                    push(Value.ICONST_0);
                    swap();
                    visitInsn(Opcodes.ISUB);
                    return;
                }
                case Opcodes.ISHL:
                case Opcodes.ISHR:
                case Opcodes.IUSHR:
                case Opcodes.IMUL:
                case Opcodes.IAND:
                case Opcodes.IOR:
                case Opcodes.IXOR:
                case Opcodes.IADD:
                case Opcodes.FMUL:
                case Opcodes.FADD: {
                    CommutativeBinaryOpImpl op = new CommutativeBinaryOpImpl();
                    op.setKind(CommutativeBinaryOp.Kind.fromOpcode(opcode));
                    op.setLeft(pop());
                    op.setRight(pop());
                    push(op);
                    return;
                }
                case Opcodes.LSHL:
                case Opcodes.LSHR:
                case Opcodes.LUSHR:
                case Opcodes.LMUL:
                case Opcodes.LAND:
                case Opcodes.LOR:
                case Opcodes.LXOR:
                case Opcodes.LADD:
                case Opcodes.DMUL:
                case Opcodes.DADD: {
                    CommutativeBinaryOpImpl op = new CommutativeBinaryOpImpl();
                    op.setKind(CommutativeBinaryOp.Kind.fromOpcode(opcode));
                    op.setLeft(pop2());
                    op.setRight(pop2());
                    push2(op);
                    return;
                }
                case Opcodes.LNEG:
                case Opcodes.FNEG:
                case Opcodes.DNEG:
                case Opcodes.ACONST_NULL:
                case Opcodes.ICONST_M1:
                case Opcodes.LCONST_0:
                case Opcodes.LCONST_1:
                case Opcodes.FCONST_0:
                case Opcodes.FCONST_1:
                case Opcodes.FCONST_2:
                case Opcodes.DCONST_0:
                case Opcodes.DCONST_1:
                case Opcodes.IALOAD:
                case Opcodes.LALOAD:
                case Opcodes.FALOAD:
                case Opcodes.DALOAD:
                case Opcodes.AALOAD:
                case Opcodes.BALOAD:
                case Opcodes.CALOAD:
                case Opcodes.SALOAD:
                case Opcodes.IASTORE:
                case Opcodes.LASTORE:
                case Opcodes.FASTORE:
                case Opcodes.DASTORE:
                case Opcodes.AASTORE:
                case Opcodes.BASTORE:
                case Opcodes.CASTORE:
                case Opcodes.SASTORE:
                case Opcodes.DUP_X1:
                case Opcodes.DUP_X2:
                case Opcodes.DUP2_X1:
                case Opcodes.DUP2_X2:
                case Opcodes.ISUB:
                case Opcodes.LSUB:
                case Opcodes.FSUB:
                case Opcodes.DSUB:
                case Opcodes.IDIV:
                case Opcodes.LDIV:
                case Opcodes.FDIV:
                case Opcodes.DDIV:
                case Opcodes.IREM:
                case Opcodes.LREM:
                case Opcodes.FREM:
                case Opcodes.DREM:
                case Opcodes.I2L:
                case Opcodes.I2F:
                case Opcodes.I2D:
                case Opcodes.L2I:
                case Opcodes.L2F:
                case Opcodes.L2D:
                case Opcodes.F2I:
                case Opcodes.F2L:
                case Opcodes.F2D:
                case Opcodes.D2I:
                case Opcodes.D2L:
                case Opcodes.D2F:
                case Opcodes.I2B:
                case Opcodes.I2C:
                case Opcodes.I2S:
                case Opcodes.LCMP:
                case Opcodes.FCMPL:
                case Opcodes.FCMPG:
                case Opcodes.DCMPL:
                case Opcodes.DCMPG:
                case Opcodes.ARRAYLENGTH:
                case Opcodes.ATHROW:
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT: {
                    throw new UnsupportedOperationException();
                }
                case Opcodes.LRETURN:
                case Opcodes.DRETURN: {
                    Value retVal = pop2();
                    ReturnValueInstructionImpl insn = new ReturnValueInstructionImpl();
                    insn.setReturnValue(retVal);
                    BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                    currentBlock.setTerminalInstruction(insn);
                    enter(noBlock);
                    return;
                }
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN: {
                    Value retVal = pop();
                    ReturnValueInstructionImpl insn = new ReturnValueInstructionImpl();
                    insn.setReturnValue(retVal);
                    BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                    currentBlock.setTerminalInstruction(insn);
                    enter(noBlock);
                    return;
                }
                case Opcodes.RETURN: {
                    ReturnInstructionImpl insn = new ReturnInstructionImpl();
                    BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                    currentBlock.setTerminalInstruction(insn);
                    enter(noBlock);
                    return;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }

        public void visitJumpInsn(final int opcode, final Label label) {
            switch (opcode) {
                case Opcodes.GOTO: {
                    BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                    NodeHandle jumpTarget = getOrMakeBlockHandle(label);
                    registerEntererCapture(currentBlock, jumpTarget, capture());
                    GotoInstructionImpl goto_ = new GotoInstructionImpl();
                    //todo: jumpTarget is unresolved, push it for phi analysis
                    goto_.setTarget(jumpTarget);
                    goto_.setDependency(prevInst);
                    currentBlock.setTerminalInstruction(goto_);
                    enter(noBlock);
                    return;
                }
                case Opcodes.IFEQ:
                case Opcodes.IFNE: {
                    CommutativeBinaryOp op = new CommutativeBinaryOpImpl();
                    op.setKind(CommutativeBinaryOp.Kind.fromOpcode(opcode));
                    op.setRight(Value.ICONST_0);
                    op.setLeft(pop());
                    handleIfInsn(op, label);
                    return;
                }
                case Opcodes.IFLT:
                case Opcodes.IFGT:
                case Opcodes.IFLE:
                case Opcodes.IFGE: {
                    NonCommutativeBinaryOp op = new NonCommutativeBinaryOpImpl();
                    op.setKind(NonCommutativeBinaryOp.Kind.fromOpcode(opcode));
                    op.setRight(Value.ICONST_0);
                    op.setLeft(pop());
                    handleIfInsn(op, label);
                    return;
                }
                case Opcodes.IF_ICMPEQ:
                case Opcodes.IF_ICMPNE: {
                    CommutativeBinaryOp op = new CommutativeBinaryOpImpl();
                    op.setKind(CommutativeBinaryOp.Kind.fromOpcode(opcode));
                    op.setRight(pop());
                    op.setLeft(pop());
                    handleIfInsn(op, label);
                    return;
                }
                case Opcodes.IF_ICMPLE:
                case Opcodes.IF_ICMPLT:
                case Opcodes.IF_ICMPGE:
                case Opcodes.IF_ICMPGT: {
                    NonCommutativeBinaryOp op = new NonCommutativeBinaryOpImpl();
                    op.setKind(NonCommutativeBinaryOp.Kind.fromOpcode(opcode));
                    op.setRight(pop());
                    op.setLeft(pop());
                    handleIfInsn(op, label);
                    return;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }

        public void visitIincInsn(final int var, final int increment) {
            CommutativeBinaryOpImpl op = new CommutativeBinaryOpImpl();
            op.setKind(CommutativeBinaryOp.Kind.ADD);
            op.setLeft(locals.get(var));
            op.setRight(Value.iconst(1));
            locals.set(var, op);
        }

        void handleIfInsn(Value cond, Label label) {
            BasicBlock currentBlock = GraphBuilder.this.currentBlock;
            NodeHandle jumpTarget = getOrMakeBlockHandle(label);
            IfInstructionImpl if_ = new IfInstructionImpl();
            if_.setCondition(cond);
            if_.setTrueBranch(jumpTarget);
            BasicBlockImpl newBlock = new BasicBlockImpl();
            if_.setFalseBranch(newBlock);
            Capture capture = capture();
            registerEntererCapture(currentBlock, jumpTarget, capture);
            currentBlock.setTerminalInstruction(if_);
            phiAndCapture(newBlock);
            GraphBuilder.this.currentBlock = newBlock;
            enter(initial);
        }

        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            //  todo
        }

        public void visitIntInsn(final int opcode, final int operand) {
            switch (opcode) {
                case Opcodes.BIPUSH:
                case Opcodes.SIPUSH: {
                    push(Value.iconst(operand));
                    return;
                }
                case Opcodes.NEWARRAY: {
                    throw new UnsupportedOperationException();
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }

        public void visitVarInsn(final int opcode, final int var) {
            switch (opcode) {
                case Opcodes.ILOAD:
                case Opcodes.ALOAD: {
                    push(locals.get(var));
                    return;
                }
                case Opcodes.DLOAD:
                case Opcodes.LLOAD: {
                    push2(locals.get(var));
                    return;
                }
                case Opcodes.ISTORE:
                case Opcodes.ASTORE: {
                    while (locals.size() <= var) {
                        locals.add(null);
                    }
                    locals.set(var, pop());
                    return;
                }
                case Opcodes.DSTORE:
                case Opcodes.LSTORE: {
                    while (locals.size() <= var) {
                        locals.add(null);
                    }
                    locals.set(var, pop2());
                    return;
                }
            }
        }

        public void visitLabel(final Label label) {
            // something enters at this point, so we need to start up a new block
            BasicBlockImpl newBlock = new BasicBlockImpl();
            NodeHandle blockHandle = getBlockHandleIfExists(label);
            if (blockHandle == null) {
                blockHandle = newBlock.getHandle();
                allBlocks.put(label, blockHandle);
            } else {
                blockHandle.setTarget(newBlock);
            }
            BasicBlock currentBlock = GraphBuilder.this.currentBlock;
            // terminate it
            GotoInstructionImpl goto_ = new GotoInstructionImpl();
            goto_.setTarget(blockHandle);
            goto_.setDependency(prevInst);
            currentBlock.setTerminalInstruction(goto_);
            phiAndCapture(currentBlock);
            GraphBuilder.this.currentBlock = newBlock;
            enter(initial);
        }

    }

    final class NoBlock extends State {
        NoBlock() {
        }

        void handleEntry(final State previous) {
            currentBlock = null;
        }

        public void visitLabel(final Label label) {
            // a new block begins
            pendingLabels.add(label);
            enter(possibleBlock);
        }

        public void visitEnd() {
            // OK
            return;
        }
    }

    final class PossibleBlock extends State {
        PossibleBlock() {
        }

        public void visitLabel(final Label label) {
            pendingLabels.add(label);
        }

        void handleExit(final State next) {
            assert next == gotInst;
            // a new block begins
            BasicBlockImpl newBlock = new BasicBlockImpl();
            for (Label label : pendingLabels) {
                NodeHandle blockHandle = getBlockHandleIfExists(label);
                if (blockHandle == null) {
                    blockHandle = newBlock.getHandle();
                    allBlocks.put(label, blockHandle);
                } else {
                    blockHandle.setTarget(newBlock);
                }
            }
            currentBlock = newBlock;
            phiAndCapture(newBlock);
            pendingLabels.clear();
        }

        public void visitInsn(final int opcode) {
            enter(gotInst);
            outer().visitInsn(opcode);
        }

        public void visitIntInsn(final int opcode, final int operand) {
            enter(gotInst);
            outer().visitIntInsn(opcode, operand);
        }

        public void visitVarInsn(final int opcode, final int var) {
            enter(gotInst);
            outer().visitVarInsn(opcode, var);
        }

        public void visitTypeInsn(final int opcode, final String type) {
            enter(gotInst);
            outer().visitTypeInsn(opcode, type);
        }

        public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
            enter(gotInst);
            outer().visitFieldInsn(opcode, owner, name, descriptor);
        }

        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            enter(gotInst);
            outer().visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
            enter(gotInst);
            outer().visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        public void visitJumpInsn(final int opcode, final Label label) {
            enter(gotInst);
            outer().visitJumpInsn(opcode, label);
        }

        public void visitLdcInsn(final Object value) {
            enter(gotInst);
            outer().visitLdcInsn(value);
        }

        public void visitIincInsn(final int var, final int increment) {
            enter(gotInst);
            outer().visitIincInsn(var, increment);
        }

        public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
            enter(gotInst);
            outer().visitTableSwitchInsn(min, max, dflt, labels);
        }

        public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
            enter(gotInst);
            outer().visitLookupSwitchInsn(dflt, keys, labels);
        }

        public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
            enter(gotInst);
            outer().visitMultiANewArrayInsn(descriptor, numDimensions);
        }

        public void visitEnd() {
            // OK
        }
    }

    // Captured state of stack & locals
    static final class Capture {
        final Value[] stack;
        final Value[] locals;

        Capture(final Value[] stack, final Value[] locals) {
            this.stack = stack;
            this.locals = locals;
        }
    }
}
