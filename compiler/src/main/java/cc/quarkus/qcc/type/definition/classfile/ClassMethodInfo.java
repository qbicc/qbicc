package cc.quarkus.qcc.type.definition.classfile;

import static cc.quarkus.qcc.type.definition.classfile.ClassFile.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.InvokableElement;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.TypeParameterContext;
import cc.quarkus.qcc.type.generic.TypeSignature;

/**
 * The defined method information.
 */
final class ClassMethodInfo {
    private static final short[] NO_SHORTS = new short[0];

    private final ClassFileImpl classFile;
    private final int modifiers;
    private final int index;
    private final ByteBuffer codeAttr;
    private final int maxStack;
    private final int maxLocals;
    private final int codeOffs;
    private final int codeLen;
    private final short[] exTable; // format: start end handler type_idx

    private final LineNumberTable lineNumberTable;

    private final short[] entryPoints; // format: dest-bci (unsigned), sorted by dest-bci
    /**
     * Each entry is an array of non-overlapping intervals for the corresponding local var slot.
     *
     * Entry Format:
     * - start_pc
     * - length
     * - name_idx
     * - desc_idx
     * - sig_idx
     *
     * Sorted by start_pc (ascending).
     */
    private final short[][] localVariables;
    private final int stackMapTableOffs;
    private final int stackMapTableLen;
    private final int visibleTypeAnnotationsOffs;
    private final int visibleTypeAnnotationsLen;
    private final int invisibleTypeAnnotationsOffs;
    private final int invisibleTypeAnnotationsLen;

    private final Type[][] variableTypes;

    ClassMethodInfo(final ClassFileImpl classFile, ExecutableElement element, final int modifiers, final int index, final ByteBuffer codeAttr) {
        this.classFile = classFile;
        this.modifiers = modifiers;
        this.index = index;
        this.codeAttr = codeAttr;
        int save = codeAttr.position();
        maxStack = codeAttr.getShort() & 0xffff;
        maxLocals = codeAttr.getShort() & 0xffff;
        codeLen = codeAttr.getInt();
        codeOffs = codeAttr.position();
        int lim = codeAttr.limit();
        codeAttr.limit(codeOffs + codeLen);
        ByteBuffer bc = codeAttr.slice();
        codeAttr.limit(lim);
        codeAttr.position(codeOffs + codeLen);
        // process bytecodes for entry points
        int target;
        BitSet enteredOnce = new BitSet(bc.capacity());
        BitSet enteredMulti = new BitSet(bc.capacity());
        enteredOnce.set(0); // method entry
        while (bc.position() < bc.limit()) {
            int src = bc.position();
            int opcode = bc.get() & 0xff;
            switch (opcode) {
                // interesting cases first
                case OP_JSR: {
                    target = src + bc.getShort();
                    if (enteredOnce.get(target)) {
                        enteredMulti.set(target);
                    } else {
                        enteredOnce.set(target);
                    }
                    // next instruction is entered by ret as well as fall-through
                    enteredMulti.set(bc.position());
                    break;
                }
                case OP_JSR_W: {
                    target = src + 5;
                    if (enteredOnce.get(target)) {
                        enteredMulti.set(target);
                    } else {
                        enteredOnce.set(target);
                    }
                    // next instruction is entered by ret as well as fall-through
                    enteredMulti.set(bc.position());
                    break;
                }
                case OP_GOTO: {
                    target = src + bc.getShort();
                    if (enteredOnce.get(target)) {
                        enteredMulti.set(target);
                    } else {
                        enteredOnce.set(target);
                    }
                    // do not set entered flag for next instruction
                    continue;
                }
                case OP_GOTO_W: {
                    target = src + bc.getInt();
                    if (enteredOnce.get(target)) {
                        enteredMulti.set(target);
                    } else {
                        enteredOnce.set(target);
                    }
                    // do not set entered flag for next instruction
                    continue;
                }
                case OP_IF_ACMPEQ:
                case OP_IF_ACMPNE:
                case OP_IF_ICMPEQ:
                case OP_IF_ICMPNE:
                case OP_IF_ICMPLT:
                case OP_IF_ICMPGE:
                case OP_IF_ICMPGT:
                case OP_IF_ICMPLE:
                case OP_IFEQ:
                case OP_IFNE:
                case OP_IFLT:
                case OP_IFLE:
                case OP_IFGT:
                case OP_IFGE:
                case OP_IFNONNULL:
                case OP_IFNULL: {
                    target = src + bc.getShort();
                    if (enteredOnce.get(target)) {
                        enteredMulti.set(target);
                    } else {
                        enteredOnce.set(target);
                    }
                    break;
                }
                case OP_RET: {
                    // next instruction is not entered
                    bc.get();
                    continue;
                }
                case OP_ATHROW:
                case OP_RETURN:
                case OP_DRETURN:
                case OP_FRETURN:
                case OP_IRETURN:
                case OP_LRETURN:
                case OP_ARETURN: {
                    // next instruction is not entered
                    continue;
                }
                case OP_LOOKUPSWITCH: {
                    align(bc, 4);
                    target = src + bc.getInt();
                    if (enteredOnce.get(target)) {
                        enteredMulti.set(target);
                    } else {
                        enteredOnce.set(target);
                    }
                    int cnt = bc.getInt();
                    for (int i = 0; i < cnt; i ++) {
                        bc.getInt(); // match
                        target = src + bc.getInt();
                        if (enteredOnce.get(target)) {
                            enteredMulti.set(target);
                        } else {
                            enteredOnce.set(target);
                        }
                    }
                    // do not set entered flag for next instruction
                    continue;
                }
                case OP_TABLESWITCH: {
                    align(bc, 4);
                    target = src + bc.getInt();
                    if (enteredOnce.get(target)) {
                        enteredMulti.set(target);
                    } else {
                        enteredOnce.set(target);
                    }
                    int low = bc.getInt();
                    int high = bc.getInt();
                    int cnt = high - low + 1;
                    if (cnt < 0) {
                        throw new InvalidTableSwitchRangeException();
                    }
                    for (int i = 0; i < cnt; i ++) {
                        target = src + bc.getInt();
                        if (enteredOnce.get(target)) {
                            enteredMulti.set(target);
                        } else {
                            enteredOnce.set(target);
                        }
                    }
                    // do not set entered flag for next instruction
                    continue;
                }
                default: {
                    skipInstruction(bc, opcode);
                    break;
                }
            }
            // next instruction is entered by continuing execution
            src = bc.position();
            if (enteredOnce.get(src)) {
                enteredMulti.set(src);
            } else {
                enteredOnce.set(src);
            }
        }
        int exTableLen = codeAttr.getShort() & 0xffff;
        short[] exTable = new short[exTableLen << 2];
        int base;
        for (int i = 0; i < exTableLen; i ++) {
            base = i << 2;
            exTable[base] = codeAttr.getShort();
            exTable[base + 1] = codeAttr.getShort();
            target = exTable[base + 2] = codeAttr.getShort();
            exTable[base + 3] = codeAttr.getShort();
            if (enteredOnce.get(target)) {
                enteredMulti.set(target);
            } else {
                enteredOnce.set(target);
            }
        }
        int etCnt = enteredMulti.cardinality();
        short[] entryPoints = new short[etCnt];
        for (int j = 0, i = enteredMulti.nextSetBit(0); i >= 0; i = enteredMulti.nextSetBit(i+1)) {
            entryPoints[j ++] = (short) i;
        }
        this.exTable = exTable;
        int attrCnt = codeAttr.getShort() & 0xffff;
        int stackMapTableOffs = classFile.getMajorVersion() < 50 ? -1 : 0;
        int stackMapTableLen = 0;
        int visibleTypeAnnotationsOffs = -1;
        int visibleTypeAnnotationsLen = 0;
        int invisibleTypeAnnotationsOffs = -1;
        int invisibleTypeAnnotationsLen = 0;
        short[][] localVariables = new short[maxLocals][];
        int[] lvtLengths = new int[maxLocals];
        Arrays.fill(localVariables, NO_SHORTS);
        int localVariablesLen = 0;
        boolean lvt;
        LineNumberTable.Builder lineNumberTable = new LineNumberTable.Builder();
        for (int i = 0; i < attrCnt; i ++) {
            int nameIdx = codeAttr.getShort() & 0xffff;
            int len = codeAttr.getInt();
            if (classFile.utf8ConstantEquals(nameIdx, "LineNumberTable")) {
                lineNumberTable.appendTableFromAttribute(codeAttr.duplicate().limit(codeAttr.position() + len).slice());
                codeAttr.position(codeAttr.position() + len);
            } else if ((lvt = classFile.utf8ConstantEquals(nameIdx, "LocalVariableTable")) || classFile.utf8ConstantEquals(nameIdx, "LocalVariableTypeTable")) {
                int cnt = codeAttr.getShort() & 0xffff;
                // sanity check the length
                if (cnt * 10 != len - 2) {
                    throw new InvalidAttributeLengthException();
                }
                // this attribute can appear more than once
                for (int j = 0; j < cnt; j ++) {
                    int startPc = codeAttr.getShort() & 0xffff;
                    int length = codeAttr.getShort() & 0xffff;
                    int varNameIdx = codeAttr.getShort() & 0xffff;
                    int typeIdx = codeAttr.getShort() & 0xffff;
                    int varIndex = codeAttr.getShort() & 0xffff;
                    if (varIndex >= maxLocals) {
                        throw new InvalidLocalVariableIndexException();
                    }
                    short[] array = localVariables[varIndex];
                    if (array.length == 0) {
                        localVariables[varIndex] = array = new short[10];
                        array[0] = (short) startPc;
                        array[1] = (short) length;
                        array[2] = (short) varNameIdx;
                        array[lvt ? 3 : 4] = (short) typeIdx;
                        lvtLengths[varIndex] = 1;
                    } else {
                        int lvtLength = lvtLengths[varIndex];
                        int idx = findLocalVariableEntry(array, lvtLength, startPc, length);
                        if (idx >= 0) {
                            base = idx * 5;
                            // merge
                            if (array[base + (lvt ? 3 : 4)] != 0 || varNameIdx != array[base + 2]) {
                                // already have an entry for it
                                throw new InvalidLocalVariableIndexException();
                            }
                            array[lvt ? 3 : 4] = (short) typeIdx;
                        } else {
                            // insert
                            idx = -idx - 1;
                            base = idx * 5;
                            if (lvtLength * 5 == array.length) {
                                // grow
                                localVariables[varIndex] = array = Arrays.copyOf(array, (lvtLength << 1) * 5);
                            }
                            if (idx < lvtLength) {
                                // make a hole
                                System.arraycopy(array, base, array, (idx + 1) * 5, (lvtLength - idx) * 5);
                            }
                            array[base] = (short) startPc;
                            array[base + 1] = (short) length;
                            array[base + 2] = (short) varNameIdx;
                            array[base + (lvt ? 4 : 3)] = 0;
                            array[base + (lvt ? 3 : 4)] = (short) typeIdx;
                            lvtLengths[varIndex] = lvtLength + 1;
                        }
                    }
                }
            } else if (classFile.utf8ConstantEquals(nameIdx, "StackMapTable")) {
                // do not reorder
                stackMapTableLen = codeAttr.getShort() & 0xffff;
                stackMapTableOffs = codeAttr.position();
            } else if (classFile.utf8ConstantEquals(nameIdx, "RuntimeVisibleTypeAnnotations")) {
                visibleTypeAnnotationsLen = codeAttr.getShort() & 0xffff;
                visibleTypeAnnotationsOffs = codeAttr.position();
            } else if (classFile.utf8ConstantEquals(nameIdx, "RuntimeInvisibleTypeAnnotations")) {
                invisibleTypeAnnotationsLen = codeAttr.getShort() & 0xffff;
                invisibleTypeAnnotationsOffs = codeAttr.position();
            } else {
                // skip it
                codeAttr.position(codeAttr.position() + len);
            }
        }
        for (int i = 0; i < maxLocals; i ++) {
            if (localVariables[i].length > lvtLengths[i] * 5) {
                localVariables[i] = Arrays.copyOf(localVariables[i], lvtLengths[i] * 5);
            }
        }
        this.localVariables = localVariables;
        this.stackMapTableOffs = stackMapTableOffs;
        this.stackMapTableLen = stackMapTableLen;
        this.visibleTypeAnnotationsOffs = visibleTypeAnnotationsOffs;
        this.visibleTypeAnnotationsLen = visibleTypeAnnotationsLen;
        this.invisibleTypeAnnotationsOffs = invisibleTypeAnnotationsOffs;
        this.invisibleTypeAnnotationsLen = invisibleTypeAnnotationsLen;
        this.entryPoints = entryPoints;
        codeAttr.position(save);
        Type[][] variableTypes1 = new Type[maxLocals][];
        TypeParameterContext paramCtxt = element instanceof InvokableElement ? (InvokableElement) element : element.getEnclosingType();
        for (int i = 0; i < maxLocals; i ++) {
            int cnt = getLocalVarEntryCount(i);
            Type[] array = variableTypes1[i] = new Type[cnt];
            for (int j = 0; j < cnt; j ++) {
                int idx = getLocalVarDescriptorIndex(i, j);
                if (idx == 0) {
                    throw new MissingLocalVariableDescriptorException();
                }
                TypeDescriptor desc = (TypeDescriptor) classFile.getDescriptorConstant(idx);
                ClassContext ctxt = classFile.getClassContext();
                idx = getLocalVarSignatureIndex(i, j);
                TypeSignature sig;
                if (idx == 0) {
                    sig = TypeSignature.synthesize(ctxt, desc);
                } else {
                    sig = TypeSignature.parse(ctxt, classFile.getUtf8ConstantAsBuffer(idx));
                }
                array[j] = ctxt.resolveTypeFromDescriptor(desc,
                    paramCtxt, sig, TypeAnnotationList.empty(), TypeAnnotationList.empty());
            }
        }
        this.variableTypes = variableTypes1;
        this.lineNumberTable = lineNumberTable.build();
    }

    private void skipInstruction(final ByteBuffer codeAttr, final int opcode) {
        int cnt;
        switch (opcode) {

            // special
            case OP_LOOKUPSWITCH:
                align(codeAttr, 4);
                codeAttr.getInt();
                cnt = codeAttr.getInt();
                codeAttr.position(codeAttr.position() + (cnt << 3));
                break;

            case OP_TABLESWITCH:
                align(codeAttr, 4);
                codeAttr.getInt();
                int hb = codeAttr.getInt();
                int lb = codeAttr.getInt();
                cnt = hb - lb + 1;
                if (cnt < 0) {
                    throw new InvalidTableSwitchRangeException();
                }
                codeAttr.position(codeAttr.position() + (cnt << 2));
                break;

            // five-byte opcodes
            case OP_GOTO_W:
            case OP_INVOKEDYNAMIC:
            case OP_INVOKEINTERFACE:
            case OP_JSR_W:
                codeAttr.getInt();
                break;

            // four-byte opcodes
            case OP_MULTIANEWARRAY:
                codeAttr.getShort();
                codeAttr.get();
                break;

            // three-byte opcodes
            case OP_ANEWARRAY:
            case OP_CHECKCAST:
            case OP_GETFIELD:
            case OP_GETSTATIC:
            case OP_GOTO:
            case OP_IF_ACMPEQ:
            case OP_IF_ACMPNE:
            case OP_IF_ICMPEQ:
            case OP_IF_ICMPNE:
            case OP_IF_ICMPLT:
            case OP_IF_ICMPGE:
            case OP_IF_ICMPGT:
            case OP_IF_ICMPLE:
            case OP_IFEQ:
            case OP_IFNE:
            case OP_IFLT:
            case OP_IFGE:
            case OP_IFGT:
            case OP_IFLE:
            case OP_IFNONNULL:
            case OP_IFNULL:
            case OP_IINC:
            case OP_INSTANCEOF:
            case OP_INVOKESPECIAL:
            case OP_INVOKESTATIC:
            case OP_INVOKEVIRTUAL:
            case OP_JSR:
            case OP_LDC_W:
            case OP_LDC2_W:
            case OP_NEW:
            case OP_PUTFIELD:
            case OP_PUTSTATIC:
            case OP_SIPUSH:
                codeAttr.getShort();
                break;

            // two-byte opcodes
            case OP_ALOAD:
            case OP_ASTORE:
            case OP_BIPUSH:
            case OP_DLOAD:
            case OP_DSTORE:
            case OP_FLOAD:
            case OP_FSTORE:
            case OP_ILOAD:
            case OP_ISTORE:
            case OP_LDC:
            case OP_LLOAD:
            case OP_LSTORE:
            case OP_NEWARRAY:
            case OP_RET:
                codeAttr.get();
                break;

            // one-byte opcodes
            case OP_AALOAD:
            case OP_AASTORE:
            case OP_ACONST_NULL:
            case OP_ALOAD_0:
            case OP_ALOAD_1:
            case OP_ALOAD_2:
            case OP_ALOAD_3:
            case OP_ARETURN:
            case OP_ARRAYLENGTH:
            case OP_ASTORE_0:
            case OP_ASTORE_1:
            case OP_ASTORE_2:
            case OP_ASTORE_3:
            case OP_ATHROW:
            case OP_BALOAD:
            case OP_BASTORE:
            case OP_CALOAD:
            case OP_CASTORE:
            case OP_D2F:
            case OP_D2I:
            case OP_D2L:
            case OP_DADD:
            case OP_DALOAD:
            case OP_DASTORE:
            case OP_DCMPG:
            case OP_DCMPL:
            case OP_DCONST_0:
            case OP_DCONST_1:
            case OP_DDIV:
            case OP_DLOAD_0:
            case OP_DLOAD_1:
            case OP_DLOAD_2:
            case OP_DLOAD_3:
            case OP_DMUL:
            case OP_DNEG:
            case OP_DREM:
            case OP_DRETURN:
            case OP_DSTORE_0:
            case OP_DSTORE_1:
            case OP_DSTORE_2:
            case OP_DSTORE_3:
            case OP_DSUB:
            case OP_DUP:
            case OP_DUP_X1:
            case OP_DUP_X2:
            case OP_DUP2:
            case OP_DUP2_X1:
            case OP_DUP2_X2:
            case OP_F2D:
            case OP_F2I:
            case OP_F2L:
            case OP_FADD:
            case OP_FALOAD:
            case OP_FASTORE:
            case OP_FCMPG:
            case OP_FCMPL:
            case OP_FCONST_0:
            case OP_FCONST_1:
            case OP_FCONST_2:
            case OP_FDIV:
            case OP_FLOAD_0:
            case OP_FLOAD_1:
            case OP_FLOAD_2:
            case OP_FLOAD_3:
            case OP_FMUL:
            case OP_FNEG:
            case OP_FREM:
            case OP_FRETURN:
            case OP_FSTORE_0:
            case OP_FSTORE_1:
            case OP_FSTORE_2:
            case OP_FSTORE_3:
            case OP_FSUB:
            case OP_I2B:
            case OP_I2C:
            case OP_I2D:
            case OP_I2F:
            case OP_I2L:
            case OP_I2S:
            case OP_IADD:
            case OP_IALOAD:
            case OP_IAND:
            case OP_IASTORE:
            case OP_ICONST_M1:
            case OP_ICONST_0:
            case OP_ICONST_1:
            case OP_ICONST_2:
            case OP_ICONST_3:
            case OP_ICONST_4:
            case OP_ICONST_5:
            case OP_IDIV:
            case OP_ILOAD_0:
            case OP_ILOAD_1:
            case OP_ILOAD_2:
            case OP_ILOAD_3:
            case OP_IMUL:
            case OP_INEG:
            case OP_IOR:
            case OP_IREM:
            case OP_IRETURN:
            case OP_ISHL:
            case OP_ISHR:
            case OP_ISTORE_0:
            case OP_ISTORE_1:
            case OP_ISTORE_2:
            case OP_ISTORE_3:
            case OP_ISUB:
            case OP_IUSHR:
            case OP_IXOR:
            case OP_L2D:
            case OP_L2F:
            case OP_L2I:
            case OP_LADD:
            case OP_LALOAD:
            case OP_LAND:
            case OP_LASTORE:
            case OP_LCMP:
            case OP_LCONST_0:
            case OP_LCONST_1:
            case OP_LDIV:
            case OP_LLOAD_0:
            case OP_LLOAD_1:
            case OP_LLOAD_2:
            case OP_LLOAD_3:
            case OP_LMUL:
            case OP_LNEG:
            case OP_LOR:
            case OP_LREM:
            case OP_LRETURN:
            case OP_LSHL:
            case OP_LSHR:
            case OP_LSTORE_0:
            case OP_LSTORE_1:
            case OP_LSTORE_2:
            case OP_LSTORE_3:
            case OP_LSUB:
            case OP_LUSHR:
            case OP_LXOR:
            case OP_MONITORENTER:
            case OP_MONITOREXIT:
            case OP_NOP:
            case OP_POP:
            case OP_POP2:
            case OP_RETURN:
            case OP_SALOAD:
            case OP_SASTORE:
            case OP_SWAP:
                break;

            case OP_WIDE:
                skipWideInstruction(codeAttr, codeAttr.get() & 0xff);
                break;

            // unknown
            default:
                throw new InvalidByteCodeException();
        }
    }

    private void skipWideInstruction(final ByteBuffer codeAttr, final int opcode) {
        switch (opcode) {
            case OP_ILOAD:
            case OP_FLOAD:
            case OP_ALOAD:
            case OP_LLOAD:
            case OP_DLOAD:
            case OP_ISTORE:
            case OP_FSTORE:
            case OP_ASTORE:
            case OP_LSTORE:
            case OP_DSTORE:
            case OP_RET:
                codeAttr.getShort();
                break;
            case OP_IINC:
                codeAttr.getInt();
                break;
            // unknown
            default:
                throw new InvalidByteCodeException();
        }
    }

    static void align(ByteBuffer buf, int align) {
        assert Integer.bitCount(align) == 1;
        int p = buf.position();
        int mask = align - 1;
        int amt = mask - (p - 1 & mask);
        buf.position(p + amt);
    }

    static int findLocalVariableEntry(short[] array, int size, int startPc, int length) {
        if (array == null) {
            return -1;
        }
        int low = 0;
        int high = size - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = array[mid * 5] & 0xffff;
            if (midVal < startPc) {
                low = mid + 1;
            } else if (midVal > startPc) {
                high = mid - 1;
            } else {
                if (length != array[mid * 5 + 1]) {
                    // overlapping for same var
                    throw new InvalidLocalVariableIndexException();
                }
                return mid;
            }
        }
        return -low - 1;
    }

    ClassFileImpl getClassFile() {
        return classFile;
    }

    int getModifiers() {
        return modifiers;
    }

    int getIndex() {
        return index;
    }

    ByteBuffer getCodeAttr() {
        return codeAttr;
    }

    int getMaxStack() {
        return maxStack;
    }

    int getMaxLocals() {
        return maxLocals;
    }

    int getEntryPointCount() {
        return entryPoints.length;
    }

    int getEntryPointIndex(final int target) {
        int low = 0;
        int high = getEntryPointCount() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = getEntryPointTarget(mid);
            if (midVal < target) {
                low = mid + 1;
            } else if (midVal > target) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -low - 1;
    }

    int getEntryPointTarget(final int index) {
        return entryPoints[index] & 0xffff;
    }

    int getLineNumber(int bci) {
        return lineNumberTable.getLineNumber(bci);
    }

    int getLocalVarEntryCount(int varIdx) {
        return localVariables[varIdx].length / 5;
    }

    int getLocalVarStartPc(int varIdx, int entryIdx) {
        return localVariables[varIdx][entryIdx * 5] & 0xffff;
    }

    int getLocalVarLength(int varIdx, int entryIdx) {
        return localVariables[varIdx][entryIdx * 5 + 1] & 0xffff;
    }

    int getLocalVarNameIndex(int varIdx, int entryIdx) {
        return localVariables[varIdx][entryIdx * 5 + 2] & 0xffff;
    }

    int getLocalVarDescriptorIndex(int varIdx, int entryIdx) {
        return localVariables[varIdx][entryIdx * 5 + 3] & 0xffff;
    }

    int getLocalVarSignatureIndex(int varIdx, int entryIdx) {
        return localVariables[varIdx][entryIdx * 5 + 4] & 0xffff;
    }

    int getCodeOffs() {
        return codeOffs;
    }

    int getCodeLen() {
        return codeLen;
    }

    int getExTableLen() {
        return exTable.length >> 2;
    }

    int getExTableEntryStartPc(int entry) {
        return exTable[entry << 2] & 0xffff;
    }

    int getExTableEntryEndPc(int entry) {
        return exTable[(entry << 2) + 1] & 0xffff;
    }

    int getExTableEntryHandlerPc(int entry) {
        return exTable[(entry << 2) + 2] & 0xffff;
    }

    int getExTableEntryTypeIdx(int entry) {
        return exTable[(entry << 2) + 3] & 0xffff;
    }

    int getStackMapTableOffs() {
        return stackMapTableOffs;
    }

    int getStackMapTableLen() {
        return stackMapTableLen;
    }

    int getVisibleTypeAnnotationsOffs() {
        return visibleTypeAnnotationsOffs;
    }

    int getVisibleTypeAnnotationsLen() {
        return visibleTypeAnnotationsLen;
    }

    int getInvisibleTypeAnnotationsOffs() {
        return invisibleTypeAnnotationsOffs;
    }

    int getInvisibleTypeAnnotationsLen() {
        return invisibleTypeAnnotationsLen;
    }
}
