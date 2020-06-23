package cc.quarkus.qcc.graph;

import static java.lang.Math.*;
import static org.objectweb.asm.Type.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ResolvedMethodDefinition;
import cc.quarkus.qcc.type.definition.ResolvedTypeDefinition;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.descriptor.MethodTypeDescriptor;
import cc.quarkus.qcc.type.universe.Universe;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

/**
 *
 */
public final class GraphBuilder extends MethodVisitor {
    final BasicBlockImpl firstBlock;
    final Universe universe;
    Type[] frameStackTypes;
    int fsp; // points after the last frame stack element
    Type[] frameLocalTypes;
    int flp; // points after the last frame local in use
    Value[] locals;
    int lp; // points to after the last local in use
    Value[] stack;
    int sp; // points after the last stack element
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
    final State possibleBlockState = new PossibleBlock();
    int line;

    public GraphBuilder(final int mods, final String name, final String descriptor, final ResolvedTypeDefinition typeDefinition) {
        super(Universe.ASM_VERSION);
        this.universe = typeDefinition.getDefiningClassLoader();
        boolean isStatic = (mods & Opcodes.ACC_STATIC) != 0;
        org.objectweb.asm.Type[] argTypes = getArgumentTypes(descriptor);
        int localsCount = argTypes.length;
        if (! isStatic) {
            // there's a receiver
            localsCount += 1;
        }
        // first block cannot be entered, so don't bother adding an enter for it
        firstBlock = new BasicBlockImpl();
        // set up the initial stack maps
        int initialLocalsSize = localsCount << 1;
        locals = new Value[initialLocalsSize];
        frameLocalTypes = new Type[16];
        Value thisValue = null;
        Type thisType = typeDefinition.getClassType();
        if (localsCount == 0) {
            originalParams = List.of();
        } else {
            List<ParameterValue> params = Arrays.asList(new ParameterValue[localsCount]);
            int j = 0;
            if (! isStatic) {
                // "this" receiver - todo: ThisValue
                ParameterValue pv = new ParameterValueImpl();
                pv.setOwner(firstBlock);
                pv.setType(thisType);
                thisValue = pv;
                pv.setIndex(j);
                setLocal(j, pv);
                addFrameLocal(pv.getType());
                j++;
            }
            for (int i = 0; i < argTypes.length; i ++) {
                ParameterValue pv = new ParameterValueImpl();
                pv.setOwner(firstBlock);
                pv.setType(typeOfAsmType(argTypes[i]));
                pv.setIndex(j);
                if (argTypes[i] == LONG_TYPE || argTypes[i] == DOUBLE_TYPE) {
                    setLocal(j, pv);
                    j+= 2;
                } else {
                    setLocal(j, pv);
                    j++;
                }
                addFrameLocal(pv.getType());
                params.set(i, pv);
            }
            originalParams = params;
        }
        this.thisValue = thisValue;
        flp = lp;
        frameStackTypes = new Type[16];
        stack = new Value[16];
        sp = 0;
        fsp = 0;
    }

    private Type typeOfAsmType(final org.objectweb.asm.Type asmType) {
        switch (asmType.getSort()) {
            case org.objectweb.asm.Type.BOOLEAN: return Type.BOOL;
            case org.objectweb.asm.Type.BYTE: return Type.S8;
            case org.objectweb.asm.Type.SHORT: return Type.S16;
            case org.objectweb.asm.Type.INT: return Type.S32;
            case org.objectweb.asm.Type.LONG: return Type.S64;
            case org.objectweb.asm.Type.CHAR: return Type.U16;
            case org.objectweb.asm.Type.FLOAT: return Type.F32;
            case org.objectweb.asm.Type.DOUBLE: return Type.F64;
            case org.objectweb.asm.Type.VOID: /* TODO void is not a type */ return Type.VOID;
            case org.objectweb.asm.Type.ARRAY: return Type.arrayOf(typeOfAsmType(asmType.getElementType())); // todo cache
            case org.objectweb.asm.Type.OBJECT: return universe.findClass(asmType.getInternalName()).verify().getClassType();
            default: throw new IllegalStateException();
        }
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

    Type topOfStackType() {
        return peek().getType();
    }

    void clearStack() {
        Arrays.fill(stack, 0, sp, null);
        sp = 0;
    }

    Value popSmart() {
        return pop(topOfStackType().isClass2Type());
    }

    Value pop(boolean wide) {
        int tos = sp - 1;
        Value value = stack[tos];
        Type type = value.getType();
        if (type.isClass2Type() != wide) {
            throw new IllegalStateException("Bad pop");
        }
        stack[tos] = null;
        sp = tos;
        return value;
    }

    Value pop2() {
        int tos = sp - 1;
        Type type = topOfStackType();
        Value value;
        if (type.isClass2Type()) {
            value = stack[tos];
            stack[tos] = null;
            sp = tos;
        } else {
            value = stack[tos];
            stack[tos] = null;
            stack[tos - 1] = null;
            sp = tos - 1;
        }
        return value;
    }

    Value pop() {
        int tos = sp - 1;
        Type type = topOfStackType();
        Value value;
        if (type.isClass2Type()) {
            throw new IllegalStateException("Bad pop");
        }
        value = stack[tos];
        stack[tos] = null;
        sp = tos;
        return value;
    }

    void ensureStackSize(int size) {
        int len = stack.length;
        if (len < size) {
            stack = Arrays.copyOf(stack, len << 1);
        }
    }

    void dup() {
        Type type = topOfStackType();
        if (type.isClass2Type()) {
            throw new IllegalStateException("Bad dup");
        }
        push(peek());
    }

    void dup2() {
        Type type = topOfStackType();
        if (type.isClass2Type()) {
            push(peek());
        } else {
            Value v2 = pop();
            Value v1 = pop();
            push(v1);
            push(v2);
            push(v1);
            push(v2);
        }
    }

    void swap() {
        Type type = topOfStackType();
        if (type.isClass2Type()) {
            throw new IllegalStateException("Bad swap");
        }
        Value v2 = pop();
        Value v1 = pop();
        push(v2);
        push(v1);
    }

    void push(Value value) {
        int sp = this.sp;
        ensureStackSize(sp + 1);
        stack[sp] = value;
        this.sp = sp + 1;
    }

    Value peek() {
        return stack[sp - 1];
    }

    // Locals manipulation

    void ensureLocalSize(int size) {
        int len = locals.length;
        if (len < size) {
            locals = Arrays.copyOf(locals, len << 1);
        }
    }

    void clearLocals() {
        Arrays.fill(locals, 0, lp, null);
        lp = 0;
    }

    void setLocal(int index, Value value) {
        if (value.getType().isClass2Type()) {
            ensureLocalSize(index + 2);
            locals[index + 1] = null;
            lp = max(index + 2, lp);
        } else {
            ensureLocalSize(index + 1);
            lp = max(index + 1, lp);
        }
        locals[index] = value;
    }

    Value getLocal(int index) {
        if (index > lp) {
            throw new IllegalStateException("Invalid local index");
        }
        Value value = locals[index];
        if (value == null) {
            throw new IllegalStateException("Invalid get local (no value)");
        }
        return value;
    }

    // Frame stack manipulation

    void ensureFrameStackSize(int size) {
        int len = frameStackTypes.length;
        if (len < size) {
            frameStackTypes = Arrays.copyOf(frameStackTypes, len << 1);
        }
    }

    void clearFrameStack() {
        Arrays.fill(frameStackTypes, 0, fsp, null);
        fsp = 0;
    }

    void addFrameStackItem(Type type) {
        int fsp = this.fsp;
        ensureFrameStackSize(fsp + 1);
        frameStackTypes[fsp] = type;
        this.fsp = fsp + 1;
    }

    // Frame locals manipulation

    void ensureFrameLocalSize(int size) {
        int len = frameLocalTypes.length;
        if (len < size) {
            frameLocalTypes = Arrays.copyOf(frameLocalTypes, len << 1);
        }
    }

    void addFrameLocal(Type type) {
        int flp = this.flp;
        ensureFrameLocalSize(flp + 1);
        frameLocalTypes[flp] = type;
        this.flp = flp + 1;
    }

    Type removeFrameLocal() {
        int flp = this.flp;
        Type old = frameLocalTypes[flp - 1];
        frameLocalTypes[flp - 1] = null;
        this.flp = flp - 1;
        return old;
    }

    void clearFrameLocals() {
        Arrays.fill(frameLocalTypes, 0, flp, null);
        flp = 0;
    }

    // Capture

    Capture capture() {
        Value[] captureStack = Arrays.copyOf(stack, sp);
        Value[] captureLocals = Arrays.copyOf(locals, lp);
        return new Capture(captureStack, captureLocals);
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
        // todo: need to efficiently map label to line and ensure things happen in the correct order
        this.line = line;
    }

    Type typeOfFrameObject(Object o) {
        if (o == Opcodes.INTEGER) {
            return Type.S32;
        } else if (o == Opcodes.LONG) {
            return Type.S64;
        } else if (o == Opcodes.FLOAT) {
            return Type.F32;
        } else if (o == Opcodes.DOUBLE) {
            return Type.F64;
        } else if (o == Opcodes.NULL) {
            // todo
            throw new IllegalStateException();
        } else if (o == Opcodes.UNINITIALIZED_THIS) {
            // todo: self type
            throw new IllegalStateException();
        } else if (o instanceof Label) {
            // todo: map labelled new instructions to their output types
            throw new IllegalStateException();
        } else if (o instanceof String) {
            // regular reference type
            return universe.findClass((String) o).verify().getClassType();
        } else {
            throw new IllegalStateException();
        }
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
                addFrameStackItem(typeOfFrameObject(stack[0]));
                return;
            }
            case Opcodes.F_APPEND: {
                clearStack();
                for (int i = 0; i < numLocal; i++) {
                    addFrameLocal(typeOfFrameObject(local[i]));
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
                    addFrameLocal(typeOfFrameObject(local[i]));
                }
                for (int i = 0; i < numStack; i++) {
                    addFrameStackItem(typeOfFrameObject(stack[i]));
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
            PhiValue value = (PhiValue) enterState.stack[i];
            value.setValueForBlock(exitingBlock, exitState.stack[i]);
        }
        // now locals
        int localSize = enterState.locals.length;
        if (exitState.locals.length < localSize) {
            throw new IllegalStateException("Local vars mismatch");
        }
        for (int i = 0; i < localSize; i ++) {
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
            PhiValueImpl pv = new PhiValueImpl();
            pv.setType(stack[i].getType());
            pv.setOwner(currentBlock);
            stack[i] = pv;
        }
        for (int i = 0; i < lp; i ++) {
            if (locals[i] != null) {
                PhiValueImpl pv = new PhiValueImpl();
                pv.setType(locals[i].getType());
                pv.setOwner(currentBlock);
                locals[i] = pv;
            }
        }
        blockEnters.putIfAbsent(currentBlock, capture());
    }

    void propagateFrameStackAndLocals() {
        assert futureBlock == null && currentBlock != null;
        for (int i = 0; i < fsp; i ++) {
            Type type = frameStackTypes[i];
            PhiValueImpl pv = new PhiValueImpl();
            pv.setType(type);
            pv.setOwner(currentBlock);
            push(pv);
        }
        for (int i = 0; i < flp; i ++) {
            Type type = frameLocalTypes[i];
            if (type != null) {
                PhiValueImpl pv = new PhiValueImpl();
                pv.setType(type);
                pv.setOwner(currentBlock);
                setLocal(i, pv);
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
            switch (opcode) {
                case Opcodes.NEW:
                case Opcodes.ANEWARRAY:
                case Opcodes.CHECKCAST:
                case Opcodes.INSTANCEOF:
                default: {
                    super.visitTypeInsn(opcode, type);
                }
            }
        }

        public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
            gotInstr = true;
            switch (opcode) {
                case Opcodes.GETSTATIC: {
                    FieldReadValue read = new StaticFieldReadValueImpl();
                    read.setMemoryDependency(memoryState);
                    read.setSourceLine(line);
                    // todo: set descriptor
                    memoryState = read;
                    // todo: size from field
                    push(read);
                    break;
                }
                case Opcodes.GETFIELD: {
                    InstanceFieldReadValue read = new InstanceFieldReadValueImpl();
                    read.setInstance(pop());
                    read.setMemoryDependency(memoryState);
                    read.setSourceLine(line);
                    // todo: set descriptor
                    memoryState = read;
                    // todo: size from field
                    push(read);
                    break;
                }
                case Opcodes.PUTSTATIC: {
                    // todo: get pop type from field descriptor
                    Value value = popSmart();
                    FieldWrite write = new StaticFieldWriteImpl();
                    write.setWriteValue(value);
                    write.setMemoryDependency(memoryState);
                    write.setSourceLine(line);
                    memoryState = write;
                    break;
                }
                case Opcodes.PUTFIELD: {
                    Value value = popSmart();
                    InstanceFieldWrite write = new InstanceFieldWriteImpl();
                    write.setWriteValue(value);
                    write.setMemoryDependency(memoryState);
                    write.setSourceLine(line);
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
                push(Value.const_(((Integer) value).intValue()));
            } else if (value instanceof Long) {
                push(Value.const_(((Long) value).longValue()));
            } else if (value instanceof Float) {
                push(Value.const_(((Float) value).floatValue()));
            } else if (value instanceof Double) {
                push(Value.const_(((Double) value).doubleValue()));
            } else if (value instanceof String) {
                push(Value.const_((String) value));
            } else {
                throw new IllegalStateException();
            }
        }

        public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
            gotInstr = true;
            BasicBlock currentBlock = GraphBuilder.this.currentBlock;
            SwitchImpl switch_ = new SwitchImpl();
            switch_.setSourceLine(line);
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
            switch_.setSourceLine(line);
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
                    push(Value.ICONST_0);
                    return;
                }
                case Opcodes.ICONST_1:
                case Opcodes.ICONST_2:
                case Opcodes.ICONST_3:
                case Opcodes.ICONST_4:
                case Opcodes.ICONST_5: {
                    push(Value.const_(opcode - Opcodes.ICONST_0));
                    return;
                }
                case Opcodes.ICONST_M1: {
                    push(Value.const_(-1));
                    return;
                }
                case Opcodes.LCONST_0:
                case Opcodes.LCONST_1: {
                    push(Value.const_(opcode - Opcodes.LCONST_0));
                    break;
                }
                case Opcodes.FCONST_0:
                case Opcodes.FCONST_1:
                case Opcodes.FCONST_2: {
                    // todo: cache
                    push(CastValue.create(Value.const_(opcode - Opcodes.FCONST_0), Type.F32));
                    break;
                }
                case Opcodes.DCONST_0:
                case Opcodes.DCONST_1: {
                    // todo: cache
                    push(CastValue.create(Value.const_(opcode - Opcodes.DCONST_0), Type.F64));
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
                case Opcodes.DUP_X1: {
                    Value v1 = pop();
                    Value v2 = pop();
                    push(v1);
                    push(v2);
                    push(v1);
                    return;
                }
                case Opcodes.DUP_X2: {
                    Value v1 = pop();
                    Value v2 = pop();
                    Value v3 = pop();
                    push(v1);
                    push(v3);
                    push(v2);
                    push(v1);
                    return;
                }
                case Opcodes.DUP2_X1: {
                    if (! topOfStackType().isClass2Type()) {
                        // form 1
                        Value v1 = pop();
                        Value v2 = pop();
                        Value v3 = pop();
                        push(v2);
                        push(v1);
                        push(v3);
                        push(v2);
                        push(v1);
                    } else {
                        // form 2
                        Value v1 = pop2();
                        Value v2 = pop2();
                        push(v1);
                        push(v2);
                        push(v1);
                    }
                    return;
                }
                case Opcodes.DUP2_X2: {
                    if (! topOfStackType().isClass2Type()) {
                        Value v1 = pop();
                        Value v2 = pop();
                        Value v3;
                        if (! topOfStackType().isClass2Type()) {
                            // form 1
                            v3 = pop();
                            Value v4 = pop();
                            push(v2);
                            push(v1);
                            push(v4);
                        } else {
                            // form 3
                            v3 = pop2();
                            push(v2);
                            push(v1);
                        }
                        // form 1 or 3
                        push(v3);
                        push(v2);
                        push(v1);
                    } else {
                        Value v1 = pop2();
                        Value v2;
                        if (! topOfStackType().isClass2Type()) {
                            // form 2
                            v2 = pop();
                            Value v3 = pop();
                            push(v1);
                            push(v2);
                            push(v3);
                        } else {
                            // form 4
                            v2 = pop2();
                            push(v1);
                            push(v2);
                        }
                        // form 2 or 4
                        push(v1);
                    }
                    return;
                }

                case Opcodes.INEG: {
                    push(Value.ICONST_0);
                    swap();
                    visitInsn(Opcodes.ISUB);
                    return;
                }
                case Opcodes.LNEG: {
                    Value rhs = pop2();
                    push(Value.LCONST_0);
                    push(rhs);
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
                    CommutativeBinaryValueImpl op = new CommutativeBinaryValueImpl();
                    op.setSourceLine(line);
                    op.setOwner(currentBlock);
                    op.setKind(CommutativeBinaryValue.Kind.fromOpcode(opcode));
                    op.setLeftInput(popSmart());
                    op.setRightInput(popSmart());
                    push(op);
                    return;
                }
                case Opcodes.FSUB:
                case Opcodes.DSUB:
                case Opcodes.ISHL:
                case Opcodes.ISHR:
                case Opcodes.IUSHR:
                case Opcodes.ISUB:
                case Opcodes.LSHL:
                case Opcodes.LSHR:
                case Opcodes.LUSHR:
                case Opcodes.LSUB: {
                    NonCommutativeBinaryValueImpl op = new NonCommutativeBinaryValueImpl();
                    op.setSourceLine(line);
                    op.setOwner(currentBlock);
                    op.setKind(NonCommutativeBinaryValue.Kind.fromOpcode(opcode));
                    op.setLeftInput(popSmart());
                    op.setRightInput(popSmart());
                    push(op);
                    return;
                }
                case Opcodes.LCMP: {
                    Value v2 = pop2();
                    Value v1 = pop2();
                    NonCommutativeBinaryValueImpl c1 = new NonCommutativeBinaryValueImpl();
                    c1.setSourceLine(line);
                    c1.setOwner(currentBlock);
                    c1.setKind(NonCommutativeBinaryValue.Kind.CMP_LT);
                    c1.setLeftInput(v1);
                    c1.setRightInput(v2);
                    NonCommutativeBinaryValueImpl c2 = new NonCommutativeBinaryValueImpl();
                    c2.setSourceLine(line);
                    c2.setOwner(currentBlock);
                    c2.setKind(NonCommutativeBinaryValue.Kind.CMP_GT);
                    c2.setLeftInput(v1);
                    c2.setRightInput(v2);
                    IfValueImpl op1 = new IfValueImpl();
                    op1.setSourceLine(line);
                    op1.setOwner(currentBlock);
                    op1.setCond(c1);
                    op1.setTrueValue(Value.const_(-1));
                    IfValueImpl op2 = new IfValueImpl();
                    op2.setSourceLine(line);
                    op2.setOwner(currentBlock);
                    op2.setCond(c2);
                    op2.setTrueValue(Value.const_(1));
                    op2.setFalseValue(Value.const_(0));
                    op1.setFalseValue(op2);
                    push(op1);
                    return;
                }

                case Opcodes.FCMPL:
                case Opcodes.DCMPL: {
                    // todo: fix up NaN semantics
                    Value v2 = popSmart();
                    Value v1 = popSmart();
                    NonCommutativeBinaryValueImpl c1 = new NonCommutativeBinaryValueImpl();
                    c1.setSourceLine(line);
                    c1.setOwner(currentBlock);
                    c1.setKind(NonCommutativeBinaryValue.Kind.CMP_LT);
                    c1.setLeftInput(v1);
                    c1.setRightInput(v2);
                    NonCommutativeBinaryValueImpl c2 = new NonCommutativeBinaryValueImpl();
                    c2.setSourceLine(line);
                    c2.setOwner(currentBlock);
                    c2.setKind(NonCommutativeBinaryValue.Kind.CMP_GT);
                    c2.setLeftInput(v1);
                    c2.setRightInput(v2);
                    IfValueImpl op1 = new IfValueImpl();
                    op1.setSourceLine(line);
                    op1.setOwner(currentBlock);
                    op1.setCond(c1);
                    op1.setTrueValue(Value.const_(-1));
                    IfValueImpl op2 = new IfValueImpl();
                    op2.setSourceLine(line);
                    op2.setOwner(currentBlock);
                    op2.setCond(c2);
                    op2.setTrueValue(Value.const_(1));
                    op2.setFalseValue(Value.const_(0));
                    op1.setFalseValue(op2);
                    push(op1);
                    return;
                }
                case Opcodes.FCMPG:
                case Opcodes.DCMPG: {
                    // todo: fix up NaN semantics
                    Value v2 = popSmart();
                    Value v1 = popSmart();
                    NonCommutativeBinaryValueImpl c1 = new NonCommutativeBinaryValueImpl();
                    c1.setSourceLine(line);
                    c1.setOwner(currentBlock);
                    c1.setKind(NonCommutativeBinaryValue.Kind.CMP_LT);
                    c1.setLeftInput(v1);
                    c1.setRightInput(v2);
                    NonCommutativeBinaryValueImpl c2 = new NonCommutativeBinaryValueImpl();
                    c2.setSourceLine(line);
                    c2.setOwner(currentBlock);
                    c2.setKind(NonCommutativeBinaryValue.Kind.CMP_GT);
                    c2.setLeftInput(v1);
                    c2.setRightInput(v2);
                    IfValueImpl op1 = new IfValueImpl();
                    op1.setSourceLine(line);
                    op1.setOwner(currentBlock);
                    op1.setCond(c1);
                    op1.setTrueValue(Value.const_(-1));
                    IfValueImpl op2 = new IfValueImpl();
                    op2.setSourceLine(line);
                    op2.setOwner(currentBlock);
                    op2.setCond(c2);
                    op2.setTrueValue(Value.const_(1));
                    op2.setFalseValue(Value.const_(0));
                    op1.setFalseValue(op2);
                    push(op1);
                    return;
                }

                case Opcodes.ARRAYLENGTH: {
                    Value v = pop();
                    UnaryValueImpl uv = new UnaryValueImpl();
                    uv.setSourceLine(line);
                    uv.setKind(UnaryValue.Kind.LENGTH_OF);
                    uv.setInput(v);
                    push(uv);
                    return;
                }
                case Opcodes.FNEG:
                case Opcodes.DNEG: {
                    Value v = popSmart();
                    UnaryValueImpl uv = new UnaryValueImpl();
                    uv.setSourceLine(line);
                    uv.setKind(UnaryValue.Kind.NEGATE);
                    uv.setInput(v);
                    push(uv);
                    return;
                }

                case Opcodes.I2L: {
                    push(WordCastValue.create(pop(), WordCastValue.Kind.SIGN_EXTEND, Type.S64, line));
                    return;
                }
                case Opcodes.L2I: {
                    push(WordCastValue.create(pop2(), WordCastValue.Kind.TRUNCATE, Type.S32, line));
                    return;
                }
                case Opcodes.I2B: {
                    push(WordCastValue.create(pop(), WordCastValue.Kind.TRUNCATE, Type.S8, line));
                    return;
                }
                case Opcodes.I2C: {
                    push(WordCastValue.create(pop(), WordCastValue.Kind.TRUNCATE, Type.U16, line));
                    return;
                }
                case Opcodes.I2S: {
                    push(WordCastValue.create(pop(), WordCastValue.Kind.TRUNCATE, Type.S16, line));
                    return;
                }
                case Opcodes.I2F: {
                    push(WordCastValue.create(pop(), WordCastValue.Kind.VALUE_CONVERT, Type.F32, line));
                    return;
                }
                case Opcodes.I2D:
                case Opcodes.F2D: {
                    push(WordCastValue.create(pop(), WordCastValue.Kind.VALUE_CONVERT, Type.F64, line));
                    return;
                }
                case Opcodes.L2F:
                case Opcodes.D2F: {
                    push(WordCastValue.create(pop2(), WordCastValue.Kind.VALUE_CONVERT, Type.F32, line));
                    return;
                }
                case Opcodes.L2D: {
                    push(WordCastValue.create(pop2(), WordCastValue.Kind.VALUE_CONVERT, Type.F64, line));
                    return;
                }
                case Opcodes.F2I: {
                    push(WordCastValue.create(pop(), WordCastValue.Kind.VALUE_CONVERT, Type.S32, line));
                    return;
                }
                case Opcodes.F2L: {
                    push(WordCastValue.create(pop(), WordCastValue.Kind.VALUE_CONVERT, Type.S64, line));
                    return;
                }
                case Opcodes.D2I: {
                    push(WordCastValue.create(pop2(), WordCastValue.Kind.VALUE_CONVERT, Type.S32, line));
                    return;
                }
                case Opcodes.D2L: {
                    push(WordCastValue.create(pop2(), WordCastValue.Kind.VALUE_CONVERT, Type.S64, line));
                    return;
                }

                case Opcodes.IDIV:
                case Opcodes.IREM: {

                }

                case Opcodes.LDIV:
                case Opcodes.LREM:

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
                case Opcodes.FDIV:
                case Opcodes.DDIV:
                case Opcodes.FREM:
                case Opcodes.DREM:
                case Opcodes.ATHROW:
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT: {
                    throw new UnsupportedOperationException();
                }
                case Opcodes.LRETURN:
                case Opcodes.DRETURN: {
                    Value retVal = pop2();
                    ValueReturnImpl insn = new ValueReturnImpl();
                    insn.setSourceLine(line);
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
                    insn.setSourceLine(line);
                    insn.setMemoryDependency(memoryState);
                    insn.setReturnValue(retVal);
                    BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                    currentBlock.setTerminator(insn);
                    enter(possibleBlockState);
                    return;
                }
                case Opcodes.RETURN: {
                    ReturnImpl insn = new ReturnImpl();
                    insn.setSourceLine(line);
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
                    goto_.setSourceLine(line);
                    goto_.setTarget(jumpTarget);
                    goto_.setMemoryDependency(memoryState);
                    currentBlock.setTerminator(goto_);
                    enter(possibleBlockState);
                    return;
                }
                case Opcodes.IFEQ:
                case Opcodes.IFNE: {
                    CommutativeBinaryValue op = new CommutativeBinaryValueImpl();
                    op.setSourceLine(line);
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
                    op.setSourceLine(line);
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
                    op.setSourceLine(line);
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
                    op.setSourceLine(line);
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
            op.setSourceLine(line);
            op.setOwner(currentBlock);
            op.setKind(CommutativeBinaryValue.Kind.ADD);
            op.setLeftInput(getLocal(var));
            op.setRightInput(Value.const_(increment));
            setLocal(var, op);
        }

        void handleIfInsn(Value cond, Label label) {
            BasicBlock currentBlock = GraphBuilder.this.currentBlock;
            NodeHandle jumpTarget = getOrMakeBlockHandle(label);
            IfImpl if_ = new IfImpl();
            if_.setSourceLine(line);
            currentBlock.setTerminator(if_);
            if_.setMemoryDependency(memoryState);
            if_.setCondition(cond);
            if_.setFalseBranch(jumpTarget);
            enter(futureBlockState);
            if_.setTrueBranch(futureBlock);
        }

        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            gotInstr = true;
            DefinedTypeDefinition def = universe.findClass(owner);
            org.objectweb.asm.Type[] types = getArgumentTypes(descriptor);
            Type[] actualTypes;
            int i;
            int length = types.length;
            int actualLength;
            if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEINTERFACE) {
                actualLength = length + 1;
                // add receiver
                actualTypes = new Type[actualLength];
                actualTypes[0] = def.verify().getClassType();
                i = 1;
            } else {
                actualLength = length;
                actualTypes = new Type[actualLength];
                i = 0;
            }
            for (int j = 0; j < length; j++, i++) {
                actualTypes[i] = typeOfAsmType(types[j]);
            }
            ResolvedMethodDefinition methodDef;
            Type returnType = typeOfAsmType(getReturnType(descriptor));
            MethodIdentifier identifier = MethodIdentifier.of(name, MethodTypeDescriptor.of(returnType, actualTypes));
            if (isInterface) {
                methodDef = def.verify().resolve().resolveMethod(identifier);
            } else {
                methodDef = def.verify().resolve().resolveInterfaceMethod(identifier);
            }
            Invocation val;
            if (returnType == Type.VOID) {
                if (false /* try in progress */) {
                    val = new TryInvocationImpl();
                } else {
                    // no try
                    val = new InvocationImpl();
                }
            } else {
                if (false /* try in progress */) {
                    val = new TryInvocationValueImpl();
                } else {
                    // no try
                    val = new InvocationValueImpl();
                }
            }
            val.setMethodOwner(def.verify().getClassType());
            // use the resolved method identifier
            val.setInvocationTarget(methodDef.getMethodIdentifier());
            val.setSourceLine(line);
            val.setOwner(currentBlock);
            val.setArgumentCount(actualLength);
            for (int k = actualLength; k > 0; k --) {
                val.setArgument(k - 1, popSmart());
            }
            val.setMemoryDependency(memoryState);
            if (val instanceof Value) {
                push((Value) val);
            }
            memoryState = val;
            if (val instanceof Terminator) {
                Terminator terminator = (Terminator) val;
                currentBlock.setTerminator(terminator);
                enter(futureBlockState);
                ((Goto) val).setNextBlock(futureBlock);
            }
        }

        public void visitIntInsn(final int opcode, final int operand) {
            gotInstr = true;
            switch (opcode) {
                case Opcodes.BIPUSH:
                case Opcodes.SIPUSH: {
                    push(Value.const_(operand));
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
                case Opcodes.ALOAD:
                case Opcodes.DLOAD:
                case Opcodes.LLOAD: {
                    push(getLocal(var));
                    return;
                }
                case Opcodes.ISTORE:
                case Opcodes.ASTORE: {
                    setLocal(var, pop());
                    return;
                }
                case Opcodes.DSTORE:
                case Opcodes.LSTORE: {
                    setLocal(var, pop2());
                    return;
                }
            }
        }

        public void visitLabel(final Label label) {
            if (gotInstr) {
                // treat it like a goto
                BasicBlock currentBlock = GraphBuilder.this.currentBlock;
                GotoImpl goto_ = new GotoImpl();
                goto_.setSourceLine(line);
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
            assert currentBlock != null;
            if (next == possibleBlockState) {
                // exiting the block with no subsequent instruction
                Capture capture = capture();
                if (blockExits.putIfAbsent(currentBlock, capture) != null) {
                    throw new IllegalStateException("Block exited twice");
                }
                currentBlock = null;
            }
            memoryState = null;
            // the next state decides whether to clear locals/stack or use that info to build the enter state
        }

        void handleEntry(final State previous) {
            assert currentBlock != null;
            gotInstr = false;
            // the locals/stack are already set up as well
        }

        public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
            super.visitTryCatchBlock(start, end, handler, type);
        }

        public AnnotationVisitor visitTryCatchAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
            return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
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

    /**
     * In this state the control flow has come from a previous instruction, and the new block might
     * also be a jump target.
     */
    final class FutureBlockState extends EnterBlockOnInsnState {
        FutureBlockState() {
        }

        void handleEntry(final State previous) {
            assert previous == inBlockState && currentBlock != null;
            if (futureBlock == null) {
                futureBlock = new BasicBlockImpl();
            }
        }

        void handleExit(final State next) {
            assert next == inBlockState;
            // capture exit state
            Capture capture = capture();
            if (blockExits.putIfAbsent(currentBlock, capture) != null) {
                throw new IllegalStateException("Block exited twice");
            }
            currentBlock = futureBlock;
            futureBlock = null;
            propagateCurrentStackAndLocals();
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

    final class PossibleBlock extends EnterBlockOnInsnState {
        PossibleBlock() {
        }

        void handleEntry(final State previous) {
            assert previous == inBlockState;
            assert pendingLabels.isEmpty();
            clearStack();
            clearLocals();
        }

        public void visitLabel(final Label label) {
            pendingLabels.add(label);
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
            assert next == inBlockState;
            currentBlock = newBlock;
            // we have to set up the locals from the frame
            propagateFrameStackAndLocals();
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

        Capture(final Value[] stack, final Value[] locals) {
            this.stack = stack;
            this.locals = locals;
        }
    }
}
