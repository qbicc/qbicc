package org.qbicc.type.definition.classfile;

import static org.qbicc.type.definition.classfile.ClassFile.*;
import static org.qbicc.type.definition.classfile.ClassMethodInfo.align;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Action;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.MemberSelector;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Ret;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.TerminatorVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.interpreter.InterpreterHaltedException;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.methodhandle.ExecutableMethodHandleConstant;
import org.qbicc.type.methodhandle.MethodHandleConstant;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

final class MethodParser implements BasicBlockBuilder.ExceptionHandlerPolicy {
    private static final Logger interpLog = Logger.getLogger("org.qbicc.interpreter");

    final ClassMethodInfo info;
    final Value[] stack;
    final Value[] locals;
    final BlockLabel[] blockHandles;
    final ByteBuffer buffer;
    private Map<BasicBlock, Value[]> retStacks;
    private Map<BasicBlock, Value[]> retLocals;
    private final BasicBlockBuilder gf;
    private final ClassContext ctxt;
    private final LiteralFactory lf;
    private final TypeSystem ts;
    private final DefinedTypeDefinition jlo;
    private final DefinedTypeDefinition throwable;
    private final LocalVariableElement[][] varsByTableEntry;
    private int currentbci;
    /**
     * Exception handlers by index, then by delegate.
     */
    private final List<Map<BasicBlockBuilder.ExceptionHandler, ExceptionHandlerImpl>> exceptionHandlers;
    int sp;
    private ValueType[][] varTypesByEntryPoint;
    private ValueType[][] stackTypesByEntryPoint;

    MethodParser(final ClassContext ctxt, final ClassMethodInfo info, final ByteBuffer buffer, final BasicBlockBuilder graphFactory) {
        this.ctxt = ctxt;
        lf = ctxt.getLiteralFactory();
        ts = ctxt.getTypeSystem();
        this.info = info;
        stack = new Value[info.getMaxStack()];
        int maxLocals = info.getMaxLocals();
        locals = new Value[maxLocals];
        this.buffer = buffer;
        currentbci = buffer.position();
        int cnt = info.getEntryPointCount();
        BlockLabel[] blockHandles = new BlockLabel[cnt];
        // make a "canonical" node handle for each block
        for (int i = 0; i < cnt; i ++) {
            blockHandles[i] = new BlockLabel();
        }
        this.blockHandles = blockHandles;
        // it's not an entry point
        gf = graphFactory;
        // catch mapper is sensitive to buffer index
        gf.setExceptionHandlerPolicy(this);
        int exCnt = info.getExTableLen();
        exceptionHandlers = exCnt == 0 ? List.of() : new ArrayList<>(Collections.nCopies(exCnt, null));
        jlo = ctxt.findDefinedType("java/lang/Object");
        throwable = ctxt.findDefinedType("java/lang/Throwable");
        LocalVariableElement[][] varsByTableEntry = new LocalVariableElement[maxLocals][];
        for (int slot = 0; slot < maxLocals; slot ++) {
            int entryCount = info.getLocalVarEntryCount(slot);
            varsByTableEntry[slot] = new LocalVariableElement[entryCount];
            for (int entry = 0; entry < entryCount; entry ++) {
                int cons = info.getLocalVarNameIndex(slot, entry);
                boolean realName = false;
                String name;
                if (cons == 0) {
                    name = "var" + slot + "_" + entry;
                } else {
                    realName = true;
                    name = info.getClassFile().getUtf8Constant(cons);
                }
                cons = info.getLocalVarDescriptorIndex(slot, entry);
                if (cons == 0) {
                    throw new IllegalStateException("No descriptor for local variable");
                }
                TypeDescriptor typeDescriptor = (TypeDescriptor) info.getClassFile().getDescriptorConstant(cons);
                LocalVariableElement.Builder builder = LocalVariableElement.builder(name, typeDescriptor);
                int startPc = info.getLocalVarStartPc(slot, entry);
                if (startPc == 0) {
                    builder.setReflectsParameter(true);
                }
                builder.setBci(startPc);
                builder.setLine(info.getLineNumber(startPc));
                builder.setIndex(slot);
                builder.setEnclosingType(gf.getCurrentElement().getEnclosingType());
                builder.setTypeParameterContext(gf.getCurrentElement().getTypeParameterContext());
                cons = info.getLocalVarSignatureIndex(slot, entry);
                if (cons == 0) {
                    builder.setSignature(TypeSignature.synthesize(ctxt, typeDescriptor));
                } else {
                    builder.setSignature(TypeSignature.parse(ctxt, info.getClassFile().getUtf8ConstantAsBuffer(cons)));
                }

                // Heuristically merge type compatible entries with the same name.
                // TODO: This might not be conservative enough...but it is enough to hack by some known problems
                LocalVariableElement cand = builder.build();
                for (int j=0; j<entry; j++) {
                    LocalVariableElement prior = varsByTableEntry[slot][j];
                    if (prior.getTypeDescriptor().equals(cand.getTypeDescriptor()) && (!realName || prior.getName().equals(cand.getName()))){
                        cand = prior;
                        break;
                    }
                }
                varsByTableEntry[slot][entry] = cand;
            }
        }
        this.varsByTableEntry = varsByTableEntry;
    }

    // exception handler policy

    public BasicBlockBuilder.ExceptionHandler computeCurrentExceptionHandler(BasicBlockBuilder.ExceptionHandler delegate) {
        int etl = info.getExTableLen();
        if (etl > 0) {
            ExceptionHandlerImpl handler;
            for (int i = etl - 1; i >= 0; i --) {
                if (info.getExTableEntryStartPc(i) <= currentbci && currentbci < info.getExTableEntryEndPc(i)) {
                    // in range...
                    Map<BasicBlockBuilder.ExceptionHandler, ExceptionHandlerImpl> handlerMap = exceptionHandlers.get(i);
                    if (handlerMap == null) {
                        exceptionHandlers.set(i, handlerMap = new HashMap<>(1));
                    }
                    handler = handlerMap.get(delegate);
                    if (handler == null) {
                        // we also need to create the handler for this index...
                        handlerMap.put(delegate, handler = new ExceptionHandlerImpl(i, delegate));
                    }
                    delegate = handler;
                }
            }
        }
        return delegate;
    }

    void setTypeInformation(final ValueType[][] varTypesByEntryPoint, final ValueType[][] stackTypesByEntryPoint) {
        this.varTypesByEntryPoint = varTypesByEntryPoint;
        this.stackTypesByEntryPoint = stackTypesByEntryPoint;
    }

    class ExceptionHandlerImpl implements BasicBlockBuilder.ExceptionHandler {
        private final int index;
        private final BasicBlockBuilder.ExceptionHandler delegate;
        private final PhiValue phi;

        ExceptionHandlerImpl(final int index, final BasicBlockBuilder.ExceptionHandler delegate) {
            this.index = index;
            this.delegate = delegate;
            this.phi = gf.phi(throwable.load().getType().getReference(), new BlockLabel(), PhiValue.Flag.NOT_NULL);
        }

        public BlockLabel getHandler() {
            return phi.getPinnedBlockLabel();
        }

        public void enterHandler(final BasicBlock from, final BasicBlock landingPad, final Value exceptionValue) {
            Value[] savedStack = saveStack();
            Value[] savedLocals = saveLocals();
            // the PC of the actual exception handler
            int pc = info.getExTableEntryHandlerPc(index);
            // just like entering a new block except we synthesize this one
            if (landingPad != null) {
                // if we have a landing pad, then that's where we've actually entered from
                saveExitValues(landingPad);
            } else {
                // no landing pad means a throw directly transferred control here
                saveExitValues(from);
            }
            clearStack();
            // lock in the thrown exception value
            phi.setValueForBlock(ctxt.getCompilationContext(), gf.getCurrentElement(), landingPad != null ? landingPad : from, gf.notNull(exceptionValue));
            // the phi is the exception value input to our stage of the `if` tree;
            // its label is the entry to our synthetic block
            BlockLabel label = phi.getPinnedBlockLabel();
            if (visited.add(label)) {
                // not yet entered
                setUpNewBlock(pc, label);
                int oldPos = buffer.position();
                int oldPc = gf.setBytecodeIndex(pc);
                gf.setLineNumber(info.getLineNumber(pc));
                // preserve stack and locals so the caller doesn't get mixed up
                // get the block for the exception handler
                BlockLabel block = getBlockForIndexIfExists(pc);
                boolean single = block == null;
                if (single) {
                    // the exception handler is only entered once, so there's no EP registered for it
                    block = new BlockLabel();
                } else {
                    // configure the stack map items for our block with the same types of locals as the target handler
                    int epIdx = info.getEntryPointIndex(pc);
                    if (epIdx < 0) {
                        throw new IllegalStateException("No entry point for block at bci " + pc);
                    }
                    ValueType[] varTypes = varTypesByEntryPoint[epIdx];
                    for (int i = 0; i < locals.length && i < varTypes.length; i ++) {
                        if (varTypes[i] != null) {
                            PhiValue phiValue = gf.phi(varTypes[i], label);
                            locals[i] = phiValue;
                            phiValueSlots.put(phiValue, getVarSlot(i));
                        } else {
                            locals[i] = null;
                        }
                    }
                    // clear remaining slots
                    Arrays.fill(locals, varTypes.length, locals.length, null);
                }
                clearStack();
                // get the exception reference type
                int exTypeIdx = info.getExTableEntryTypeIdx(index);
                boolean catchAll = exTypeIdx == 0;
                ReferenceType exType;
                if (exTypeIdx == 0) {
                    exType = null;
                } else {
                    ObjectType objType = (ObjectType) getClassFile().getTypeConstant(exTypeIdx, TypeParameterContext.of(gf.getCurrentElement()));
                    if (objType.equals(throwable.load().getType())) {
                        catchAll = true;
                        exType = null;
                    } else {
                        exType = objType.getReference();
                    }
                }
                // start up our block
                clearStack();
                gf.begin(label);
                // Safe to pass the upperBound as the classFileType to the instanceOf node here as catch blocks can
                // only catch subclasses of Throwable as enforced by the verifier
                BasicBlock innerFrom;
                Value outboundExValue;
                if (catchAll) {
                    outboundExValue = phi;
                    innerFrom = gf.goto_(block);
                    // make sure the phi is correctly registered even though we don't really use a stack here
                    push1(outboundExValue);
                    saveExitValues(innerFrom);
                    pop1();
                } else {
                    Value[] ourLocals = saveLocals();
                    outboundExValue = gf.bitCast(phi, exType);
                    innerFrom = gf.if_(gf.instanceOf(phi, exType.getUpperBound(), 0), block, delegate.getHandler());
                    // enter the delegate handler if the exception type didn't match
                    // make sure the phi is correctly registered even though we don't really use a stack here
                    push1(outboundExValue);
                    saveExitValues(innerFrom);
                    pop1();
                    delegate.enterHandler(innerFrom, null, phi);
                    clearStack();
                    restoreLocals(ourLocals);
                }
                // enter our handler if the exception type did match
                int previousbci = currentbci;
                buffer.position(pc);
                if (single) {
                    gf.begin(block);
                    // the handler expects the thrown exception on the stack
                    push1(outboundExValue);
                    // only entered from one place, so there's no need to create another layer of phis
                    processNewBlock();
                } else {
                    // the handler expects the thrown exception on the stack
                    push1(outboundExValue);
                    // entered from multiple possible places, so formally process the target block
                    processBlock(innerFrom);
                }
                // restore everything like nothing happened...
                buffer.position(oldPos);
                gf.setBytecodeIndex(oldPc);
                gf.setLineNumber(info.getLineNumber(oldPc));
                currentbci = previousbci;
                restoreLocals(savedLocals);
            }
            restoreStack(savedStack);
        }
    }

    // stack manipulation

    void clearStack() {
        Arrays.fill(stack, 0, sp, null);
        sp = 0;
    }

    Value pop2() {
        int tos = sp - 1;
        if (tos < 1) {
            throw new IllegalStateException("Stack underflow");
        }
        Value value = stack[tos];
        stack[tos] = null;
        // skip the second value as well
        stack[tos - 1] = null;
        sp = tos - 1;
        if (value == null) {
            throw new IllegalStateException("Invalid stack state");
        }
        return value;
    }

    boolean tosIsClass2() {
        return sp > 1 && stack[sp - 2] == null;
    }

    Value pop1() {
        int tos = sp - 1;
        if (tos < 0) {
            throw new IllegalStateException("Stack underflow");
        }
        if (tos > 1 && stack[tos - 1] == null) {
            // class 2 value
            throw new IllegalStateException("Bad pop");
        }
        Value value = stack[tos];
        if (value == null) {
            throw new IllegalStateException("Invalid stack state");
        }
        stack[tos] = null;
        sp = tos;
        return value;
    }

    Value pop(boolean class2) {
        return class2 ? pop2() : pop1();
    }

    void push1(Value value) {
        Assert.checkNotNullParam("value", value);
        stack[sp++] = value;
    }

    void push2(Value value) {
        Assert.checkNotNullParam("value", value);
        stack[sp++] = null;
        stack[sp++] = value;
    }

    void push(Value value, boolean class2) {
        if (class2) {
            push2(value);
        } else {
            push1(value);
        }
    }

    // Locals manipulation

    void setLocal2(int index, Value value, int bci) {
        locals[index] = value;
        locals[index + 1] = null;
        LocalVariableElement lve = getLocalVariableElement(bci, index);
        if (lve != null) {
            gf.store(gf.localVariable(lve), storeTruncate(value, lve.getTypeDescriptor()), MemoryAtomicityMode.NONE);
        }
    }

    void setLocal1(int index, Value value, int bci) {
        locals[index] = value;
        LocalVariableElement lve = getLocalVariableElement(bci, index);
        if (lve != null) {
            gf.store(gf.localVariable(lve), storeTruncate(value, lve.getTypeDescriptor()), MemoryAtomicityMode.NONE);
        }
    }

    void setLocal(int index, Value value, boolean class2, int bci) {
        if (class2) {
            setLocal2(index, value, bci);
        } else {
            setLocal1(index, value, bci);
        }
    }

    LocalVariableElement getLocalVariableElement(int bci, int index) {
        int idx = info.getLocalVarEntryIndex(index, bci);
        if (idx >= 0) {
            return varsByTableEntry[index][idx];
        }
        return null;
    }

    Value getLocal(int index, int bci) {
        final LocalVariableElement lve = getLocalVariableElement(bci, index);
        Value value = locals[index];
        if (value == null) {
            throw new IllegalStateException("Invalid get local (no value)");
        }
        if (value.getType() instanceof ReferenceType) {
            // address cannot be taken of this variable; do not indirect through the LV
            return value;
        }
        if (lve != null) {
            return promote(gf.load(gf.localVariable(lve), MemoryAtomicityMode.NONE), lve.getTypeDescriptor());
        }
        return value;
    }

    Literal convertToBaseTypeLiteral(ReferenceArrayObjectType type) {
        ObjectType baseType = type.getLeafElementType();
        return ctxt.getLiteralFactory().literalOfType(baseType);
    }

    Value getConstantValue(int cpIndex, TypeParameterContext paramCtxt) {
        Literal literal = getClassFile().getConstantValue(cpIndex, paramCtxt);
        if (literal instanceof TypeLiteral) {
            int dims = 0;
            ValueType type = ((TypeLiteral)literal).getValue();
            if (type instanceof ReferenceArrayObjectType) {
                literal = convertToBaseTypeLiteral((ReferenceArrayObjectType)type);
                dims = ((ReferenceArrayObjectType)type).getDimensionCount();
            }
            return gf.classOf(literal, ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), dims));
        } else {
            return literal;
        }
    }

    BlockLabel getBlockForIndex(int target) {
        BlockLabel block = getBlockForIndexIfExists(target);
        if (block == null) {
            throw new IllegalStateException("Block not found for target bci " + target);
        }
        return block;
    }

    BlockLabel getBlockForIndexIfExists(int target) {
        int idx = info.getEntryPointIndex(target);
        return idx >= 0 ? blockHandles[idx] : null;
    }

    void replaceAll(Value from, Value to) {
        if (from == to) {
            return;
        }
        for (int i = 0; i < sp; i ++) {
            if (stack[i] == from) {
                stack[i] = to;
            }
        }
        for (int i = 0; i < locals.length; i ++) {
            if (locals[i] == from) {
                locals[i] = to;
            }
        }
    }

    Value[] saveStack() {
        return Arrays.copyOfRange(stack, 0, sp);
    }

    void restoreStack(Value[] stack) {
        if (sp > stack.length) {
            Arrays.fill(this.stack, sp, this.stack.length, null);
        }
        System.arraycopy(stack, 0, this.stack, 0, stack.length);
        sp = stack.length;
    }

    Value[] saveLocals() {
        return locals.clone();
    }

    void restoreLocals(Value[] locals) {
        assert locals.length == this.locals.length;
        System.arraycopy(locals, 0, this.locals, 0, locals.length);
    }

    static final class Slot {
        private final boolean stack;
        private final int index;

        Slot(boolean stack, int index) {
            this.stack = stack;
            this.index = index;
        }

        public boolean isStack() {
            return stack;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(index) * Boolean.hashCode(stack);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Slot && equals((Slot) obj);
        }

        public boolean equals(Slot obj) {
            return obj == this || obj != null && obj.stack == stack && obj.index == index;
        }

        @Override
        public String toString() {
            return (stack ? "stack" : "var") + '[' + index + ']';
        }
    }

    final List<Slot> stackSlotCache = new ArrayList<>(4);
    final List<Slot> varSlotCache = new ArrayList<>(8);

    /**
     * Each phi value exists only in one logical slot when it is created.  This map records the slot for each
     * phi value.
     */
    final Map<PhiValue, Slot> phiValueSlots = new HashMap<>();
    /**
     * The values that are <em>available</em> at the end of each block. Successor blocks may choose
     * not to consume these values.
     */
    final Map<BasicBlock, Map<Slot, Value>> blockExitValues = new HashMap<>();
    /**
     * The set of blocks whose bytecode has been processed.
     */
    final Set<BlockLabel> visited = new HashSet<>();

    Slot getStackSlot(int idx) {
        int size = stackSlotCache.size();
        Slot slot;
        if (idx >= size) {
            stackSlotCache.addAll(Collections.nCopies(idx - size + 1, null));
            slot = null;
        } else {
            slot = stackSlotCache.get(idx);
        }
        if (slot == null) {
            slot = new Slot(true, idx);
            stackSlotCache.set(idx, slot);
        }
        return slot;
    }

    Slot getVarSlot(int idx) {
        int size = varSlotCache.size();
        Slot slot;
        if (idx >= size) {
            varSlotCache.addAll(Collections.nCopies(idx - size + 1, null));
            slot = null;
        } else {
            slot = varSlotCache.get(idx);
        }
        if (slot == null)  {
            slot = new Slot(false, idx);
            varSlotCache.set(idx, slot);
        }
        return slot;
    }

    /**
     * Process a single block.  The current stack and locals are used as a template for the phi value types within
     * the block.  At exit the stack and locals are in an indeterminate state.
     *
     * @param from the source (exiting) block
     */
    void processBlock(BasicBlock from) {
        if (from != null) {
            // save exit values so we can match them later
            saveExitValues(from);
        }
        ByteBuffer buffer = this.buffer;
        // this is the canonical map key handle
        int bci = buffer.position();
        BlockLabel block = getBlockForIndex(bci);
        if (visited.add(block)) {
            // not registered yet; process new block first
            gf.setBytecodeIndex(bci);
            gf.setLineNumber(info.getLineNumber(bci));
            setUpNewBlock(bci, block);
            gf.begin(block);
            processNewBlock();
        }
    }

    private final Set<BlockLabel> setUpBlocks = new HashSet<>();

    private void setUpNewBlock(final int bci, final BlockLabel block) {
        if (!setUpBlocks.add(block)) {
            throw new IllegalStateException("Set up twice");
        }
        PhiValue[] entryStack;
        PhiValue[] entryLocalsArray;
        entryLocalsArray = new PhiValue[locals.length];
        entryStack = new PhiValue[sp];
        int epIdx = info.getEntryPointIndex(bci);
        if (epIdx < 0) {
            throw new IllegalStateException("No entry point for block at bci " + bci);
        }
        ValueType[] varTypes = varTypesByEntryPoint[epIdx];
        for (int i = 0; i < locals.length && i < varTypes.length; i ++) {
            if (varTypes[i] != null) {
                phiValueSlots.put(entryLocalsArray[i] = gf.phi(varTypes[i], block), getVarSlot(i));
            }
        }
        ValueType[] stackTypes = stackTypesByEntryPoint[epIdx];
        for (int i = 0; i < sp; i ++) {
            if (stackTypes[i] != null) {
                phiValueSlots.put(entryStack[i] = gf.phi(stackTypes[i], block), getStackSlot(i));
            }
        }
        restoreStack(entryStack);
        restoreLocals(entryLocalsArray);
    }

    void processNewBlock() {
        ByteBuffer buffer = this.buffer;
        Value v1, v2, v3, v4;
        int opcode;
        int src;
        boolean wide;
        ClassMethodInfo info = this.info;
        try {
            while (buffer.hasRemaining()) {
                currentbci = src = buffer.position();
                gf.setBytecodeIndex(src);
                gf.setLineNumber(info.getLineNumber(src));
                opcode = buffer.get() & 0xff;
                wide = opcode == OP_WIDE;
                if (wide) {
                    opcode = buffer.get() & 0xff;
                }
                switch (opcode) {
                    case OP_NOP:
                        break;
                    case OP_ACONST_NULL:
                        push1(lf.zeroInitializerLiteralOfType(jlo.load().getClassType().getReference()));
                        break;
                    case OP_ICONST_M1:
                    case OP_ICONST_0:
                    case OP_ICONST_1:
                    case OP_ICONST_2:
                    case OP_ICONST_3:
                    case OP_ICONST_4:
                    case OP_ICONST_5:
                        push1(lf.literalOf(opcode - OP_ICONST_0));
                        break;
                    case OP_LCONST_0:
                    case OP_LCONST_1:
                        push2(lf.literalOf((long) opcode - OP_LCONST_0));
                        break;
                    case OP_FCONST_0:
                    case OP_FCONST_1:
                    case OP_FCONST_2:
                        push1(lf.literalOf((float) opcode - OP_FCONST_0));
                        break;
                    case OP_DCONST_0:
                    case OP_DCONST_1:
                        push2(lf.literalOf((double) opcode - OP_DCONST_0));
                        break;
                    case OP_BIPUSH:
                        push1(lf.literalOf((int) buffer.get()));
                        break;
                    case OP_SIPUSH:
                        push1(lf.literalOf((int) buffer.getShort()));
                        break;
                    case OP_LDC:
                        push1(getConstantValue(buffer.get() & 0xff, TypeParameterContext.of(gf.getCurrentElement())));
                        break;
                    case OP_LDC_W:
                        push1(getConstantValue(buffer.getShort() & 0xffff, TypeParameterContext.of(gf.getCurrentElement())));
                        break;
                    case OP_LDC2_W:
                        push2(getConstantValue(buffer.getShort() & 0xffff, TypeParameterContext.of(gf.getCurrentElement())));
                        break;
                    case OP_ILOAD:
                    case OP_FLOAD:
                    case OP_ALOAD:
                        push1(getLocal(getWidenableValue(buffer, wide), src));
                        break;
                    case OP_LLOAD:
                    case OP_DLOAD:
                        push2(getLocal(getWidenableValue(buffer, wide), src));
                        break;
                    case OP_ILOAD_0:
                    case OP_ILOAD_1:
                    case OP_ILOAD_2:
                    case OP_ILOAD_3:
                        push1(getLocal(opcode - OP_ILOAD_0, src));
                        break;
                    case OP_LLOAD_0:
                    case OP_LLOAD_1:
                    case OP_LLOAD_2:
                    case OP_LLOAD_3:
                        push2(getLocal(opcode - OP_LLOAD_0, src));
                        break;
                    case OP_FLOAD_0:
                    case OP_FLOAD_1:
                    case OP_FLOAD_2:
                    case OP_FLOAD_3:
                        push1(getLocal(opcode - OP_FLOAD_0, src));
                        break;
                    case OP_DLOAD_0:
                    case OP_DLOAD_1:
                    case OP_DLOAD_2:
                    case OP_DLOAD_3:
                        push2(getLocal(opcode - OP_DLOAD_0, src));
                        break;
                    case OP_ALOAD_0:
                    case OP_ALOAD_1:
                    case OP_ALOAD_2:
                    case OP_ALOAD_3:
                        push1(getLocal(opcode - OP_ALOAD_0, src));
                        break;
                    case OP_AALOAD: {
                        v2 = pop1();
                        v1 = pop1();
                        if (v1 instanceof MemberSelector ms) {
                            v1 = ms.getInput();
                            if (v1.getType() instanceof PointerType pt) {
                                ValueHandle element;
                                if (pt.getPointeeType() instanceof ArrayType) {
                                    element = gf.elementOf(gf.pointerHandle(v1), v2);
                                } else {
                                    ctxt.getCompilationContext().error(gf.getLocation(), "Invalid array dereference");
                                    throw new BlockEarlyTermination(gf.unreachable());
                                }
                                v1 = gf.selectMember(gf.addressOf(element));
                            } else {
                                ctxt.getCompilationContext().error(gf.getLocation(), "Invalid array dereference");
                                throw new BlockEarlyTermination(gf.unreachable());
                            }
                        } else if (v1.getType() instanceof ArrayType) {
                            v1 = gf.extractElement(v1, v2);
                        } else if (v1.getType() instanceof PointerType) {
                            v1 = gf.load(gf.pointerHandle(v1, v2), MemoryAtomicityMode.NONE);
                        } else {
                            v1 = gf.load(gf.elementOf(gf.referenceHandle(v1), v2), MemoryAtomicityMode.UNORDERED);
                        }
                        push1(v1);
                        break;
                    }
                    case OP_DALOAD:
                    case OP_LALOAD: {
                        v2 = pop1();
                        v1 = pop1();
                        if (v1 instanceof MemberSelector ms) {
                            v1 = ms.getInput();
                            if (v1.getType() instanceof PointerType pt) {
                                ValueHandle element;
                                if (pt.getPointeeType() instanceof ArrayType) {
                                    element = gf.elementOf(gf.pointerHandle(v1), v2);
                                } else {
                                    ctxt.getCompilationContext().error(gf.getLocation(), "Invalid array dereference");
                                    throw new BlockEarlyTermination(gf.unreachable());
                                }
                                v1 = gf.selectMember(gf.addressOf(element));
                            } else {
                                ctxt.getCompilationContext().error(gf.getLocation(), "Invalid array dereference");
                                throw new BlockEarlyTermination(gf.unreachable());
                            }
                        } else if (v1.getType() instanceof ArrayType) {
                            v1 = gf.extractElement(v1, v2);
                        } else if (v1.getType() instanceof PointerType) {
                            v1 = gf.load(gf.pointerHandle(v1, v2), MemoryAtomicityMode.NONE);
                        } else {
                            v1 = gf.load(gf.elementOf(gf.referenceHandle(v1), v2), MemoryAtomicityMode.UNORDERED);
                        }
                        push2(v1);
                        break;
                    }
                    case OP_IALOAD:
                    case OP_FALOAD:
                    case OP_BALOAD:
                    case OP_SALOAD:
                    case OP_CALOAD: {
                        v2 = pop1();
                        v1 = pop1();
                        if (v1 instanceof MemberSelector ms) {
                            v1 = ms.getInput();
                            if (v1.getType() instanceof PointerType pt) {
                                ValueHandle element;
                                if (pt.getPointeeType() instanceof ArrayType) {
                                    element = gf.elementOf(gf.pointerHandle(v1), v2);
                                } else {
                                    ctxt.getCompilationContext().error(gf.getLocation(), "Invalid array dereference");
                                    throw new BlockEarlyTermination(gf.unreachable());
                                }
                                v1 = gf.selectMember(gf.addressOf(element));
                            } else {
                                ctxt.getCompilationContext().error(gf.getLocation(), "Invalid array dereference");
                                throw new BlockEarlyTermination(gf.unreachable());
                            }
                        } else if (v1.getType() instanceof ArrayType) {
                            v1 = promote(gf.extractElement(v1, v2));
                        } else if (v1.getType() instanceof PointerType) {
                            v1 = promote(gf.load(gf.pointerHandle(v1, v2), MemoryAtomicityMode.NONE));
                        } else {
                            v1 = promote(gf.load(gf.elementOf(gf.referenceHandle(v1), v2), MemoryAtomicityMode.UNORDERED));
                        }
                        push1(v1);
                        break;
                    }
                    case OP_ISTORE:
                    case OP_FSTORE:
                    case OP_ASTORE:
                        setLocal1(getWidenableValue(buffer, wide), pop1(), buffer.position());
                        break;
                    case OP_LSTORE:
                    case OP_DSTORE:
                        setLocal2(getWidenableValue(buffer, wide), pop2(), buffer.position());
                        break;
                    case OP_ISTORE_0:
                    case OP_ISTORE_1:
                    case OP_ISTORE_2:
                    case OP_ISTORE_3:
                        setLocal1(opcode - OP_ISTORE_0, pop1(), buffer.position());
                        break;
                    case OP_LSTORE_0:
                    case OP_LSTORE_1:
                    case OP_LSTORE_2:
                    case OP_LSTORE_3:
                        setLocal2(opcode - OP_LSTORE_0, pop2(), buffer.position());
                        break;
                    case OP_FSTORE_0:
                    case OP_FSTORE_1:
                    case OP_FSTORE_2:
                    case OP_FSTORE_3:
                        setLocal1(opcode - OP_FSTORE_0, pop1(), buffer.position());
                        break;
                    case OP_DSTORE_0:
                    case OP_DSTORE_1:
                    case OP_DSTORE_2:
                    case OP_DSTORE_3:
                        setLocal2(opcode - OP_DSTORE_0, pop2(), buffer.position());
                        break;
                    case OP_ASTORE_0:
                    case OP_ASTORE_1:
                    case OP_ASTORE_2:
                    case OP_ASTORE_3:
                        setLocal1(opcode - OP_ASTORE_0, pop1(), buffer.position());
                        break;
                    case OP_IASTORE:
                    case OP_FASTORE:
                    case OP_AASTORE:
                        v3 = pop1();
                        v2 = pop1();
                        v1 = pop1();
                        if (v1.getType() instanceof ArrayType) {
                            replaceAll(v1, gf.insertElement(v1, v2, v3));
                        } else if (v1.getType() instanceof PointerType) {
                            gf.store(gf.pointerHandle(v1, v2), v3, MemoryAtomicityMode.NONE);
                        } else {
                            gf.store(gf.elementOf(gf.referenceHandle(v1), v2), v3, MemoryAtomicityMode.UNORDERED);
                            if (v1.getType() instanceof ReferenceType) {
                                replaceAll(v1, gf.notNull(v1));
                            }
                        }
                        break;
                    case OP_BASTORE:
                        v3 = gf.truncate(pop1(), ts.getSignedInteger8Type());
                        v2 = pop1();
                        v1 = pop1();
                        if (v1.getType() instanceof ArrayType) {
                            replaceAll(v1, gf.insertElement(v1, v2, v3));
                        } else if (v1.getType() instanceof PointerType) {
                            gf.store(gf.pointerHandle(v1, v2), v3, MemoryAtomicityMode.NONE);
                        } else {
                            gf.store(gf.elementOf(gf.referenceHandle(v1), v2), v3, MemoryAtomicityMode.UNORDERED);
                            if (v1.getType() instanceof ReferenceType) {
                                replaceAll(v1, gf.notNull(v1));
                            }
                        }
                        break;
                    case OP_SASTORE:
                        v3 = gf.truncate(pop1(), ts.getSignedInteger16Type());
                        v2 = pop1();
                        v1 = pop1();
                        if (v1.getType() instanceof ArrayType) {
                            replaceAll(v1, gf.insertElement(v1, v2, v3));
                        } else if (v1.getType() instanceof PointerType) {
                            gf.store(gf.pointerHandle(v1, v2), v3, MemoryAtomicityMode.NONE);
                        } else {
                            gf.store(gf.elementOf(gf.referenceHandle(v1), v2), v3, MemoryAtomicityMode.UNORDERED);
                            if (v1.getType() instanceof ReferenceType) {
                                replaceAll(v1, gf.notNull(v1));
                            }
                        }
                        break;
                    case OP_CASTORE:
                        v3 = gf.truncate(pop1(), ts.getUnsignedInteger16Type());
                        v2 = pop1();
                        v1 = pop1();
                        if (v1.getType() instanceof ArrayType) {
                            replaceAll(v1, gf.insertElement(v1, v2, v3));
                        } else if (v1.getType() instanceof PointerType) {
                            gf.store(gf.pointerHandle(v1, v2), v3, MemoryAtomicityMode.NONE);
                        } else {
                            gf.store(gf.elementOf(gf.referenceHandle(v1), v2), v3, MemoryAtomicityMode.UNORDERED);
                            if (v1.getType() instanceof ReferenceType) {
                                replaceAll(v1, gf.notNull(v1));
                            }
                        }
                        break;
                    case OP_LASTORE:
                    case OP_DASTORE:
                        v3 = pop2();
                        v2 = pop1();
                        v1 = pop1();
                        if (v1.getType() instanceof ArrayType) {
                            replaceAll(v1, gf.insertElement(v1, v2, v3));
                        } else if (v1.getType() instanceof PointerType) {
                            gf.store(gf.pointerHandle(v1, v2), v3, MemoryAtomicityMode.NONE);
                        } else {
                            gf.store(gf.elementOf(gf.referenceHandle(v1), v2), v3, MemoryAtomicityMode.UNORDERED);
                            if (v1.getType() instanceof ReferenceType) {
                                replaceAll(v1, gf.notNull(v1));
                            }
                        }
                        break;
                    case OP_POP:
                        pop1();
                        break;
                    case OP_POP2:
                        pop2();
                        break;
                    case OP_DUP:
                        v1 = pop1();
                        push1(v1);
                        push1(v1);
                        break;
                    case OP_DUP_X1:
                        v1 = pop1();
                        v2 = pop1();
                        push1(v1);
                        push1(v2);
                        push1(v1);
                        break;
                    case OP_DUP_X2:
                        v1 = pop1();
                        v2 = pop1();
                        v3 = pop1();
                        push1(v1);
                        push1(v3);
                        push1(v2);
                        push1(v1);
                        break;
                    case OP_DUP2:
                        if (tosIsClass2()) {
                            v1 = pop2();
                            push2(v1);
                            push2(v1);
                        } else {
                            v2 = pop1();
                            v1 = pop1();
                            push1(v1);
                            push1(v2);
                            push1(v1);
                            push1(v2);
                        }
                        break;
                    case OP_DUP2_X1:
                        if (! tosIsClass2()) {
                            // form 1
                            v1 = pop1();
                            v2 = pop1();
                            v3 = pop1();
                            push1(v2);
                            push1(v1);
                            push1(v3);
                            push1(v2);
                            push1(v1);
                        } else {
                            // form 2
                            v1 = pop2();
                            v2 = pop1();
                            push2(v1);
                            push1(v2);
                            push2(v1);
                        }
                        break;
                    case OP_DUP2_X2:
                        if (! tosIsClass2()) {
                            v1 = pop1();
                            v2 = pop1();
                            if (! tosIsClass2()) {
                                // form 1
                                v3 = pop1();
                                v4 = pop1();
                                push1(v2);
                                push1(v1);
                                push1(v4);
                                push1(v3);
                            } else {
                                // form 3
                                v3 = pop2();
                                push1(v2);
                                push1(v1);
                                push2(v3);
                            }
                            push1(v2);
                            push1(v1);
                        } else {
                            v1 = pop2();
                            if (! tosIsClass2()) {
                                // form 2
                                v2 = pop1();
                                v3 = pop1();
                                push2(v1);
                                push1(v2);
                                push1(v3);
                            } else {
                                // form 4
                                v2 = pop2();
                                push2(v1);
                                push2(v2);
                            }
                            // form 2 or 4
                            push2(v1);
                        }
                        break;
                    case OP_SWAP:
                        v2 = pop1();
                        v1 = pop1();
                        push1(v2);
                        push1(v1);
                        break;
                    case OP_IADD:
                    case OP_FADD:
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.add(v1, v2));
                        break;
                    case OP_LADD:
                    case OP_DADD:
                        v2 = pop2();
                        v1 = pop2();
                        push2(gf.add(v1, v2));
                        break;
                    case OP_ISUB:
                    case OP_FSUB:
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.sub(v1, v2));
                        break;
                    case OP_LSUB:
                    case OP_DSUB:
                        v2 = pop2();
                        v1 = pop2();
                        push2(gf.sub(v1, v2));
                        break;
                    case OP_IMUL:
                    case OP_FMUL:
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.multiply(v1, v2));
                        break;
                    case OP_LMUL:
                    case OP_DMUL:
                        v2 = pop2();
                        v1 = pop2();
                        push2(gf.multiply(v1, v2));
                        break;
                    case OP_IDIV:
                    case OP_FDIV:
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.divide(v1, v2));
                        break;
                    case OP_LDIV:
                    case OP_DDIV:
                        v2 = pop2();
                        v1 = pop2();
                        push2(gf.divide(v1, v2));
                        break;
                    case OP_IREM:
                    case OP_FREM:
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.remainder(v1, v2));
                        break;
                    case OP_LREM:
                    case OP_DREM:
                        v2 = pop2();
                        v1 = pop2();
                        push2(gf.remainder(v1, v2));
                        break;
                    case OP_INEG:
                    case OP_FNEG:
                        push1(gf.negate(pop1()));
                        break;
                    case OP_LNEG:
                    case OP_DNEG:
                        push2(gf.negate(pop2()));
                        break;
                    case OP_ISHL:
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.shl(v1, v2));
                        break;
                    case OP_LSHL:
                        v2 = pop1();
                        v1 = pop2();
                        push2(gf.shl(v1, v2));
                        break;
                    case OP_ISHR: {
                        v2 = pop1();
                        v1 = pop1();
                        IntegerType it = (IntegerType) v1.getType();
                        if (it instanceof SignedIntegerType) {
                            push1(gf.shr(v1, v2));
                        } else {
                            push1(gf.bitCast(gf.shr(gf.bitCast(v1, it.asSigned()), v2), it));
                        }
                        break;
                    }
                    case OP_LSHR: {
                        v2 = pop1();
                        v1 = pop2();
                        IntegerType it = (IntegerType) v1.getType();
                        if (it instanceof SignedIntegerType) {
                            push2(gf.shr(v1, v2));
                        } else {
                            push2(gf.bitCast(gf.shr(gf.bitCast(v1, it.asSigned()), v2), it));
                        }
                        break;
                    }
                    case OP_IUSHR: {
                        v2 = pop1();
                        v1 = pop1();
                        IntegerType it = (IntegerType) v1.getType();
                        if (it instanceof UnsignedIntegerType) {
                            push1(gf.shr(v1, v2));
                        } else {
                            push1(gf.bitCast(gf.shr(gf.bitCast(v1, it.asUnsigned()), v2), it));
                        }
                        break;
                    }
                    case OP_LUSHR: {
                        v2 = pop1();
                        v1 = pop2();
                        IntegerType it = (IntegerType) v1.getType();
                        if (it instanceof UnsignedIntegerType) {
                            push2(gf.shr(v1, v2));
                        } else {
                            push2(gf.bitCast(gf.shr(gf.bitCast(v1, it.asUnsigned()), v2), it));
                        }
                        break;
                    }
                    case OP_IAND:
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.and(v1, v2));
                        break;
                    case OP_LAND:
                        v2 = pop2();
                        v1 = pop2();
                        push2(gf.and(v1, v2));
                        break;
                    case OP_IOR:
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.or(v1, v2));
                        break;
                    case OP_LOR:
                        v2 = pop2();
                        v1 = pop2();
                        push2(gf.or(v1, v2));
                        break;
                    case OP_IXOR:
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.xor(v1, v2));
                        break;
                    case OP_LXOR:
                        v2 = pop2();
                        v1 = pop2();
                        push2(gf.xor(v1, v2));
                        break;
                    case OP_IINC:
                        int idx = getWidenableValue(buffer, wide);
                        // it's unlikely but possible that the identity of the var has changed between load and store
                        setLocal1(idx, gf.add(getLocal(idx, src), lf.literalOf(getWidenableValueSigned(buffer, wide))), buffer.position());
                        break;
                    case OP_I2L:
                        push2(gf.extend(pop1(), ts.getSignedInteger64Type()));
                        break;
                    case OP_I2F:
                        push1(gf.valueConvert(pop1(), ts.getFloat32Type()));
                        break;
                    case OP_I2D:
                        push2(gf.valueConvert(pop1(), ts.getFloat64Type()));
                        break;
                    case OP_L2I:
                        push1(gf.truncate(pop2(), ts.getSignedInteger32Type()));
                        break;
                    case OP_L2F:
                        push1(gf.valueConvert(pop2(), ts.getFloat32Type()));
                        break;
                    case OP_L2D:
                        push2(gf.valueConvert(pop2(), ts.getFloat64Type()));
                        break;
                    case OP_F2I:
                        push1(gf.valueConvert(pop1(), ts.getSignedInteger32Type()));
                        break;
                    case OP_F2L:
                        push2(gf.valueConvert(pop1(), ts.getSignedInteger64Type()));
                        break;
                    case OP_F2D:
                        push2(gf.extend(pop1(), ts.getFloat64Type()));
                        break;
                    case OP_D2I:
                        push1(gf.valueConvert(pop2(), ts.getSignedInteger32Type()));
                        break;
                    case OP_D2L:
                        push2(gf.valueConvert(pop2(), ts.getSignedInteger64Type()));
                        break;
                    case OP_D2F:
                        push1(gf.truncate(pop2(), ts.getFloat32Type()));
                        break;
                    case OP_I2B:
                        push1(gf.extend(gf.truncate(pop1(), ts.getSignedInteger8Type()), ts.getSignedInteger32Type()));
                        break;
                    case OP_I2C:
                        push1(gf.extend(gf.truncate(pop1(), ts.getUnsignedInteger16Type()), ts.getSignedInteger32Type()));
                        break;
                    case OP_I2S:
                        push1(gf.extend(gf.truncate(pop1(), ts.getSignedInteger16Type()), ts.getSignedInteger32Type()));
                        break;
                    case OP_LCMP: {
                        v2 = pop2();
                        v1 = pop2();
                        push1(gf.cmp(v1, v2));
                        break;
                    }
                    case OP_DCMPL: {
                        v2 = pop2();
                        v1 = pop2();
                        push1(gf.cmpL(v1, v2));
                        break;
                    }
                    case OP_FCMPL: {
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.cmpL(v1, v2));
                        break;
                    }
                    case OP_DCMPG: {
                        v2 = pop2();
                        v1 = pop2();
                        push1(gf.cmpG(v1, v2));
                        break;
                    }
                    case OP_FCMPG: {
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.cmpG(v1, v2));
                        break;
                    }
                    case OP_IFEQ:
                        v1 = pop1();
                        processIf(buffer, gf.isEq(v1, lf.zeroInitializerLiteralOfType(v1.getType())), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IFNE:
                        v1 = pop1();
                        processIf(buffer, gf.isNe(v1, lf.zeroInitializerLiteralOfType(v1.getType())), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IFLT:
                        v1 = pop1();
                        processIf(buffer, gf.isLt(v1, lf.zeroInitializerLiteralOfType(v1.getType())), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IFGE:
                        v1 = pop1();
                        processIf(buffer, gf.isGe(v1, lf.zeroInitializerLiteralOfType(v1.getType())), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IFGT:
                        v1 = pop1();
                        processIf(buffer, gf.isGt(v1, lf.zeroInitializerLiteralOfType(v1.getType())), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IFLE:
                        v1 = pop1();
                        processIf(buffer, gf.isLe(v1, lf.zeroInitializerLiteralOfType(v1.getType())), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IF_ICMPEQ:
                    case OP_IF_ACMPEQ:
                        v2 = pop1();
                        v1 = pop1();
                        processIf(buffer, gf.isEq(v1, v2), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IF_ICMPNE:
                    case OP_IF_ACMPNE:
                        v2 = pop1();
                        v1 = pop1();
                        processIf(buffer, gf.isNe(v1, v2), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IF_ICMPLT:
                        v2 = pop1();
                        v1 = pop1();
                        processIf(buffer, gf.isLt(v1, v2), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IF_ICMPGE:
                        v2 = pop1();
                        v1 = pop1();
                        processIf(buffer, gf.isGe(v1, v2), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IF_ICMPGT:
                        v2 = pop1();
                        v1 = pop1();
                        processIf(buffer, gf.isGt(v1, v2), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IF_ICMPLE:
                        v2 = pop1();
                        v1 = pop1();
                        processIf(buffer, gf.isLe(v1, v2), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_GOTO:
                    case OP_GOTO_W: {
                        int target = src + (opcode == OP_GOTO ? buffer.getShort() : buffer.getInt());
                        BlockLabel block = getBlockForIndexIfExists(target);
                        BasicBlock from;
                        if (block == null) {
                            // only one entry point
                            block = new BlockLabel();
                            gf.goto_(block);
                            // set the position after, so that the bci for the instruction is correct
                            buffer.position(target);
                            gf.begin(block);
                            processNewBlock();
                        } else {
                            from = gf.goto_(block);
                            // set the position after, so that the bci for the instruction is correct
                            buffer.position(target);
                            processBlock(from);
                        }
                        return;
                    }
                    case OP_JSR:
                    case OP_JSR_W: {
                        int target = src + (opcode == OP_JSR ? buffer.getShort() : buffer.getInt());
                        int ret = buffer.position();
                        // jsr destination
                        BlockLabel dest = getBlockForIndexIfExists(target);
                        // ret point is always registered as a multiple return point
                        BlockLabel retBlock = getBlockForIndex(ret);
                        push1(lf.literalOf(retBlock));
                        if (dest == null) {
                            // only called from one site
                            dest = new BlockLabel();
                            gf.jsr(dest, lf.literalOf(retBlock));
                            buffer.position(target);
                            gf.begin(dest);
                            processNewBlock();
                        } else {
                            // the jsr call
                            BasicBlock termBlock = gf.jsr(dest, lf.literalOf(retBlock));
                            // process the jsr call target block with our current stack
                            buffer.position(target);
                            processBlock(termBlock);
                        }
                        // now process the return block once for each returning path (as if the ret is a goto);
                        // the ret visitor will continue parsing if the jsr returns (as opposed to throwing)
                        new RetVisitor(buffer, ret).handleBlock(BlockLabel.getTargetOf(dest));
                        return;
                    }
                    case OP_RET:
                        // each ret records the output stack and locals at the point of the ret, and then exits.
                        setJsrExitState(gf.ret(pop1()), saveStack(), saveLocals());
                        // exit one level of recursion
                        return;
                    case OP_TABLESWITCH: {
                        align(buffer, 4);
                        int db = buffer.getInt();
                        int low = buffer.getInt();
                        int high = buffer.getInt();
                        int cnt = high - low + 1;
                        int[] dests = new int[cnt];
                        int[] vals = new int[cnt];
                        boolean[] singles = new boolean[cnt];
                        BlockLabel[] handles = new BlockLabel[cnt];
                        for (int i = 0; i < cnt; i++) {
                            vals[i] = low + i;
                            BlockLabel block = getBlockForIndexIfExists(dests[i] = buffer.getInt() + src);
                            if (block == null) {
                                handles[i] = new BlockLabel();
                                singles[i] = true;
                            } else {
                                handles[i] = block;
                            }
                        }
                        Set<BlockLabel> seen = new HashSet<>();
                        boolean defaultSingle;
                        BlockLabel defaultBlock = getBlockForIndexIfExists(db + src);
                        if (defaultBlock == null) {
                            defaultSingle = true;
                            defaultBlock = new BlockLabel();
                        } else {
                            defaultSingle = false;
                        }
                        seen.add(defaultBlock);
                        BasicBlock exited = gf.switch_(pop1(), vals, handles, defaultBlock);
                        Value[] stackSnap = saveStack();
                        Value[] varSnap = saveLocals();
                        buffer.position(db + src);
                        if (defaultSingle) {
                            gf.begin(defaultBlock);
                            processNewBlock();
                        } else {
                            processBlock(exited);
                        }
                        for (int i = 0; i < handles.length; i++) {
                            if (seen.add(handles[i])) {
                                restoreStack(stackSnap);
                                restoreLocals(varSnap);
                                buffer.position(dests[i]);
                                if (singles[i]) {
                                    gf.begin(handles[i]);
                                    processNewBlock();
                                } else {
                                    processBlock(exited);
                                }
                            }
                        }
                        // done
                        return;
                    }
                    case OP_LOOKUPSWITCH: {
                        align(buffer, 4);
                        int db = buffer.getInt();
                        int cnt = buffer.getInt();
                        int[] dests = new int[cnt];
                        int[] vals = new int[cnt];
                        boolean[] singles = new boolean[cnt];
                        BlockLabel[] handles = new BlockLabel[cnt];
                        for (int i = 0; i < cnt; i++) {
                            vals[i] = buffer.getInt();
                            BlockLabel block = getBlockForIndexIfExists(dests[i] = buffer.getInt() + src);
                            if (block == null) {
                                handles[i] = new BlockLabel();
                                singles[i] = true;
                            } else {
                                handles[i] = block;
                            }
                        }
                        Set<BlockLabel> seen = new HashSet<>();
                        boolean defaultSingle;
                        BlockLabel defaultBlock = getBlockForIndexIfExists(db + src);
                        if (defaultBlock == null) {
                            defaultSingle = true;
                            defaultBlock = new BlockLabel();
                        } else {
                            defaultSingle = false;
                        }
                        seen.add(defaultBlock);
                        BasicBlock exited = gf.switch_(pop1(), vals, handles, defaultBlock);
                        Value[] stackSnap = saveStack();
                        Value[] varSnap = saveLocals();
                        buffer.position(db + src);
                        if (defaultSingle) {
                            gf.begin(defaultBlock);
                            processNewBlock();
                        } else {
                            processBlock(exited);
                        }
                        for (int i = 0; i < handles.length; i++) {
                            if (seen.add(handles[i])) {
                                restoreStack(stackSnap);
                                restoreLocals(varSnap);
                                buffer.position(dests[i]);
                                if (singles[i]) {
                                    gf.begin(handles[i]);
                                    processNewBlock();
                                } else {
                                    processBlock(exited);
                                }
                            }
                        }
                        // done
                        return;
                    }
                    case OP_IRETURN: {
                        FunctionType fnType = gf.getCurrentElement().getType();
                        ValueType returnType = fnType.getReturnType();
                        gf.return_(gf.truncate(pop1(), (WordType) returnType));
                        // block complete
                        return;
                    }
                    case OP_FRETURN:
                    case OP_ARETURN:
                        gf.return_(pop1());
                        // block complete
                        return;
                    case OP_LRETURN:
                    case OP_DRETURN:
                        gf.return_(pop2());
                        // block complete
                        return;
                    case OP_RETURN:
                        gf.return_();
                        // block complete
                        return;
                    case OP_GETSTATIC: {
                        // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                        int fieldRef = buffer.getShort() & 0xffff;
                        TypeDescriptor owner = getClassFile().getClassConstantAsDescriptor(getClassFile().getFieldrefConstantClassIndex(fieldRef));
                        TypeDescriptor desc = getDescriptorOfFieldRef(fieldRef);
                        String name = getNameOfFieldRef(fieldRef);
                        // todo: signature context
                        ValueHandle handle = gf.staticField(owner, name, desc);
                        Value value = promote(gf.load(handle, handle.getDetectedMode()), desc);
                        push(value, desc.isClass2());
                        break;
                    }
                    case OP_PUTSTATIC: {
                        // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                        int fieldRef = buffer.getShort() & 0xffff;
                        TypeDescriptor owner = getClassFile().getClassConstantAsDescriptor(getClassFile().getFieldrefConstantClassIndex(fieldRef));
                        TypeDescriptor desc = getDescriptorOfFieldRef(fieldRef);
                        String name = getNameOfFieldRef(fieldRef);
                        ValueHandle handle = gf.staticField(owner, name, desc);
                        gf.store(handle, storeTruncate(pop(desc.isClass2()), desc), handle.getDetectedMode());
                        break;
                    }
                    case OP_GETFIELD: {
                        // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                        int fieldRef = buffer.getShort() & 0xffff;
                        TypeDescriptor owner = getClassFile().getClassConstantAsDescriptor(getClassFile().getFieldrefConstantClassIndex(fieldRef));
                        TypeDescriptor desc = getDescriptorOfFieldRef(fieldRef);
                        String name = getNameOfFieldRef(fieldRef);
                        v1 = pop1();
                        if (v1 instanceof MemberSelector ms) {
                            v1 = ms.getInput();
                            if (v1.getType() instanceof PointerType pt) {
                                ValueHandle member;
                                if (pt.getPointeeType() instanceof CompoundType ct) {
                                    member = gf.memberOf(gf.pointerHandle(v1), ct.getMember(name));
                                } else if (pt.getPointeeType() instanceof PhysicalObjectType) {
                                    member = gf.instanceFieldOf(gf.pointerHandle(v1), owner, name, desc);
                                } else {
                                    ctxt.getCompilationContext().error(gf.getLocation(), "Invalid field dereference of '%s'", name);
                                    throw new BlockEarlyTermination(gf.unreachable());
                                }
                                push(gf.selectMember(gf.addressOf(member)), false);
                            } else {
                                ctxt.getCompilationContext().error(gf.getLocation(), "Invalid field dereference of '%s'", name);
                                throw new BlockEarlyTermination(gf.unreachable());
                            }
                        } else if (v1.getType() instanceof CompoundType ct) {
                            final CompoundType.Member member = ct.getMember(name);
                            push(promote(gf.extractMember(v1, member)), desc.isClass2());
                        } else {
                            ValueHandle handle = gf.instanceFieldOf(gf.referenceHandle(v1), owner, name, desc);
                            Value value = promote(gf.load(handle, handle.getDetectedMode()), desc);
                            push(value, desc.isClass2());
                            if (v1.getType() instanceof ReferenceType) {
                                replaceAll(v1, gf.notNull(v1));
                            }
                        }
                        break;
                    }
                    case OP_PUTFIELD: {
                        // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                        int fieldRef = buffer.getShort() & 0xffff;
                        TypeDescriptor owner = getClassFile().getClassConstantAsDescriptor(getClassFile().getFieldrefConstantClassIndex(fieldRef));
                        TypeDescriptor desc = getDescriptorOfFieldRef(fieldRef);
                        String name = getNameOfFieldRef(fieldRef);
                        v2 = pop(desc.isClass2());
                        v1 = pop1();
                        if (v1.getType() instanceof CompoundType ct) {
                            final CompoundType.Member member = ct.getMember(name);
                            replaceAll(v1, gf.insertMember(v1, member, storeTruncate(v2, desc)));
                        } else {
                            ValueHandle handle = gf.instanceFieldOf(gf.referenceHandle(v1), owner, name, desc);
                            gf.store(handle, storeTruncate(v2, desc), handle.getDetectedMode());
                            if (v1.getType() instanceof ReferenceType) {
                                replaceAll(v1, gf.notNull(v1));
                            }
                        }
                        break;
                    }
                    case OP_INVOKEVIRTUAL:
                    case OP_INVOKESPECIAL:
                    case OP_INVOKESTATIC:
                    case OP_INVOKEINTERFACE: {
                        int methodRef = buffer.getShort() & 0xffff;
                        TypeDescriptor owner = getClassFile().getClassConstantAsDescriptor(getClassFile().getMethodrefConstantClassIndex(methodRef));
                        int nameAndType = getNameAndTypeOfMethodRef(methodRef);
                        if (opcode == OP_INVOKEINTERFACE) {
                            buffer.get(); // discard `count`
                            buffer.get(); // discard 0
                        }
                        if (owner == null) {
                            throw new InvalidConstantException("Method owner is null");
                        }
                        String name = getNameOfMethodRef(methodRef);
                        if (name == null) {
                            throw new InvalidConstantException("Method name is null");
                        }
                        MethodDescriptor desc = (MethodDescriptor) getClassFile().getDescriptorConstant(getClassFile().getNameAndTypeConstantDescriptorIdx(nameAndType));
                        if (desc == null) {
                            throw new InvalidConstantException("Method descriptor is null");
                        }
                        int cnt = desc.getParameterTypes().size();
                        Value[] args = new Value[cnt];
                        for (int i = cnt - 1; i >= 0; i --) {
                            if (desc.getParameterTypes().get(i).isClass2()) {
                                args[i] = pop2();
                            } else {
                                args[i] = pop1();
                            }
                        }
                        if (opcode != OP_INVOKESTATIC) {
                            // pop the receiver
                            v1 = pop1();
                        } else {
                            // definite initialization
                            v1 = null;
                        }
                        if (name.equals("<init>")) {
                            if (opcode != OP_INVOKESPECIAL) {
                                throw new InvalidByteCodeException();
                            }
                            gf.call(gf.constructorOf(v1, owner, desc), List.of(args));
                        } else {
                            TypeDescriptor returnType = desc.getReturnType();
                            ValueHandle handle;
                            if (opcode == OP_INVOKESTATIC) {
                                handle = gf.staticMethod(owner, name, desc);
                            } else if (opcode == OP_INVOKESPECIAL) {
                                handle = gf.exactMethodOf(v1, owner, name, desc);
                            } else if (opcode == OP_INVOKEVIRTUAL) {
                                handle = gf.virtualMethodOf(v1, owner, name, desc);
                            } else {
                                assert opcode == OP_INVOKEINTERFACE;
                                handle = gf.interfaceMethodOf(v1, owner, name, desc);
                            }
                            if (handle.isNoReturn()) {
                                gf.callNoReturn(handle, List.of(args));
                                return;
                            }
                            Value result = handle.isNoSideEffect() ? gf.callNoSideEffects(handle, List.of(args)) : gf.call(handle, List.of(args));
                            if (returnType != BaseTypeDescriptor.V) {
                                push(promote(result, returnType), desc.getReturnType().isClass2());
                            }
                        }
                        if (v1 != null && v1.getType() instanceof ReferenceType) {
                            replaceAll(v1, gf.notNull(v1));
                        }
                        break;
                    }
                    case OP_INVOKEDYNAMIC: {
                        int indyIdx = buffer.getShort() & 0xffff;
                        buffer.getShort(); // discard 0s
                        int bootstrapMethodIdx = getClassFile().getInvokeDynamicBootstrapMethodIndex(indyIdx);
                        int indyNameAndTypeIdx = getClassFile().getInvokeDynamicNameAndTypeIndex(indyIdx);
                        // get the bootstrap handle descriptor
                        MethodHandleConstant bootstrapHandleRaw = getClassFile().getMethodHandleConstant(getClassFile().getBootstrapMethodHandleRef(bootstrapMethodIdx));
                        if (bootstrapHandleRaw == null) {
                            ctxt.getCompilationContext().error(gf.getLocation(), "Missing bootstrap method handle");
                            gf.unreachable();
                            return;
                        }
                        if (! (bootstrapHandleRaw instanceof ExecutableMethodHandleConstant)) {
                            ctxt.getCompilationContext().error(gf.getLocation(), "Wrong bootstrap method handle type");
                            gf.unreachable();
                            return;
                        }
                        ExecutableMethodHandleConstant bootstrapHandle = (ExecutableMethodHandleConstant) bootstrapHandleRaw;
                        // now get the literal method handle, requires a live interpreter
                        VmThread thread = Vm.requireCurrentThread();
                        Vm vm = thread.getVM();
                        VmObject bootstrapHandleObj;
                        try {
                            bootstrapHandleObj = vm.createMethodHandle(ctxt, bootstrapHandle);
                        } catch (InterpreterHaltedException ignored) {
                            throw new BlockEarlyTermination(gf.unreachable());
                        } catch (Thrown thrown) {
                            ctxt.getCompilationContext().warning(gf.getLocation(), "Failed to create bootstrap method handle: %s", thrown);
                            // todo: we should consider putting stack traces into the location of the diagnostics
                            interpLog.debug("Failed to create a bootstrap method handle", thrown);
                            // instead, throw an run time error
                            ClassTypeDescriptor bmeDesc = ClassTypeDescriptor.synthesize(ctxt, "java/lang/BootstrapMethodError");
                            ClassTypeDescriptor thrDesc = ClassTypeDescriptor.synthesize(ctxt, "java/lang/Throwable");
                            Value error = gf.new_(bmeDesc);
                            gf.call(gf.constructorOf(error, bmeDesc, MethodDescriptor.synthesize(ctxt, BaseTypeDescriptor.V, List.of(thrDesc))), List.of(lf.literalOf(thrown.getThrowable())));
                            gf.throw_(error);
                            return;
                        }
                        VmObject callSite;
                        try {
                            // 5.4.3.6, invoking the bootstrap method handle
                            // 1. allocate the array
                            int bootstrapArgCnt = getClassFile().getBootstrapMethodArgumentCount(bootstrapMethodIdx);
                            VmClass objectClass = ctxt.findDefinedType("java/lang/Object").load().getVmClass();
                            VmReferenceArray array = vm.newArrayOf(objectClass, 3 + bootstrapArgCnt);
                            // 1.a. populate first three elements
                            array.store(0, vm.getLookup(gf.getRootElement().getEnclosingType().load().getVmClass()));
                            array.store(1, vm.intern(getClassFile().getNameAndTypeConstantName(indyNameAndTypeIdx)));
                            array.store(2, vm.createMethodType(ctxt, bootstrapHandle.getDescriptor()));
                            // 1.b. populate remaining elements with constant values
                            TypeParameterContext tpc;
                            ExecutableElement rootElement = gf.getRootElement();
                            if (rootElement instanceof TypeParameterContext) {
                                tpc = (TypeParameterContext) rootElement;
                            } else {
                                tpc = rootElement.getEnclosingType();
                            }
                            for (int i = 0; i < bootstrapArgCnt; i ++) {
                                int argIdx = getClassFile().getBootstrapMethodArgumentConstantIndex(bootstrapMethodIdx, i);
                                if (argIdx != 0) {
                                    Literal literal = getClassFile().getConstantValue(argIdx, tpc);
                                    VmObject box = vm.box(ctxt, literal);
                                    array.store(i + 3, box);
                                }
                            }
                            // 2. call bootstrap method via `invokeWithArguments`
                            LoadedTypeDefinition mhDef = ctxt.findDefinedType("java/lang/invoke/MethodHandle").load();
                            int iwaIdx = mhDef.findMethodIndex(me -> me.nameEquals("invokeWithArguments") && me.isVarargs());
                            if (iwaIdx == -1) {
                                throw new IllegalStateException();
                            }
                            MethodElement invokeWithArguments = mhDef.getMethod(iwaIdx);
                            callSite = (VmObject) vm.invokeExact(invokeWithArguments, bootstrapHandleObj, List.of(array));
                        } catch (InterpreterHaltedException ignored) {
                            throw new BlockEarlyTermination(gf.unreachable());
                        } catch (Thrown thrown) {
                            ctxt.getCompilationContext().warning(gf.getLocation(), "Failed to invoke bootstrap method handle: %s", thrown);
                            // todo: we should consider putting stack traces into the location of the diagnostics
                            interpLog.debug("Failed to create a bootstrap method handle", thrown);
                            // instead, throw an run time error
                            ClassTypeDescriptor bmeDesc = ClassTypeDescriptor.synthesize(ctxt, "java/lang/BootstrapMethodError");
                            ClassTypeDescriptor thrDesc = ClassTypeDescriptor.synthesize(ctxt, "java/lang/Throwable");
                            Value error = gf.new_(bmeDesc);
                            gf.call(gf.constructorOf(error, bmeDesc, MethodDescriptor.synthesize(ctxt, BaseTypeDescriptor.V, List.of(thrDesc))), List.of(lf.literalOf(thrown.getThrowable())));
                            gf.throw_(error);
                            return;
                        }
                        ClassTypeDescriptor callSiteDesc = ClassTypeDescriptor.synthesize(ctxt, "java/lang/invoke/CallSite");
                        // Get the method handle instance from the call site
                        ClassTypeDescriptor descOfMethodHandle = ClassTypeDescriptor.synthesize(ctxt, "java/lang/invoke/MethodHandle");
                        Value methodHandle = gf.call(gf.virtualMethodOf(lf.literalOf(callSite),
                            callSiteDesc, "getTarget",
                            MethodDescriptor.synthesize(ctxt, descOfMethodHandle, List.of())),
                            List.of());
                        // Invoke on the method handle
                        MethodDescriptor desc = (MethodDescriptor) getClassFile().getDescriptorConstant(getClassFile().getNameAndTypeConstantDescriptorIdx(indyNameAndTypeIdx));
                        if (desc == null) {
                            ctxt.getCompilationContext().error(gf.getLocation(), "Invoke dynamic has no target method descriptor");
                            gf.unreachable();
                            return;
                        }
                        List<TypeDescriptor> parameterTypes = desc.getParameterTypes();
                        int cnt = parameterTypes.size();
                        Value[] args = new Value[cnt];
                        for (int i = cnt - 1; i >= 0; i--) {
                            args[i] = pop(parameterTypes.get(i).isClass2());
                        }
                        // todo: promote the method handle directly to a ValueHandle?
                        Value result = gf.call(gf.virtualMethodOf(methodHandle, descOfMethodHandle, "invokeExact",
                            desc), List.of(args));
                        TypeDescriptor returnType = desc.getReturnType();
                        if (! returnType.isVoid()) {
                            push(promote(result, returnType), returnType.isClass2());
                        }
                        break;
                    }

                    case OP_NEW: {
                        TypeDescriptor desc = getClassFile().getClassConstantAsDescriptor(buffer.getShort() & 0xffff);
                        if (desc instanceof ClassTypeDescriptor) {
                            push1(gf.new_((ClassTypeDescriptor) desc));
                        } else {
                            ctxt.getCompilationContext().error(gf.getLocation(), "Wrong kind of descriptor for `new`: %s", desc);
                            throw new BlockEarlyTermination(gf.unreachable());
                        }
                        break;
                    }
                    case OP_NEWARRAY:
                        PrimitiveArrayObjectType arrayType;
                        switch (buffer.get() & 0xff) {
                            case T_BOOLEAN: arrayType = ts.getBooleanType().getPrimitiveArrayObjectType(); break;
                            case T_CHAR: arrayType = ts.getUnsignedInteger16Type().getPrimitiveArrayObjectType(); break;
                            case T_FLOAT: arrayType = ts.getFloat32Type().getPrimitiveArrayObjectType(); break;
                            case T_DOUBLE: arrayType = ts.getFloat64Type().getPrimitiveArrayObjectType(); break;
                            case T_BYTE: arrayType = ts.getSignedInteger8Type().getPrimitiveArrayObjectType(); break;
                            case T_SHORT: arrayType = ts.getSignedInteger16Type().getPrimitiveArrayObjectType(); break;
                            case T_INT: arrayType = ts.getSignedInteger32Type().getPrimitiveArrayObjectType(); break;
                            case T_LONG: arrayType = ts.getSignedInteger64Type().getPrimitiveArrayObjectType(); break;
                            default: throw new InvalidByteCodeException();
                        }
                        push1(gf.newArray(arrayType, pop1()));
                        break;
                    case OP_ANEWARRAY: {
                        TypeDescriptor desc = getClassFile().getClassConstantAsDescriptor(buffer.getShort() & 0xffff);
                        push1(gf.newArray(ArrayTypeDescriptor.of(ctxt, desc), pop1()));
                        break;
                    }
                    case OP_ARRAYLENGTH:
                        v1 = pop1();
                        push1(gf.load(gf.lengthOf(gf.referenceHandle(v1)), MemoryAtomicityMode.UNORDERED));
                        if (v1.getType() instanceof ReferenceType) {
                            replaceAll(v1, gf.notNull(v1));
                        }
                        break;
                    case OP_ATHROW:
                        gf.throw_(pop1());
                        // terminate
                        return;
                    case OP_CHECKCAST: {
                        v1 = pop1();
                        Value narrowed = gf.checkcast(v1, getClassFile().getClassConstantAsDescriptor(buffer.getShort() & 0xffff));
                        replaceAll(v1, narrowed);
                        push1(narrowed);
                        break;
                    }
                    case OP_INSTANCEOF: {
                        v1 = pop1();
                        push1(gf.instanceOf(v1, getClassFile().getClassConstantAsDescriptor(buffer.getShort() & 0xffff)));
                        break;
                    }
                    case OP_MONITORENTER:
                        v1 = pop1();
                        gf.monitorEnter(v1);
                        if (v1.getType() instanceof ReferenceType) {
                            replaceAll(v1, gf.notNull(v1));
                        }
                        break;
                    case OP_MONITOREXIT:
                        v1 = pop1();
                        gf.monitorExit(v1);
                        if (v1.getType() instanceof ReferenceType) {
                            replaceAll(v1, gf.notNull(v1));
                        }
                        break;
                    case OP_MULTIANEWARRAY:
                        TypeDescriptor desc = getClassFile().getClassConstantAsDescriptor(buffer.getShort() & 0xffff);
                        Value[] dims = new Value[buffer.get() & 0xff];
                        if (dims.length == 0) {
                            throw new InvalidByteCodeException();
                        }
                        for (int i = dims.length - 1; i >= 0; i --) {
                            dims[i] = pop1();
                        }
                        if (! (desc instanceof ArrayTypeDescriptor)) {
                            throw new InvalidByteCodeException();
                        }
                        push1(gf.multiNewArray((ArrayTypeDescriptor) desc, List.of(dims)));
                        break;
                    case OP_IFNULL: {
                        v1 = pop1();
                        processIf(buffer, gf.isEq(v1, lf.zeroInitializerLiteralOfType(v1.getType())), buffer.getShort() + src, buffer.position());
                        return;
                    }
                    case OP_IFNONNULL: {
                        v1 = pop1();
                        processIf(buffer, gf.isNe(v1, lf.zeroInitializerLiteralOfType(v1.getType())), buffer.getShort() + src, buffer.position());
                        return;
                    }
                    default:
                        throw new InvalidByteCodeException();
                }
                // now check to see if the new position is an entry point
                int epIdx = info.getEntryPointIndex(buffer.position());
                if (epIdx >= 0) {
                    // two or more blocks enter here; start a new block via goto
                    processBlock(gf.goto_(blockHandles[epIdx]));
                    return;
                }
            }
        } catch (BlockEarlyTermination bet) {
            // don't process any more
            return;
        }
    }

    private void saveExitValues(final BasicBlock ours) {
        if (blockExitValues.containsKey(ours)) {
            // already recorded the exit values
            return;
        }
        Map<Slot, Value> exitValues = new HashMap<>();
        for (int i = 0; i < sp; i++) {
            Value value = stack[i];
            if (value != null) {
                exitValues.put(getStackSlot(i), value);
            }
        }
        for (int i = 0; i < locals.length; i++) {
            Value value = locals[i];
            if (value != null) {
                exitValues.put(getVarSlot(i), value);
            }
        }
        blockExitValues.put(ours, exitValues);
    }

    void finish() {
        BasicBlock entryBlock = gf.getFirstBlock();
        entryBlock.getTerminator().accept(new PhiPopulator(), new PhiPopulationContext(ctxt.getCompilationContext(), phiValueSlots, blockExitValues));
    }

    static final class PhiPopulationContext {
        final CompilationContext ctxt;
        final HashSet<Node> visited = new HashSet<>();
        final Map<PhiValue, Slot> phiValueSlots;
        final Map<BasicBlock, Map<Slot, Value>> blockExitValues;

        PhiPopulationContext(CompilationContext ctxt, Map<PhiValue, Slot> phiValueSlots, Map<BasicBlock, Map<Slot, Value>> blockExitValues) {
            this.ctxt = ctxt;
            this.phiValueSlots = phiValueSlots;
            this.blockExitValues = blockExitValues;
        }
    }

    static final class PhiPopulator implements NodeVisitor<PhiPopulationContext, Void, Void, Void, Void> {
        @Override
        public Void visitUnknown(PhiPopulationContext param, Action node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(PhiPopulationContext param, Value node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(PhiPopulationContext param, ValueHandle node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(PhiPopulationContext param, Terminator node) {
            if (visitUnknown(param, (Node) node)) {
                // process reachable successors
                int cnt = node.getSuccessorCount();
                for (int i = 0; i < cnt; i ++) {
                    node.getSuccessor(i).getTerminator().accept(this, param);
                }
            }
            return null;
        }

        boolean visitUnknown(PhiPopulationContext param, Node node) {
            if (param.visited.add(node)) {
                if (node.hasValueHandleDependency()) {
                    node.getValueHandle().accept(this, param);
                }
                int cnt = node.getValueDependencyCount();
                for (int i = 0; i < cnt; i ++) {
                    node.getValueDependency(i).accept(this,param);
                }
                if (node instanceof OrderedNode) {
                    Node dependency = ((OrderedNode) node).getDependency();
                    if (dependency instanceof Action) {
                        ((Action) dependency).accept(this, param);
                    } else if (dependency instanceof Value) {
                        ((Value) dependency).accept(this, param);
                    } else if (dependency instanceof Terminator) {
                        ((Terminator) dependency).accept(this, param);
                    } else if (dependency instanceof ValueHandle) {
                        ((ValueHandle) dependency).accept(this, param);
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public Void visit(PhiPopulationContext param, Invoke.ReturnValue node) {
            visitUnknown(param, node.getInvoke());
            return NodeVisitor.super.visit(param, node);
        }

        @Override
        public Void visit(PhiPopulationContext param, PhiValue node) {
            // phi nodes have no dependencies other than their incoming value
            if (param.visited.add(node)) {
                // make sure everything is transitively reachable
                BasicBlock block = node.getPinnedBlock();
                Slot slot = param.phiValueSlots.get(node);
                if (slot == null) {
                    // this is an already-populated phi
                    for (BasicBlock incomingBlock : block.getIncoming()) {
                        if (incomingBlock.isReachable()) {
                            Value value = incomingBlock.getTerminator().getOutboundValue(node);
                            value.accept(this, param);
                        }
                    }
                } else {
                    for (BasicBlock incomingBlock : block.getIncoming()) {
                        if (incomingBlock.isReachable()) {
                            Map<Slot, Value> exitValues = param.blockExitValues.get(incomingBlock);
                            if (exitValues == null) {
                                param.ctxt.error(node.getElement().getLocation(), "No exit values from reachable block");
                            } else {
                                Value value = exitValues.get(slot);
                                if (value == null) {
                                    param.ctxt.error(node.getElement().getLocation(), "Missing outbound value on reachable phi");
                                } else {
                                    node.setValueForBlock(param.ctxt, node.getElement(), incomingBlock, value);
                                    Terminator terminator = incomingBlock.getTerminator();
                                    terminator.getOutboundValue(node).accept(this, param);
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    private void setJsrExitState(final BasicBlock retBlock, final Value[] saveStack, final Value[] saveLocals) {
        Map<BasicBlock, Value[]> retStacks = this.retStacks;
        if (retStacks == null) {
            retStacks = this.retStacks = new HashMap<>();
        }
        retStacks.put(retBlock, saveStack);
        Map<BasicBlock, Value[]> retLocals = this.retLocals;
        if (retLocals == null) {
            retLocals = this.retLocals = new HashMap<>();
        }
        retLocals.put(retBlock, saveLocals);
    }

    private void processIf(final ByteBuffer buffer, final Value cond, final int dest1, final int dest2) {
        BlockLabel b1 = getBlockForIndexIfExists(dest1);
        boolean b1s = b1 == null;
        if (b1s) {
            b1 = new BlockLabel();
        }
        BlockLabel b2 = getBlockForIndexIfExists(dest2);
        boolean b2s = b2 == null;
        if (b2s) {
            b2 = new BlockLabel();
        }
        BasicBlock from = gf.if_(cond, b1, b2);
        Value[] varSnap = saveLocals();
        Value[] stackSnap = saveStack();
        buffer.position(dest1);
        if (b1s) {
            gf.begin(b1);
            processNewBlock();
        } else {
            processBlock(from);
        }
        restoreStack(stackSnap);
        restoreLocals(varSnap);
        buffer.position(dest2);
        if (b2s) {
            gf.begin(b2);
            processNewBlock();
        } else {
            processBlock(from);
        }
    }

    Value promote(Value value, TypeDescriptor desc) {
        if (desc instanceof BaseTypeDescriptor && desc != BaseTypeDescriptor.V) {
            return promote(value);
        }
        // no promote necessary
        return value;
    }

    Value promote(Value value) {
        ValueType type = value.getType();
        if (type instanceof IntegerType && ((IntegerType) type).getMinBits() < 32) {
            // extend
            if (type instanceof UnsignedIntegerType) {
                // extend and cast
                return gf.bitCast(gf.extend(value, ts.getUnsignedInteger32Type()), ts.getSignedInteger32Type());
            } else {
                // just extend
                return gf.extend(value, ts.getSignedInteger32Type());
            }
        } else if (type instanceof BooleanType) {
            return gf.extend(value, ts.getSignedInteger32Type());
        }
        // no promote necessary
        return value;
    }

    private Value storeTruncate(Value v, TypeDescriptor desc) {
        if (desc.equals(BaseTypeDescriptor.Z)) {
            return gf.truncate(v, ts.getBooleanType());
        } else if (desc.equals(BaseTypeDescriptor.B)) {
            return gf.truncate(v, ts.getSignedInteger8Type());
        } else if (desc.equals(BaseTypeDescriptor.C)) {
            return gf.truncate(v, ts.getUnsignedInteger16Type());
        } else if (desc.equals(BaseTypeDescriptor.S)) {
            return gf.truncate(v, ts.getSignedInteger16Type());
        } else {
            return v;
        }
    }

    private ClassFileImpl getClassFile() {
        return info.getClassFile();
    }

    private TypeDescriptor getDescriptorOfFieldRef(final int fieldRef) {
        return (TypeDescriptor) getClassFile().getDescriptorConstant(getClassFile().getFieldrefConstantDescriptorIdx(fieldRef));
    }

    private String getNameOfFieldRef(final int fieldRef) {
        ClassFileImpl classFile = getClassFile();
        return classFile.getNameAndTypeConstantName(classFile.getFieldrefNameAndTypeIndex(fieldRef));
    }

    private String getNameOfMethodRef(final int methodRef) {
        return getClassFile().getMethodrefConstantName(methodRef);
    }

    private int getNameAndTypeOfMethodRef(final int methodRef) {
        return getClassFile().getMethodrefNameAndTypeIndex(methodRef);
    }

    private static int getWidenableValue(final ByteBuffer buffer, final boolean wide) {
        return wide ? buffer.getShort() & 0xffff : buffer.get() & 0xff;
    }

    private static int getWidenableValueSigned(final ByteBuffer buffer, final boolean wide) {
        return wide ? buffer.getShort() : buffer.get();
    }

    class RetVisitor implements TerminatorVisitor<Void, Void> {
        private final Set<BasicBlock> visited = new HashSet<>();
        private final ByteBuffer buffer;
        private final int ret;

        RetVisitor(final ByteBuffer buffer, final int ret) {
            this.buffer = buffer;
            this.ret = ret;
        }

        void handleBlock(final BasicBlock block) {
            if (visited.add(block)) {
                if (block.getTerminator() instanceof Ret) {
                    // goto the return block
                    restoreLocals(retLocals.get(block));
                    restoreStack(retStacks.get(block));
                    buffer.position(ret);
                    processBlock(block);
                } else {
                    block.getTerminator().accept(this, null);
                }
            }
        }

        public Void visitUnknown(final Void param, final Terminator node) {
            int s = node.getSuccessorCount();
            for (int i = 0; i < s; i ++) {
                handleBlock(node.getSuccessor(i));
            }
            return null;
        }
    }
}
