package org.qbicc.type.definition.classfile;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;
import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;
import static org.qbicc.type.definition.classfile.ClassFile.*;
import static org.qbicc.type.definition.classfile.ClassMethodInfo.align;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.smallrye.common.constraint.Assert;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.qbicc.context.ClassContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Dereference;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.Auto;
import org.qbicc.graph.literal.GlobalVariableLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.StructType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnionType;
import org.qbicc.type.UnresolvedType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.ParameterElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;
import org.qbicc.type.methodhandle.MethodHandleConstant;
import org.qbicc.type.methodhandle.MethodMethodHandleConstant;

final class MethodParser {
    final ClassMethodInfo info;
    final Value[] stack;
    final Value[] locals;
    final BlockLabel[] blockHandles;
    final boolean[] created;
    final ByteBuffer buffer;
    private final BasicBlockBuilder gf;
    private final ClassContext ctxt;
    private final LiteralFactory lf;
    private final TypeSystem ts;
    private final DefinedTypeDefinition jlo;
    private final LocalVariableElement[][] varsByTableEntry;
    int sp;
    private ValueType[][] varTypesByEntryPoint;
    private ValueType[][] stackTypesByEntryPoint;
    Map<LocalVariableElement, Value> stackAllocatedValues;

    MethodParser(final ClassContext ctxt, final ClassMethodInfo info, final ByteBuffer buffer, final ExecutableElement element) {
        this.ctxt = ctxt;
        lf = ctxt.getLiteralFactory();
        ts = ctxt.getTypeSystem();
        this.info = info;
        stack = new Value[info.getMaxStack()];
        int maxLocals = info.getMaxLocals();
        locals = new Value[maxLocals];
        this.buffer = buffer;
        int cnt = info.getEntryPointCount();
        BlockLabel[] blockHandles = new BlockLabel[cnt];
        // make a "canonical" node handle for each block
        for (int i = 0; i < cnt; i ++) {
            blockHandles[i] = new BlockLabel();
        }
        this.blockHandles = blockHandles;
        created = new boolean[blockHandles.length];
        // it's not an entry point
        jlo = ctxt.findDefinedType("java/lang/Object");
        List<ParameterElement> parameters;
        if (element instanceof InvokableElement ie) {
            parameters = ie.getParameters();
        } else {
            parameters = List.of();
        }
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
                LocalVariableElement.Builder builder = LocalVariableElement.builder(name, typeDescriptor, slot);
                int startPc = info.getLocalVarStartPc(slot, entry);
                if (startPc == 0) {
                    int offset = 0;
                    if (! element.isStatic()) {
                        offset = 1;
                    }
                    if (slot >= offset) {
                        int pos = offset;
                        for (ParameterElement parameter : parameters) {
                            if (pos == slot) {
                                builder.setReflectsParameter(parameter);
                                break;
                            }
                            if (parameter.getTypeDescriptor().isClass2()) {
                                pos += 2;
                            } else {
                                pos++;
                            }
                        }
                    }
                }
                builder.setBci(startPc);
                builder.setLine(info.getLineNumber(startPc));
                builder.setEnclosingType(element.getEnclosingType());
                builder.setTypeParameterContext(element.getTypeParameterContext());
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
        BasicBlockBuilder.FactoryContext fc = BasicBlockBuilder.FactoryContext.withInfo(BasicBlockBuilder.FactoryContext.EMPTY, MethodParser.class, this);
        BasicBlockBuilder graphFactory = element.getEnclosingType().getContext().newBasicBlockBuilder(fc, element);
        gf = new DelegatingBasicBlockBuilder(graphFactory) {
            @Override
            public <T> BasicBlock begin(BlockLabel blockLabel, T arg, BiConsumer<T, BasicBlockBuilder> maker) {
                int pos = buffer.position();
                int sp = MethodParser.this.sp;
                Value[] outerLocals = MethodParser.this.locals;
                Value[] outerStack = MethodParser.this.stack;
                Value[] stack = sp == 0 ? Value.NO_VALUES : Arrays.copyOf(outerStack, sp);
                Value[] locals = outerLocals.clone();
                try {
                    return super.begin(blockLabel, arg, maker);
                } finally {
                    System.arraycopy(locals, 0, outerLocals, 0, locals.length);
                    System.arraycopy(stack, 0, outerStack, 0, sp);
                    int oldSp = MethodParser.this.sp;
                    if (oldSp > sp) {
                        Arrays.fill(outerStack, sp, oldSp, null);
                    }
                    MethodParser.this.sp = sp;
                    buffer.position(pos);
                }
            }

            @Override
            public int setBytecodeIndex(int bci) {
                setLineNumber(info.getLineNumber(bci));
                return super.setBytecodeIndex(bci);
            }
        };
        this.varsByTableEntry = varsByTableEntry;
    }

    public BasicBlockBuilder getBlockBuilder() {
        return gf;
    }

    void setTypeInformation(final ValueType[][] varTypesByEntryPoint, final ValueType[][] stackTypesByEntryPoint) {
        this.varTypesByEntryPoint = varTypesByEntryPoint;
        this.stackTypesByEntryPoint = stackTypesByEntryPoint;
    }

    ClassMethodInfo getClassMethodInfo() {
        return info;
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
        if (value instanceof Auto al) {
            LocalVariableElement lve = getLocalVariableElement(bci, index);
            if (lve != null) {
                Value init = al.getInitializer();
                ValueType varType = init.getType();
                if (init.getType().equals(ts.getVoidType())) {
                    varType = lve.getType();
                    init = lf.zeroInitializerLiteralOfType(varType);
                }
                Value allocated = gf.stackAllocate(varType, lf.literalOf(1), lf.literalOf(Math.max(varType.getAlign(), lve.getMinimumAlignment())));
                gf.store(allocated, init, SingleUnshared);
                Map<LocalVariableElement, Value> sav = stackAllocatedValues;
                if (sav == null) {
                    stackAllocatedValues = sav = new HashMap<>();
                }
                sav.put(lve, allocated);
                gf.declareDebugAddress(lve, allocated);
            } else {
                ctxt.getCompilationContext().error(gf.getLocation(), "No local variable declaration for auto() initialized variable");
            }
            locals[index] = null;
            locals[index + 1] = null;
        } else {
            // dereference, if needed
            if (value instanceof Dereference d) {
                value = gf.load(d.getPointer(), SingleUnshared);
            }
            LocalVariableElement lve = getLocalVariableElement(bci, index);
            if (lve != null) {
                // make sure the value can be stored
                if (lve.getType() instanceof WordType wt && ! wt.equals(value.getType())) {
                    value = gf.bitCast(value, wt);
                }
                Map<LocalVariableElement, Value> sav = stackAllocatedValues;
                if (sav != null) {
                    Value ptr = sav.get(lve);
                    if (ptr != null) {
                        gf.store(ptr, value, SingleUnshared);
                        return;
                    }
                } else {
                    gf.setDebugValue(lve, value);
                }
            }
            locals[index] = value;
            locals[index + 1] = null;
        }
    }

    void setLocal1(int index, Value value, int bci) {
        if (value instanceof Auto al) {
            LocalVariableElement lve = getLocalVariableElement(bci, index);
            if (lve != null) {
                Value init = al.getInitializer();
                ValueType varType = init.getType();
                if (init.getType().equals(ts.getVoidType())) {
                    varType = lve.getType();
                    init = lf.zeroInitializerLiteralOfType(varType);
                }
                Value allocated = gf.stackAllocate(varType, lf.literalOf(1), lf.literalOf(Math.max(varType.getAlign(), lve.getMinimumAlignment())));
                gf.store(allocated, storeTruncate(init, lve.getTypeDescriptor()), SingleUnshared);
                Map<LocalVariableElement, Value> sav = stackAllocatedValues;
                if (sav == null) {
                    stackAllocatedValues = sav = new HashMap<>();
                }
                sav.put(lve, allocated);
                gf.declareDebugAddress(lve, allocated);
                locals[index] = null;
            } else {
                ctxt.getCompilationContext().error(gf.getLocation(), "No local variable declaration for auto() initialized variable");
                locals[index] = value;
            }
        } else {
            // dereference, if needed
            if (value instanceof Dereference d) {
                value = gf.load(d.getPointer(), SingleUnshared);
            }
            LocalVariableElement lve = getLocalVariableElement(bci, index);
            if (lve != null) {
                Value truncated = storeTruncate(value, lve.getTypeDescriptor());
                // make sure the value can be stored
                if (lve.getType() instanceof WordType wt && ! wt.equals(truncated.getType())) {
                    truncated = gf.bitCast(truncated, wt);
                }
                Map<LocalVariableElement, Value> sav = stackAllocatedValues;
                if (sav != null) {
                    Value ptr = sav.get(lve);
                    if (ptr != null) {
                        gf.store(ptr, truncated, SingleUnshared);
                        return;
                    }
                }
                gf.setDebugValue(lve, truncated);
                locals[index] = truncated;
            } else {
                locals[index] = value;
            }
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
        Value value;
        if (lve != null) {
            Map<LocalVariableElement, Value> sav = stackAllocatedValues;
            if (sav != null) {
                Value ptr = sav.get(lve);
                if (ptr != null) {
                    ValueType lveType = lve.getType();
                    if (lveType instanceof StructType || lveType instanceof ArrayType) {
                        // return a *handle* to the variable
                        // (never needs promotion)
                        return gf.deref(ptr);
                    } else {
                        return promote(gf.load(ptr, SingleUnshared), lve.getTypeDescriptor());
                    }
                }
            }
            value = promote(locals[index], lve.getTypeDescriptor());
        } else {
            value = promote(locals[index]);
        }
        if (value == null) {
            throw new IllegalStateException("Invalid get local (no value)");
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
            if (type instanceof UnresolvedType) {
                ClassTypeDescriptor cd = ClassTypeDescriptor.synthesize(ctxt, "java/lang/NoClassDefFoundError");
                Value ncdfe = gf.new_(cd);
                gf.call(gf.resolveConstructor(cd, MethodDescriptor.VOID_METHOD_DESCRIPTOR), ncdfe, List.of());
                throw new BlockEarlyTermination(gf.throw_(ncdfe));
            }
            if (type instanceof ReferenceArrayObjectType) {
                literal = convertToBaseTypeLiteral((ReferenceArrayObjectType)type);
                dims = ((ReferenceArrayObjectType)type).getDimensionCount();
            }
            return gf.classOf(literal, ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), dims));
        } else {
            return literal;
        }
    }

    BlockLabel getOrCreateBlockForIndex(int target) {
        BlockLabel block = getBlockForIndex(target);
        if (setCreated(target)) {
            gf.begin(block, this, (mp, gf) -> {
                mp.buffer.position(target);
                gf.setBytecodeIndex(target);
                setUpNewBlock(target, block);
                processNewBlock();
            });
        }
        return block;
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

    boolean setCreated(int target) {
        int idx = info.getEntryPointIndex(target);
        if (idx >= 0) {
            if (created[idx]) {
                return false;
            } else {
                created[idx] = true;
                return true;
            }
        }
        return false;
    }

    Value replaceAll(Value from, Value to) {
        if (from == to) {
            return to;
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
        return to;
    }

    public Map<Slot, Value> captureOutbound() {
        Value[] stack = this.stack;
        Value[] locals = this.locals;
        int sp = this.sp;
        int localsCnt = locals.length;
        MutableMap<Slot, Value> map = Maps.mutable.ofInitialCapacity(sp + localsCnt);
        Value value;
        for (int i = 0; i < sp; i ++) {
            value = stack[i];
            if (value != null) {
                map.put(Slot.stack(i), value);
            }
        }
        for (int i = 0; i < localsCnt; i ++) {
            value = locals[i];
            if (value != null) {
                map.put(Slot.variable(i), value);
            }
        }
        return map.toImmutable().castToMap();
    }

    void doSaved(Runnable task) {
        doSaved(Runnable::run, task);
    }

    <T> void doSaved(Consumer<T> task, T param) {
        doSaved(Consumer::accept, task, param);
    }

    /**
     * Perform an action and restore the stack & locals state after it completes.
     *
     * @param task the task to run (must not be {@code null})
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     */
    <T, U> void doSaved(BiConsumer<T, U> task, T param1, U param2) {
        Value[] stack = Arrays.copyOf(this.stack, sp);
        Value[] locals = this.locals.clone();
        try {
            task.accept(param1, param2);
        } finally {
            if (sp > stack.length) {
                Arrays.fill(this.stack, sp, this.stack.length, null);
            }
            System.arraycopy(stack, 0, this.stack, 0, stack.length);
            sp = stack.length;
            System.arraycopy(locals, 0, this.locals, 0, locals.length);
        }
    }

    /**
     * The set of blocks whose bytecode has been processed.
     */
    final Set<BlockLabel> visited = new HashSet<>();

    /**
     * Process a single block.
     */
    void processBlock() {
        ByteBuffer buffer = this.buffer;
        // this is the canonical map key handle
        int bci = buffer.position();
        BlockLabel block = getBlockForIndex(bci);
        if (visited.add(block)) {
            // not registered yet; process new block first
            gf.setBytecodeIndex(bci);
            gf.begin(block);
            setUpNewBlock(bci, block);
            processNewBlock();
        }
    }

    private final Set<BlockLabel> setUpBlocks = new HashSet<>();

    private void setUpNewBlock(final int bci, final BlockLabel block) {
        if (!setUpBlocks.add(block)) {
            throw new IllegalStateException("Set up twice");
        }
        int epIdx = info.getEntryPointIndex(bci);
        if (epIdx < 0) {
            throw new IllegalStateException("No entry point for block at bci " + bci);
        }
        ValueType[] varTypes = varTypesByEntryPoint[epIdx];
        for (int i = 0; i < locals.length && i < varTypes.length; i++) {
            ValueType varType = varTypes[i];
            if (varType != null) {
                LocalVariableElement lve = getLocalVariableElement(bci, i);
                if (lve != null) {
                    Map<LocalVariableElement, Value> sav = stackAllocatedValues;
                    if (sav != null) {
                        Value ptr = sav.get(lve);
                        if (ptr != null) {
                            // special case: stack-allocated variable
                            locals[i] = null;
                            continue;
                        }
                    }
                    BlockParameter bp = gf.addParam(block, Slot.variable(i), lve.getType());
                    gf.setDebugValue(lve, bp);
                    locals[i] = bp;
                } else {
                    locals[i] = gf.addParam(block, Slot.variable(i), varType);
                }
            } else {
                locals[i] = null;
            }
        }
        ValueType[] stackTypes = stackTypesByEntryPoint[epIdx];
        clearStack();
        for (int i = 0; i < stackTypes.length; i++) {
            ValueType stackType = stackTypes[i];
            if (stackType != null) {
                stack[i] = gf.addParam(block, Slot.stack(i), stackType);
            }
        }
        sp = stackTypes.length;
    }

    void processNewBlock() {
        ByteBuffer buffer = this.buffer;
        BasicBlockBuilder gf = this.gf;
        Value v1, v2, v3, v4;
        int opcode;
        int src;
        boolean wide;
        ClassMethodInfo info = this.info;
        try {
            while (buffer.hasRemaining()) {
                src = buffer.position();
                gf.setBytecodeIndex(src);
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
                        if (v1 instanceof Dereference deref) {
                            v1 = gf.deref(gf.elementOf(deref.getPointer(), v2));
                        } else if (v1.getType() instanceof ArrayType) {
                            v1 = gf.extractElement(v1, v2);
                        } else if (v1.getType() instanceof PointerType) {
                            v1 = gf.deref(gf.offsetPointer(v1, v2));
                        } else {
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                            v1 = gf.load(gf.elementOf(gf.decodeReference(v1), v2));
                        }
                        push1(v1);
                        break;
                    }
                    case OP_DALOAD:
                    case OP_LALOAD: {
                        v2 = pop1();
                        v1 = pop1();
                        if (v1 instanceof Dereference deref) {
                            v1 = gf.deref(gf.elementOf(deref.getPointer(), v2));
                        } else if (v1.getType() instanceof ArrayType) {
                            v1 = gf.extractElement(v1, v2);
                        } else if (v1.getType() instanceof PointerType) {
                            v1 = gf.deref(gf.offsetPointer(v1, v2));
                        } else {
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                            v1 = gf.load(gf.elementOf(gf.decodeReference(v1), v2));
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
                        if (v1 instanceof Dereference deref) {
                            v1 = gf.deref(gf.elementOf(deref.getPointer(), v2));
                        } else if (v1.getType() instanceof ArrayType) {
                            v1 = promote(gf.extractElement(v1, v2));
                        } else if (v1.getType() instanceof PointerType) {
                            v1 = gf.deref(gf.offsetPointer(v1, v2));
                        } else {
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                            v1 = promote(gf.load(gf.elementOf(gf.decodeReference(v1), v2)));
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
                        if (v1 instanceof Dereference deref) {
                            gf.store(gf.elementOf(deref.getPointer(), v2), v3, SinglePlain);
                        } else if (v1 instanceof Auto auto) {
                            replaceAll(v1, gf.auto(gf.insertElement(auto.getInitializer(), v2, v3)));
                        } else if (v1.getType() instanceof ArrayType) {
                            replaceAll(v1, gf.insertElement(v1, v2, v3));
                        } else if (v1.getType() instanceof PointerType) {
                            gf.store(gf.offsetPointer(v1, v2), v3, SingleUnshared);
                        } else {
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                            gf.store(gf.elementOf(gf.decodeReference(v1), v2), v3);
                        }
                        break;
                    case OP_BASTORE:
                        v3 = gf.truncate(pop1(), ts.getSignedInteger8Type());
                        v2 = pop1();
                        v1 = pop1();
                        if (v1 instanceof Dereference deref) {
                            gf.store(gf.elementOf(deref.getPointer(), v2), v3, SinglePlain);
                        } else if (v1 instanceof Auto auto) {
                            replaceAll(v1, gf.auto(gf.insertElement(auto.getInitializer(), v2, v3)));
                        } else if (v1.getType() instanceof ArrayType) {
                            replaceAll(v1, gf.insertElement(v1, v2, v3));
                        } else if (v1.getType() instanceof PointerType) {
                            gf.store(gf.offsetPointer(v1, v2), v3, SingleUnshared);
                        } else {
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                            gf.store(gf.elementOf(gf.decodeReference(v1), v2), v3);
                        }
                        break;
                    case OP_SASTORE:
                        v3 = gf.truncate(pop1(), ts.getSignedInteger16Type());
                        v2 = pop1();
                        v1 = pop1();
                        if (v1 instanceof Dereference deref) {
                            gf.store(gf.elementOf(deref.getPointer(), v2), v3, SinglePlain);
                        } else if (v1 instanceof Auto auto) {
                            replaceAll(v1, gf.auto(gf.insertElement(auto.getInitializer(), v2, v3)));
                        } else if (v1.getType() instanceof ArrayType) {
                            replaceAll(v1, gf.insertElement(v1, v2, v3));
                        } else if (v1.getType() instanceof PointerType) {
                            gf.store(gf.offsetPointer(v1, v2), v3, SingleUnshared);
                        } else {
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                            gf.store(gf.elementOf(gf.decodeReference(v1), v2), v3);
                        }
                        break;
                    case OP_CASTORE:
                        v3 = gf.truncate(pop1(), ts.getUnsignedInteger16Type());
                        v2 = pop1();
                        v1 = pop1();
                        if (v1 instanceof Dereference deref) {
                            gf.store(gf.elementOf(deref.getPointer(), v2), v3, SinglePlain);
                        } else if (v1 instanceof Auto auto) {
                            replaceAll(v1, gf.auto(gf.insertElement(auto.getInitializer(), v2, v3)));
                        } else if (v1.getType() instanceof ArrayType) {
                            replaceAll(v1, gf.insertElement(v1, v2, v3));
                        } else if (v1.getType() instanceof PointerType) {
                            gf.store(gf.offsetPointer(v1, v2), v3, SingleUnshared);
                        } else {
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                            gf.store(gf.elementOf(gf.decodeReference(v1), v2), v3);
                        }
                        break;
                    case OP_LASTORE:
                    case OP_DASTORE:
                        v3 = pop2();
                        v2 = pop1();
                        v1 = pop1();
                        if (v1 instanceof Dereference deref) {
                            gf.store(gf.elementOf(deref.getPointer(), v2), v3, SinglePlain);
                        } else if (v1 instanceof Auto auto) {
                            replaceAll(v1, gf.auto(gf.insertElement(auto.getInitializer(), v2, v3)));
                        } else if (v1.getType() instanceof ArrayType) {
                            replaceAll(v1, gf.insertElement(v1, v2, v3));
                        } else if (v1.getType() instanceof PointerType) {
                            gf.store(gf.offsetPointer(v1, v2), v3, SingleUnshared);
                        } else {
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                            gf.store(gf.elementOf(gf.decodeReference(v1), v2), v3);
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
                        push1(gf.divide(v1, replaceAll(v2, gf.divisorCheck(v2))));
                        break;
                    case OP_LDIV:
                    case OP_DDIV:
                        v2 = pop2();
                        v1 = pop2();
                        push2(gf.divide(v1, replaceAll(v2, gf.divisorCheck(v2))));
                        break;
                    case OP_IREM:
                    case OP_FREM:
                        v2 = pop1();
                        v1 = pop1();
                        push1(gf.remainder(v1, replaceAll(v2, gf.divisorCheck(v2))));
                        break;
                    case OP_LREM:
                    case OP_DREM:
                        v2 = pop2();
                        v1 = pop2();
                        push2(gf.remainder(v1, replaceAll(v2, gf.divisorCheck(v2))));
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
                        processGoto(buffer, src + (opcode == OP_GOTO ? buffer.getShort() : buffer.getInt()));
                        return;
                    }
                    case OP_JSR:
                    case OP_JSR_W: {
                        int target = src + (opcode == OP_JSR ? buffer.getShort() : buffer.getInt());
                        int ret = buffer.position();
                        // jsr destination
                        BlockLabel dest = getBlockForIndexIfExists(target);
                        // push return address
                        BlockLabel retBlock = getBlockForIndex(ret);
                        push1(lf.literalOf(retBlock));
                        if (dest == null) {
                            // only called from one site
                            dest = new BlockLabel();
                            gf.goto_(dest, captureOutbound());
                            buffer.position(target);
                            gf.begin(dest);
                            processNewBlock();
                        } else {
                            // the jsr call
                            gf.goto_(dest, captureOutbound());
                            // recursively process subroutine
                            buffer.position(target);
                            processBlock();
                        }
                        // continue on from return point
                        gf.begin(retBlock);
                        processBlock();
                        return;
                    }
                    case OP_RET:
                        // return address is on stack
                        gf.ret(pop1(), captureOutbound());
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
                        gf.switch_(pop1(), vals, handles, defaultBlock, captureOutbound());
                        buffer.position(db + src);
                        if (defaultSingle) {
                            gf.begin(defaultBlock);
                            doSaved(MethodParser::processNewBlock, this);
                        } else {
                            doSaved(MethodParser::processBlock, this);
                        }
                        for (int i = 0; i < handles.length; i++) {
                            if (seen.add(handles[i])) {
                                buffer.position(dests[i]);
                                if (singles[i]) {
                                    gf.begin(handles[i]);
                                    doSaved(MethodParser::processNewBlock, this);
                                } else {
                                    doSaved(MethodParser::processBlock, this);
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
                        gf.switch_(pop1(), vals, handles, defaultBlock, captureOutbound());
                        buffer.position(db + src);
                        if (defaultSingle) {
                            gf.begin(defaultBlock);
                            doSaved(MethodParser::processNewBlock, this);
                        } else {
                            doSaved(MethodParser::processBlock, this);
                        }
                        for (int i = 0; i < handles.length; i++) {
                            if (seen.add(handles[i])) {
                                buffer.position(dests[i]);
                                if (singles[i]) {
                                    gf.begin(handles[i]);
                                    doSaved(MethodParser::processNewBlock, this);
                                } else {
                                    doSaved(MethodParser::processBlock, this);
                                }
                            }
                        }
                        // done
                        return;
                    }
                    case OP_IRETURN: {
                        InvokableType fnType = gf.getCurrentElement().getType();
                        ValueType returnType = fnType.getReturnType();
                        gf.return_(gf.truncate(pop1(), (WordType) returnType));
                        // block complete
                        return;
                    }
                    case OP_FRETURN: {
                        gf.return_(pop1());
                        // block complete
                        return;
                    }
                    case OP_ARETURN: {
                        v1 = pop1();
                        if (v1 instanceof Literal lit && lit.isZero()) {
                            InvokableType fnType = gf.getCurrentElement().getType();
                            ValueType returnType = fnType.getReturnType();
                            gf.return_(lf.zeroInitializerLiteralOfType(returnType));
                        } else {
                            gf.return_(v1);
                        }
                        // block complete
                        return;
                    }
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
                        Value handle = gf.resolveStaticField(owner, name, desc);
                        Value value;
                        if (handle instanceof GlobalVariableLiteral || handle instanceof ProgramObjectLiteral || handle.getPointeeType() instanceof StructType) {
                            if (desc == BaseTypeDescriptor.B || desc == BaseTypeDescriptor.C || desc == BaseTypeDescriptor.S || desc == BaseTypeDescriptor.Z) {
                                ctxt.getCompilationContext().warning(gf.getLocation(), "Type promotion to `int` causes an eager load");
                            }
                            value = promote(gf.deref(handle), desc);
                        } else {
                            value = promote(gf.load(handle, handle.getDetectedMode().getReadAccess()), desc);
                        }
                        push(value, desc.isClass2());
                        break;
                    }
                    case OP_PUTSTATIC: {
                        // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                        int fieldRef = buffer.getShort() & 0xffff;
                        TypeDescriptor owner = getClassFile().getClassConstantAsDescriptor(getClassFile().getFieldrefConstantClassIndex(fieldRef));
                        TypeDescriptor desc = getDescriptorOfFieldRef(fieldRef);
                        String name = getNameOfFieldRef(fieldRef);
                        Value handle = gf.resolveStaticField(owner, name, desc);
                        gf.store(handle, storeTruncate(pop(desc.isClass2()), desc), handle.getDetectedMode().getWriteAccess());
                        break;
                    }
                    case OP_GETFIELD: {
                        // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                        int fieldRef = buffer.getShort() & 0xffff;
                        TypeDescriptor owner = getClassFile().getClassConstantAsDescriptor(getClassFile().getFieldrefConstantClassIndex(fieldRef));
                        TypeDescriptor desc = getDescriptorOfFieldRef(fieldRef);
                        String name = getNameOfFieldRef(fieldRef);
                        v1 = pop1();
                        if (v1 instanceof Dereference deref) {
                            Value pointer = deref.getPointer();
                            ValueType valueType = pointer.getPointeeType();
                            if (valueType instanceof StructType st) {
                                push1(gf.deref(gf.memberOf(pointer, st.getMember(name))));
                            } else if (valueType instanceof UnionType ut) {
                                push1(gf.deref(gf.memberOfUnion(pointer,  ut.getMember(name))));
                            } else if (valueType instanceof PhysicalObjectType) {
                                push1(gf.deref(gf.instanceFieldOf(pointer, owner, name, desc)));
                            } else {
                                ctxt.getCompilationContext().error(gf.getLocation(), "Invalid field dereference of '%s'", name);
                                throw new BlockEarlyTermination(gf.unreachable());
                            }
                        } else if (v1.getType() instanceof PointerType pt && pt.getPointeeType() instanceof StructType st) {
                            // always a dereference
                            push1(gf.deref(gf.memberOf(v1, st.getMember(name))));
                        } else if (v1.getType() instanceof PointerType pt && pt.getPointeeType() instanceof UnionType ut) {
                            // always a dereference
                            push1(gf.deref(gf.memberOfUnion(v1, ut.getMember(name))));
                        } else if (v1.getType() instanceof StructType st) {
                            final StructType.Member member = st.getMember(name);
                            push(promote(gf.extractMember(v1, member)), desc.isClass2());
                        } else {
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                            Value handle = gf.instanceFieldOf(gf.decodeReference(v1), owner, name, desc);
                            Value value = promote(gf.load(handle, handle.getDetectedMode().getReadAccess()), desc);
                            push(value, desc.isClass2());
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
                        if (v1 instanceof Dereference deref) {
                            Value pointer = deref.getPointer();
                            ValueType valueType = pointer.getPointeeType();
                            if (valueType instanceof StructType st) {
                                gf.store(gf.memberOf(pointer, st.getMember(name)), storeTruncate(v2, desc), SinglePlain);
                            } else if (valueType instanceof UnionType ut) {
                                gf.store(gf.memberOfUnion(pointer, ut.getMember(name)), storeTruncate(v2, desc), SinglePlain);
                            } else if (valueType instanceof PhysicalObjectType) {
                                gf.store(gf.instanceFieldOf(pointer, owner, name, desc), storeTruncate(v2, desc), SinglePlain);
                            } else {
                                ctxt.getCompilationContext().error(gf.getLocation(), "Invalid field dereference of '%s'", name);
                                throw new BlockEarlyTermination(gf.unreachable());
                            }
                        } else if (v1.getType() instanceof StructType st) {
                            final StructType.Member member = st.getMember(name);
                            replaceAll(v1, gf.insertMember(v1, member, storeTruncate(v2, desc)));
                        } else {
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                            Value handle = gf.instanceFieldOf(gf.decodeReference(v1), owner, name, desc);
                            gf.store(handle, storeTruncate(v2, desc), handle.getDetectedMode().getWriteAccess());
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
                            v1 = replaceAll(v1, gf.nullCheck(v1));
                        } else {
                            // definite initialization
                            v1 = gf.emptyVoid();
                        }
                        if (name.equals("<init>")) {
                            if (opcode != OP_INVOKESPECIAL) {
                                throw new InvalidByteCodeException();
                            }
                            gf.call(gf.resolveConstructor(owner, desc), v1, List.of(demote(args, desc)));
                        } else {
                            TypeDescriptor returnType = desc.getReturnType();
                            Value handle;
                            if (opcode == OP_INVOKESTATIC) {
                                handle = gf.resolveStaticMethod(owner, name, desc);
                            } else if (opcode == OP_INVOKESPECIAL) {
                                handle = gf.resolveInstanceMethod(owner, name, desc);
                            } else if (opcode == OP_INVOKEVIRTUAL) {
                                handle = gf.lookupVirtualMethod(v1, owner, name, desc);
                            } else {
                                assert opcode == OP_INVOKEINTERFACE;
                                handle = gf.lookupInterfaceMethod(v1, owner, name, desc);
                            }
                            if (handle.isNoReturn()) {
                                gf.callNoReturn(handle, v1, List.of(demote(args, desc)));
                                return;
                            }
                            Value result = handle.isNoSideEffect() ? gf.callNoSideEffects(handle, v1, List.of(demote(args, desc))) : gf.call(handle, v1, List.of(demote(args, desc)));
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
                            throw new BlockEarlyTermination(gf.unreachable());
                        }
                        if (! (bootstrapHandleRaw instanceof MethodMethodHandleConstant bootstrapHandle)) {
                            ctxt.getCompilationContext().error(gf.getLocation(), "Wrong bootstrap method handle type");
                            throw new BlockEarlyTermination(gf.unreachable());
                        }
                        String indyName = getClassFile().getNameAndTypeConstantName(indyNameAndTypeIdx);
                        MethodDescriptor desc = (MethodDescriptor) getClassFile().getNameAndTypeConstantDescriptor(indyNameAndTypeIdx);
                        int bootstrapArgCnt = getClassFile().getBootstrapMethodArgumentCount(bootstrapMethodIdx);
                        ArrayList<Literal> bootstrapArgs = new ArrayList<>(bootstrapArgCnt);
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
                                bootstrapArgs.add(getClassFile().getConstantValue(argIdx, tpc));
                            } else {
                                ctxt.getCompilationContext().error(gf.getLocation(), "Non-loadable argument to bootstrap method");
                                throw new BlockEarlyTermination(gf.unreachable());
                            }
                        }
                        // Invoke on the method handle
                        List<TypeDescriptor> parameterTypes = desc.getParameterTypes();
                        int cnt = parameterTypes.size();
                        Value[] args = new Value[cnt];
                        for (int i = cnt - 1; i >= 0; i--) {
                            args[i] = pop(parameterTypes.get(i).isClass2());
                        }
                        Value result = gf.invokeDynamic(bootstrapHandle, bootstrapArgs, indyName, desc, List.of(args));
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
                        v1 = replaceAll(v1, gf.nullCheck(v1));
                        push1(gf.loadLength(gf.decodeReference(v1)));
                        if (v1.getType() instanceof ReferenceType) {
                            replaceAll(v1, gf.notNull(v1));
                        }
                        break;
                    case OP_ATHROW:
                        v1 = pop1();
                        v1 = replaceAll(v1, gf.nullCheck(v1));
                        gf.throw_(v1);
                        // terminate
                        return;
                    case OP_CHECKCAST: {
                        v1 = pop1();
                        Value narrowed = gf.checkcast(v1, getClassFile().getClassConstantAsDescriptor(buffer.getShort() & 0xffff));
                        if (narrowed.getType() instanceof ReferenceType) {
                            replaceAll(v1, narrowed);
                        }
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
                        v1 = replaceAll(v1, gf.nullCheck(v1));
                        gf.monitorEnter(v1);
                        if (v1.getType() instanceof ReferenceType) {
                            replaceAll(v1, gf.notNull(v1));
                        }
                        break;
                    case OP_MONITOREXIT:
                        v1 = pop1();
                        v1 = replaceAll(v1, gf.nullCheck(v1));
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
                    gf.goto_(blockHandles[epIdx], captureOutbound());
                    processBlock();
                    return;
                }
            }
        } catch (BlockEarlyTermination bet) {
            // don't process any more
            return;
        }
    }

    private void processGoto(final ByteBuffer buffer, final int target) {
        BlockLabel block = getBlockForIndexIfExists(target);
        if (block == null) {
            // only one entry point
            block = new BlockLabel();
            gf.goto_(block, Map.of()); // keep current stack & locals
            // set the position after, so that the bci for the instruction is correct
            buffer.position(target);
            gf.begin(block);
            processNewBlock();
        } else {
            gf.goto_(block, captureOutbound());
            // set the position after, so that the bci for the instruction is correct
            buffer.position(target);
            processBlock();
        }
    }

    private void processIf(final ByteBuffer buffer, final Value cond, final int dest1, final int dest2) {
        // do not parse missed branch of constant conditions
        if (cond.isDefEq(lf.literalOf(true))) {
            processGoto(buffer, dest1);
            return;
        } else if (cond.isDefEq(lf.literalOf(false))) {
            processGoto(buffer, dest2);
            return;
        }
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
        // only capture if we need to
        gf.if_(cond, b1, b2, b1s && b2s ? Map.of() : captureOutbound());
        buffer.position(dest1);
        if (b1s) {
            gf.begin(b1);
            doSaved(MethodParser::processNewBlock, this);
        } else {
            doSaved(MethodParser::processBlock, this);
        }
        // stack & vars are restored as they were before processing b1
        buffer.position(dest2);
        if (b2s) {
            gf.begin(b2);
            processNewBlock();
        } else {
            processBlock();
        }
    }

    Value promote(Value value, TypeDescriptor desc) {
        if (desc instanceof BaseTypeDescriptor && desc != BaseTypeDescriptor.V) {
            return promote(value);
        }
        // no promote necessary
        return value;
    }

    Value[] demote(Value[] values, MethodDescriptor desc) {
        List<TypeDescriptor> parameterTypes = desc.getParameterTypes();
        int cnt = values.length;
        for (int i = 0; i < cnt; i ++) {
            TypeDescriptor paramDesc = parameterTypes.get(i);
            if (paramDesc == BaseTypeDescriptor.B) {
                // int -> byte
                values[i] = gf.truncate(values[i], ts.getSignedInteger8Type());
            } else if (paramDesc == BaseTypeDescriptor.C) {
                // int -> char
                values[i] = gf.truncate(values[i], ts.getUnsignedInteger16Type());
            } else if (paramDesc == BaseTypeDescriptor.S) {
                // int -> short
                values[i] = gf.truncate(values[i], ts.getSignedInteger16Type());
            } else if (paramDesc == BaseTypeDescriptor.Z) {
                // int -> boolean
                values[i] = gf.truncate(values[i], ts.getBooleanType());
            }
        }
        return values;
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
}
