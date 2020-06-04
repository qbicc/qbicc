package cc.quarkus.qcc.graph;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.type.universe.Universe;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 *
 */
public final class GraphBuilder extends MethodVisitor {
    final BasicBlockImpl firstBlock;
    ItemSize[] frameLocalMap;
    ItemSize[] frameStackMap;
    ItemSize[] localMap;
    ItemSize[] stackMap;
    int fsp; // points after the last frame stack element
    int flp; // points after the last frame local in use
    Value[] locals;
    Value[] stack;
    int sp; // points after the last stack element
    int lp; // points to after the last local in use
    // block exit values are whatever
    final Map<BasicBlock, Capture> blockExits = new HashMap<>();
    // block enter values are *all* PhiValues bound to PhiInstructions on the entered block
    final Map<BasicBlock, Capture> blockEnters = new HashMap<>();
    final Value thisValue;
    final List<ParameterValue> originalParams;
    int pni = 0;
    BasicBlockImpl futureBlock;
    BasicBlockImpl currentBlock;
    MemoryState memoryState;
    final Map<Label, NodeHandle> allBlocks = new IdentityHashMap<>();
    final List<Label> pendingLabels = new ArrayList<>();
    final State inBlockState = new InBlock();
    final State futureBlockState = new FutureBlockState();
    final State mayNeedFrameState = new MayNeedFrameState();
    final State possibleBlockState = new PossibleBlock();

    public GraphBuilder(final int mods, final String name, final String descriptor, final String signature, final String[] exceptions) {
        super(Universe.ASM_VERSION);
        boolean isStatic = (mods & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = Type.getArgumentTypes(descriptor);
        int localsCount = argTypes.length;
        if (! isStatic) {
            // there's a receiver
            localsCount += 1;
        }
        // first block cannot be entered, so don't bother adding an enter for it
        firstBlock = new BasicBlockImpl();
        // set up the initial stack maps
        int initialLocalsSize = localsCount << 1;
        localMap = new ItemSize[initialLocalsSize];
        locals = new Value[initialLocalsSize];
        Value thisValue = null;
        if (localsCount == 0) {
            originalParams = List.of();
        } else {
            List<ParameterValue> params = Arrays.asList(new ParameterValue[localsCount]);
            int j = 0;
            if (! isStatic) {
                // "this" receiver - todo: ThisValue
                ParameterValue pv = new ParameterValueImpl();
                pv.setOwner(firstBlock);
                thisValue = pv;
                pv.setIndex(j);
                setLocal(ItemSize.SINGLE, j, pv);
                j++;
            }
            for (int i = 0; i < argTypes.length; i ++) {
                ParameterValue pv = new ParameterValueImpl();
                pv.setOwner(firstBlock);
                pv.setIndex(j);
                if (argTypes[i] == Type.LONG_TYPE || argTypes[i] == Type.DOUBLE_TYPE) {
                    setLocal(ItemSize.DOUBLE, j, pv);
                    j+= 2;
                } else {
                    setLocal(ItemSize.SINGLE, j, pv);
                    j++;
                }
                params.set(i, pv);
            }
            originalParams = params;
        }
        this.thisValue = thisValue;
        frameLocalMap = localMap.clone();
        flp = lp;
        stackMap = new ItemSize[16];
        stack = new Value[16];
        sp = 0;
        frameStackMap = new ItemSize[16];
        fsp = 0;
    }

    public void visitParameter(final String name, final int access) {
        ParameterValue pv = originalParams.get(pni++);
        pv.setName(name);
    }

    public void visitCode() {
        currentBlock = firstBlock;
        enter(inBlockState);
    }

    // stack manipulation

    ItemSize topOfStackItemSize() {
        return stackMap[sp - 1];
    }

    void clearStack() {
        Arrays.fill(stack, 0, sp, null);
        Arrays.fill(stackMap, 0, sp, null);
        sp = 0;
    }

    Value popSmart() {
        int tos = sp - 1;
        ItemSize type = stackMap[tos];
        Value value = stack[tos];
        stack[tos] = null;
        stackMap[tos] = null;
        sp = tos;
        return value;
    }

    Value pop2() {
        int tos = sp - 1;
        ItemSize type = stackMap[tos];
        Value value;
        if (type == ItemSize.DOUBLE) {
            value = stack[tos];
            stack[tos] = null;
            stackMap[tos] = null;
            sp = tos;
        } else {
            value = stack[tos];
            stack[tos] = null;
            stackMap[tos] = null;
            stack[tos - 1] = null;
            stackMap[tos - 1] = null;
            sp = tos - 1;
        }
        return value;
    }

    Value pop() {
        int tos = sp - 1;
        ItemSize type = stackMap[tos];
        Value value;
        if (type == ItemSize.DOUBLE) {
            throw new IllegalStateException("Bad pop");
        }
        value = stack[tos];
        stack[tos] = null;
        stackMap[tos] = null;
        sp = tos;
        return value;
    }

    void ensureStackSize(int size) {
        int len = stack.length;
        assert len == stackMap.length;
        if (len < size) {
            stack = Arrays.copyOf(stack, len << 1);
            stackMap = Arrays.copyOf(stackMap, len << 1);
        }
    }

    void dup() {
        ItemSize type = stackMap[sp - 1];
        if (type == ItemSize.DOUBLE) {
            throw new IllegalStateException("Bad dup");
        }
        push(type, peek());
    }

    void dup2() {
        ItemSize type = stackMap[sp - 1];
        if (type == ItemSize.DOUBLE) {
            push(type, peek());
        } else {
            Value v2 = pop();
            Value v1 = pop();
            push(type, v1);
            push(type, v2);
            push(type, v1);
            push(type, v2);
        }
    }

    void swap() {
        ItemSize type = stackMap[sp - 1];
        if (type == ItemSize.DOUBLE) {
            throw new IllegalStateException("Bad swap");
        }
        Value v2 = pop();
        Value v1 = pop();
        push(ItemSize.SINGLE, v2);
        push(ItemSize.SINGLE, v1);
    }

    void push(ItemSize type, Value value) {
        int sp = this.sp;
        ensureStackSize(sp + 1);
        stack[sp] = value;
        stackMap[sp] = type;
        this.sp = sp + 1;
    }

    Value peek() {
        return stack[sp];
    }

    // Locals manipulation

    void ensureLocalSize(int size) {
        int len = localMap.length;
        assert len == locals.length;
        if (len < size) {
            localMap = Arrays.copyOf(localMap, len << 1);
            locals = Arrays.copyOf(locals, len << 1);
        }
    }

    void clearLocals() {
        Arrays.fill(localMap, 0, lp, null);
        Arrays.fill(locals, 0, lp, null);
        lp = 0;
    }

    void setLocal(ItemSize type, int index, Value value) {
        if (type == ItemSize.DOUBLE) {
            ensureLocalSize(index + 2);
            localMap[index + 1] = null;
            locals[index + 1] = null;
            lp = max(index + 2, lp);
        } else {
            ensureLocalSize(index + 1);
            lp = max(index + 1, lp);
        }
        localMap[index] = type;
        locals[index] = value;
    }

    Value getLocal(ItemSize type, int index) {
        if (index > lp) {
            throw new IllegalStateException("Invalid local index");
        }
        ItemSize curType = localMap[index];
        if (curType == null) {
            throw new IllegalStateException("Invalid get local (no value)");
        }
        if (type != curType) {
            throw new IllegalStateException("Bad type for getLocal");
        }
        return locals[index];
    }

    // Frame stack manipulation

    void ensureFrameStackSize(int size) {
        int len = frameStackMap.length;
        if (len < size) {
            frameStackMap = Arrays.copyOf(frameStackMap, len << 1);
        }
    }

    void clearFrameStack() {
        Arrays.fill(frameStackMap, 0, fsp, null);
        fsp = 0;
    }

    void addFrameStackItem(ItemSize type) {
        int fsp = this.fsp;
        ensureFrameStackSize(fsp);
        frameStackMap[fsp] = type;
        this.fsp = fsp + 1;
    }

    // Frame locals manipulation

    void ensureFrameLocalSize(int size) {
        int len = frameLocalMap.length;
        if (len < size) {
            frameLocalMap = Arrays.copyOf(frameLocalMap, len << 1);
        }
    }

    void addFrameLocal(ItemSize type) {
        int flp = this.flp;
        ensureFrameLocalSize(flp + 1);
        frameLocalMap[flp] = type;
        this.flp = flp + 1;
    }

    ItemSize removeFrameLocal() {
        int flp = this.flp;
        ItemSize old = frameLocalMap[flp - 1];
        frameLocalMap[flp - 1] = null;
        this.flp = flp - 1;
        return old;
    }

    void clearFrameLocals() {
        Arrays.fill(frameLocalMap, 0, flp, null);
        flp = 0;
    }

    // Capture

    Capture capture() {
        ItemSize[] captureStackMap = Arrays.copyOf(stackMap, sp);
        ItemSize[] captureLocalMap = Arrays.copyOf(localMap, lp);
        Value[] captureStack = Arrays.copyOf(stack, sp);
        Value[] captureLocals = Arrays.copyOf(locals, lp);
        return new Capture(captureStackMap, captureLocalMap, captureStack, captureLocals);
    }

    NodeHandle getOrMakeBlockHandle(Label label) {
        NodeHandle nodeHandle = allBlocks.get(label);
        if (nodeHandle == null) {
            nodeHandle = new NodeHandle();
            allBlocks.put(label, nodeHandle);
        }
        return nodeHandle;
    }

    public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
        // todo
    }

    public void visitLocalVariable(final String name, final String descriptor, final String signature, final Label start, final Label end, final int index) {
    }

    public void visitLineNumber(final int line, final Label start) {
        // todo
    }

    public void visitFrame(final int type, final int numLocal, final Object[] local, final int numStack, final Object[] stack) {
        switch (type) {
            case Opcodes.F_SAME: {
                clearStack();
                return;
            }
            case Opcodes.F_SAME1: {
                clearStack();
                assert numStack == 1;
                if (stack[0] == Opcodes.DOUBLE || stack[0] == Opcodes.LONG) {
                    addFrameStackItem(ItemSize.DOUBLE);
                } else {
                    addFrameStackItem(ItemSize.SINGLE);
                }
                return;
            }
            case Opcodes.F_APPEND: {
                clearStack();
                for (int i = 0; i < numLocal; i++) {
                    if (local[i] == Opcodes.DOUBLE || local[i] == Opcodes.LONG) {
                        addFrameLocal(ItemSize.DOUBLE);
                    } else {
                        addFrameLocal(ItemSize.SINGLE);
                    }
                }
                return;
            }
            case Opcodes.F_CHOP: {
                clearStack();
                for (int i = 0; i < numLocal; i ++) {
                    // todo: check type
                    removeFrameLocal();
                }
                return;
            }
            case Opcodes.F_FULL: {
                clearStack();
                clearFrameLocals();
                for (int i = 0; i < numLocal; i++) {
                    if (local[i] == Opcodes.DOUBLE || local[i] == Opcodes.LONG) {
                        addFrameLocal(ItemSize.DOUBLE);
                    } else {
                        addFrameLocal(ItemSize.SINGLE);
                    }
                }
                for (int i = 0; i < numStack; i++) {
                    if (local[i] == Opcodes.DOUBLE || local[i] == Opcodes.LONG) {
                        addFrameStackItem(ItemSize.DOUBLE);
                    } else {
                        addFrameStackItem(ItemSize.SINGLE);
                    }
                }
                return;
            }
            default: {
                throw new IllegalStateException();
            }
        }
    }

    public void visitEnd() {
        // fail if the state is invalid
        super.visitEnd();
        // now wrap up the phis for every block that exits
        for (Map.Entry<BasicBlock, Capture> i : blockExits.entrySet()) {
            BasicBlock exitingBlock = i.getKey();
            Terminator ti = exitingBlock.getTerminator();
            if (ti instanceof Goto) {
                wirePhis(exitingBlock, ((Goto) ti).getNextBlock());
            } else if (ti instanceof If) {
                If ifTi = (If) ti;
                wirePhis(exitingBlock, ifTi.getTrueBranch());
                wirePhis(exitingBlock, ifTi.getFalseBranch());
            }
        }
    }

    private void wirePhis(final BasicBlock exitingBlock, final BasicBlock enteringBlock) {
        Capture exitState = blockExits.get(exitingBlock);
        Capture enterState = blockEnters.get(enteringBlock);
        // first check & map stack
        int stackSize = enterState.stack.length;
        if (exitState.stack.length != stackSize) {
            throw new IllegalStateException("Stack length mismatch");
        }
        for (int i = 0; i < stackSize; i ++) {
            if (enterState.stackMap[i] != exitState.stackMap[i]) {
                throw new IllegalStateException("Stack entry type mismatch");
            }
            PhiValue value = (PhiValue) enterState.stack[i];
            value.setValueForBlock(exitingBlock, exitState.stack[i]);
        }
        // now locals
        int localSize = enterState.locals.length;
        if (exitState.locals.length < localSize) {
            throw new IllegalStateException("Local vars mismatch");
        }
        for (int i = 0; i < localSize; i ++) {
            if (enterState.localsMap[i] != exitState.localsMap[i]) {
                throw new IllegalStateException("Locals entry type mismatch");
            }
            PhiValue value = (PhiValue) enterState.locals[i];
            // might be null gaps for big values
            if (value != null) {
                value.setValueForBlock(exitingBlock, exitState.locals[i]);
            }
        }
    }

    void enter(State state) {
        State old = (State) this.mv;
        if (old == state) {
            return;
        }
        if (old != null) {
            old.handleExit(state);
        }
        this.mv = state;
        state.handleEntry(old);
    }

    void propagateCurrentStackAndLocals() {
        assert futureBlock == null && currentBlock != null;
        for (int i = 0; i < sp; i ++) {
            ItemSize type = stackMap[i];
            PhiValueImpl pv = new PhiValueImpl();
            pv.setOwner(currentBlock);
            stack[i] = pv;
        }
        for (int i = 0; i < lp; i ++) {
            if (locals[i] != null) {
                PhiValueImpl pv = new PhiValueImpl();
                pv.setOwner(currentBlock);
                locals[i] = pv;
            }
        }
        blockEnters.putIfAbsent(currentBlock, capture());
    }

    void propagateFrameStackAndLocals() {
        assert futureBlock == null && currentBlock != null;
        clearStack();
        clearLocals();
        for (int i = 0; i < fsp; i ++) {
            ItemSize type = frameStackMap[i];
            PhiValueImpl pv = new PhiValueImpl();
            pv.setOwner(currentBlock);
            push(type, pv);
        }
        for (int i = 0; i < flp; i ++) {
            ItemSize type = frameLocalMap[i];
            if (type != null) {
                PhiValueImpl pv = new PhiValueImpl();
                pv.setOwner(currentBlock);
                setLocal(type, i, pv);
            }
        }
        blockEnters.putIfAbsent(currentBlock, capture());
    }

    MethodVisitor outer() {
        return this;
    }

    public BasicBlock getEntryBlock() {
        return this.firstBlock;
    }

    public List<ParameterValue> getParameters() {
        return this.originalParams;
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

    final class InBlock extends State {
        boolean gotInstr;

        InBlock() {
        }

        public void visitTypeInsn(final int opcode, final String type) {
            gotInstr = true;
            super.visitTypeInsn(opcode, type);
        }

        public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
            gotInstr = true;
            switch (opcode) {
                case Opcodes.GETSTATIC: {
                    FieldReadValue read = new StaticFieldReadValueImpl();
                    read.setMemoryDependency(memoryState);
                    // todo: set descriptor
                    memoryState = read;
                    // todo: size from field
                    push(ItemSize.SINGLE, read);
                    break;
                }
                case Opcodes.GETFIELD: {
                    InstanceFieldReadValue read = new InstanceFieldReadValueImpl();
                    read.setInstance(pop());
                    read.setMemoryDependency(memoryState);
                    // todo: set descriptor
                    memoryState = read;
                    // todo: size from field
                    push(ItemSize.SINGLE, read);
                    break;
                }
                case Opcodes.PUTSTATIC: {
                    // todo: get pop type from field descriptor
                    Value value = stackMap[sp - 1] == ItemSize.DOUBLE ? pop2() : pop();
                    FieldWrite write = new StaticFieldWriteImpl();
                    write.setWriteValue(value);
                    write.setMemoryDependency(memoryState);
                    memoryState = write;
                    break;
                }
                case Opcodes.PUTFIELD: {
                    Value value = stackMap[sp - 1] == ItemSize.DOUBLE ? pop2() : pop();
                    InstanceFieldWrite write = new InstanceFieldWriteImpl();
                    write.setWriteValue(value);
                    write.setMemoryDependency(memoryState);
                    memoryState = write;
                    break;
                }
                default: {
                    super.visitFieldInsn(opcode, owner, name, descriptor);
                }
            }
        }

        public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
            gotInstr = true;
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        public void visitLdcInsn(final Object value) {
            gotInstr = true;
            if (value instanceof Integer) {
                push(ItemSize.SINGLE, Value.iconst(((Integer) value).intValue()));
            } else if (value instanceof Long) {
                // TODO: fix this
                push(ItemSize.DOUBLE, Value.iconst((int) ((Long) value).longValue()));
            } else {
                throw new IllegalStateException();
            }
        }

        public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
            gotInstr = true;
            BasicBlock currentBlock = GraphBuilder.this.currentBlock;
            SwitchImpl switch_ = new SwitchImpl();
            switch_.setDefaultTarget(getOrMakeBlockHandle(dflt));
            for (int i = 0; i < labels.length; i ++) {
                switch_.setTargetForValue(min + i, getOrMakeBlockHandle(labels[i]));
            }
            switch_.setMemoryDependency(memoryState);
            currentBlock.setTerminator(switch_);
            enter(possibleBlockState);
        }

        public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
            gotInstr = true;
            BasicBlock currentBlock = GraphBuilder.this.currentBlock;
            SwitchImpl switch_ = new SwitchImpl();
            switch_.setDefaultTarget(getOrMakeBlockHandle(dflt));
            for (int i = 0; i < keys.length; i ++) {
                switch_.setTargetForValue(keys[i], getOrMakeBlockHandle(labels[i]));
            }
            switch_.setMemoryDependency(memoryState);
            currentBlock.setTerminator(switch_);
            enter(possibleBlockState);
        }

        public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
            gotInstr = true;
            super.visitMultiANewArrayInsn(descriptor, numDimensions);
        }

        public void visitInsn(final int opcode) {
            gotInstr = true;
            switch (opcode) {
                case Opcodes.NOP: {
                    return;
                }
                case Opcodes.ICONST_0: {
                    push(ItemSize.SINGLE, Value.ICONST_0);
                    return;
                }
                case Opcodes.ICONST_1:
                case Opcodes.ICONST_2:
                case Opcodes.ICONST_3:
                case Opcodes.ICONST_4:
                case Opcodes.ICONST_5: {
                    push(ItemSize.SINGLE, Value.iconst(opcode - Opcodes.ICONST_0));
                    return;
                }
                case Opcodes.ICONST_M1: {
                    push(ItemSize.SINGLE, Value.iconst(-1));
                    return;
                }
                case Opcodes.LCONST_0:
                case Opcodes.LCONST_1: {
                    push(ItemSize.DOUBLE, Value.lconst(opcode - Opcodes.LCONST_0));
                    break;
                }
                case Opcodes.FCONST_0:
                case Opcodes.FCONST_1:
                case Opcodes.FCONST_2: {
                    // todo: cache
                    push(ItemSize.SINGLE, CastValue.create(Value.iconst(opcode - Opcodes.FCONST_0), cc.quarkus.qcc.graph.Type.F32));
                    break;
                }
                case Opcodes.DCONST_0:
                case Opcodes.DCONST_1: {
                    // todo: cache
                    push(ItemSize.DOUBLE, CastValue.create(Value.iconst(opcode - Opcodes.DCONST_0), cc.quarkus.qcc.graph.Type.F64));
                    break;
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
                    push(ItemSize.SINGLE, Value.ICONST_0);
                    swap();
                    visitInsn(Opcodes.ISUB);
                    return;
                }
                case Opcodes.LNEG: {
                    Value rhs = pop2();
                    push(ItemSize.DOUBLE, Value.LCONST_0);
                    push(ItemSize.DOUBLE, rhs);
                    visitInsn(Opcodes.LSUB);
                    return;
                }

                case Opcodes.IMUL:
                case Opcodes.IAND:
                case Opcodes.IOR:
                case Opcodes.IXOR:
                case Opcodes.IADD:
                case Opcodes.FMUL:
                case Opcodes.FADD:
                case Opcodes.LMUL:
                case Opcodes.LAND:
                case Opcodes.LOR:
                case Opcodes.LXOR:
                case Opcodes.LADD:
                case Opcodes.DMUL:
                case Opcodes.DADD: {
                    ItemSize itemSize = topOfStackItemSize();
                    CommutativeBinaryValueImpl op = new CommutativeBinaryValueImpl();
                    op.setOwner(currentBlock);
                    op.setKind(CommutativeBinaryValue.Kind.fromOpcode(opcode));
                    op.setLeftInput(popSmart());
                    op.setRightInput(popSmart());
                    push(itemSize, op);
                    return;
                }
                case Opcodes.ISHL:
                case Opcodes.ISHR:
                case Opcodes.IUSHR:
                case Opcodes.ISUB:
                case Opcodes.LSHL:
                case Opcodes.LSHR:
                case Opcodes.LUSHR:
                case Opcodes.LSUB: {
                    ItemSize itemSize = topOfStackItemSize();
                    NonCommutativeBinaryValueImpl op = new NonCommutativeBinaryValueImpl();
                    op.setOwner(currentBlock);
                    op.setKind(NonCommutativeBinaryValue.Kind.fromOpcode(opcode));
                    op.setLeftInput(popSmart());
                    op.setRightInput(popSmart());
                    push(itemSize, op);
                    return;
                }
                case Opcodes.LCMP: {
                    Value v2 = pop2();
                    Value v1 = pop2();
                    NonCommutativeBinaryValueImpl c1 = new NonCommutativeBinaryValueImpl();
                    c1.setOwner(currentBlock);
                    c1.setKind(NonCommutativeBinaryValue.Kind.CMP_LT);
                    c1.setLeftInput(v1);
                    c1.setRightInput(v2);
                    NonCommutativeBinaryValueImpl c2 = new NonCommutativeBinaryValueImpl();
                    c2.setOwner(currentBlock);
                    c2.setKind(NonCommutativeBinaryValue.Kind.CMP_GT);
                    c2.setLeftInput(v1);
                    c2.setRightInput(v2);
                    IfValueImpl op1 = new IfValueImpl();
                    op1.setOwner(currentBlock);
                    op1.setCond(c1);
                    op1.setTrueValue(Value.iconst(-1));
                    IfValueImpl op2 = new IfValueImpl();
                    op2.setOwner(currentBlock);
                    op2.setCond(c2);
                    op2.setTrueValue(Value.iconst(1));
                    op2.setFalseValue(Value.iconst(0));
                    op1.setFalseValue(op2);
                    push(ItemSize.SINGLE, op1);
                    return;
                }

                case Opcodes.FCMPL:
                case Opcodes.DCMPL: {
                    // todo: fix up NaN semantics
                    Value v2 = popSmart();
                    Value v1 = popSmart();
                    NonCommutativeBinaryValueImpl c1 = new NonCommutativeBinaryValueImpl();
                    c1.setOwner(currentBlock);
                    c1.setKind(NonCommutativeBinaryValue.Kind.CMP_LT);
                    c1.setLeftInput(v1);
                    c1.setRightInput(v2);
                    NonCommutativeBinaryValueImpl c2 = new NonCommutativeBinaryValueImpl();
                    c2.setOwner(currentBlock);
                    c2.setKind(NonCommutativeBinaryValue.Kind.CMP_GT);
                    c2.setLeftInput(v1);
                    c2.setRightInput(v2);
                    IfValueImpl op1 = new IfValueImpl();
                    op1.setOwner(currentBlock);
                    op1.setCond(c1);
                    op1.setTrueValue(Value.iconst(-1));
                    IfValueImpl op2 = new IfValueImpl();
                    op2.setOwner(currentBlock);
                    op2.setCond(c2);
                    op2.setTrueValue(Value.iconst(1));
                    op2.setFalseValue(Value.iconst(0));
                    op1.setFalseValue(op2);
                    push(ItemSize.SINGLE, op1);
                    return;
                }
                case Opcodes.FCMPG:
                case Opcodes.DCMPG: {
                    // todo: fix up NaN semantics
                    Value v2 = popSmart();
                    Value v1 = popSmart();
                    NonCommutativeBinaryValueImpl c1 = new NonCommutativeBinaryValueImpl();
                    c1.setOwner(currentBlock);
                    c1.setKind(NonCommutativeBinaryValue.Kind.CMP_LT);
                    c1.setLeftInput(v1);
                    c1.setRightInput(v2);
                    NonCommutativeBinaryValueImpl c2 = new NonCommutativeBinaryValueImpl();
                    c2.setOwner(currentBlock);
                    c2.setKind(NonCommutativeBinaryValue.Kind.CMP_GT);
                    c2.setLeftInput(v1);
                    c2.setRightInput(v2);
                    IfValueImpl op1 = new IfValueImpl();
                    op1.setOwner(currentBlock);
                    op1.setCond(c1);
                    op1.setTrueValue(Value.iconst(-1));
                    IfValueImpl op2 = new IfValueImpl();
                    op2.setOwner(currentBlock);
                    op2.setCond(c2);
                    op2.setTrueValue(Value.iconst(1));
                    op2.setFalseValue(Value.iconst(0));
                    op1.setFalseValue(op2);
                    push(ItemSize.SINGLE, op1);
                    return;
                }

                case Opcodes.ARRAYLENGTH: {
                    Value v = pop();
                    UnaryValueImpl uv = new UnaryValueImpl();
                    uv.setKind(UnaryValue.Kind.LENGTH_OF);
                    uv.setInput(v);
                    push(ItemSize.SINGLE, uv);
                    return;
                }

                case Opcodes.IDIV:
                case Opcodes.IREM: {

                }

                case Opcodes.LDIV:
                case Opcodes.LREM:

                case Opcodes.FNEG:
                case Opcodes.DNEG:
                case Opcodes.ACONST_NULL:
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
                case Opcodes.FSUB:
                case Opcodes.DSUB:
                case Opcodes.FDIV:
                case Opcodes.DDIV:
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
                case Opcodes.ATHROW:
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT: {
                    throw new UnsupportedOperationException();
                }
                case Opcodes.LRETURN:
                case Opcodes.DRETURN: {
                    Value retVal = pop2();
                    ValueReturnImpl insn = new ValueReturnImpl();
                    insn.setMemoryDependency(memoryState);
                    insn.setReturnValue(retVal);
                    BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                    currentBlock.setTerminator(insn);
                    enter(possibleBlockState);
                    return;
                }
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN: {
                    Value retVal = pop();
                    ValueReturnImpl insn = new ValueReturnImpl();
                    insn.setMemoryDependency(memoryState);
                    insn.setReturnValue(retVal);
                    BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                    currentBlock.setTerminator(insn);
                    enter(possibleBlockState);
                    return;
                }
                case Opcodes.RETURN: {
                    ReturnImpl insn = new ReturnImpl();
                    insn.setMemoryDependency(memoryState);
                    BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                    currentBlock.setTerminator(insn);
                    enter(possibleBlockState);
                    return;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }

        public void visitJumpInsn(final int opcode, final Label label) {
            gotInstr = true;
            switch (opcode) {
                case Opcodes.GOTO: {
                    BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                    NodeHandle jumpTarget = getOrMakeBlockHandle(label);
                    GotoImpl goto_ = new GotoImpl();
                    goto_.setTarget(jumpTarget);
                    goto_.setMemoryDependency(memoryState);
                    currentBlock.setTerminator(goto_);
                    enter(possibleBlockState);
                    return;
                }
                case Opcodes.IFEQ:
                case Opcodes.IFNE: {
                    CommutativeBinaryValue op = new CommutativeBinaryValueImpl();
                    op.setOwner(currentBlock);
                    op.setKind(CommutativeBinaryValue.Kind.fromOpcode(opcode));
                    op.setRightInput(Value.ICONST_0);
                    op.setLeftInput(pop());
                    handleIfInsn(op, label);
                    return;
                }
                case Opcodes.IFLT:
                case Opcodes.IFGT:
                case Opcodes.IFLE:
                case Opcodes.IFGE: {
                    NonCommutativeBinaryValue op = new NonCommutativeBinaryValueImpl();
                    op.setOwner(currentBlock);
                    op.setKind(NonCommutativeBinaryValue.Kind.fromOpcode(opcode));
                    op.setRightInput(Value.ICONST_0);
                    op.setLeftInput(pop());
                    handleIfInsn(op, label);
                    return;
                }
                case Opcodes.IF_ICMPEQ:
                case Opcodes.IF_ICMPNE: {
                    CommutativeBinaryValue op = new CommutativeBinaryValueImpl();
                    op.setOwner(currentBlock);
                    op.setKind(CommutativeBinaryValue.Kind.fromOpcode(opcode));
                    op.setRightInput(pop());
                    op.setLeftInput(pop());
                    handleIfInsn(op, label);
                    return;
                }
                case Opcodes.IF_ICMPLE:
                case Opcodes.IF_ICMPLT:
                case Opcodes.IF_ICMPGE:
                case Opcodes.IF_ICMPGT: {
                    NonCommutativeBinaryValue op = new NonCommutativeBinaryValueImpl();
                    op.setOwner(currentBlock);
                    op.setKind(NonCommutativeBinaryValue.Kind.fromOpcode(opcode));
                    op.setRightInput(pop());
                    op.setLeftInput(pop());
                    handleIfInsn(op, label);
                    return;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }

        public void visitIincInsn(final int var, final int increment) {
            gotInstr = true;
            CommutativeBinaryValueImpl op = new CommutativeBinaryValueImpl();
            op.setOwner(currentBlock);
            op.setKind(CommutativeBinaryValue.Kind.ADD);
            op.setLeftInput(getLocal(ItemSize.SINGLE, var));
            op.setRightInput(Value.iconst(increment));
            setLocal(ItemSize.SINGLE, var, op);
        }

        void handleIfInsn(Value cond, Label label) {
            BasicBlock currentBlock = GraphBuilder.this.currentBlock;
            NodeHandle jumpTarget = getOrMakeBlockHandle(label);
            IfImpl if_ = new IfImpl();
            currentBlock.setTerminator(if_);
            if_.setMemoryDependency(memoryState);
            if_.setCondition(cond);
            if_.setFalseBranch(jumpTarget);
            enter(mayNeedFrameState);
            if_.setTrueBranch(futureBlock);
        }

        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            gotInstr = true;
            //  todo
        }

        public void visitIntInsn(final int opcode, final int operand) {
            gotInstr = true;
            switch (opcode) {
                case Opcodes.BIPUSH:
                case Opcodes.SIPUSH: {
                    push(ItemSize.SINGLE, Value.iconst(operand));
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
            gotInstr = true;
            switch (opcode) {
                case Opcodes.ILOAD:
                case Opcodes.ALOAD: {
                    push(ItemSize.SINGLE, getLocal(ItemSize.SINGLE, var));
                    return;
                }
                case Opcodes.DLOAD:
                case Opcodes.LLOAD: {
                    push(ItemSize.DOUBLE, getLocal(ItemSize.DOUBLE, var));
                    return;
                }
                case Opcodes.ISTORE:
                case Opcodes.ASTORE: {
                    setLocal(ItemSize.SINGLE, var, pop());
                    return;
                }
                case Opcodes.DSTORE:
                case Opcodes.LSTORE: {
                    setLocal(ItemSize.DOUBLE, var, pop2());
                    return;
                }
            }
        }

        public void visitLabel(final Label label) {
            if (gotInstr) {
                // treat it like a goto
                BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                GotoImpl goto_ = new GotoImpl();
                currentBlock.setTerminator(goto_);
                goto_.setMemoryDependency(memoryState);
                enter(futureBlockState);
                assert futureBlock != null;
                goto_.setNextBlock(futureBlock);
                outer().visitLabel(label);
            } else {
                NodeHandle handle = allBlocks.get(label);
                if (handle == null) {
                    allBlocks.put(label, NodeHandle.of(currentBlock));
                } else {
                    handle.setTarget(currentBlock);
                }
            }
        }

        void handleExit(final State next) {
            // exit the current block, capturing state
            BasicBlock currentBlock = GraphBuilder.this.currentBlock;
            assert currentBlock != null;
            Capture capture = capture();
            if (blockExits.putIfAbsent(currentBlock, capture) != null) {
                throw new IllegalStateException("Block exited twice");
            }
            GraphBuilder.this.currentBlock = null;
            memoryState = null;
            // the next state decides whether to clear locals/stack or use that info to build the enter state
        }

        void handleEntry(final State previous) {
            // TODO verify this assertion.
            //assert previous == futureBlockState || previous == mayNeedFrameState : "expected previous=futureBlockState or mayNeedFrameState but was " + previous;
            assert currentBlock != null;
            gotInstr = false;
            // the locals/stack are already set up as well
        }

        public void visitFrame(final int type, final int numLocal, final Object[] local, final int numStack, final Object[] stack) {
            // abandon this block, and get a new frame state to use
            enter(futureBlockState);
            outer().visitFrame(type, numLocal, local, numStack, stack);
        }
    }

    abstract class EnterBlockOnInsnState extends State {
        EnterBlockOnInsnState() {
        }

        public void visitInsn(final int opcode) {
            enter(inBlockState);
            outer().visitInsn(opcode);
        }

        public void visitIntInsn(final int opcode, final int operand) {
            enter(inBlockState);
            outer().visitIntInsn(opcode, operand);
        }

        public void visitVarInsn(final int opcode, final int var) {
            enter(inBlockState);
            outer().visitVarInsn(opcode, var);
        }

        public void visitTypeInsn(final int opcode, final String type) {
            enter(inBlockState);
            outer().visitTypeInsn(opcode, type);
        }

        public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
            enter(inBlockState);
            outer().visitFieldInsn(opcode, owner, name, descriptor);
        }

        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            enter(inBlockState);
            outer().visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
            enter(inBlockState);
            outer().visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        public void visitJumpInsn(final int opcode, final Label label) {
            enter(inBlockState);
            outer().visitJumpInsn(opcode, label);
        }

        public void visitLdcInsn(final Object value) {
            enter(inBlockState);
            outer().visitLdcInsn(value);
        }

        public void visitIincInsn(final int var, final int increment) {
            enter(inBlockState);
            outer().visitIincInsn(var, increment);
        }

        public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
            enter(inBlockState);
            outer().visitTableSwitchInsn(min, max, dflt, labels);
        }

        public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
            enter(inBlockState);
            outer().visitLookupSwitchInsn(dflt, keys, labels);
        }

        public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
            enter(inBlockState);
            outer().visitMultiANewArrayInsn(descriptor, numDimensions);
        }

    }

    final class MayNeedFrameState extends EnterBlockOnInsnState {
        MayNeedFrameState() {
        }

        void handleEntry(final State previous) {
            assert previous == inBlockState && futureBlock == null;
            futureBlock = new BasicBlockImpl();
        }

        void handleExit(final State next) {
            if (next == inBlockState) {
                currentBlock = futureBlock;
                futureBlock = null;
                // we never got a frame, so that means we have to set up stack & locals from the current stack & locals
                propagateCurrentStackAndLocals();
            } else {
                assert next == futureBlockState;
                clearLocals();
                clearStack();
            }
        }

        public void visitLabel(final Label label) {
            NodeHandle handle = allBlocks.get(label);
            if (handle == null) {
                allBlocks.put(label, futureBlock.getHandle());
            } else {
                handle.setTarget(futureBlock);
            }
        }

        public void visitFrame(final int type, final int numLocal, final Object[] local, final int numStack, final Object[] stack) {
            clearStack();
            clearLocals();
            enter(futureBlockState);
            outer().visitFrame(type, numLocal, local, numStack, stack);
        }
    }

    final class FutureBlockState extends EnterBlockOnInsnState {
        FutureBlockState() {
        }

        void handleEntry(final State previous) {
            assert previous == inBlockState && futureBlock == null
                || previous == mayNeedFrameState && futureBlock != null
                || previous == possibleBlockState && futureBlock != null;
            if (futureBlock == null) {
                futureBlock = new BasicBlockImpl();
            }
        }

        void handleExit(final State next) {
            assert next == inBlockState;
            currentBlock = futureBlock;
            futureBlock = null;
            // set up the stack & locals from the frame state
            propagateFrameStackAndLocals();
        }

        public void visitLabel(final Label label) {
            NodeHandle handle = allBlocks.get(label);
            if (handle == null) {
                allBlocks.put(label, futureBlock.getHandle());
            } else {
                handle.setTarget(futureBlock);
            }
        }


    }

    final class NoBlock extends State {

        NoBlock() {
        }

        void handleEntry(final State previous) {
            currentBlock = null;
            memoryState = null;
        }

        public void visitLabel(final Label label) {
            // a new block begins
            pendingLabels.add(label);
            enter(possibleBlockState);
        }

        public void visitEnd() {
            // OK
            return;
        }
    }

    final class PossibleBlock extends EnterBlockOnInsnState {
        PossibleBlock() {
        }

        void handleEntry(final State previous) {
            assert previous == inBlockState;
            assert pendingLabels.isEmpty();
        }

        public void visitLabel(final Label label) {
            pendingLabels.add(label);
        }

        public void visitFrame(final int type, final int numLocal, final Object[] local, final int numStack, final Object[] stack) {
            enter(futureBlockState);
            outer().visitFrame(type, numLocal, local, numStack, stack);
        }

        void handleExit(final State next) {
            // a new block begins
            BasicBlockImpl newBlock = new BasicBlockImpl();
            for (Label label : pendingLabels) {
                NodeHandle blockHandle = allBlocks.get(label);
                if (blockHandle == null) {
                    blockHandle = newBlock.getHandle();
                    allBlocks.put(label, blockHandle);
                } else {
                    blockHandle.setTarget(newBlock);
                }
            }
            assert memoryState == null;
            if (next == futureBlockState) {
                futureBlock = newBlock;
                // got a frame directive
                clearStack();
                clearLocals();
            } else {
                assert next == inBlockState;
                // we have to set up the frame manually
                currentBlock = newBlock;
                propagateCurrentStackAndLocals();
            }
            pendingLabels.clear();
        }

        public void visitEnd() {
            // OK
        }
    }

    // Captured state of stack & locals
    static final class Capture {
        final Value[] stack;
        final Value[] locals;
        final ItemSize[] stackMap;
        final ItemSize[] localsMap;

        Capture(final ItemSize[] stackMap, final ItemSize[] localsMap, final Value[] stack, final Value[] locals) {
            this.stackMap = stackMap;
            this.localsMap = localsMap;
            this.stack = stack;
            this.locals = locals;
        }
    }

    enum ItemSize {
        SINGLE,
        DOUBLE,
    }
}
