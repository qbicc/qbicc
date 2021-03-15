package cc.quarkus.qcc.type.definition.classfile;

import static cc.quarkus.qcc.type.definition.classfile.ClassFile.*;
import static cc.quarkus.qcc.type.definition.classfile.ClassMethodInfo.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockEarlyTermination;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Ret;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.TerminatorVisitor;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeLiteral;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.PoisonType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InvokableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodHandleDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.TypeSignature;
import io.smallrye.common.constraint.Assert;

final class MethodParser implements BasicBlockBuilder.ExceptionHandlerPolicy {
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
        locals = new Value[info.getMaxLocals()];
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
            this.phi = gf.phi(throwable.validate().getType().getReference().asNullable(), new BlockLabel());
        }

        public BlockLabel getHandler() {
            return phi.getPinnedBlockLabel();
        }

        public void enterHandler(final BasicBlock from, final Value exceptionValue) {
            int pc = info.getExTableEntryHandlerPc(index);
            // generate the `if` branch for the current handler's type
            BlockLabel label = phi.getPinnedBlockLabel();
            phi.setValueForBlock(ctxt.getCompilationContext(), gf.getCurrentElement(), from, exceptionValue);
            if (! label.hasTarget()) {
                // first time being entered
                gf.begin(label);
                int exTypeIdx = info.getExTableEntryTypeIdx(index);
                ReferenceType exType;
                if (exTypeIdx == 0) {
                    exType = throwable.validate().getType().getReference();
                } else {
                    exType = (ReferenceType) getClassFile().getTypeConstant(exTypeIdx);
                }
                BlockLabel block = getBlockForIndexIfExists(pc);
                boolean single = block == null;
                if (single) {
                    block = new BlockLabel();
                }
                gf.setBytecodeIndex(pc);
                gf.setLineNumber(info.getLineNumber(pc));
                // Safe to pass the upperBound as the classFileType to the instanceOf node here as catch blocks can
                // only catch subclasses of Throwable as enforced by the verifier
                BasicBlock innerFrom = gf.if_(gf.instanceOf(phi, exType.getUpperBound()), block, delegate.getHandler());
                // enter the delegate handler
                delegate.enterHandler(innerFrom, phi);
                // enter our handler
                Value[] locals = saveLocals();
                Value[] stack = saveStack();
                clearStack();
                int pos = buffer.position();
                buffer.position(pc);
                if (single) {
                    gf.begin(block);
                    push1(gf.narrow(phi, exType));
                    processNewBlock();
                } else {
                    push1(gf.narrow(phi, exType));
                    processBlock(innerFrom);
                }
                // restore everything like nothing happened...
                buffer.position(pos);
                restoreLocals(locals);
                restoreStack(stack);
            }
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

    void setLocal2(int index, Value value) {
        locals[index] = value;
        locals[index + 1] = null;
    }

    void setLocal1(int index, Value value) {
        locals[index] = value;
    }

    void setLocal(int index, Value value, boolean class2) {
        if (class2) {
            setLocal2(index, value);
        } else {
            setLocal1(index, value);
        }
    }

    Value getLocal(int index) {
        Value value = locals[index];
        if (value == null) {
            throw new IllegalStateException("Invalid get local (no value)");
        }
        return value;
    }

    Value getConstantValue(int cpIndex) {
        Literal literal = getClassFile().getConstantValue(cpIndex);
        if (literal instanceof TypeLiteral) {
            return gf.classOf(literal);
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

    Map<BlockLabel, PhiValue[]> entryLocalsArrays = new HashMap<>();
    Map<BlockLabel, PhiValue[]> entryStacks = new HashMap<>();
    Map<BlockLabel, Set<BasicBlock>> visitedFrom = new HashMap<>();

    /**
     * Process a single block.  The current stack and locals are used as a template for the phi value types within
     * the block.  At exit the stack and locals are in an indeterminate state.
     *
     * @param from the source (exiting) block
     */
    void processBlock(BasicBlock from) {
        ByteBuffer buffer = this.buffer;
        // this is the canonical map key handle
        int bci = buffer.position();
        BlockLabel block = getBlockForIndex(bci);
        gf.setBytecodeIndex(bci);
        gf.setLineNumber(info.getLineNumber(bci));
        PhiValue[] entryLocalsArray;
        PhiValue[] entryStack;
        CompilationContext cmpCtxt = ctxt.getCompilationContext();
        ExecutableElement element = gf.getCurrentElement();
        if (entryStacks.containsKey(block)) {
            // already registered
            if (visitedFrom.get(block).add(from)) {
                entryLocalsArray = entryLocalsArrays.get(block);
                entryStack = entryStacks.get(block);
                // complete phis
                for (int i = 0; i < locals.length; i ++) {
                    Value val = locals[i];
                    if (val != null) {
                        PhiValue phiValue = entryLocalsArray[i];
                        // some local slots will be empty
                        if (phiValue != null) {
                            phiValue.setValueForBlock(cmpCtxt, element, from, val);
                        }
                    }
                }
                for (int i = 0; i < sp; i ++) {
                    Value val = stack[i];
                    if (val != null) {
                        entryStack[i].setValueForBlock(cmpCtxt, element, from, val);
                    }
                }
            }
        } else {
            // not registered yet; process new block first
            assert ! block.hasTarget();
            HashSet<BasicBlock> set = new HashSet<>();
            set.add(from);
            visitedFrom.put(block, set);
            gf.begin(block);
            entryLocalsArray = new PhiValue[locals.length];
            entryStack = new PhiValue[sp];
            int epIdx = info.getEntryPointIndex(bci);
            if (epIdx < 0) {
                throw new IllegalStateException("No entry point for block at bci " + bci);
            }
            ValueType[] varTypes = varTypesByEntryPoint[epIdx];
            for (int i = 0; i < locals.length && i < varTypes.length; i ++) {
                Value val = locals[i];
                if (varTypes[i] != null && ! (varTypes[i] instanceof PoisonType)) {
                    PhiValue phiValue = gf.phi(varTypes[i], block);
                    entryLocalsArray[i] = phiValue;
                    phiValue.setValueForBlock(cmpCtxt, element, from, val);
                }
            }
            ValueType[] stackTypes = stackTypesByEntryPoint[epIdx];
            for (int i = 0; i < sp; i ++) {
                Value val = stack[i];
                if (stackTypes[i] != null && ! (stackTypes[i] instanceof PoisonType)) {
                    PhiValue phiValue = gf.phi(stackTypes[i], block);
                    entryStack[i] = phiValue;
                    phiValue.setValueForBlock(cmpCtxt, element, from, val);
                }
            }
            entryLocalsArrays.put(block, entryLocalsArray);
            entryStacks.put(block, entryStack);
            restoreStack(entryStack);
            restoreLocals(entryLocalsArray);
            processNewBlock();
        }
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
                        push1(lf.zeroInitializerLiteralOfType(jlo.validate().getClassType().getReference().asNullable()));
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
                        push1(getConstantValue(buffer.get() & 0xff));
                        break;
                    case OP_LDC_W:
                        push1(getConstantValue(buffer.getShort() & 0xffff));
                        break;
                    case OP_LDC2_W:
                        push2(getConstantValue(buffer.getShort() & 0xffff));
                        break;
                    case OP_ILOAD:
                    case OP_FLOAD:
                    case OP_ALOAD:
                        push1(getLocal(getWidenableValue(buffer, wide)));
                        break;
                    case OP_LLOAD:
                    case OP_DLOAD:
                        push2(getLocal(getWidenableValue(buffer, wide)));
                        break;
                    case OP_ILOAD_0:
                    case OP_ILOAD_1:
                    case OP_ILOAD_2:
                    case OP_ILOAD_3:
                        push1(getLocal(opcode - OP_ILOAD_0));
                        break;
                    case OP_LLOAD_0:
                    case OP_LLOAD_1:
                    case OP_LLOAD_2:
                    case OP_LLOAD_3:
                        push2(getLocal(opcode - OP_LLOAD_0));
                        break;
                    case OP_FLOAD_0:
                    case OP_FLOAD_1:
                    case OP_FLOAD_2:
                    case OP_FLOAD_3:
                        push1(getLocal(opcode - OP_FLOAD_0));
                        break;
                    case OP_DLOAD_0:
                    case OP_DLOAD_1:
                    case OP_DLOAD_2:
                    case OP_DLOAD_3:
                        push2(getLocal(opcode - OP_DLOAD_0));
                        break;
                    case OP_ALOAD_0:
                    case OP_ALOAD_1:
                    case OP_ALOAD_2:
                    case OP_ALOAD_3:
                        push1(getLocal(opcode - OP_ALOAD_0));
                        break;
                    case OP_DALOAD:
                    case OP_LALOAD: {
                        v2 = pop1();
                        v1 = pop1();
                        v1 = gf.load(gf.elementOf(gf.referenceHandle(v1), v2), MemoryAtomicityMode.UNORDERED);
                        push2(v1);
                        break;
                    }
                    case OP_IALOAD:
                    case OP_AALOAD:
                    case OP_FALOAD:
                    case OP_BALOAD:
                    case OP_SALOAD:
                    case OP_CALOAD: {
                        v2 = pop1();
                        v1 = pop1();
                        v1 = promote(gf.load(gf.elementOf(gf.referenceHandle(v1), v2), MemoryAtomicityMode.UNORDERED));
                        push1(v1);
                        break;
                    }
                    case OP_ISTORE:
                    case OP_FSTORE:
                    case OP_ASTORE:
                        setLocal1(getWidenableValue(buffer, wide), pop1());
                        break;
                    case OP_LSTORE:
                    case OP_DSTORE:
                        setLocal2(getWidenableValue(buffer, wide), pop2());
                        break;
                    case OP_ISTORE_0:
                    case OP_ISTORE_1:
                    case OP_ISTORE_2:
                    case OP_ISTORE_3:
                        setLocal1(opcode - OP_ISTORE_0, pop1());
                        break;
                    case OP_LSTORE_0:
                    case OP_LSTORE_1:
                    case OP_LSTORE_2:
                    case OP_LSTORE_3:
                        setLocal2(opcode - OP_LSTORE_0, pop2());
                        break;
                    case OP_FSTORE_0:
                    case OP_FSTORE_1:
                    case OP_FSTORE_2:
                    case OP_FSTORE_3:
                        setLocal1(opcode - OP_FSTORE_0, pop1());
                        break;
                    case OP_DSTORE_0:
                    case OP_DSTORE_1:
                    case OP_DSTORE_2:
                    case OP_DSTORE_3:
                        setLocal2(opcode - OP_DSTORE_0, pop2());
                        break;
                    case OP_ASTORE_0:
                    case OP_ASTORE_1:
                    case OP_ASTORE_2:
                    case OP_ASTORE_3:
                        setLocal1(opcode - OP_ASTORE_0, pop1());
                        break;
                    case OP_IASTORE:
                    case OP_FASTORE:
                    case OP_AASTORE:
                        v3 = pop1();
                        v2 = pop1();
                        v1 = pop1();
                        gf.store(gf.elementOf(gf.referenceHandle(v1), v2), v3, MemoryAtomicityMode.UNORDERED);
                        break;
                    case OP_BASTORE:
                        v3 = pop1();
                        v2 = pop1();
                        v1 = pop1();
                        gf.store(gf.elementOf(gf.referenceHandle(v1), v2), gf.truncate(v3, ts.getSignedInteger8Type()), MemoryAtomicityMode.UNORDERED);
                        break;
                    case OP_SASTORE:
                        v3 = pop1();
                        v2 = pop1();
                        v1 = pop1();
                        gf.store(gf.elementOf(gf.referenceHandle(v1), v2), gf.truncate(v3, ts.getSignedInteger16Type()), MemoryAtomicityMode.UNORDERED);
                        break;
                    case OP_CASTORE:
                        v3 = pop1();
                        v2 = pop1();
                        v1 = pop1();
                        gf.store(gf.elementOf(gf.referenceHandle(v1), v2), gf.truncate(v3, ts.getUnsignedInteger16Type()), MemoryAtomicityMode.UNORDERED);
                        break;
                    case OP_LASTORE:
                    case OP_DASTORE:
                        v3 = pop2();
                        v2 = pop1();
                        v1 = pop1();
                        gf.store(gf.elementOf(gf.referenceHandle(v1), v2), v3, MemoryAtomicityMode.UNORDERED);
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
                        setLocal1(idx, gf.add(getLocal(idx), lf.literalOf(getWidenableValueSigned(buffer, wide))));
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
                        processIf(buffer, gf.isEq(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IFNE:
                        processIf(buffer, gf.isNe(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IFLT:
                        processIf(buffer, gf.isLt(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IFGE:
                        processIf(buffer, gf.isGe(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IFGT:
                        processIf(buffer, gf.isGt(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                        return;
                    case OP_IFLE:
                        processIf(buffer, gf.isLe(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
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
                        int cnt = high - low;
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
                        FunctionType fnType = gf.getCurrentElement().getType(List.of());
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
                        Value value = promote(gf.load(handle, handle.getDetectedMode()));
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
                        // todo: signature context
                        ValueHandle handle = gf.instanceFieldOf(gf.referenceHandle(pop1()), owner, name, desc);
                        Value value = promote(gf.load(handle, handle.getDetectedMode()));
                        push(value, desc.isClass2());
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
                        ValueHandle handle = gf.instanceFieldOf(gf.referenceHandle(v1), owner, name, desc);
                        gf.store(handle, storeTruncate(v2, desc), handle.getDetectedMode());
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
                            gf.classNotFoundError(getClassFile().getMethodrefConstantClassName(methodRef));
                            return;
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
                            v2 = gf.invokeConstructor(v1, owner, desc, List.of(args));
                            replaceAll(v1, v2);
                        } else {
                            TypeDescriptor returnType = desc.getReturnType();
                            if (returnType == BaseTypeDescriptor.V) {
                                if (opcode == OP_INVOKESTATIC) {
                                    // return type is implicitly void
                                    gf.invokeStatic(owner, name, desc, List.of(args));
                                } else {
                                    // return type is implicitly void
                                    gf.invokeInstance(DispatchInvocation.Kind.fromOpcode(opcode), v1, owner, name, desc, List.of(args));
                                }
                            } else {
                                Value result;
                                if (opcode == OP_INVOKESTATIC) {
                                    result = gf.invokeValueStatic(owner, name, desc, List.of(args));
                                } else {
                                    result = gf.invokeValueInstance(DispatchInvocation.Kind.fromOpcode(opcode), v1, owner, name, desc, List.of(args));
                                }
                                push(promote(result), desc.getReturnType().isClass2());
                            }
                        }
                        break;
                    }
                    case OP_INVOKEDYNAMIC: {
                        int indyIdx = buffer.getShort() & 0xffff;
                        buffer.getShort(); // discard 0s
                        int bootstrapMethodIdx = getClassFile().getInvokeDynamicBootstrapMethodIndex(indyIdx);
                        // get the bootstrap handle descriptor
                        MethodHandleDescriptor bootstrapHandle = getClassFile().getMethodHandleDescriptor(getClassFile().getBootstrapMethodRef(bootstrapMethodIdx));
                        if (bootstrapHandle == null) {
                            ctxt.getCompilationContext().error(gf.getLocation(), "Missing bootstrap method handle");
                            gf.unreachable();
                            return;
                        }
                        DefinedTypeDefinition enclosingType = gf.getCurrentElement().getEnclosingType();
                        ClassTypeDescriptor callSiteDesc = ClassTypeDescriptor.synthesize(ctxt, "java/lang/invoke/CallSite");
                        FieldElement.Builder callSiteBuilder = FieldElement.builder();
                        callSiteBuilder.setDescriptor(callSiteDesc);
                        callSiteBuilder.setName("callSite_" + gf.getCurrentElement().getIndex() + "_" + src);
                        callSiteBuilder.setModifiers(ACC_STATIC | ACC_FINAL | I_ACC_HIDDEN);
                        callSiteBuilder.setSignature(TypeSignature.synthesize(ctxt, callSiteDesc));
                        callSiteBuilder.setEnclosingType(enclosingType);
                        FieldElement callSiteHolder = callSiteBuilder.build();
                        // inject a new field for the call site
                        enclosingType.validate().injectField(callSiteHolder);
                        // TODO: inject code into the initializer to initialize the call site object by calling the bootstrap
                        // ...
                        // Get the call site
                        ValueHandle holderHandle = gf.staticField(callSiteHolder);
                        Value callSite = gf.load(holderHandle, holderHandle.getDetectedMode());
                        // Get the method handle instance from the call site
                        ClassTypeDescriptor descOfMethodHandle = ClassTypeDescriptor.synthesize(ctxt, "java/lang/invoke/MethodHandle");
                        Value methodHandle = gf.invokeValueInstance(DispatchInvocation.Kind.VIRTUAL, callSite,
                            callSiteDesc, "getTarget",
                            MethodDescriptor.synthesize(ctxt, descOfMethodHandle, List.of()),
                            List.of());
                        // Invoke on the method handle
                        int nameAndTypeRef = getClassFile().getInvokeDynamicNameAndTypeIndex(indyIdx);
                        MethodDescriptor desc = (MethodDescriptor) getClassFile().getDescriptorConstant(getClassFile().getNameAndTypeConstantDescriptorIdx(nameAndTypeRef));
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
                        Value result = gf.invokeValueInstance(DispatchInvocation.Kind.EXACT, methodHandle, descOfMethodHandle, "invokeExact",
                            desc, List.of(args));
                        if (! desc.getReturnType().isVoid()) {
                            push(promote(result), desc.getReturnType().isClass2());
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
                        ArrayObjectType arrayType;
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
                        // todo: check for negative array size
                        push1(gf.newArray(arrayType, pop1()));
                        break;
                    case OP_ANEWARRAY: {
                        TypeDescriptor desc = getClassFile().getClassConstantAsDescriptor(buffer.getShort() & 0xffff);
                        // todo: check for negative array size
                        push1(gf.newArray(ArrayTypeDescriptor.of(ctxt, desc), pop1()));
                        break;
                    }
                    case OP_ARRAYLENGTH:
                        push1(gf.arrayLength(gf.referenceHandle(pop1())));
                        break;
                    case OP_ATHROW:
                        gf.throw_(pop1());
                        // terminate
                        return;
                    case OP_CHECKCAST: {
                        v1 = pop1();
                        Value narrowed = gf.narrow(v1, getClassFile().getClassConstantAsDescriptor(buffer.getShort() & 0xffff));
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
                        gf.monitorEnter(pop1());
                        break;
                    case OP_MONITOREXIT:
                        gf.monitorExit(pop1());
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
        } catch (BlockEarlyTermination ignored) {
            // don't process any more
            return;
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
        return getClassFile().getFieldrefConstantName(fieldRef);
    }

    private ObjectType getOwnerOfFieldRef(final int fieldRef) {
        return resolveClass(getClassFile().getFieldrefConstantClassName(fieldRef));
    }

    private String getNameOfMethodRef(final int methodRef) {
        return getClassFile().getMethodrefConstantName(methodRef);
    }

    private boolean nameOfMethodRefEquals(final int methodRef, final String expected) {
        return getClassFile().methodrefConstantNameEquals(methodRef, expected);
    }

    private ObjectType getOwnerOfMethodRef(final int methodRef) {
        ValueType owner = getClassFile().getTypeConstant(getClassFile().getMethodrefConstantClassIndex(methodRef));
        if (owner instanceof ReferenceType) {
            return ((ReferenceType) owner).getUpperBound();
        } else if (owner instanceof ObjectType) {
            return (ObjectType) owner;
        }
        ctxt.getCompilationContext().error(gf.getLocation(), "Owner of method is not a valid object type (%s)", owner);
        // return *something*
        return gf.getCurrentElement().getEnclosingType().validate().getType();
    }

    private int getNameAndTypeOfMethodRef(final int methodRef) {
        return getClassFile().getMethodrefNameAndTypeIndex(methodRef);
    }

    private FieldElement resolveTargetOfFieldRef(final int fieldRef) {
        ValidatedTypeDefinition definition = getOwnerOfFieldRef(fieldRef).getDefinition().validate();
        FieldElement field = definition.validate().resolveField(getDescriptorOfFieldRef(fieldRef), getNameOfFieldRef(fieldRef));
        if (field == null) {
            // todo
            throw new IllegalStateException();
        }
        return field;
    }

    private InvokableElement resolveTargetOfDescriptor(ValidatedTypeDefinition resolved, final MethodDescriptor desc, final int nameAndTypeRef) {
        boolean ctor = getClassFile().nameAndTypeConstantNameEquals(nameAndTypeRef, "<init>");
        int idx;
        if (ctor) {
            idx = resolved.findConstructorIndex(desc);
            return idx == -1 ? null : resolved.getConstructor(idx);
        } else {
            idx = resolved.findMethodIndex(getClassFile().getNameAndTypeConstantName(nameAndTypeRef), desc);
            return idx == -1 ? null : resolved.getMethod(idx);
        }
    }

    private MethodElement resolveVirtualTargetOfDescriptor(ValidatedTypeDefinition resolved, final MethodDescriptor desc, final int nameAndTypeRef) {
        return resolved.resolveMethodElementVirtual(getClassFile().getNameAndTypeConstantName(nameAndTypeRef), desc);
    }

    private MethodElement resolveInterfaceTargetOfDescriptor(ValidatedTypeDefinition resolved, final MethodDescriptor desc, final int nameAndTypeRef) {
        return resolved.resolveMethodElementInterface(getClassFile().getNameAndTypeConstantName(nameAndTypeRef), desc);
    }

    private ObjectType resolveClass(String name) {
        return getClassFile().getClassContext().findDefinedType(name).validate().getType();
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
