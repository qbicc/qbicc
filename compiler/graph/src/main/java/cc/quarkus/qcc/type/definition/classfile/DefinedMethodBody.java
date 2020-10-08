package cc.quarkus.qcc.type.definition.classfile;

import static cc.quarkus.qcc.type.definition.classfile.ClassFile.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * The defined method body content, before verification.
 */
final class DefinedMethodBody {
    private static final short[] NO_SHORTS = new short[0];

    private final ClassFileImpl classFile;
    private final int modifiers;
    private final int index;
    private final ByteBuffer codeAttr;
    private final int maxStack;
    private final int maxLocals;
    private final int codeOffs;
    private final int codeLen;
    private final int exTableOffs;
    private final int exTableLen;

    private final short[] entryPoints; // format: dest-bci (unsigned) cnt (unsigned), sorted by dest-bci

    private final short[] lineNumbers; // format: start_pc line_number (alternating), sorted uniquely by start_pc (ascending)
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

    DefinedMethodBody(final ClassFileImpl classFile, final int modifiers, final int index, final ByteBuffer codeAttr) {
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
        short[] entryPoints = NO_SHORTS;
        int entryPointLen = 0;
        int entryPointSourcesLen = 0;
        // process bytecodes for entry points
        int target;
        while (bc.position() < bc.limit()) {
            int src = bc.position();
            int opcode = bc.get() & 0xff;
            switch (opcode) {
                // interesting cases first
                case OP_JSR:
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
                case OP_IFNONNULL:
                case OP_IFNULL: {
                    // just like GOTO except we also need to fall through
                    target = src + 3;
                    int idx = findEntryPoint(entryPoints, entryPointLen, target);
                    if (idx < 0) {
                        entryPoints = insertNewEntryPoint(entryPoints, idx, entryPointLen++, target);
                    }
                    //goto case OP_GOTO;
                }
                case OP_GOTO: {
                    target = src + bc.getShort();
                    int idx = findEntryPoint(entryPoints, entryPointLen, target);
                    if (idx < 0) {
                        entryPoints = insertNewEntryPoint(entryPoints, idx, entryPointLen++, target);
                    }
                    break;
                }
                case OP_JSR_W: {
                    // just like GOTO_W except we also need to fall through
                    target = src + 5;
                    int idx = findEntryPoint(entryPoints, entryPointLen, target);
                    if (idx < 0) {
                        entryPoints = insertNewEntryPoint(entryPoints, idx, entryPointLen++, target);
                    }
                    //goto case OP_GOTO_W;
                }
                case OP_GOTO_W: {
                    target = src + bc.getInt();
                    int idx = findEntryPoint(entryPoints, entryPointLen, target);
                    if (idx < 0) {
                        entryPoints = insertNewEntryPoint(entryPoints, idx, entryPointLen++, target);
                    }
                    break;
                }
                case OP_LOOKUPSWITCH: {
                    align(bc, 4);
                    target = src + bc.getInt();
                    int idx = findEntryPoint(entryPoints, entryPointLen, target);
                    if (idx < 0) {
                        entryPoints = insertNewEntryPoint(entryPoints, idx, entryPointLen++, target);
                    }
                    int cnt = bc.getInt();
                    for (int i = 0; i < cnt; i ++) {
                        bc.getInt(); // match
                        target = src + bc.getInt();
                        idx = findEntryPoint(entryPoints, entryPointLen, target);
                        if (idx < 0) {
                            entryPoints = insertNewEntryPoint(entryPoints, idx, entryPointLen++, target);
                        }
                    }
                    break;
                }
                case OP_TABLESWITCH: {
                    align(bc, 4);
                    target = src + bc.getInt();
                    int idx = findEntryPoint(entryPoints, entryPointLen, target);
                    if (idx < 0) {
                        entryPoints = insertNewEntryPoint(entryPoints, idx, entryPointLen++, target);
                    }
                    int cnt = -(bc.getInt() - bc.getInt());
                    if (cnt < 0) {
                        throw new InvalidTableSwitchRangeException();
                    }
                    for (int i = 0; i < cnt; i ++) {
                        bc.getInt(); // match
                        target = src + bc.getInt();
                        idx = findEntryPoint(entryPoints, entryPointLen, target);
                        if (idx < 0) {
                            entryPoints = insertNewEntryPoint(entryPoints, idx, entryPointLen++, target);
                        }
                    }
                    break;
                }
                default: {
                    skipInstruction(bc, opcode);
                    break;
                }
            }
        }
        exTableLen = codeAttr.getShort() & 0xffff;
        exTableOffs = codeAttr.position();
        codeAttr.position(exTableOffs + (exTableLen << 3));
        int attrCnt = codeAttr.getShort() & 0xffff;
        short[] lineNumberTable = NO_SHORTS;
        int lineNumberTableLen = 0;
        int stackMapTableOffs = -1;
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
        for (int i = 0; i < attrCnt; i ++) {
            int nameIdx = codeAttr.getShort() & 0xffff;
            int len = codeAttr.getInt();
            if (classFile.utf8ConstantEquals(nameIdx, "LineNumberTable")) {
                int cnt = codeAttr.getShort() & 0xffff;
                // sanity check the length
                if (cnt << 2 != len - 2) {
                    throw new InvalidAttributeLengthException();
                }
                // add some line numbers; this attribute can appear more than once
                if ((lineNumberTable.length >>> 1) - lineNumberTableLen < cnt) {
                    lineNumberTable = Arrays.copyOf(lineNumberTable, lineNumberTableLen + (cnt << 1));
                }
                for (int j = 0; j < cnt; j ++) {
                    int startPc = codeAttr.getShort() & 0xffff;
                    int lineNumber = codeAttr.getShort() & 0xffff;
                    if (lineNumberTable.length == 0) {
                        lineNumberTable = new short[cnt];
                        lineNumberTable[0] = (short) startPc;
                        lineNumberTable[1] = (short) lineNumber;
                    } else {
                        int idx = findLineNumber(lineNumberTable, lineNumberTableLen, startPc);
                        if (idx >= 0) {
                            // ignore
                        } else {
                            idx = -idx - 1;
                            if (idx < lineNumberTableLen) {
                                // make a hole
                                System.arraycopy(lineNumberTable, idx << 1, lineNumberTable, 1 + (idx << 1), (lineNumberTableLen - idx) << 1);
                            }
                            // add list entry
                            lineNumberTable[idx << 1] = (short) startPc;
                            lineNumberTable[(idx << 1) + 1] = (short) lineNumber;
                            lineNumberTableLen++;
                        }
                    }
                }
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
                        localVariables[varIndex] = array = new short[8];
                        array[0] = (short) startPc;
                        array[1] = (short) length;
                        array[2] = (short) varNameIdx;
                        array[lvt ? 3 : 4] = (short) typeIdx;
                        lvtLengths[varIndex] = 1;
                    } else {
                        int lvtLength = lvtLengths[varIndex];
                        int idx = findLocalVariableEntry(array, lvtLength, startPc, length);
                        int base = idx * 5;
                        if (idx >= 0) {
                            // merge
                            if (array[base + (lvt ? 3 : 4)] != 0 || varNameIdx != array[base + 2]) {
                                // already have an entry for it
                                throw new InvalidLocalVariableIndexException();
                            }
                            array[lvt ? 3 : 4] = (short) typeIdx;
                        } else {
                            // insert
                            idx = -idx - 1;
                            if (lvtLength == array.length) {
                                // grow
                                localVariables[varIndex] = array = Arrays.copyOf(array, lvtLength << 1);
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
        // save a copy if we can
        this.lineNumbers = lineNumberTableLen == lineNumberTable.length ? lineNumberTable : Arrays.copyOf(lineNumberTable, lineNumberTableLen);
        for (int i = 0; i < maxLocals; i ++) {
            if (localVariables[i].length > lvtLengths[i]) {
                localVariables[i] = Arrays.copyOf(localVariables[i], lvtLengths[i]);
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
                cnt = -(codeAttr.getInt() - codeAttr.getInt());
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

    static short[] insertNewEntryPoint(short[] entryPoints, int idx, final int entryPointLen, final int target) {
        assert idx < 0;
        idx = -idx - 1;
        if (entryPointLen << 1 == entryPoints.length) {
            entryPoints = Arrays.copyOf(entryPoints, entryPointLen == 0 ? 4 : entryPointLen << 2);
        }
        int base = idx << 1;
        if (base < entryPointLen) {
            // make a hole
            System.arraycopy(entryPoints, base, entryPoints, base + 2, (entryPointLen - idx) << 1);
        }
        entryPoints[base] = (short) target;
        entryPoints[base + 1] = 1;
        return entryPoints;
    }

    static int findEntryPoint(final short[] entryPoints, final int entryPointLen, final int target) {
        if (entryPoints == null) {
            return -1;
        }
        int low = 0;
        int high = entryPointLen - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = entryPoints[mid << 1] & 0xffff;
            if (midVal < target) {
                low = mid + 1;
            } else if (midVal > target) {
                high = mid - 1;
            } else {
                // target matches; bump the count and return it
                entryPoints[(mid << 1) + 1]++;
                return mid;
            }
        }
        // not present
        return -low - 1;
    }

    static void align(ByteBuffer buf, int align) {
        assert Integer.bitCount(align) == 1;
        int p = buf.position();
        int mask = align - 1;
        int amt = mask - (p - 1 & mask);
        buf.position(p + amt);
    }

    static int findLineNumber(short[] table, int size, int bci) {
        if (table == null) {
            return -1;
        }
        int low = 0;
        int high = size - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = table[mid << 1] & 0xffff;
            if (midVal < bci) {
                low = mid + 1;
            } else if (midVal > bci) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -low - 1;
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
        return entryPoints.length >> 1;
    }

    int getEntryPointDestination(int index) {
        return entryPoints[index << 1];
    }

    int getEntryPointIndex(final int target) {
        int low = 0;
        int high = getEntryPointCount() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = entryPoints[mid << 1];
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

    int getEntryPointSourceCount(final int epIdx) {
        return entryPoints[(index << 1) + 1] & 0xffff;
    }

    int getLineNumber(int bci) {
        int low = 0;
        int high = (lineNumbers.length >>> 1) - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = lineNumbers[mid << 1] & 0xffff;
            if (midVal < bci) {
                low = mid + 1;
            } else if (midVal > bci) {
                high = mid - 1;
            } else {
                // exact match
                return lineNumbers[(mid << 1) + 1];
            }
        }
        // return previous entry
        if (low == 0) {
            return -1;
        } else {
            return lineNumbers[((low - 1) << 1) + 1];
        }
    }

    int getLocalVarEntryCount(int varIdx) {
        return localVariables[varIdx].length / 5;
    }

    int getLocalVarStartPc(int varIdx, int entryIdx) {
        return localVariables[varIdx][entryIdx * 5];
    }

    int getLocalVarLength(int varIdx, int entryIdx) {
        return localVariables[varIdx][entryIdx * 5 + 1];
    }

    int getLocalVarNameIndex(int varIdx, int entryIdx) {
        return localVariables[varIdx][entryIdx * 5 + 2];
    }

    int getLocalVarDescriptorIndex(int varIdx, int entryIdx) {
        return localVariables[varIdx][entryIdx * 5 + 3];
    }

    int getLocalVarSignatureIndex(int varIdx, int entryIdx) {
        return localVariables[varIdx][entryIdx * 5 + 4];
    }

    int getCodeOffs() {
        return codeOffs;
    }

    int getCodeLen() {
        return codeLen;
    }

    int getExTableOffs() {
        return exTableOffs;
    }

    int getExTableLen() {
        return exTableLen;
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
