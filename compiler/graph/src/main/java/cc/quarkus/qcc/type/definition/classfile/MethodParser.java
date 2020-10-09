package cc.quarkus.qcc.type.definition.classfile;

import static cc.quarkus.qcc.type.definition.classfile.ClassFile.*;
import static cc.quarkus.qcc.type.definition.classfile.DefinedMethodBody.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.graph.ArrayClassType;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.ConstantValue;
import cc.quarkus.qcc.graph.Goto;
import cc.quarkus.qcc.graph.GraphFactory;
import cc.quarkus.qcc.graph.GraphVisitor;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.InstanceInvocation;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.Jsr;
import cc.quarkus.qcc.graph.LineNumberGraphFactory;
import cc.quarkus.qcc.graph.NodeHandle;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Ret;
import cc.quarkus.qcc.graph.Switch;
import cc.quarkus.qcc.graph.TryInstanceInvocation;
import cc.quarkus.qcc.graph.TryInstanceInvocationValue;
import cc.quarkus.qcc.graph.TryInvocation;
import cc.quarkus.qcc.graph.TryInvocationValue;
import cc.quarkus.qcc.graph.TryThrow;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.WordType;
import cc.quarkus.qcc.interpreter.JavaVM;
import cc.quarkus.qcc.type.definition.ResolvedTypeDefinition;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;
import cc.quarkus.qcc.type.descriptor.ConstructorDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

final class MethodParser {
    static final ConstantValue INT_SHIFT_MASK = Value.const_(0x1f);
    static final ConstantValue LONG_SHIFT_MASK = Value.const_(0x3f);
    final VerifiedMethodBody verifiedMethodBody;
    final Value[] stack;
    final Value[] locals;
    final NodeHandle[] blockHandles;
    private Map<BasicBlock, Value[]> retStacks;
    private Map<BasicBlock, Value[]> retLocals;
    private final LineNumberGraphFactory gf;
    int sp;
    NodeHandle currentBlockHandle;

    MethodParser(final VerifiedMethodBody verifiedMethodBody, final GraphFactory graphFactory) {
        this.verifiedMethodBody = verifiedMethodBody;
        DefinedMethodBody definedBody = verifiedMethodBody.getDefinedBody();
        stack = new Value[definedBody.getMaxStack()];
        locals = new Value[definedBody.getMaxLocals()];
        int cnt = definedBody.getEntryPointCount();
        NodeHandle[] blockHandles = new NodeHandle[cnt];
        int dest = -1;
        // make a "canonical" node handle for each block
        for (int i = 0; i < cnt; i ++) {
            blockHandles[i] = new NodeHandle();
        }
        this.blockHandles = blockHandles;
        // it's not an entry point
        currentBlockHandle = new NodeHandle();
        gf = new LineNumberGraphFactory(graphFactory);
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

    Value pop(Type assignableType) {
        Value v = pop(assignableType.isClass2Type());
        if (! assignableType.isAssignableFrom(v.getType())) {
            throw new TypeMismatchException();
        }
        return v;
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

    Value pop1() {
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
            Value v2 = pop1();
            Value v1 = pop1();
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
        Value v2 = pop1();
        Value v1 = pop1();
        push(v2);
        push(v1);
    }

    Value promote(GraphFactory.Context ctxt, Value value) {
        // we must automatically promote values we push on the stack
        Type type = value.getType();
        if (type == Type.S8 || type == Type.S16) {
            return gf.extend(ctxt, value, Type.S32);
        } else if (type == Type.U16) {
            return gf.bitCast(ctxt, gf.extend(ctxt, value, Type.U32), Type.S32);
        } else if (type == Type.BOOL) {
            return gf.if_(ctxt, value, Value.const_(1), Value.const_(0));
        } else {
            return value;
        }
    }

    Value demote(GraphFactory.Context ctxt, Value value, Type toType) {
        Type type = value.getType();
        if (type == Type.S32) {
            if (toType == Type.S8 || toType == Type.S16 || toType == Type.U16) {
                return gf.truncate(ctxt, value, (WordType) toType);
            } else if (toType == Type.BOOL) {
                return gf.cmpNe(ctxt, value, Value.const_(0, type));
            }
        }
        return value;
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
        if (value.getType().isClass2Type()) {
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

    Value getConstantValue(int cpIndex) {
        ClassFileImpl classFile = getClassFile();
        int constantType = classFile.getConstantType(cpIndex);
        switch (constantType) {
            case CONSTANT_Class: return Value.const_(classFile.resolveSingleType(cpIndex));
            case CONSTANT_String: return Value.const_(classFile.getStringConstant(cpIndex));
            case CONSTANT_Integer: return Value.const_(classFile.getIntConstant(cpIndex));
            case CONSTANT_Float: return Value.const_(classFile.getFloatConstant(cpIndex));
            case CONSTANT_Long: return Value.const_(classFile.getLongConstant(cpIndex));
            case CONSTANT_Double: return Value.const_(classFile.getDoubleConstant(cpIndex));
            default: {
                throw new IllegalArgumentException("Unexpected constant type at index " + cpIndex);
            }
        }
    }

    NodeHandle getBlockForIndex(int target) {
        int idx = verifiedMethodBody.getDefinedBody().getEntryPointIndex(target);
        if (idx < 0) {
            throw new IllegalStateException("Block not found");
        }
        return blockHandles[idx];
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

    Map<NodeHandle, PhiValue[]> entryLocalsArrays = new HashMap<>();
    Map<NodeHandle, PhiValue[]> entryStacks = new HashMap<>();

    /**
     * Process a single block.  The current stack and locals are used as a template for the phi value types within
     * the block.  At exit the stack and locals are in an indeterminate state.
     *
     * @param buffer the bytecode buffer
     * @param from the source (exiting) block
     */
    void processBlock(ByteBuffer buffer, BasicBlock from) {
        // this is the canonical map key handle
        NodeHandle block = getBlockForIndex(buffer.position());
        assert block != null : "No block registered for BCI " + buffer.position();
        PhiValue[] entryLocalsArray;
        PhiValue[] entryStack;
        BasicBlock resolvedBlock;
        if (entryStacks.containsKey(block)) {
            // already registered
            entryLocalsArray = entryLocalsArrays.get(block);
            entryStack = entryStacks.get(block);
        } else {
            // not registered yet; process new block first
            entryLocalsArray = new PhiValue[locals.length];
            entryStack = new PhiValue[sp];
            for (int i = 0; i < locals.length; i ++) {
                if (locals[i] != null) {
                    entryLocalsArray[i] = gf.phi(locals[i].getType(), block);
                }
            }
            for (int i = 0; i < sp; i ++) {
                if (stack[i] != null) {
                    entryStack[i] = gf.phi(stack[i].getType(), block);
                }
            }
            entryLocalsArrays.put(block, entryLocalsArray);
            entryStacks.put(block, entryStack);
            restoreStack(entryStack);
            restoreLocals(entryLocalsArray);
            processNewBlock(buffer, block);
        }
        // complete phis
        for (int i = 0; i < locals.length; i ++) {
            if (locals[i] != null) {
                PhiValue phiValue = entryLocalsArray[i];
                // some local slots will be empty
                if (phiValue != null) {
                    phiValue.setValueForBlock(from, locals[i]);
                }
            }
        }
        for (int i = 0; i < sp; i ++) {
            Value old = stack[i];
            if (old != null) {
                entryStack[i].setValueForBlock(from, old);
            }
        }
    }

    void processNewBlock(ByteBuffer buffer, final NodeHandle block) {
        assert ! block.hasTarget() : "Block entered twice";
        GraphFactory.Context ctxt = new GraphFactory.Context(block);
        Value v1, v2, v3, v4;
        int opcode;
        int src;
        boolean wide;
        DefinedMethodBody definedBody = verifiedMethodBody.getDefinedBody();
        while (buffer.hasRemaining()) {
            src = buffer.position();
            gf.setBytecodeIndex(src);
            gf.setLineNumber(definedBody.getLineNumber(src));
            opcode = buffer.get() & 0xff;
            wide = opcode == OP_WIDE;
            if (wide) {
                opcode = buffer.get() & 0xff;
            }
            switch (opcode) {
                case OP_NOP:
                    break;
                case OP_ACONST_NULL:
                    push(Value.NULL);
                    break;
                case OP_ICONST_M1:
                case OP_ICONST_0:
                case OP_ICONST_1:
                case OP_ICONST_2:
                case OP_ICONST_3:
                case OP_ICONST_4:
                case OP_ICONST_5:
                    push(Value.const_(opcode - OP_ICONST_0));
                    break;
                case OP_LCONST_0:
                case OP_LCONST_1:
                    push(Value.const_((long)opcode - OP_LCONST_0));
                    break;
                case OP_FCONST_0:
                case OP_FCONST_1:
                case OP_FCONST_2:
                    push(Value.const_((float) opcode - OP_FCONST_0));
                    break;
                case OP_DCONST_0:
                case OP_DCONST_1:
                    push(Value.const_((double) opcode - OP_DCONST_0));
                    break;
                case OP_BIPUSH:
                    push(Value.const_(buffer.get()));
                    break;
                case OP_SIPUSH:
                    push(Value.const_(buffer.getShort()));
                    break;
                case OP_LDC:
                    v1 = getConstantValue(buffer.get() & 0xff);
                    if (v1.getType().isClass2Type()) {
                        throw new InvalidConstantException();
                    }
                    push(v1);
                    break;
                case OP_LDC_W:
                    v1 = getConstantValue(buffer.getShort() & 0xffff);
                    if (v1.getType().isClass2Type()) {
                        throw new InvalidConstantException();
                    }
                    push(v1);
                    break;
                case OP_LDC2_W:
                    v1 = getConstantValue(buffer.getShort() & 0xffff);
                    if (! v1.getType().isClass2Type()) {
                        throw new InvalidConstantException();
                    }
                    push(v1);
                    break;
                case OP_ILOAD:
                    push(promote(ctxt, getLocal(getWidenableValue(buffer, wide))));
                    break;
                case OP_LLOAD:
                    push(getLocal(getWidenableValue(buffer, wide), Type.S64));
                    break;
                case OP_FLOAD:
                    push(getLocal(getWidenableValue(buffer, wide), Type.F32));
                    break;
                case OP_DLOAD:
                    push(getLocal(getWidenableValue(buffer, wide), Type.F64));
                    break;
                case OP_ALOAD:
                    push(getLocal(getWidenableValue(buffer, wide)));
                    break;
                case OP_ILOAD_0:
                case OP_ILOAD_1:
                case OP_ILOAD_2:
                case OP_ILOAD_3:
                    push(promote(ctxt, getLocal(opcode - OP_ILOAD_0)));
                    break;
                case OP_LLOAD_0:
                case OP_LLOAD_1:
                case OP_LLOAD_2:
                case OP_LLOAD_3:
                    push(getLocal(opcode - OP_LLOAD_0, Type.S64));
                    break;
                case OP_FLOAD_0:
                case OP_FLOAD_1:
                case OP_FLOAD_2:
                case OP_FLOAD_3:
                    push(getLocal(opcode - OP_FLOAD_0, Type.F32));
                    break;
                case OP_DLOAD_0:
                case OP_DLOAD_1:
                case OP_DLOAD_2:
                case OP_DLOAD_3:
                    push(getLocal(opcode - OP_DLOAD_0, Type.F64));
                    break;
                case OP_ALOAD_0:
                case OP_ALOAD_1:
                case OP_ALOAD_2:
                case OP_ALOAD_3:
                    push(getLocal(opcode - OP_ALOAD_0));
                    break;
                case OP_IALOAD:
                    v1 = pop(Type.JAVA_INT_ARRAY);
                    v2 = pop1();
                    v1 = gf.readArrayValue(ctxt, v1, v2, JavaAccessMode.PLAIN);
                    push(v1);
                    break;
                case OP_LALOAD:
                    v1 = pop(Type.JAVA_LONG_ARRAY);
                    v2 = pop1();
                    v1 = gf.readArrayValue(ctxt, v1, v2, JavaAccessMode.PLAIN);
                    push(v1);
                    break;
                case OP_FALOAD:
                    v1 = pop(Type.JAVA_FLOAT_ARRAY);
                    v2 = pop1();
                    v1 = gf.readArrayValue(ctxt, v1, v2, JavaAccessMode.PLAIN);
                    push(v1);
                    break;
                case OP_DALOAD:
                    v1 = pop(Type.JAVA_DOUBLE_ARRAY);
                    v2 = pop1();
                    v1 = gf.readArrayValue(ctxt, v1, v2, JavaAccessMode.PLAIN);
                    push(v1);
                    break;
                case OP_AALOAD:
                    v1 = pop1(); // XXX: make sure it's a ref type
                    v2 = pop1();
                    v1 = gf.readArrayValue(ctxt, v1, v2, JavaAccessMode.PLAIN);
                    push(v1);
                    break;
                case OP_BALOAD:
                    v1 = pop1();
                    if (v1.getType() != Type.JAVA_BYTE_ARRAY && v1.getType() != Type.JAVA_BOOLEAN_ARRAY) {
                        throw new TypeMismatchException();
                    }
                    v2 = pop1();
                    v1 = gf.readArrayValue(ctxt, v1, v2, JavaAccessMode.PLAIN);
                    push(promote(ctxt, v1));
                    break;
                case OP_CALOAD:
                    v1 = pop(Type.JAVA_CHAR_ARRAY);
                    v2 = pop1();
                    v1 = gf.readArrayValue(ctxt, v1, v2, JavaAccessMode.PLAIN);
                    push(promote(ctxt, v1));
                    break;
                case OP_SALOAD:
                    v1 = pop(Type.JAVA_SHORT_ARRAY);
                    v2 = pop1();
                    v1 = gf.readArrayValue(ctxt, v1, v2, JavaAccessMode.PLAIN);
                    push(promote(ctxt, v1));
                    break;
                case OP_ISTORE:
                    setLocal(getWidenableValue(buffer, wide), pop(Type.S32));
                    break;
                case OP_LSTORE:
                    setLocal(getWidenableValue(buffer, wide), pop(Type.S64));
                    break;
                case OP_FSTORE:
                    setLocal(getWidenableValue(buffer, wide), pop(Type.F32));
                    break;
                case OP_DSTORE:
                    setLocal(getWidenableValue(buffer, wide), pop(Type.F64));
                    break;
                case OP_ASTORE:
                    setLocal(getWidenableValue(buffer, wide), pop()); // XXX: verify object type
                    break;
                case OP_ISTORE_0:
                case OP_ISTORE_1:
                case OP_ISTORE_2:
                case OP_ISTORE_3:
                    setLocal(opcode - OP_ISTORE_0, pop(Type.S32));
                    break;
                case OP_LSTORE_0:
                case OP_LSTORE_1:
                case OP_LSTORE_2:
                case OP_LSTORE_3:
                    setLocal(opcode - OP_LSTORE_0, pop(Type.S64));
                    break;
                case OP_FSTORE_0:
                case OP_FSTORE_1:
                case OP_FSTORE_2:
                case OP_FSTORE_3:
                    setLocal(opcode - OP_FSTORE_0, pop(Type.F32));
                    break;
                case OP_DSTORE_0:
                case OP_DSTORE_1:
                case OP_DSTORE_2:
                case OP_DSTORE_3:
                    setLocal(opcode - OP_DSTORE_0, pop(Type.F64));
                    break;
                case OP_ASTORE_0:
                case OP_ASTORE_1:
                case OP_ASTORE_2:
                case OP_ASTORE_3:
                    setLocal(opcode - OP_ASTORE_0, pop()); // XXX: verify object type
                    break;
                case OP_IASTORE:
                    gf.writeArrayValue(ctxt, pop(Type.JAVA_INT_ARRAY), pop(Type.S32), pop(Type.S32), JavaAccessMode.PLAIN);
                    break;
                case OP_LASTORE:
                    gf.writeArrayValue(ctxt, pop(Type.JAVA_LONG_ARRAY), pop(Type.S32), pop(Type.S64), JavaAccessMode.PLAIN);
                    break;
                case OP_FASTORE:
                    gf.writeArrayValue(ctxt, pop(Type.JAVA_FLOAT_ARRAY), pop(Type.S32), pop(Type.F32), JavaAccessMode.PLAIN);
                    break;
                case OP_DASTORE:
                    gf.writeArrayValue(ctxt, pop(Type.JAVA_DOUBLE_ARRAY), pop(Type.S32), pop(Type.F64), JavaAccessMode.PLAIN);
                    break;
                case OP_AASTORE:
                    gf.writeArrayValue(ctxt, pop(/* Object[] */), pop(Type.S32), pop(/* Object */), JavaAccessMode.PLAIN);
                    break;
                case OP_BASTORE:
                    v1 = pop();
                    gf.writeArrayValue(ctxt, v1, pop(Type.S32), demote(ctxt, pop(Type.S32), ((ArrayClassType)v1.getType()).getElementType()), JavaAccessMode.PLAIN);
                    break;
                case OP_CASTORE:
                    gf.writeArrayValue(ctxt, pop(Type.JAVA_CHAR_ARRAY), pop(Type.S32), demote(ctxt, pop(Type.S32), Type.U16), JavaAccessMode.PLAIN);
                    break;
                case OP_SASTORE:
                    gf.writeArrayValue(ctxt, pop(Type.JAVA_SHORT_ARRAY), pop(Type.S32), demote(ctxt, pop(Type.S32), Type.S16), JavaAccessMode.PLAIN);
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
                    if (! topOfStackType().isClass2Type()) {
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
                    if (! topOfStackType().isClass2Type()) {
                        v1 = pop1();
                        v2 = pop1();
                        if (! topOfStackType().isClass2Type()) {
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
                        if (! topOfStackType().isClass2Type()) {
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
                    push(gf.add(ctxt, pop(Type.S32), pop(Type.S32)));
                    break;
                case OP_LADD:
                    push(gf.add(ctxt, pop(Type.S64), pop(Type.S64)));
                    break;
                case OP_FADD:
                    push(gf.add(ctxt, pop(Type.F32), pop(Type.F32)));
                    break;
                case OP_DADD:
                    push(gf.add(ctxt, pop(Type.F64), pop(Type.F64)));
                    break;
                case OP_ISUB:
                    push(gf.sub(ctxt, pop(Type.S32), pop(Type.S32)));
                    break;
                case OP_LSUB:
                    push(gf.sub(ctxt, pop(Type.S64), pop(Type.S64)));
                    break;
                case OP_FSUB:
                    push(gf.sub(ctxt, pop(Type.F32), pop(Type.F32)));
                    break;
                case OP_DSUB:
                    push(gf.sub(ctxt, pop(Type.F64), pop(Type.F64)));
                    break;
                case OP_IMUL:
                    push(gf.multiply(ctxt, pop(Type.S32), pop(Type.S32)));
                    break;
                case OP_LMUL:
                    push(gf.multiply(ctxt, pop(Type.S64), pop(Type.S64)));
                    break;
                case OP_FMUL:
                    push(gf.multiply(ctxt, pop(Type.F32), pop(Type.F32)));
                    break;
                case OP_DMUL:
                    push(gf.multiply(ctxt, pop(Type.F64), pop(Type.F64)));
                    break;
                case OP_IDIV:
                    push(gf.divide(ctxt, pop(Type.S32), pop(Type.S32)));
                    break;
                case OP_LDIV:
                    push(gf.divide(ctxt, pop(Type.S64), pop(Type.S64)));
                    break;
                case OP_FDIV:
                    push(gf.divide(ctxt, pop(Type.F32), pop(Type.F32)));
                    break;
                case OP_DDIV:
                    push(gf.divide(ctxt, pop(Type.F64), pop(Type.F64)));
                    break;
                case OP_IREM:
                    push(gf.remainder(ctxt, pop(Type.S32), pop(Type.S32)));
                    break;
                case OP_LREM:
                    push(gf.remainder(ctxt, pop(Type.S64), pop(Type.S64)));
                    break;
                case OP_FREM:
                    push(gf.remainder(ctxt, pop(Type.F32), pop(Type.F32)));
                    break;
                case OP_DREM:
                    push(gf.remainder(ctxt, pop(Type.F64), pop(Type.F64)));
                    break;
                case OP_FNEG:
                    push(gf.negate(ctxt, pop(Type.F32)));
                    break;
                case OP_DNEG:
                    push(gf.negate(ctxt, pop(Type.F64)));
                    break;
                case OP_ISHL:
                    push(gf.shl(ctxt, pop(Type.S32), pop(Type.S32)));
                    break;
                case OP_LSHL:
                    push(gf.shl(ctxt, pop(Type.S64), pop(Type.S32)));
                    break;
                case OP_ISHR:
                    push(gf.shr(ctxt, pop(Type.S32), pop(Type.S32)));
                    break;
                case OP_LSHR:
                    push(gf.shr(ctxt, pop(Type.S64), pop(Type.S32)));
                    break;
                case OP_IUSHR:
                    // there is no unsigned shift operation, just shift on unsigned types
                    push(gf.bitCast(ctxt, gf.shr(ctxt, gf.bitCast(ctxt, pop(Type.S32), Type.U32), pop(Type.S32)), Type.S32));
                    break;
                case OP_LUSHR:
                    // there is no unsigned shift operation, just shift on unsigned types
                    push(gf.bitCast(ctxt, gf.shr(ctxt, gf.bitCast(ctxt, pop(Type.S64), Type.U64), pop(Type.S32)), Type.S64));
                    break;
                case OP_IAND:
                    push(gf.and(ctxt, pop(Type.S32), pop(Type.S32)));
                    break;
                case OP_LAND:
                    push(gf.and(ctxt, pop(Type.S64), pop(Type.S64)));
                    break;
                case OP_IOR:
                    push(gf.or(ctxt, pop(Type.S32), pop(Type.S32)));
                    break;
                case OP_LOR:
                    push(gf.or(ctxt, pop(Type.S64), pop(Type.S64)));
                    break;
                case OP_IXOR:
                    push(gf.xor(ctxt, pop(Type.S32), pop(Type.S32)));
                    break;
                case OP_LXOR:
                    push(gf.xor(ctxt, pop(Type.S64), pop(Type.S64)));
                    break;
                case OP_IINC:
                    int idx = getWidenableValue(buffer, wide);
                    setLocal(idx, gf.add(ctxt, getLocal(idx, Type.S32), Value.const_(getWidenableValueSigned(buffer, wide))));
                    break;
                case OP_I2L:
                    push(gf.extend(ctxt, pop(Type.S32), Type.S64));
                    break;
                case OP_I2F:
                    push(gf.valueConvert(ctxt, pop(Type.S32), Type.F32));
                    break;
                case OP_I2D:
                    push(gf.valueConvert(ctxt, pop(Type.S32), Type.F64));
                    break;
                case OP_L2I:
                    push(gf.truncate(ctxt, pop(Type.S64), Type.S32));
                    break;
                case OP_L2F:
                    push(gf.valueConvert(ctxt, pop(Type.S64), Type.F32));
                    break;
                case OP_L2D:
                    push(gf.valueConvert(ctxt, pop(Type.S64), Type.F64));
                    break;
                case OP_F2I:
                    push(gf.valueConvert(ctxt, pop(Type.F32), Type.S32));
                    break;
                case OP_F2L:
                    push(gf.valueConvert(ctxt, pop(Type.F32), Type.S64));
                    break;
                case OP_F2D:
                    push(gf.extend(ctxt, pop(Type.F32), Type.F64));
                    break;
                case OP_D2I:
                    push(gf.valueConvert(ctxt, pop(Type.F64), Type.S32));
                    break;
                case OP_D2L:
                    push(gf.valueConvert(ctxt, pop(Type.F64), Type.S64));
                    break;
                case OP_D2F:
                    push(gf.truncate(ctxt, pop(Type.F64), Type.F32));
                    break;
                case OP_I2B:
                    push(gf.extend(ctxt, gf.truncate(ctxt, pop(Type.S32), Type.S8), Type.S32));
                    break;
                case OP_I2C:
                    push(gf.extend(ctxt, gf.truncate(ctxt, pop(Type.S32), Type.U16), Type.S32));
                    break;
                case OP_I2S:
                    push(gf.extend(ctxt, gf.truncate(ctxt, pop(Type.S32), Type.S16), Type.S32));
                    break;
                case OP_LCMP:
                    v2 = pop(Type.S64);
                    v1 = pop(Type.S64);
                    v3 = gf.cmpLt(ctxt, v1, v2);
                    v4 = gf.cmpGt(ctxt, v1, v2);
                    push(gf.if_(ctxt, v3, Value.const_(-1), gf.if_(ctxt, v4, Value.const_(1), Value.const_(0))));
                    break;
                case OP_FCMPL:
                    v2 = pop(Type.F32);
                    v1 = pop(Type.F32);
                    v3 = gf.cmpLt(ctxt, v1, v2);
                    v4 = gf.cmpGt(ctxt, v1, v2);
                    push(gf.if_(ctxt, v3, Value.const_(-1), gf.if_(ctxt, v4, Value.const_(1), Value.const_(0))));
                    break;
                case OP_FCMPG:
                    v2 = pop(Type.F32);
                    v1 = pop(Type.F32);
                    v3 = gf.cmpLt(ctxt, v1, v2);
                    v4 = gf.cmpGt(ctxt, v1, v2);
                    push(gf.if_(ctxt, v4, Value.const_(1), gf.if_(ctxt, v3, Value.const_(-1), Value.const_(0))));
                    break;
                case OP_DCMPL:
                    v2 = pop(Type.F64);
                    v1 = pop(Type.F64);
                    v3 = gf.cmpLt(ctxt, v1, v2);
                    v4 = gf.cmpGt(ctxt, v1, v2);
                    push(gf.if_(ctxt, v3, Value.const_(-1), gf.if_(ctxt, v4, Value.const_(1), Value.const_(0))));
                    break;
                case OP_DCMPG:
                    v2 = pop(Type.F64);
                    v1 = pop(Type.F64);
                    v3 = gf.cmpLt(ctxt, v1, v2);
                    v4 = gf.cmpGt(ctxt, v1, v2);
                    push(gf.if_(ctxt, v4, Value.const_(1), gf.if_(ctxt, v3, Value.const_(-1), Value.const_(0))));
                    break;
                case OP_IFEQ:
                    processIf(buffer, ctxt, gf.cmpEq(ctxt, pop(Type.S32), Value.ICONST_0), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFNE:
                    processIf(buffer, ctxt, gf.cmpNe(ctxt, pop(Type.S32), Value.ICONST_0), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFLT:
                    processIf(buffer, ctxt, gf.cmpLt(ctxt, pop(Type.S32), Value.ICONST_0), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFGE:
                    processIf(buffer, ctxt, gf.cmpGe(ctxt, pop(Type.S32), Value.ICONST_0), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFGT:
                    processIf(buffer, ctxt, gf.cmpGt(ctxt, pop(Type.S32), Value.ICONST_0), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFLE:
                    processIf(buffer, ctxt, gf.cmpLe(ctxt, pop(Type.S32), Value.ICONST_0), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPEQ:
                    processIf(buffer, ctxt, gf.cmpEq(ctxt, pop(Type.S32), pop(Type.S32)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPNE:
                    processIf(buffer, ctxt, gf.cmpNe(ctxt, pop(Type.S32), pop(Type.S32)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPLT:
                    processIf(buffer, ctxt, gf.cmpLt(ctxt, pop(Type.S32), pop(Type.S32)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPGE:
                    processIf(buffer, ctxt, gf.cmpGe(ctxt, pop(Type.S32), pop(Type.S32)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPGT:
                    processIf(buffer, ctxt, gf.cmpGt(ctxt, pop(Type.S32), pop(Type.S32)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ICMPLE:
                    processIf(buffer, ctxt, gf.cmpLe(ctxt, pop(Type.S32), pop(Type.S32)), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ACMPEQ:
                    processIf(buffer, ctxt, gf.cmpEq(ctxt, pop(), pop()), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IF_ACMPNE:
                    processIf(buffer, ctxt, gf.cmpNe(ctxt, pop(), pop()), buffer.getShort() + src, buffer.position());
                    return;
                case OP_GOTO:
                case OP_GOTO_W: {
                    int target = src + (opcode == OP_GOTO ? buffer.getShort() : buffer.getInt());
                    BasicBlock from = gf.goto_(ctxt, getBlockForIndex(target));
                    // set the position after, so that the bci for the instruction is correct
                    processBlock(buffer.position(target), from);
                    return;
                }
                case OP_JSR:
                case OP_JSR_W: {
                    int target = src + (opcode == OP_JSR ? buffer.getShort() : buffer.getInt());
                    int ret = buffer.position();
                    // jsr destination
                    NodeHandle dest = getBlockForIndex(target);
                    // return address is really the target, used to compute the exit state
                    ConstantValue ra = Value.const_(target, ReturnAddressType.INSTANCE);
                    push(ra);
                    NodeHandle retBlock = getBlockForIndex(ret);
                    // the jsr call
                    BasicBlock termBlock = gf.jsr(ctxt, dest, retBlock);
                    int pos = buffer.position();
                    // process the jsr call target block
                    processBlock(buffer.position(target), termBlock);
                    BasicBlock jsrTargetBlock = NodeHandle.getTargetOf(dest);
                    // traverse the JSR target block to find the exit state, if any
                    RetPhiComputingVisitor jv = new RetPhiComputingVisitor();
                    jv.handleBlock(NodeHandle.getTargetOf(dest), jsrTargetBlock);
                    // if we never exit (e.g. throw from all paths) then we don't need to continue
                    if (jv.exited) {
                        restoreStack(jv.exitStack);
                        restoreLocals(jv.exitLocals);
                        buffer.position(pos);
                        processBlock(buffer.position(pos), termBlock);
                    }
                    return;
                }
                case OP_RET:
                    // each ret records the output stack and locals at the point of the ret, and then exits.
                    Value rat = pop(ReturnAddressType.INSTANCE);
                    setJsrExitState(gf.ret(ctxt, rat), saveStack(), saveLocals());
                    // exit one level of recursion
                    return;
                case OP_TABLESWITCH:
                    align(buffer, 4);
                    int db = buffer.getInt();
                    int low = buffer.getInt();
                    int high = buffer.getInt();
                    int cnt = high - low;
                    int[] dests = new int[cnt];
                    int[] vals = new int[cnt];
                    NodeHandle[] handles = new NodeHandle[cnt];
                    for (int i = 0; i < cnt; i ++) {
                        vals[i] = low + i;
                        handles[i] = getBlockForIndex(dests[i] = buffer.getInt() + src);
                    }
                    BasicBlock exited = gf.switch_(ctxt, pop(Type.S32), vals, handles, getBlockForIndex(db + src));
                    Value[] stackSnap = saveStack();
                    Value[] varSnap = saveLocals();
                    processBlock(buffer.position(db + src), exited);
                    for (int i = 0; i < handles.length; i++) {
                        restoreStack(stackSnap);
                        restoreLocals(varSnap);
                        processBlock(buffer.position(dests[i]), exited);
                    }
                    // done
                    return;
                case OP_LOOKUPSWITCH:
                    align(buffer, 4);
                    db = buffer.getInt();
                    cnt = buffer.getInt();
                    dests = new int[cnt];
                    vals = new int[cnt];
                    handles = new NodeHandle[cnt];
                    for (int i = 0; i < cnt; i ++) {
                        vals[i] = buffer.getInt();
                        handles[i] = getBlockForIndex(dests[i] = buffer.getInt() + src);
                    }
                    exited = gf.switch_(ctxt, pop(Type.S32), vals, handles, getBlockForIndex(db + src));
                    stackSnap = saveStack();
                    varSnap = saveLocals();
                    processBlock(buffer.position(db + src), exited);
                    for (int i = 0; i < handles.length; i++) {
                        restoreStack(stackSnap);
                        restoreLocals(varSnap);
                        processBlock(buffer.position(dests[i]), exited);
                    }
                    // done
                    return;
                case OP_IRETURN:
                    gf.return_(ctxt, pop(Type.S32));
                    // block complete
                    return;
                case OP_LRETURN:
                    gf.return_(ctxt, pop(Type.S64));
                    // block complete
                    return;
                case OP_FRETURN:
                    gf.return_(ctxt, pop(Type.F32));
                    // block complete
                    return;
                case OP_DRETURN:
                    gf.return_(ctxt, pop(Type.F64));
                    // block complete
                    return;
                case OP_ARETURN:
                    gf.return_(ctxt, pop());
                    // block complete
                    return;
                case OP_RETURN:
                    gf.return_(ctxt);
                    // block complete
                    return;
                case OP_GETSTATIC:
                    // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                    int fieldRef = buffer.getShort() & 0xffff;
                    push(promote(ctxt, gf.readStaticField(ctxt, getOwnerOfFieldRef(fieldRef), getNameOfFieldRef(fieldRef), JavaAccessMode.DETECT)));
                    break;
                case OP_PUTSTATIC:
                    // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                    fieldRef = buffer.getShort() & 0xffff;
                    Type type = getTypeOfFieldRef(fieldRef);
                    gf.writeStaticField(ctxt, getOwnerOfFieldRef(fieldRef), getNameOfFieldRef(fieldRef), demote(ctxt, pop(), type), JavaAccessMode.DETECT);
                    break;
                case OP_GETFIELD:
                    // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                    fieldRef = buffer.getShort() & 0xffff;
                    v1 = pop(getOwnerOfFieldRef(fieldRef));
                    push(promote(ctxt, gf.readInstanceField(ctxt, v1, getOwnerOfFieldRef(fieldRef), getNameOfFieldRef(fieldRef), JavaAccessMode.DETECT)));
                    break;
                case OP_PUTFIELD:
                    // todo: try/catch this, and substitute NoClassDefFoundError/LinkageError/etc. on resolution error
                    fieldRef = buffer.getShort() & 0xffff;
                    v2 = demote(ctxt, pop(), getTypeOfFieldRef(fieldRef));
                    v1 = pop(getOwnerOfFieldRef(fieldRef));
                    gf.writeInstanceField(ctxt, v1, getOwnerOfFieldRef(fieldRef), getNameOfFieldRef(fieldRef), v2, JavaAccessMode.DETECT);
                    break;
                case OP_INVOKEVIRTUAL:
                case OP_INVOKESPECIAL:
                case OP_INVOKESTATIC:
                case OP_INVOKEINTERFACE:
                    int methodRef = buffer.getShort() & 0xffff;
                    // todo: try/catch, replace with error node
                    // todo: swap this so that the owner is returned as a ResolvedTypeDefinition
                    ClassType ownerType = getOwnerOfMethodRef(methodRef);
                    VerifiedTypeDefinition owner = ownerType.getDefinition();
                    int nameAndType = getNameAndTypeOfMethodRef(methodRef);
                    ParameterizedExecutableElement target = resolveTargetOfMethodNameAndType(owner, nameAndType);
                    if (target == null) {
                        // todo
                        throw new UnsupportedOperationException("Insert no such method node here");
                    }
                    cnt = target.getParameterCount();
                    Value[] args = new Value[cnt];
                    for (int i = cnt - 1; i >= 0; i --) {
                        args[i] = pop(target.getParameter(i).getType());
                    }
                    if (opcode != OP_INVOKESTATIC) {
                        // pop the receiver
                        v1 = pop(ownerType);
                    } else {
                        // definite initialization
                        v1 = null;
                    }
                    if (target instanceof ConstructorElement) {
                        if (opcode != OP_INVOKESPECIAL) {
                            throw new InvalidByteCodeException();
                        }
                        gf.invokeInstanceMethod(ctxt, v1, InstanceInvocation.Kind.EXACT, target, List.of(args));
                    } else {
                        assert target instanceof MethodElement;
                        MethodElement method = (MethodElement) target;
                        Type returnType = method.getReturnType();
                        if (opcode == OP_INVOKESTATIC) {
                            if (returnType == Type.VOID) {
                                // return type is implicitly void
                                gf.invokeMethod(ctxt, method, List.of(args));
                            } else {
                                push(promote(ctxt, gf.invokeValueMethod(ctxt, method, List.of(args))));
                            }
                        } else {
                            if (returnType == Type.VOID) {
                                // return type is implicitly void
                                gf.invokeInstanceMethod(ctxt, v1, InstanceInvocation.Kind.fromOpcode(opcode), method, List.of(args));
                            } else {
                                push(promote(ctxt, gf.invokeInstanceValueMethod(ctxt, v1, InstanceInvocation.Kind.fromOpcode(opcode), method, List.of(args))));
                            }
                        }
                    }
                    break;
                case OP_INVOKEDYNAMIC:
                    throw new UnsupportedOperationException();
                case OP_NEW:
                    push(gf.new_(ctxt, getClassFile().resolveSingleType(buffer.getShort() & 0xffff)));
                    break;
                case OP_NEWARRAY:
                    ArrayClassType arrayType;
                    switch (buffer.get() & 0xff) {
                        case T_BOOLEAN: arrayType = Type.JAVA_BOOLEAN_ARRAY; break;
                        case T_CHAR: arrayType = Type.JAVA_CHAR_ARRAY; break;
                        case T_FLOAT: arrayType = Type.JAVA_FLOAT_ARRAY; break;
                        case T_DOUBLE: arrayType = Type.JAVA_DOUBLE_ARRAY; break;
                        case T_BYTE: arrayType = Type.JAVA_BYTE_ARRAY; break;
                        case T_SHORT: arrayType = Type.JAVA_SHORT_ARRAY; break;
                        case T_INT: arrayType = Type.JAVA_INT_ARRAY; break;
                        case T_LONG: arrayType = Type.JAVA_LONG_ARRAY; break;
                        default: throw new InvalidByteCodeException();
                    }
                    // todo: check for negative array size
                    push(gf.newArray(ctxt, arrayType, pop(Type.S32)));
                    break;
                case OP_ANEWARRAY:
                    arrayType = getClassFile().resolveSingleDescriptor(buffer.getShort() & 0xffff).getArrayClassType();
                    // todo: check for negative array size
                    push(gf.newArray(ctxt, arrayType, pop(Type.S32)));
                    break;
                case OP_ARRAYLENGTH:
                    push(gf.lengthOfArray(ctxt, pop(/* any array type */)));
                    break;
                case OP_ATHROW:
                    gf.throw_(ctxt, pop());
                    // terminate
                    return;
                case OP_CHECKCAST:
                    ClassType clazz = resolveClass(buffer.getShort() & 0xffff);
                    v1 = pop();
                    NodeHandle okHandle = new NodeHandle();
                    NodeHandle notNullHandle = new NodeHandle();
                    gf.if_(ctxt, gf.cmpEq(ctxt, v1, Value.NULL), okHandle, notNullHandle);
                    ctxt.setCurrentBlock(notNullHandle);
                    NodeHandle castFailedHandle = new NodeHandle();
                    gf.if_(ctxt, gf.instanceOf(ctxt, v1, clazz), okHandle, castFailedHandle);
                    ctxt.setCurrentBlock(castFailedHandle);
                    ClassType cce = resolveClass("java/lang/ClassCastException");
                    ResolvedTypeDefinition resolvedCce = cce.getDefinition().resolve();
                    // do not change stack depth starting here
                    v1 = gf.new_(ctxt, cce);
                    // todo: look these up ahead of time on the VM instance?
                    int constructorIndex = resolvedCce.findConstructorIndex(JavaVM.requireCurrent().getConstructorDescriptor());
                    assert constructorIndex != -1;
                    ConstructorElement invTarget = resolvedCce.getConstructor(constructorIndex);
                    gf.invokeInstanceMethod(ctxt, v1, InstanceInvocation.Kind.EXACT, invTarget, List.of());
                    gf.throw_(ctxt, v1);
                    // do not change stack depth ending here
                    ctxt.setCurrentBlock(okHandle);
                    break;
                case OP_INSTANCEOF:
                    clazz = resolveClass(buffer.getShort() & 0xffff);
                    v1 = pop();
                    NodeHandle nullHandle = new NodeHandle();
                    notNullHandle = new NodeHandle();
                    gf.if_(ctxt, gf.cmpEq(ctxt, v1, Value.NULL), nullHandle, notNullHandle);
                    ctxt.setCurrentBlock(notNullHandle);
                    v1 = gf.instanceOf(ctxt, v1, clazz);
                    NodeHandle mergeHandle = new NodeHandle();
                    BasicBlock t1 = gf.goto_(ctxt, mergeHandle);
                    ctxt.setCurrentBlock(nullHandle);
                    BasicBlock t2 = gf.goto_(ctxt, mergeHandle);
                    ctxt.setCurrentBlock(mergeHandle);
                    PhiValue phi = gf.phi(Type.BOOL, mergeHandle);
                    phi.setValueForBlock(t2, Value.FALSE);
                    phi.setValueForBlock(t1, v1);
                    push(phi);
                    break;
                case OP_MONITORENTER:
                    gf.monitorEnter(ctxt, pop());
                    break;
                case OP_MONITOREXIT:
                    gf.monitorExit(ctxt, pop());
                    break;
                case OP_MULTIANEWARRAY:
                    int cpIdx = buffer.getShort() & 0xffff;
                    Value[] dims = new Value[buffer.get() & 0xff];
                    if (dims.length == 0) {
                        throw new InvalidByteCodeException();
                    }
                    for (int i = 0; i < dims.length; i ++) {
                        dims[i] = pop(Type.S32/*.nonNegative()*/);
                    }
                    push(gf.multiNewArray(ctxt, resolveDescriptor(cpIdx).getArrayClassType(), dims));
                    break;
                case OP_IFNULL:
                    processIf(buffer, ctxt, gf.cmpEq(ctxt, pop(), Value.NULL), buffer.getShort() + src, buffer.position());
                    return;
                case OP_IFNONNULL:
                    processIf(buffer, ctxt, gf.cmpNe(ctxt, pop(), Value.NULL), buffer.getShort() + src, buffer.position());
                    return;
                default:
                    throw new InvalidByteCodeException();
            }
            // now check to see if the new position is an entry point
            int epIdx = definedBody.getEntryPointIndex(buffer.position());
            if (epIdx >= 0 && definedBody.getEntryPointSourceCount(epIdx) > 1) {
                // two or more blocks enter here; start a new block via goto
                processBlock(buffer, gf.goto_(ctxt, blockHandles[epIdx]));
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

    private void processIf(final ByteBuffer buffer, final GraphFactory.Context ctxt, final Value cond, final int dest1, final int dest2) {
        NodeHandle b1 = getBlockForIndex(dest1);
        NodeHandle b2 = getBlockForIndex(dest2);
        BasicBlock from = gf.if_(ctxt, cond, b1, b2);
        Value[] varSnap = saveLocals();
        Value[] stackSnap = saveStack();
        processBlock(buffer.position(dest1), from);
        restoreStack(stackSnap);
        restoreLocals(varSnap);
        processBlock(buffer.position(dest2), from);
    }

    private ClassFileImpl getClassFile() {
        return verifiedMethodBody.getDefinedBody().getClassFile();
    }

    private Type getTypeOfFieldRef(final int fieldRef) {
        return getClassFile().resolveSingleDescriptor(getClassFile().getFieldrefConstantDescriptorIdx(fieldRef));
    }

    private String getNameOfFieldRef(final int fieldRef) {
        return getClassFile().getFieldrefConstantName(fieldRef);
    }

    private ClassType getOwnerOfFieldRef(final int fieldRef) {
        return resolveClass(getClassFile().getFieldrefConstantClassName(fieldRef));
    }

    private String getNameOfMethodRef(final int methodRef) {
        return getClassFile().getMethodrefConstantName(methodRef);
    }

    private boolean nameOfMethodRefEquals(final int methodRef, final String expected) {
        return getClassFile().methodrefConstantNameEquals(methodRef, expected);
    }

    private ClassType getOwnerOfMethodRef(final int methodRef) {
        return resolveClass(getClassFile().getMethodrefConstantClassName(methodRef));
    }

    private int getNameAndTypeOfMethodRef(final int methodRef) {
        return getClassFile().getMethodrefNameAndTypeIndex(methodRef);
    }

    private ParameterizedExecutableElement resolveTargetOfMethodNameAndType(final VerifiedTypeDefinition owner, final int nameAndTypeRef) {
        int idx;
        ClassFileImpl classFile = getClassFile();
        int descIdx = classFile.getNameAndTypeConstantDescriptorIdx(nameAndTypeRef);
        if (classFile.nameAndTypeConstantNameEquals(nameAndTypeRef, "<init>")) {
            // constructor
            ConstructorDescriptor desc = classFile.getConstructorDescriptor(descIdx);
            idx = owner.resolve().findConstructorIndex(desc);
            return idx == -1 ? null : owner.getConstructor(idx);
        } else {
            // method
            MethodDescriptor desc = classFile.getMethodDescriptor(descIdx);
            idx = owner.resolve().findMethodIndex(classFile.getNameAndTypeConstantName(nameAndTypeRef), desc);
            return idx == -1 ? null : owner.getMethod(idx);
        }
    }

    private Type resolveDescriptor(int cpIdx) {
        return getClassFile().resolveSingleDescriptor(cpIdx);
    }

    private ClassType resolveClass(int cpIdx) {
        return getClassFile().resolveSingleType(cpIdx);
    }

    private ClassType resolveClass(String name) {
        return getClassFile().resolveSingleType(name);
    }

    private static int getWidenableValue(final ByteBuffer buffer, final boolean wide) {
        return wide ? buffer.getShort() & 0xffff : buffer.get() & 0xff;
    }

    private static int getWidenableValueSigned(final ByteBuffer buffer, final boolean wide) {
        return wide ? buffer.getShort() : buffer.get();
    }

    private class RetPhiComputingVisitor implements GraphVisitor<BasicBlock> {
        PhiValue[] exitStack;
        PhiValue[] exitLocals;
        boolean exited;

        void handleBlock(final BasicBlock returnBlock, final BasicBlock block) {
            if (block.getTerminator() instanceof Ret) {
                Value[] stack = retStacks.get(block);
                Value[] locals = retLocals.get(block);
                PhiValue[] phiStack;
                PhiValue[] phiLocals;
                if (exitStack == null) {
                    // make phis
                    phiStack = new PhiValue[stack.length];
                    phiLocals = new PhiValue[locals.length];
                    for (int i = 0; i < stack.length; i ++) {
                        phiStack[i] = gf.phi(stack[i].getType(), returnBlock);
                        phiStack[i].setValueForBlock(block, stack[i]);
                    }
                    for (int i = 0; i < locals.length; i ++) {
                        phiLocals[i] = gf.phi(locals[i].getType(), returnBlock);
                        phiLocals[i].setValueForBlock(block, locals[i]);
                    }
                    exitStack = phiStack;
                    exitLocals = phiLocals;
                    exited = true;
                } else {
                    phiStack = exitStack;
                    phiLocals = exitLocals;
                    for (int i = 0; i < stack.length; i ++) {
                        phiStack[i].setValueForBlock(block, stack[i]);
                    }
                    for (int i = 0; i < locals.length; i ++) {
                        phiLocals[i].setValueForBlock(block, locals[i]);
                    }
                }
            } else {
                block.getTerminator().accept(this, returnBlock);
            }
        }

        public void visit(final BasicBlock returnBlock, final If node) {
            handleBlock(returnBlock, node.getTrueBranch());
            handleBlock(returnBlock, node.getFalseBranch());
        }

        public void visit(final BasicBlock returnBlock, final Goto node) {
            handleBlock(returnBlock, node.getTarget());
        }

        public void visit(final BasicBlock returnBlock, final Jsr node) {
            handleBlock(returnBlock, node.getReturn());
        }

        public void visit(final BasicBlock returnBlock, final Switch node) {
            handleBlock(returnBlock, node.getDefaultTarget());
            int cnt = node.getNumberOfValues();
            for (int i = 0; i < cnt; i ++) {
                handleBlock(returnBlock, node.getTargetForValue(i));
            }
        }

        public void visit(final BasicBlock returnBlock, final TryInstanceInvocation node) {
            handleBlock(returnBlock, node.getCatchHandler());
            handleBlock(returnBlock, node.getTarget());
        }

        public void visit(final BasicBlock returnBlock, final TryInstanceInvocationValue node) {
            handleBlock(returnBlock, node.getCatchHandler());
            handleBlock(returnBlock, node.getTarget());
        }

        public void visit(final BasicBlock returnBlock, final TryInvocation node) {
            handleBlock(returnBlock, node.getCatchHandler());
            handleBlock(returnBlock, node.getTarget());
        }

        public void visit(final BasicBlock returnBlock, final TryInvocationValue node) {
            handleBlock(returnBlock, node.getCatchHandler());
            handleBlock(returnBlock, node.getTarget());
        }

        public void visit(final BasicBlock returnBlock, final TryThrow node) {
            handleBlock(returnBlock, node.getCatchHandler());
        }
    }
}
