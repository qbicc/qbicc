package cc.quarkus.qcc.type.definition.classfile;

import static cc.quarkus.qcc.type.definition.classfile.ClassFile.*;
import static cc.quarkus.qcc.type.definition.classfile.ClassMethodInfo.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Ret;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.TerminatorVisitor;
import cc.quarkus.qcc.graph.Try;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.ArrayTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ResolvedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;
import cc.quarkus.qcc.type.descriptor.ConstructorDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.ParameterizedExecutableDescriptor;

final class MethodParser {
    final ClassMethodInfo info;
    final Value[] stack;
    final Value[] locals;
    final HashSet<Value> fatValues;
    final BlockLabel[] blockHandles;
    final ByteBuffer buffer;
    private Map<BasicBlock, Value[]> retStacks;
    private Map<BasicBlock, Value[]> retLocals;
    private final BasicBlockBuilder gf;
    private final ClassContext ctxt;
    private final LiteralFactory lf;
    private final TypeSystem ts;
    int sp;

    MethodParser(final ClassContext ctxt, final ClassMethodInfo info, final ByteBuffer buffer, final BasicBlockBuilder graphFactory) {
        this.ctxt = ctxt;
        lf = ctxt.getLiteralFactory();
        ts = ctxt.getTypeSystem();
        this.info = info;
        stack = new Value[info.getMaxStack()];
        locals = new Value[info.getMaxLocals()];
        this.buffer = buffer;
        int cnt = info.getEntryPointCount();
        BlockLabel[] blockHandles = new BlockLabel[cnt];
        int dest = -1;
        // make a "canonical" node handle for each block
        for (int i = 0; i < cnt; i ++) {
            blockHandles[i] = new BlockLabel();
        }
        this.blockHandles = blockHandles;
        // it's not an entry point
        gf = graphFactory;
        fatValues = new HashSet<>();
        // catch mapper is sensitive to buffer index
        gf.setCatchMapper(new Try.CatchMapper() {
            public int getCatchCount() {
                int etl = info.getExTableLen();
                int pos = buffer.position();
                int cnt = 0;
                for (int i = 0; i < etl; i++) {
                    if (info.getExTableEntryStartPc(i) <= pos && pos < info.getExTableEntryEndPc(i)) {
                        cnt++;
                    }
                }
                return cnt;
            }

            public ClassTypeIdLiteral getCatchType(final int index) {
                int etl = info.getExTableLen();
                int pos = buffer.position();
                int cnt = -1;
                for (int i = 0; i < etl; i++) {
                    if (info.getExTableEntryStartPc(i) <= pos && pos < info.getExTableEntryEndPc(i)) {
                        cnt++;
                    }
                    if (cnt == index) {
                        return (ClassTypeIdLiteral) getConstantValue(info.getExTableEntryTypeIdx(i));
                    }
                }
                throw new IndexOutOfBoundsException();
            }

            public BlockLabel getCatchHandler(final int index) {
                int etl = info.getExTableLen();
                int pos = buffer.position();
                int cnt = -1;
                for (int i = 0; i < etl; i++) {
                    if (info.getExTableEntryStartPc(i) <= pos && pos < info.getExTableEntryEndPc(i)) {
                        cnt++;
                    }
                    if (cnt == index) {
                        return getBlockForIndex(info.getExTableEntryHandlerPc(i));
                    }
                }
                throw new IndexOutOfBoundsException();
            }

            public void setCatchValue(final int index, final BasicBlock from, final Value value) {
                int etl = info.getExTableLen();
                int pos = buffer.position();
                int cnt = -1;
                for (int i = 0; i < etl; i++) {
                    if (info.getExTableEntryStartPc(i) <= pos && pos < info.getExTableEntryEndPc(i)) {
                        cnt++;
                    }
                    if (cnt == index) {
                        int handlerPc = info.getExTableEntryHandlerPc(i);
                        BlockLabel block = getBlockForIndex(handlerPc);
                        // we must enter this block from the `from` block; so we have to save our state
                        Value[] locals = saveLocals();
                        Value[] stack = saveStack();
                        clearStack();
                        push(value);
                        buffer.position(handlerPc);
                        processBlock(from);
                        // restore everything like nothing happened...
                        buffer.position(pos);
                        restoreLocals(locals);
                        restoreStack(stack);
                        // and we're done
                        return;
                    }
                }
                throw new IndexOutOfBoundsException();
            }
        });
    }

    // fat values

    boolean isFat(Value value) {
        return fatValues.contains(value);
    }

    Value fatten(Value value) {
        fatValues.add(value);
        return value;
    }

    Value unfatten(Value value) {
        fatValues.remove(value);
        return value;
    }

    // stack manipulation

    Type topOfStackType() {
        return peek().getType();
    }

    void clearStack() {
        Arrays.fill(stack, 0, sp, null);
        sp = 0;
    }

    Value pop() {
        return pop(isFat(peek()));
    }

    Value pop(boolean fat) {
        int tos = sp - 1;
        Value value = stack[tos];
        if (isFat(value) != fat) {
            throw new IllegalStateException("Bad pop");
        }
        stack[tos] = null;
        sp = tos;
        return value;
    }

    Value pop2() {
        int tos = sp - 1;
        Value value = stack[tos];
        if (isFat(value)) {
            stack[tos] = null;
            sp = tos;
        } else {
            stack[tos] = null;
            stack[tos - 1] = null;
            sp = tos - 1;
        }
        return value;
    }

    Value pop1() {
        int tos = sp - 1;
        Value value = stack[tos];
        if (isFat(value)) {
            throw new IllegalStateException("Bad pop");
        }
        stack[tos] = null;
        sp = tos;
        return value;
    }

    void dup() {
        Value value = peek();
        if (isFat(value)) {
            throw new IllegalStateException("Bad dup");
        }
        push(value);
    }

    void dup2() {
        Value peek = peek();
        if (isFat(peek)) {
            push(peek);
        } else {
            Value v2 = pop1();
            Value v1 = pop1();
            push(v1);
            push(v2);
            push(v1);
            push(v2);
        }
    }

    void swap() {
        if (isFat(peek())) {
            throw new IllegalStateException("Bad swap");
        }
        Value v2 = pop1();
        Value v1 = pop1();
        push(v2);
        push(v1);
    }

    <V extends Value> V push(V value) {
        stack[sp++] = value;
        return value;
    }

    Value peek() {
        return stack[sp - 1];
    }

    // Locals manipulation

    void clearLocals() {
        Arrays.fill(locals, null);
    }

    void setLocal(int index, Value value) {
        if (isFat(value)) {
            locals[index + 1] = null;
        }
        locals[index] = value;
    }

    Value getLocal(int index) {
        Value value = locals[index];
        if (value == null) {
            throw new IllegalStateException("Invalid get local (no value)");
        }
        return value;
    }

    Value getLocal(int index, Type expectedType) {
        Value value = getLocal(index);
        if (value.getType() != expectedType) {
            throw new TypeMismatchException();
        }
        return value;
    }

    <L extends Literal> L getConstantValue(int cpIndex, Class<L> expectedType) {
        return expectedType.cast(getConstantValue(cpIndex));
    }

    Value getFattenedConstantValue(int cpIndex) {
        Literal value = getConstantValue(cpIndex);
        return value.getType() instanceof WordType && ((WordType) value.getType()).getMinBits() == 64 ? fatten(value) : value;
    }

    Literal getConstantValue(int cpIndex) {
        return getClassFile().getConstantValue(cpIndex);
    }

    BlockLabel getBlockForIndex(int target) {
        int idx = info.getEntryPointIndex(target);
        if (idx < 0) {
            throw new IllegalStateException("Block not found for target bci " + target);
        }
        return blockHandles[idx];
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
        BlockLabel block = getBlockForIndex(buffer.position());
        assert block != null : "No block registered for BCI " + buffer.position();
        PhiValue[] entryLocalsArray;
        PhiValue[] entryStack;
        BasicBlock resolvedBlock;
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
                            phiValue.setValueForBlock(from, val);
                        }
                    }
                }
                for (int i = 0; i < sp; i ++) {
                    Value val = stack[i];
                    if (val != null) {
                        entryStack[i].setValueForBlock(from, val);
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
            for (int i = 0; i < locals.length; i ++) {
                Value val = locals[i];
                if (val != null) {
                    PhiValue phiValue = gf.phi(val.getType(), block);
                    entryLocalsArray[i] = phiValue;
                    phiValue.setValueForBlock(from, val);
                    if (isFat(val)) {
                        fatten(phiValue);
                    }
                }
            }
            for (int i = 0; i < sp; i ++) {
                Value val = stack[i];
                if (val != null) {
                    PhiValue phiValue = gf.phi(val.getType(), block);
                    entryStack[i] = phiValue;
                    phiValue.setValueForBlock(from, val);
                    if (isFat(val)) {
                        fatten(phiValue);
                    }
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
        while (buffer.hasRemaining()) {
            src = buffer.position();
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
                    push(lf.literalOfNull());
                    break;
                case OP_ICONST_M1:
                case OP_ICONST_0:
                case OP_ICONST_1:
                case OP_ICONST_2:
                case OP_ICONST_3:
                case OP_ICONST_4:
                case OP_ICONST_5:
                    push(lf.literalOf(opcode - OP_ICONST_0));
                    break;
                case OP_LCONST_0:
                case OP_LCONST_1:
                    push(fatten(lf.literalOf((long) opcode - OP_LCONST_0)));
                    break;
                case OP_FCONST_0:
                case OP_FCONST_1:
                case OP_FCONST_2:
                    push(lf.literalOf((float) opcode - OP_FCONST_0));
                    break;
                case OP_DCONST_0:
                case OP_DCONST_1:
                    push(fatten(lf.literalOf((double) opcode - OP_DCONST_0)));
                    break;
                case OP_BIPUSH:
                    push(lf.literalOf((int) buffer.get()));
                    break;
                case OP_SIPUSH:
                    push(lf.literalOf((int) buffer.getShort()));
                    break;
                case OP_LDC:
                    push(unfatten(getConstantValue(buffer.get() & 0xff)));
                    break;
                case OP_LDC_W:
                    push(unfatten(getConstantValue(buffer.getShort() & 0xffff)));
                    break;
                case OP_LDC2_W:
                    push(fatten(getConstantValue(buffer.getShort() & 0xffff)));
                    break;
                case OP_ILOAD:
                    push(getLocal(getWidenableValue(buffer, wide)));
                    break;
                case OP_LLOAD:
                    // already fat
                    push(getLocal(getWidenableValue(buffer, wide)));
                    break;
                case OP_FLOAD:
                    push(getLocal(getWidenableValue(buffer, wide)));
                    break;
                case OP_DLOAD:
                    // already fat
                    push(getLocal(getWidenableValue(buffer, wide)));
                    break;
                case OP_ALOAD:
                    push(getLocal(getWidenableValue(buffer, wide)));
                    break;
                case OP_ILOAD_0:
                case OP_ILOAD_1:
                case OP_ILOAD_2:
                case OP_ILOAD_3:
                    push(getLocal(opcode - OP_ILOAD_0));
                    break;
                case OP_LLOAD_0:
                case OP_LLOAD_1:
                case OP_LLOAD_2:
                case OP_LLOAD_3:
                    // already fat
                    push(getLocal(opcode - OP_LLOAD_0));
                    break;
                case OP_FLOAD_0:
                case OP_FLOAD_1:
                case OP_FLOAD_2:
                case OP_FLOAD_3:
                    push(getLocal(opcode - OP_FLOAD_0));
                    break;
                case OP_DLOAD_0:
                case OP_DLOAD_1:
                case OP_DLOAD_2:
                case OP_DLOAD_3:
                    // already fat
                    push(getLocal(opcode - OP_DLOAD_0));
                    break;
                case OP_ALOAD_0:
                case OP_ALOAD_1:
                case OP_ALOAD_2:
                case OP_ALOAD_3:
                    push(getLocal(opcode - OP_ALOAD_0));
                    break;
                case OP_DALOAD:
                case OP_LALOAD: {
                    v2 = pop1();
                    v1 = pop1();
                    v1 = gf.readArrayValue(v1, v2, JavaAccessMode.PLAIN);
                    push(fatten(v1));
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
                    v1 = gf.readArrayValue(v1, v2, JavaAccessMode.PLAIN);
                    push(unfatten(v1));
                    break;
                }
                case OP_ISTORE:
                case OP_FSTORE:
                case OP_ASTORE:
                    setLocal(getWidenableValue(buffer, wide), pop1());
                    break;
                case OP_LSTORE:
                case OP_DSTORE:
                    // already fat
                    setLocal(getWidenableValue(buffer, wide), pop2());
                    break;
                case OP_ISTORE_0:
                case OP_ISTORE_1:
                case OP_ISTORE_2:
                case OP_ISTORE_3:
                    setLocal(opcode - OP_ISTORE_0, pop1());
                    break;
                case OP_LSTORE_0:
                case OP_LSTORE_1:
                case OP_LSTORE_2:
                case OP_LSTORE_3:
                    // already fat
                    setLocal(opcode - OP_LSTORE_0, pop2());
                    break;
                case OP_FSTORE_0:
                case OP_FSTORE_1:
                case OP_FSTORE_2:
                case OP_FSTORE_3:
                    setLocal(opcode - OP_FSTORE_0, pop1());
                    break;
                case OP_DSTORE_0:
                case OP_DSTORE_1:
                case OP_DSTORE_2:
                case OP_DSTORE_3:
                    // already fat
                    setLocal(opcode - OP_DSTORE_0, pop2());
                    break;
                case OP_ASTORE_0:
                case OP_ASTORE_1:
                case OP_ASTORE_2:
                case OP_ASTORE_3:
                    setLocal(opcode - OP_ASTORE_0, pop1());
                    break;
                case OP_IASTORE:
                case OP_FASTORE:
                case OP_AASTORE:
                case OP_BASTORE:
                case OP_SASTORE:
                case OP_CASTORE:
                    gf.writeArrayValue(pop1(), pop1(), pop1(), JavaAccessMode.PLAIN);
                    break;
                case OP_LASTORE:
                case OP_DASTORE:
                    gf.writeArrayValue(pop1(), pop1(), pop2(), JavaAccessMode.PLAIN);
                    break;
                case OP_POP:
                    pop1();
                    break;
                case OP_POP2:
                    pop2();
                    break;
                case OP_DUP:
                    dup();
                    break;
                case OP_DUP_X1:
                    v1 = pop1();
                    v2 = pop1();
                    push(v1);
                    push(v2);
                    push(v1);
                    break;
                case OP_DUP_X2:
                    v1 = pop();
                    v2 = pop();
                    v3 = pop();
                    push(v1);
                    push(v3);
                    push(v2);
                    push(v1);
                    break;
                case OP_DUP2:
                    dup2();
                    break;
                case OP_DUP2_X1:
                    if (! isFat(peek())) {
                        // form 1
                        v1 = pop();
                        v2 = pop();
                        v3 = pop();
                        push(v2);
                        push(v1);
                        push(v3);
                    } else {
                        // form 2
                        v1 = pop2();
                        v2 = pop2();
                        push(v1);
                    }
                    push(v2);
                    push(v1);
                    break;
                case OP_DUP2_X2:
                    if (! isFat(peek())) {
                        v1 = pop1();
                        v2 = pop1();
                        if (! isFat(peek())) {
                            // form 1
                            v3 = pop1();
                            v4 = pop1();
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
                    } else {
                        v1 = pop2();
                        if (! isFat(peek())) {
                            // form 2
                            v2 = pop1();
                            v3 = pop1();
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
                    }
                    push(v1);
                    break;
                case OP_SWAP:
                    swap();
                    break;
                case OP_IADD:
                case OP_FADD:
                    push(gf.add(pop1(), pop1()));
                    break;
                case OP_LADD:
                case OP_DADD:
                    push(fatten(gf.add(pop2(), pop2())));
                    break;
                case OP_ISUB:
                case OP_FSUB:
                    push(gf.sub(pop1(), pop1()));
                    break;
                case OP_LSUB:
                case OP_DSUB:
                    push(fatten(gf.sub(pop2(), pop2())));
                    break;
                case OP_IMUL:
                case OP_FMUL:
                    push(gf.multiply(pop1(), pop1()));
                    break;
                case OP_LMUL:
                case OP_DMUL:
                    push(fatten(gf.multiply(pop2(), pop2())));
                    break;
                case OP_IDIV:
                case OP_FDIV:
                    push(gf.divide(pop1(), pop1()));
                    break;
                case OP_LDIV:
                case OP_DDIV:
                    push(fatten(gf.divide(pop2(), pop2())));
                    break;
                case OP_IREM:
                case OP_FREM:
                    push(gf.remainder(pop1(), pop1()));
                    break;
                case OP_LREM:
                case OP_DREM:
                    push(fatten(gf.remainder(pop2(), pop2())));
                    break;
                case OP_INEG:
                case OP_FNEG:
                    push(gf.negate(pop1()));
                    break;
                case OP_LNEG:
                case OP_DNEG:
                    push(fatten(gf.negate(pop2())));
                    break;
                case OP_ISHL:
                    push(gf.shl(pop1(), pop1()));
                    break;
                case OP_LSHL:
                    push(fatten(gf.shl(pop2(), pop1())));
                    break;
                case OP_ISHR: {
                    v1 = pop1();
                    v2 = pop1();
                    IntegerType it = (IntegerType) v1.getType();
                    if (it instanceof SignedIntegerType) {
                        push(gf.shr(v1, v2));
                    } else {
                        push(gf.bitCast(gf.shr(gf.bitCast(v1, it.asSigned()), v2), it));
                    }
                    break;
                }
                case OP_LSHR: {
                    v1 = pop2();
                    v2 = pop1();
                    IntegerType it = (IntegerType) v1.getType();
                    if (it instanceof SignedIntegerType) {
                        push(fatten(gf.shr(v1, v2)));
                    } else {
                        push(fatten(gf.bitCast(gf.shr(gf.bitCast(v1, it.asSigned()), v2), it)));
                    }
                    break;
                }
                case OP_IUSHR: {
                    v1 = pop1();
                    v2 = pop1();
                    IntegerType it = (IntegerType) v1.getType();
                    if (it instanceof UnsignedIntegerType) {
                        push(gf.shr(v1, v2));
                    } else {
                        push(gf.bitCast(gf.shr(gf.bitCast(v1, it.asUnsigned()), v2), it));
                    }
                    break;
                }
                case OP_LUSHR: {
                    v1 = pop2();
                    v2 = pop1();
                    IntegerType it = (IntegerType) v1.getType();
                    if (it instanceof UnsignedIntegerType) {
                        push(fatten(gf.shr(v1, v2)));
                    } else {
                        push(fatten(gf.bitCast(gf.shr(gf.bitCast(v1, it.asUnsigned()), v2), it)));
                    }
                    break;
                }
                case OP_IAND:
                    push(gf.and(pop1(), pop1()));
                    break;
                case OP_LAND:
                    push(fatten(gf.and(pop2(), pop2())));
                    break;
                case OP_IOR:
                    push(gf.or(pop1(), pop1()));
                    break;
                case OP_LOR:
                    push(fatten(gf.or(pop2(), pop2())));
                    break;
                case OP_IXOR:
                    push(gf.xor(pop1(), pop1()));
                    break;
                case OP_LXOR:
                    push(fatten(gf.xor(pop2(), pop2())));
                    break;
                case OP_IINC:
                    int idx = getWidenableValue(buffer, wide);
                    setLocal(idx, gf.add(getLocal(idx), lf.literalOf(getWidenableValueSigned(buffer, wide))));
                    break;
                case OP_I2L:
                    push(fatten(gf.extend(pop1(), ts.getSignedInteger64Type())));
                    break;
                case OP_I2F:
                    push(gf.valueConvert(pop1(), ts.getFloat32Type()));
                    break;
                case OP_I2D:
                    push(fatten(gf.valueConvert(pop1(), ts.getFloat64Type())));
                    break;
                case OP_L2I:
                    push(gf.truncate(pop2(), ts.getSignedInteger32Type()));
                    break;
                case OP_L2F:
                    push(gf.valueConvert(pop2(), ts.getFloat32Type()));
                    break;
                case OP_L2D:
                    push(fatten(gf.valueConvert(pop2(), ts.getFloat64Type())));
                    break;
                case OP_F2I:
                    push(gf.valueConvert(pop1(), ts.getSignedInteger32Type()));
                    break;
                case OP_F2L:
                    push(fatten(gf.valueConvert(pop1(), ts.getSignedInteger64Type())));
                    break;
                case OP_F2D:
                    push(fatten(gf.extend(pop1(), ts.getFloat64Type())));
                    break;
                case OP_D2I:
                    push(gf.valueConvert(pop2(), ts.getSignedInteger32Type()));
                    break;
                case OP_D2L:
                    push(fatten(gf.valueConvert(pop2(), ts.getSignedInteger64Type())));
                    break;
                case OP_D2F:
                    push(gf.truncate(pop2(), ts.getFloat32Type()));
                    break;
                case OP_I2B:
                    push(gf.extend(gf.truncate(pop1(), ts.getSignedInteger8Type()), ts.getSignedInteger32Type()));
                    break;
                case OP_I2C:
                    push(gf.extend(gf.truncate(pop1(), ts.getUnsignedInteger16Type()), ts.getSignedInteger32Type()));
                    break;
                case OP_I2S:
                    push(gf.extend(gf.truncate(pop1(), ts.getSignedInteger16Type()), ts.getSignedInteger32Type()));
                    break;
                case OP_LCMP:
                case OP_DCMPL:
                case OP_FCMPL:
                    v2 = pop();
                    v1 = pop();
                    v3 = gf.cmpLt(v1, v2);
                    v4 = gf.cmpGt(v1, v2);
                    push(gf.select(v3, lf.literalOf(- 1), gf.select(v4, lf.literalOf(1), lf.literalOf(0))));
                    break;
                case OP_DCMPG:
                case OP_FCMPG:
                    v2 = pop();
                    v1 = pop();
                    v3 = gf.cmpLt(v1, v2);
                    v4 = gf.cmpGt(v1, v2);
                    push(gf.select(v4, lf.literalOf(1), gf.select(v3, lf.literalOf(- 1), lf.literalOf(0))));
                    break;
                case OP_IFEQ:
                    processIf(buffer, gf.cmpEq(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFNE:
                    processIf(buffer, gf.cmpNe(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFLT:
                    processIf(buffer, gf.cmpLt(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFGE:
                    processIf(buffer, gf.cmpGe(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFGT:
                    processIf(buffer, gf.cmpGt(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFLE:
                    processIf(buffer, gf.cmpLe(pop1(), lf.literalOf(0)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPEQ:
                case OP_IF_ACMPEQ:
                    processIf(buffer, gf.cmpEq(pop1(), pop1()), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPNE:
                case OP_IF_ACMPNE:
                    processIf(buffer, gf.cmpNe(pop1(), pop1()), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPLT:
                    processIf(buffer, gf.cmpLt(pop1(), pop1()), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPGE:
                    processIf(buffer, gf.cmpGe(pop1(), pop1()), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPGT:
                    processIf(buffer, gf.cmpGt(pop1(), pop1()), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPLE:
                    processIf(buffer, gf.cmpLe(pop1(), pop1()), buffer.getShort() + src, buffer.position());
                    return;
                case OP_GOTO:
                case OP_GOTO_W: {
                    int target = src + (opcode == OP_GOTO ? buffer.getShort() : buffer.getInt());
                    BasicBlock from = gf.goto_(getBlockForIndex(target));
                    // set the position after, so that the bci for the instruction is correct
                    buffer.position(target);
                    processBlock(from);
                    return;
                }
                case OP_JSR:
                case OP_JSR_W: {
                    int target = src + (opcode == OP_JSR ? buffer.getShort() : buffer.getInt());
                    int ret = buffer.position();
                    // jsr destination
                    BlockLabel dest = getBlockForIndex(target);
                    BlockLabel retBlock = getBlockForIndex(ret);
                    push(lf.literalOf(retBlock));
                    // the jsr call
                    BasicBlock termBlock = gf.jsr(dest, lf.literalOf(retBlock));
                    // process the jsr call target block with our current stack
                    buffer.position(target);
                    processBlock(termBlock);
                    // now process the return block once for each returning path (as if the ret is a goto);
                    // the ret visitor will continue parsing if the jsr returns (as opposed to throwing)
                    new RetVisitor(buffer, ret).handleBlock(BlockLabel.getTargetOf(dest));
                    return;
                }
                case OP_RET:
                    // each ret records the output stack and locals at the point of the ret, and then exits.
                    setJsrExitState(gf.ret(pop()), saveStack(), saveLocals());
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
                    BlockLabel[] handles = new BlockLabel[cnt];
                    for (int i = 0; i < cnt; i++) {
                        vals[i] = low + i;
                        handles[i] = getBlockForIndex(dests[i] = buffer.getInt() + src);
                    }
                    Set<BlockLabel> seen = new HashSet<>();
                    BlockLabel defaultBlock = getBlockForIndex(db + src);
                    seen.add(defaultBlock);
                    BasicBlock exited = gf.switch_(pop1(), vals, handles, defaultBlock);
                    Value[] stackSnap = saveStack();
                    Value[] varSnap = saveLocals();
                    buffer.position(db + src);
                    processBlock(exited);
                    for (int i = 0; i < handles.length; i++) {
                        if (seen.add(getBlockForIndex(dests[i]))) {
                            restoreStack(stackSnap);
                            restoreLocals(varSnap);
                            buffer.position(dests[i]);
                            processBlock(exited);
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
                    int[]vals = new int[cnt];
                    BlockLabel[] handles = new BlockLabel[cnt];
                    for (int i = 0; i < cnt; i++) {
                        vals[i] = buffer.getInt();
                        handles[i] = getBlockForIndex(dests[i] = buffer.getInt() + src);
                    }
                    Set<BlockLabel> seen = new HashSet<>();
                    BlockLabel defaultBlock = getBlockForIndex(db + src);
                    seen.add(defaultBlock);
                    BasicBlock exited = gf.switch_(pop1(), vals, handles, defaultBlock);
                    Value[] stackSnap = saveStack();
                    Value[] varSnap = saveLocals();
                    buffer.position(db + src);
                    processBlock(exited);
                    for (int i = 0; i < handles.length; i++) {
                        if (seen.add(getBlockForIndex(dests[i]))) {
                            restoreStack(stackSnap);
                            restoreLocals(varSnap);
                            buffer.position(dests[i]);
                            processBlock(exited);
                        }
                    }
                    // done
                    return;
                }
                case OP_IRETURN:
                    // TODO: narrow the return type if it's narrower than i32
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
                    FieldElement fieldElement = resolveTargetOfFieldRef(fieldRef);
                    Value value = gf.readStaticField(fieldElement, JavaAccessMode.DETECT);
                    push(fieldElement.hasClass2Type() ? fatten(value) : value);
                    break;
                }
                case OP_PUTSTATIC: {
                    // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                    int fieldRef = buffer.getShort() & 0xffff;
                    FieldElement fieldElement = resolveTargetOfFieldRef(fieldRef);
                    Type type = getTypeOfFieldRef(fieldRef);
                    gf.writeStaticField(fieldElement, fieldElement.hasClass2Type() ? pop2() : pop(), JavaAccessMode.DETECT);
                    break;
                }
                case OP_GETFIELD: {
                    // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                    int fieldRef = buffer.getShort() & 0xffff;
                    FieldElement fieldElement = resolveTargetOfFieldRef(fieldRef);
                    Value value = gf.readInstanceField(pop(), fieldElement, JavaAccessMode.DETECT);
                    push(fieldElement.hasClass2Type() ? fatten(value) : value);
                    break;
                }
                case OP_PUTFIELD: {
                    // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                    int fieldRef = buffer.getShort() & 0xffff;
                    FieldElement fieldElement = resolveTargetOfFieldRef(fieldRef);
                    getTypeOfFieldRef(fieldRef);
                    v1 = fieldElement.hasClass2Type() ? pop2() : pop();
                    v2 = pop();
                    gf.writeInstanceField(v1, fieldElement, v2, JavaAccessMode.DETECT);
                    break;
                }
                case OP_INVOKEVIRTUAL:
                case OP_INVOKESPECIAL:
                case OP_INVOKESTATIC:
                case OP_INVOKEINTERFACE:
                    int methodRef = buffer.getShort() & 0xffff;
                    TypeIdLiteral owner;
                    int nameAndType;
                    if (opcode == OP_INVOKEINTERFACE) {
                        owner = getOwnerOfInterfaceMethodRef(methodRef);
                        nameAndType = getNameAndTypeOfInterfaceMethodRef(methodRef);
                        int checkCount = buffer.get() & 0xff;
                        buffer.get(); // discard 0
                    } else {
                        owner = getOwnerOfMethodRef(methodRef);
                        nameAndType = getNameAndTypeOfMethodRef(methodRef);
                    }
                    // todo: try/catch, replace with error node
                    DefinedTypeDefinition found = ctxt.resolveDefinedTypeLiteral(owner);
                    if (found == null) {
                        gf.classNotFoundError(owner.toString());
                        return;
                    }
                    ResolvedTypeDefinition resolved = found.validate().resolve();
                    ParameterizedExecutableDescriptor desc = resolveMethodDescriptor(owner, nameAndType);
                    ParameterizedExecutableElement target;
                    if (opcode == OP_INVOKESTATIC || opcode == OP_INVOKESPECIAL) {
                        target = resolveTargetOfDescriptor(resolved, desc, nameAndType);
                    } else if (opcode == OP_INVOKEVIRTUAL) {
                        target = resolveVirtualTargetOfDescriptor(resolved, desc, nameAndType);
                    } else {
                        assert opcode == OP_INVOKEINTERFACE;
                        target = resolveInterfaceTargetOfDescriptor(resolved, desc, nameAndType);
                    }
                    if (target == null) {
                        String methodName = getClassFile().getNameAndTypeConstantName(nameAndType);
                        gf.noSuchMethodError(owner, desc, methodName);
                        // end block
                        return;
                    }
                    int cnt = target.getParameterCount();
                    Value[] args = new Value[cnt];
                    for (int i = cnt - 1; i >= 0; i --) {
                        if (target.getParameter(i).hasClass2Type()) {
                            args[i] = pop2();
                        } else {
                            args[i] = pop1();
                        }
                        // narrow arguments if needed
                        ValueType argType = args[i].getType();
                        if (argType instanceof IntegerType) {
                            IntegerType intType = (IntegerType) argType;
                            if (intType.getMinBits() < 32) {
                                args[i] = gf.truncate(args[i], intType);
                            }
                        }
                    }
                    if (opcode != OP_INVOKESTATIC) {
                        // pop the receiver
                        v1 = pop1();
                    } else {
                        // definite initialization
                        v1 = null;
                    }
                    if (target instanceof ConstructorElement) {
                        if (opcode != OP_INVOKESPECIAL) {
                            throw new InvalidByteCodeException();
                        }
                        v2 = gf.invokeConstructor(v1, (ConstructorElement) target, List.of(args));
                        replaceAll(v1, v2);
                    } else {
                        assert target instanceof MethodElement;
                        MethodElement method = (MethodElement) target;
                        Type returnType = method.getReturnType();
                        if (returnType == ts.getVoidType()) {
                            if (opcode == OP_INVOKESTATIC) {
                                // return type is implicitly void
                                gf.invokeStatic(method, List.of(args));
                            } else {
                                // return type is implicitly void
                                gf.invokeInstance(DispatchInvocation.Kind.fromOpcode(opcode), v1, method, List.of(args));
                            }
                        } else {
                            Value result;
                            if (opcode == OP_INVOKESTATIC) {
                                result = gf.invokeValueStatic(method, List.of(args));
                            } else {
                                result = gf.invokeValueInstance(DispatchInvocation.Kind.fromOpcode(opcode), v1, method, List.of(args));
                            }
                            if (returnType instanceof IntegerType) {
                                IntegerType intType = (IntegerType) returnType;
                                if (intType.getMinBits() < 32) {
                                    // extend it back out again
                                    if (intType instanceof UnsignedIntegerType) {
                                        // first extend, then cast
                                        result = gf.extend(result, ts.getUnsignedInteger32Type());
                                        result = gf.bitCast(result, ts.getSignedInteger32Type());
                                    } else {
                                        result = gf.extend(result, ts.getSignedInteger32Type());
                                    }
                                }
                            }
                            if (method.hasClass2ReturnType()) {
                                result = fatten(result);
                            }
                            push(result);
                        }
                    }
                    break;
                case OP_INVOKEDYNAMIC:
                    ctxt.getCompilationContext().error(gf.getLocation(), "`invokedynamic` not yet supported");
                    gf.throw_(lf.literalOfNull());
                    return;
                case OP_NEW:
                    push(gf.new_(getConstantValue(buffer.getShort() & 0xffff, ClassTypeIdLiteral.class)));
                    break;
                case OP_NEWARRAY:
                    ArrayTypeIdLiteral arrayType;
                    switch (buffer.get() & 0xff) {
                        case T_BOOLEAN: arrayType = lf.literalOfArrayType(ts.getBooleanType()); break;
                        case T_CHAR: arrayType = lf.literalOfArrayType(ts.getUnsignedInteger16Type()); break;
                        case T_FLOAT: arrayType = lf.literalOfArrayType(ts.getFloat32Type()); break;
                        case T_DOUBLE: arrayType = lf.literalOfArrayType(ts.getFloat64Type()); break;
                        case T_BYTE: arrayType = lf.literalOfArrayType(ts.getSignedInteger8Type()); break;
                        case T_SHORT: arrayType = lf.literalOfArrayType(ts.getSignedInteger16Type()); break;
                        case T_INT: arrayType = lf.literalOfArrayType(ts.getSignedInteger32Type()); break;
                        case T_LONG: arrayType = lf.literalOfArrayType(ts.getSignedInteger64Type()); break;
                        default: throw new InvalidByteCodeException();
                    }
                    // todo: check for negative array size
                    push(gf.newArray(arrayType, pop1()));
                    break;
                case OP_ANEWARRAY: {
                    arrayType = lf.literalOfArrayType(ts.getReferenceType(getConstantValue(buffer.getShort() & 0xffff, TypeIdLiteral.class)));
                    // todo: check for negative array size
                    push(gf.newArray(arrayType, pop1()));
                    break;
                }
                case OP_ARRAYLENGTH:
                    push(gf.arrayLength(pop1()));
                    break;
                case OP_ATHROW:
                    gf.throw_(pop1());
                    // terminate
                    return;
                case OP_CHECKCAST: {
                    TypeIdLiteral expected = getConstantValue(buffer.getShort() & 0xffff, TypeIdLiteral.class);
                    v1 = peek();
                    BlockLabel okHandle = new BlockLabel();
                    BlockLabel notNullHandle = new BlockLabel();
                    gf.if_(gf.cmpEq(v1, lf.literalOfNull()), okHandle, notNullHandle);
                    gf.begin(notNullHandle);
                    BlockLabel castFailedHandle = new BlockLabel();
                    v2 = gf.typeIdOf(v1);
                    gf.if_(gf.cmpGe(expected, v2), okHandle, castFailedHandle);
                    gf.begin(castFailedHandle);
                    gf.classCastException(v2, expected);
                    // do not change stack depth ending here
                    gf.begin(okHandle);
                    replaceAll(v1, gf.narrow(v1, expected));
                    break;
                }
                case OP_INSTANCEOF: {
                    TypeIdLiteral expected = getConstantValue(buffer.getShort() & 0xffff, TypeIdLiteral.class);
                    v1 = pop1();
                    BlockLabel mergeHandle = new BlockLabel();
                    BlockLabel notNullHandle = new BlockLabel();
                    BasicBlock t1 = gf.if_(gf.cmpEq(v1, lf.literalOfNull()), mergeHandle, notNullHandle);
                    gf.begin(notNullHandle);
                    v1 = gf.cmpGe(expected, gf.typeIdOf(v1));
                    BasicBlock t2 = gf.goto_(mergeHandle);
                    gf.begin(mergeHandle);
                    PhiValue phi = gf.phi(ts.getBooleanType(), mergeHandle);
                    phi.setValueForBlock(t1, lf.literalOf(false));
                    phi.setValueForBlock(t2, v1);
                    push(phi);
                    break;
                }
                case OP_MONITORENTER:
                    gf.monitorEnter(pop());
                    break;
                case OP_MONITOREXIT:
                    gf.monitorExit(pop());
                    break;
                case OP_MULTIANEWARRAY:
                    int cpIdx = buffer.getShort() & 0xffff;
                    Value[] dims = new Value[buffer.get() & 0xff];
                    if (dims.length == 0) {
                        throw new InvalidByteCodeException();
                    }
                    for (int i = 0; i < dims.length; i ++) {
                        dims[i] = pop1();
                    }
                    push(gf.multiNewArray(getConstantValue(cpIdx, ArrayTypeIdLiteral.class), dims));
                    break;
                case OP_IFNULL:
                    processIf(buffer, gf.cmpEq(pop(), lf.literalOfNull()), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFNONNULL:
                    processIf(buffer, gf.cmpNe(pop(), lf.literalOfNull()), buffer.getShort() + src, buffer.position());
                    return;
                default:
                    throw new InvalidByteCodeException();
            }
            // now check to see if the new position is an entry point
            int epIdx = info.getEntryPointIndex(buffer.position());
            if (epIdx >= 0 && info.getEntryPointSourceCount(epIdx) > 1) {
                // two or more blocks enter here; start a new block via goto
                processBlock(gf.goto_(blockHandles[epIdx]));
                return;
            }
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
        BlockLabel b1 = getBlockForIndex(dest1);
        BlockLabel b2 = getBlockForIndex(dest2);
        BasicBlock from = gf.if_(cond, b1, b2);
        Value[] varSnap = saveLocals();
        Value[] stackSnap = saveStack();
        buffer.position(dest1);
        processBlock(from);
        restoreStack(stackSnap);
        restoreLocals(varSnap);
        buffer.position(dest2);
        processBlock(from);
    }

    private ClassFileImpl getClassFile() {
        return info.getClassFile();
    }

    private Type getTypeOfFieldRef(final int fieldRef) {
        return getClassFile().resolveSingleDescriptor(getClassFile().getFieldrefConstantDescriptorIdx(fieldRef));
    }

    private String getNameOfFieldRef(final int fieldRef) {
        return getClassFile().getFieldrefConstantName(fieldRef);
    }

    private TypeIdLiteral getOwnerOfFieldRef(final int fieldRef) {
        return resolveClass(getClassFile().getFieldrefConstantClassName(fieldRef));
    }

    private String getNameOfMethodRef(final int methodRef) {
        return getClassFile().getMethodrefConstantName(methodRef);
    }

    private boolean nameOfMethodRefEquals(final int methodRef, final String expected) {
        return getClassFile().methodrefConstantNameEquals(methodRef, expected);
    }

    private TypeIdLiteral getOwnerOfMethodRef(final int methodRef) {
        return resolveClass(getClassFile().getMethodrefConstantClassName(methodRef));
    }

    private int getNameAndTypeOfMethodRef(final int methodRef) {
        return getClassFile().getMethodrefNameAndTypeIndex(methodRef);
    }

    private String getNameOfInterfaceMethodRef(final int methodRef) {
        return getClassFile().getInterfaceMethodrefConstantName(methodRef);
    }

    private boolean nameOfInterfaceMethodRefEquals(final int methodRef, final String expected) {
        return getClassFile().interfaceMethodrefConstantNameEquals(methodRef, expected);
    }

    private TypeIdLiteral getOwnerOfInterfaceMethodRef(final int methodRef) {
        return resolveClass(getClassFile().getInterfaceMethodrefConstantClassName(methodRef));
    }

    private int getNameAndTypeOfInterfaceMethodRef(final int methodRef) {
        return getClassFile().getInterfaceMethodrefNameAndTypeIndex(methodRef);
    }

    private FieldElement resolveTargetOfFieldRef(final int fieldRef) {
        ValidatedTypeDefinition definition = ctxt.resolveDefinedTypeLiteral(getOwnerOfFieldRef(fieldRef)).validate();
        FieldElement field = definition.resolve().resolveField(getTypeOfFieldRef(fieldRef), getNameOfFieldRef(fieldRef));
        if (field == null) {
            // todo
            throw new IllegalStateException();
        }
        return field;
    }

    private ParameterizedExecutableDescriptor resolveMethodDescriptor(final TypeIdLiteral owner, final int nameAndTypeRef) {
        int idx;
        ClassFileImpl classFile = getClassFile();
        int descIdx = classFile.getNameAndTypeConstantDescriptorIdx(nameAndTypeRef);
        ResolvedTypeDefinition resolved = ctxt.resolveDefinedTypeLiteral(owner).validate().resolve();
        if (classFile.nameAndTypeConstantNameEquals(nameAndTypeRef, "<init>")) {
            // constructor
            return classFile.getConstructorDescriptor(descIdx);
        } else {
            // method
            return classFile.getMethodDescriptor(descIdx);
        }
    }

    private ParameterizedExecutableElement resolveTargetOfDescriptor(ResolvedTypeDefinition resolved, final ParameterizedExecutableDescriptor desc, final int nameAndTypeRef) {
        int idx;
        if (desc instanceof ConstructorDescriptor) {
            idx = resolved.findConstructorIndex((ConstructorDescriptor) desc);
            return idx == -1 ? null : resolved.getConstructor(idx);
        } else {
            idx = resolved.findMethodIndex(getClassFile().getNameAndTypeConstantName(nameAndTypeRef), (MethodDescriptor) desc);
            return idx == -1 ? null : resolved.getMethod(idx);
        }
    }

    private MethodElement resolveVirtualTargetOfDescriptor(ResolvedTypeDefinition resolved, final ParameterizedExecutableDescriptor desc, final int nameAndTypeRef) {
        return resolved.resolveMethodElementVirtual(getClassFile().getNameAndTypeConstantName(nameAndTypeRef), (MethodDescriptor) desc);
    }

    private MethodElement resolveInterfaceTargetOfDescriptor(ResolvedTypeDefinition resolved, final ParameterizedExecutableDescriptor desc, final int nameAndTypeRef) {
        return resolved.resolveMethodElementInterface(getClassFile().getNameAndTypeConstantName(nameAndTypeRef), (MethodDescriptor) desc);
    }

    private ParameterizedExecutableElement resolveTargetOfMethodNameAndType(final TypeIdLiteral owner, final int nameAndTypeRef) {
        int idx;
        ClassFileImpl classFile = getClassFile();
        int descIdx = classFile.getNameAndTypeConstantDescriptorIdx(nameAndTypeRef);
        ResolvedTypeDefinition resolved = ctxt.resolveDefinedTypeLiteral(owner).validate().resolve();
        if (classFile.nameAndTypeConstantNameEquals(nameAndTypeRef, "<init>")) {
            // constructor
            ConstructorDescriptor desc = classFile.getConstructorDescriptor(descIdx);
            idx = resolved.findConstructorIndex(desc);
            return idx == -1 ? null : resolved.getConstructor(idx);
        } else {
            // method
            MethodDescriptor desc = classFile.getMethodDescriptor(descIdx);
            idx = resolved.findMethodIndex(classFile.getNameAndTypeConstantName(nameAndTypeRef), desc);
            return idx == -1 ? null : resolved.getMethod(idx);
        }
    }

    private Type resolveDescriptor(int cpIdx) {
        return getClassFile().resolveSingleDescriptor(cpIdx);
    }

    private TypeIdLiteral resolveClass(String name) {
        return getClassFile().resolveSingleType(name);
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
        boolean exited;

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
