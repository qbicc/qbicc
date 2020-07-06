package cc.quarkus.qcc.graph;

import static java.lang.Math.*;
import static org.objectweb.asm.Type.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

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
public final class BytecodeParser extends MethodVisitor {
    final LineNumberGraphFactory graphFactory;
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
    Map<Label, List<TryState>> tryStarts = new HashMap<>();
    Map<Label, List<TryState>> tryStops = new HashMap<>();
    NavigableSet<TryState> activeTry = new TreeSet<>();
    Map<NavigableSet<TryState>, CatchInfo> catchBlocks = new HashMap<>();
    int catchIndex = 0;

    public BytecodeParser(final int mods, final String name, final String descriptor, final ResolvedTypeDefinition typeDefinition) {
        super(Universe.ASM_VERSION);
        graphFactory = new LineNumberGraphFactory(GraphFactory.BASIC_FACTORY);
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
                pv.setType(thisType);
                thisValue = pv;
                pv.setIndex(j);
                setLocal(j, pv);
                addFrameLocal(pv.getType());
                j++;
            }
            for (int i = 0; i < argTypes.length; i ++) {
                ParameterValue pv = new ParameterValueImpl();
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

    public void visitLocalVariable(final String name, final String descriptor, final String signature, final Label start, final Label end, final int index) {
    }

    public void visitLineNumber(final int line, final Label start) {
        // todo: need to efficiently map label to line and ensure things happen in the correct order
        graphFactory.setLineNumber(line);
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


    interface TryState extends Comparable<TryState> {
        ClassType getExceptionType();

        NodeHandle getCatchHandler();

        int getIndex();

        void enter();

        void exit();
    }

    public void visitLabel(final Label label) {
        List<TryState> states = tryStarts.remove(label);
        if (states != null) {
            for (TryState state : states) {
                state.enter();
            }
        }
        states = tryStops.remove(label);
        if (states != null) {
            for (TryState state : states) {
                state.exit();
            }
        }
        super.visitLabel(label);
    }

    public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
        NodeHandle catchHandle = getOrMakeBlockHandle(handler);
        DefinedTypeDefinition exceptionTypeClass = type == null ? universe.findClass("java/lang/Throwable") : universe.findClass(type);
        // todo: verifier: ensure that exceptionTypeClass extends Throwable
        ClassType exceptionType = exceptionTypeClass.verify().getClassType();
        final int index = catchIndex ++;
        tryStarts.computeIfAbsent(start, x -> new ArrayList<>()).add(new TryState() {
            public ClassType getExceptionType() {
                return exceptionType;
            }

            public NodeHandle getCatchHandler() {
                return catchHandle;
            }

            public int compareTo(final TryState o) {
                return Integer.compare(getIndex(), o.getIndex());
            }

            public int getIndex() {
                return index;
            }

            public void enter() {
                tryStops.computeIfAbsent(end, x -> new ArrayList<>()).add(this);
                activeTry.add(this);
            }

            public void exit() {
                activeTry.remove(this);
            }
        });
    }

    BasicBlock getCatchHandler() {
        return getCatchHandler(activeTry);
    }

    BasicBlock getCatchHandler(NavigableSet<TryState> activeTrySet) {
        CatchInfo catchInfo = catchBlocks.get(activeTrySet);
        if (catchInfo == null) {
            // build catch block
            Capture capture = capture();
            BasicBlock catch_ = new BasicBlockImpl();
            PhiValue exceptionPhi = graphFactory.phi(universe.findClass("java/lang/Throwable").verify().getClassType(), currentBlock);
            int localCnt = capture.locals.length;
            Value[] phiLocals = new Value[localCnt];
            for (int i = 0; i < localCnt; i ++) {
                phiLocals[i] = graphFactory.phi(capture.locals[i].getType(), currentBlock);
            }
            capture = new Capture(new Value[] { exceptionPhi }, phiLocals);
            // we don't change any state
            blockEnters.put(catch_, capture);
            blockExits.put(catch_, capture);
            if (activeTrySet.isEmpty()) {
                // rethrow
                catch_.setTerminator(Throw.create(exceptionPhi));
            } else {
                TryState first = activeTrySet.first();
                // check one and continue
                Terminator if_ = graphFactory.if_(memoryState,
                    graphFactory.instanceOf(exceptionPhi, first.getExceptionType()),
                    first.getCatchHandler(),
                    NodeHandle.of(getCatchHandler(activeTrySet.tailSet(first, false)))
                );
                catch_.setTerminator(if_);
            }
            catchInfo = new CatchInfo(catch_, exceptionPhi);
            catchBlocks.put(activeTrySet, catchInfo);
        }
        return catchInfo.target;
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
            }
            if (ti instanceof If) {
                If ifTi = (If) ti;
                wirePhis(exitingBlock, ifTi.getTrueBranch());
                wirePhis(exitingBlock, ifTi.getFalseBranch());
            }
            if (ti instanceof Try) {
                Try try_ = (Try) ti;
                wirePhis(exitingBlock, try_.getCatchHandler());
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
            stack[i] = graphFactory.phi(stack[i].getType(), currentBlock);
        }
        for (int i = 0; i < lp; i ++) {
            if (locals[i] != null) {
                locals[i] = graphFactory.phi(locals[i].getType(), currentBlock);
            }
        }
        blockEnters.putIfAbsent(currentBlock, capture());
    }

    void propagateFrameStackAndLocals() {
        assert futureBlock == null && currentBlock != null;
        for (int i = 0; i < fsp; i ++) {
            Type type = frameStackTypes[i];
            push(graphFactory.phi(type, currentBlock));
        }
        for (int i = 0; i < flp; i ++) {
            Type type = frameLocalTypes[i];
            if (type != null) {
                setLocal(i, graphFactory.phi(type, currentBlock));
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
            ClassType classType = (ClassType) universe.parseSingleDescriptor(type);
            switch (opcode) {
                case Opcodes.INSTANCEOF: {
                    Value value = pop(false);
                    Value isNull = graphFactory.binaryOperation(CommutativeBinaryValue.Kind.CMP_EQ, value, Value.NULL);
                    Value isInstance = graphFactory.if_(isNull, Value.FALSE, graphFactory.instanceOf(value, classType));
                    push(isInstance);
                    return;
                }
                case Opcodes.NEW: {
                    GraphFactory.MemoryStateValue value = graphFactory.new_(memoryState, classType);
                    memoryState = value.getMemoryState();
                    push(value.getValue());
                    return;
                }
                case Opcodes.ANEWARRAY: {
                    GraphFactory.MemoryStateValue value = graphFactory.newArray(memoryState, classType.getArrayType(), pop(false));
                    memoryState = value.getMemoryState();
                    push(value.getValue());
                    return;
                }
                case Opcodes.CHECKCAST: {
                    Value value = pop(false);
                    Value isNull = graphFactory.binaryOperation(CommutativeBinaryValue.Kind.CMP_EQ, value, Value.NULL);
                    Value isOk = graphFactory.if_(isNull, Value.TRUE, graphFactory.instanceOf(value, classType));
                    NodeHandle continueHandle = new NodeHandle();
                    // make a little basic block to handle the class cast exception throw
                    ClassType cce = universe.findClass("java/lang/ClassCastException").verify().getClassType();
                    GraphFactory.MemoryStateValue newCce = graphFactory.new_(null, cce);
                    MemoryState tmpState = graphFactory.invokeInstanceMethod(newCce.getMemoryState(), newCce.getValue(), InstanceInvocation.Kind.EXACT, cce, MethodIdentifier.of("<init>", MethodTypeDescriptor.of(Type.VOID)), List.of());
                    BasicBlock throwBlock = graphFactory.block(graphFactory.throw_(tmpState, newCce.getValue()));
                    // if the cast failed, jump to the fail block
                    Terminator terminator = graphFactory.if_(memoryState, isOk, continueHandle, NodeHandle.of(throwBlock));
                    currentBlock.setTerminator(terminator);
                    enter(futureBlockState);
                    continueHandle.setTarget(futureBlock);
                    return;
                }
                default: {
                    super.visitTypeInsn(opcode, type);
                }
            }
        }

        void nullCheck(Value value) {
            Value isNull = graphFactory.binaryOperation(CommutativeBinaryValue.Kind.CMP_EQ, value, Value.NULL);
            ClassType npe = universe.findClass("java/lang/NullPointerException").verify().getClassType();
            GraphFactory.MemoryStateValue newNpe = graphFactory.new_(null, npe);
            MemoryState tmpState = graphFactory.invokeInstanceMethod(newNpe.getMemoryState(), newNpe.getValue(), InstanceInvocation.Kind.EXACT, npe, MethodIdentifier.of("<init>", MethodTypeDescriptor.of(Type.VOID)), List.of());
            BasicBlock throwBlock = graphFactory.block(graphFactory.throw_(tmpState, newNpe.getValue()));
            NodeHandle continueHandle = new NodeHandle();
            Terminator terminator = graphFactory.if_(memoryState, isNull, NodeHandle.of(throwBlock), continueHandle);
            currentBlock.setTerminator(terminator);
            enter(futureBlockState);
            continueHandle.setTarget(futureBlock);
            enter(inBlockState);
        }

        public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
            gotInstr = true;
            ClassType ownerType = universe.findClass(owner).verify().getClassType();
            Type parsedDescriptor = universe.parseSingleDescriptor(descriptor);
            // todo: check type...
            switch (opcode) {
                case Opcodes.GETSTATIC: {
                    GraphFactory.MemoryStateValue memoryStateValue = graphFactory.readStaticField(memoryState, ownerType, name, JavaAccessMode.DETECT);
                    memoryState = memoryStateValue.getMemoryState();
                    push(memoryStateValue.getValue());
                    break;
                }
                case Opcodes.GETFIELD: {
                    GraphFactory.MemoryStateValue memoryStateValue = graphFactory.readInstanceField(memoryState, pop(), ownerType, name, JavaAccessMode.DETECT);
                    memoryState = memoryStateValue.getMemoryState();
                    push(memoryStateValue.getValue());
                    break;
                }
                case Opcodes.PUTSTATIC: {
                    Value value = pop(parsedDescriptor.isClass2Type());
                    memoryState = graphFactory.writeStaticField(memoryState, ownerType, name, value, JavaAccessMode.DETECT);
                    break;
                }
                case Opcodes.PUTFIELD: {
                    Value instance = pop();
                    Value value = pop(parsedDescriptor.isClass2Type());
                    memoryState = graphFactory.writeInstanceField(memoryState, instance, ownerType, name, value, JavaAccessMode.DETECT);
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
            } else if (value instanceof org.objectweb.asm.Type) {
                int sort = ((org.objectweb.asm.Type) value).getSort();
                if (sort == org.objectweb.asm.Type.OBJECT || sort == org.objectweb.asm.Type.ARRAY) {
                    push(Value.const_((ClassType) typeOfAsmType((org.objectweb.asm.Type) value)));
                } else {
                    throw new IllegalStateException();
                }
            } else {
                throw new IllegalStateException();
            }
        }

        public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
            gotInstr = true;
            BasicBlock currentBlock = BytecodeParser.this.currentBlock;
            int length = max - min;
            int[] keys = new int[length];
            NodeHandle[] targets = new NodeHandle[length];
            for (int i = 0; i < length; i ++) {
                keys[i] = min + i;
                targets[i] = getOrMakeBlockHandle(labels[i]);
            }
            currentBlock.setTerminator(graphFactory.switch_(memoryState, pop(), keys, targets, getOrMakeBlockHandle(dflt)));
            enter(possibleBlockState);
        }

        public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
            gotInstr = true;
            BasicBlock currentBlock = BytecodeParser.this.currentBlock;
            NodeHandle[] targets = new NodeHandle[labels.length];
            for (int i = 0; i < keys.length; i ++) {
                targets[i] = getOrMakeBlockHandle(labels[i]);
            }
            currentBlock.setTerminator(graphFactory.switch_(memoryState, pop(), keys, targets, getOrMakeBlockHandle(dflt)));
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
                case Opcodes.ACONST_NULL: {
                    push(Value.NULL);
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
                    push(graphFactory.binaryOperation(CommutativeBinaryValue.Kind.fromOpcode(opcode), popSmart(), popSmart()));
                    return;
                }
                case Opcodes.FDIV:
                case Opcodes.DDIV:
                case Opcodes.FREM:
                case Opcodes.DREM:
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
                    push(graphFactory.binaryOperation(NonCommutativeBinaryValue.Kind.fromOpcode(opcode), popSmart(), popSmart()));
                    return;
                }
                case Opcodes.LCMP: {
                    Value v2 = pop2();
                    Value v1 = pop2();
                    Value lt = graphFactory.binaryOperation(NonCommutativeBinaryValue.Kind.CMP_LT, v1, v2);
                    Value gt = graphFactory.binaryOperation(NonCommutativeBinaryValue.Kind.CMP_GT, v1, v2);
                    push(graphFactory.if_(lt, Value.const_(-1), graphFactory.if_(gt, Value.const_(1), Value.const_(0))));
                    return;
                }

                case Opcodes.FCMPL:
                case Opcodes.DCMPL: {
                    Value v2 = popSmart();
                    Value v1 = popSmart();
                    Value lt = graphFactory.binaryOperation(NonCommutativeBinaryValue.Kind.CMP_LT, v1, v2);
                    Value gt = graphFactory.binaryOperation(NonCommutativeBinaryValue.Kind.CMP_GT, v1, v2);
                    push(graphFactory.if_(lt, Value.const_(-1), graphFactory.if_(gt, Value.const_(1), Value.const_(0))));
                    return;
                }
                case Opcodes.FCMPG:
                case Opcodes.DCMPG: {
                    Value v2 = popSmart();
                    Value v1 = popSmart();
                    Value lt = graphFactory.binaryOperation(NonCommutativeBinaryValue.Kind.CMP_LT, v1, v2);
                    Value gt = graphFactory.binaryOperation(NonCommutativeBinaryValue.Kind.CMP_GT, v1, v2);
                    push(graphFactory.if_(gt, Value.const_(1), graphFactory.if_(lt, Value.const_(-1), Value.const_(0))));
                    return;
                }

                case Opcodes.ARRAYLENGTH: {
                    push(graphFactory.lengthOfArray(pop()));
                    return;
                }
                case Opcodes.FNEG:
                case Opcodes.DNEG: {
                    push(graphFactory.unaryOperation(UnaryValue.Kind.NEGATE, pop()));
                    return;
                }

                case Opcodes.I2L: {
                    push(graphFactory.castOperation(WordCastValue.Kind.EXTEND, pop(), Type.S64));
                    return;
                }
                case Opcodes.L2I: {
                    push(graphFactory.castOperation(WordCastValue.Kind.TRUNCATE, pop2(), Type.S32));
                    return;
                }
                case Opcodes.I2B: {
                    push(graphFactory.castOperation(WordCastValue.Kind.TRUNCATE, pop(), Type.S8));
                    return;
                }
                case Opcodes.I2C: {
                    push(graphFactory.castOperation(WordCastValue.Kind.TRUNCATE, pop(), Type.U16));
                    return;
                }
                case Opcodes.I2S: {
                    push(graphFactory.castOperation(WordCastValue.Kind.TRUNCATE, pop(), Type.S16));
                    return;
                }
                case Opcodes.I2F: {
                    push(graphFactory.castOperation(WordCastValue.Kind.VALUE_CONVERT, pop(), Type.F32));
                    return;
                }
                case Opcodes.I2D:
                case Opcodes.F2D: {
                    push(graphFactory.castOperation(WordCastValue.Kind.VALUE_CONVERT, pop(), Type.F64));
                    return;
                }
                case Opcodes.L2F:
                case Opcodes.D2F: {
                    push(graphFactory.castOperation(WordCastValue.Kind.VALUE_CONVERT, pop2(), Type.F32));
                    return;
                }
                case Opcodes.L2D: {
                    push(graphFactory.castOperation(WordCastValue.Kind.VALUE_CONVERT, pop2(), Type.F64));
                    return;
                }
                case Opcodes.F2I: {
                    push(graphFactory.castOperation(WordCastValue.Kind.VALUE_CONVERT, pop(), Type.S32));
                    return;
                }
                case Opcodes.F2L: {
                    push(graphFactory.castOperation(WordCastValue.Kind.VALUE_CONVERT, pop(), Type.S64));
                    return;
                }
                case Opcodes.D2I: {
                    push(graphFactory.castOperation(WordCastValue.Kind.VALUE_CONVERT, pop2(), Type.S32));
                    return;
                }
                case Opcodes.D2L: {
                    push(graphFactory.castOperation(WordCastValue.Kind.VALUE_CONVERT, pop2(), Type.S64));
                    return;
                }

                case Opcodes.IALOAD:
                case Opcodes.LALOAD:
                case Opcodes.FALOAD:
                case Opcodes.DALOAD:
                case Opcodes.AALOAD:
                case Opcodes.BALOAD:
                case Opcodes.CALOAD:
                case Opcodes.SALOAD: {
                    // todo: add bounds check
                    GraphFactory.MemoryStateValue memoryStateValue = graphFactory.readArrayValue(memoryState, pop(), pop(), JavaAccessMode.PLAIN);
                    memoryState = memoryStateValue.getMemoryState();
                    push(memoryStateValue.getValue());
                    return;
                }


                case Opcodes.IASTORE:
                case Opcodes.LASTORE:
                case Opcodes.FASTORE:
                case Opcodes.DASTORE:
                case Opcodes.AASTORE:
                case Opcodes.BASTORE:
                case Opcodes.CASTORE:
                case Opcodes.SASTORE: {
                    // todo: add bounds check
                    memoryState = graphFactory.writeArrayValue(memoryState, pop(), pop(), popSmart(), JavaAccessMode.PLAIN);
                    return;
                }

                case Opcodes.IDIV:
                case Opcodes.IREM:
                case Opcodes.LDIV:
                case Opcodes.LREM: {
                    Value dividend = popSmart();
                    Value divisor = popSmart();
                    // todo: add zero check
                    push(graphFactory.binaryOperation(NonCommutativeBinaryValue.Kind.fromOpcode(opcode), dividend, divisor));
                    return;
                }

                case Opcodes.ATHROW: {
                    // todo: graph factory that tracks try/catch
                    currentBlock.setTerminator(graphFactory.throw_(memoryState, pop()));
                    enter(possibleBlockState);
                    return;
                }
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT: {
                    throw new UnsupportedOperationException();
                }
                case Opcodes.LRETURN:
                case Opcodes.DRETURN:
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN: {
                    currentBlock.setTerminator(graphFactory.return_(memoryState, popSmart()));
                    enter(possibleBlockState);
                    return;
                }
                case Opcodes.RETURN: {
                    currentBlock.setTerminator(graphFactory.return_(memoryState));
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
                    BasicBlock currentBlock = BytecodeParser.this.currentBlock;
                    NodeHandle jumpTarget = getOrMakeBlockHandle(label);
                    currentBlock.setTerminator(graphFactory.goto_(memoryState, jumpTarget));
                    enter(possibleBlockState);
                    return;
                }
                case Opcodes.IFEQ:
                case Opcodes.IFNE: {
                    handleIfInsn(graphFactory.binaryOperation(CommutativeBinaryValue.Kind.fromOpcode(opcode), pop(), Value.ICONST_0), label);
                    return;
                }
                case Opcodes.IFLT:
                case Opcodes.IFGT:
                case Opcodes.IFLE:
                case Opcodes.IFGE: {
                    handleIfInsn(graphFactory.binaryOperation(NonCommutativeBinaryValue.Kind.fromOpcode(opcode), pop(), Value.ICONST_0), label);
                    return;
                }
                case Opcodes.IF_ICMPEQ:
                case Opcodes.IF_ICMPNE: {
                    handleIfInsn(graphFactory.binaryOperation(CommutativeBinaryValue.Kind.fromOpcode(opcode), pop(), pop()), label);
                    return;
                }
                case Opcodes.IF_ICMPLE:
                case Opcodes.IF_ICMPLT:
                case Opcodes.IF_ICMPGE:
                case Opcodes.IF_ICMPGT: {
                    handleIfInsn(graphFactory.binaryOperation(NonCommutativeBinaryValue.Kind.fromOpcode(opcode), pop(), pop()), label);
                    return;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }

        public void visitIincInsn(final int var, final int increment) {
            gotInstr = true;
            setLocal(var, graphFactory.binaryOperation(CommutativeBinaryValue.Kind.ADD, getLocal(var), Value.const_(increment)));
        }

        void handleIfInsn(Value cond, Label label) {
            BasicBlock currentBlock = BytecodeParser.this.currentBlock;
            NodeHandle falseTarget = new NodeHandle();
            currentBlock.setTerminator(graphFactory.if_(memoryState, cond, getOrMakeBlockHandle(label), falseTarget));
            enter(futureBlockState);
            falseTarget.setTarget(futureBlock);
        }

        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            gotInstr = true;
            DefinedTypeDefinition def = universe.findClass(owner);
            org.objectweb.asm.Type[] types = getArgumentTypes(descriptor);
            int length = types.length;
            Type[] actualTypes = new Type[length];
            for (int i = 0; i < length; i++) {
                actualTypes[i] = typeOfAsmType(types[i]);
            }
            ResolvedMethodDefinition methodDef;
            Type returnType = typeOfAsmType(getReturnType(descriptor));
            MethodIdentifier identifier = MethodIdentifier.of(name, MethodTypeDescriptor.of(returnType, actualTypes));
            if (isInterface) {
                methodDef = def.verify().resolve().resolveMethod(identifier);
            } else {
                methodDef = def.verify().resolve().resolveInterfaceMethod(identifier);
            }
            MethodIdentifier resolved = methodDef.getMethodIdentifier();
            ClassType ownerType = def.verify().getClassType();
            List<Value> arguments;
            if (length == 0) {
                arguments = List.of();
            } else if (length == 1) {
                arguments = List.of(popSmart());
            } else if (length == 2) {
                Value v1 = popSmart();
                Value v0 = popSmart();
                arguments = List.of(v0, v1);
            } else if (length == 3) {
                Value v2 = popSmart();
                Value v1 = popSmart();
                Value v0 = popSmart();
                arguments = List.of(v0, v1, v2);
            } else {
                // just assume lots of arguments
                Value[] args = new Value[length];
                for (int k = length; k > 0; k --) {
                    args[k - 1] = popSmart();
                }
                arguments = List.of(args);
            }
            // todo: catch tracking graph factory
            if (opcode == Opcodes.INVOKESTATIC) {
                if (returnType == Type.VOID) {
                    memoryState = graphFactory.invokeMethod(memoryState, ownerType, resolved, arguments);
                } else {
                    GraphFactory.MemoryStateValue res = graphFactory.invokeValueMethod(memoryState, ownerType, resolved, arguments);
                    memoryState = res.getMemoryState();
                    push(res.getValue());
                }
            } else {
                Value receiver = pop();
                final InstanceInvocation.Kind kind;
                switch (opcode) {
                    case Opcodes.INVOKESPECIAL: kind = InstanceInvocation.Kind.EXACT; break;
                    case Opcodes.INVOKEVIRTUAL: kind = InstanceInvocation.Kind.VIRTUAL; break;
                    case Opcodes.INVOKEINTERFACE: kind = InstanceInvocation.Kind.INTERFACE; break;
                    default: throw new IllegalStateException();
                }
                if (returnType == Type.VOID) {
                    memoryState = graphFactory.invokeInstanceMethod(memoryState, receiver, kind, ownerType, resolved, arguments);
                } else {
                    GraphFactory.MemoryStateValue res = graphFactory.invokeInstanceValueMethod(memoryState, receiver, kind, ownerType, resolved, arguments);
                    memoryState = res.getMemoryState();
                    push(res.getValue());
                }
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
                BasicBlock currentBlock = BytecodeParser.this.currentBlock;
                NodeHandle target = new NodeHandle();
                currentBlock.setTerminator(graphFactory.goto_(memoryState, target));
                enter(futureBlockState);
                target.setTarget(futureBlock);
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

    static final class CatchInfo {
        final BasicBlock target;
        final PhiValue phiValue;

        CatchInfo(final BasicBlock target, final PhiValue phiValue) {
            this.target = target;
            this.phiValue = phiValue;
        }
    }
}
