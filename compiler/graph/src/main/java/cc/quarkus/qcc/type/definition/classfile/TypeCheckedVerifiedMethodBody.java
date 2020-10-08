package cc.quarkus.qcc.type.definition.classfile;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.element.MethodElement;

final class TypeCheckedVerifiedMethodBody extends VerifiedMethodBody {

    final StackMapFrame[] stackMap;

    TypeCheckedVerifiedMethodBody(final DefinedMethodBody definedBody, final MethodElement methodElement) {
        super(definedBody, methodElement);
        ClassFileImpl classFile = definedBody.getClassFile();
        int smt = definedBody.getStackMapTableOffs();
        if (smt == -1) {
            throw new MissingStackMapTableException();
        }
        // as always, we only use one entry on the stack for every value even if the value is a class 2 (long/double).
        Type[] typeStack = new Type[definedBody.getMaxStack()];
        // as always, we use two local var slots for class 2 types and one slot for class 1 types.
        Type[] typeLocals = new Type[definedBody.getMaxLocals()];
        int sp = 0;
        int lc;
        int fp = 0;
        int pc = 0;
        ByteBuffer codeAttr = definedBody.getCodeAttr();
        int savePos = codeAttr.position();
        try {
            codeAttr.position(smt);
            boolean instance = (definedBody.getModifiers() & ClassFile.ACC_STATIC) == 0;
            int parameterCount = methodElement.getParameterCount();
            lc = 0;
            if (instance) {
                typeLocals[lc++] = classFile.resolveType();
            }
            for (int i = 0; i < parameterCount; i++) {
                typeLocals[lc++] = methodElement.getParameter(i).getType();
            }
            int cnt = definedBody.getStackMapTableLen();
            StackMapFrame[] frames = new StackMapFrame[cnt + 1]; // reserve one spot for initial frame
            frames[fp++] = new FullFrame(0, buildLocalsArray(classFile, typeLocals, 0, lc, 0), Type.NO_TYPES);
            int epIdx = 0;
            int epStart;
            int offset;
            int tag;
            StackMapFrame oldFrame;
            StackMapFrame newFrame;
            for (int i = 0; i < cnt; i ++) {
                tag = codeAttr.get() & 0xff;
                oldFrame = frames[fp - 1];
                if (tag <= 63) {
                    // SAME
                    pc += tag;
                    newFrame = new SameFrame(pc, oldFrame);
                } else if (tag <= 127) {
                    // SAME_LOCALS_1_STACK_ITEM
                    pc += tag - 64;
                    newFrame = new SameLocals1StackItemFrame(pc, oldFrame, parseType(classFile, codeAttr));
                } else if (tag == 247) {
                    // SAME_LOCALS_1_STACK_ITEM_EXTENDED
                    pc += codeAttr.getShort() & 0xffff;
                    newFrame = new SameLocals1StackItemFrame(pc, oldFrame, parseType(classFile, codeAttr));
                } else if (248 <= tag && tag <= 250) {
                    // CHOP
                    pc += codeAttr.getShort() & 0xffff;
                    newFrame = new ChopFrame(pc, oldFrame, 251 - tag);
                } else if (tag == 251) {
                    // SAME_FRAME_EXTENDED
                    pc += codeAttr.getShort() & 0xffff;
                    newFrame = new SameFrame(pc, oldFrame);
                } else if (tag == 252) {
                    // APPEND (1)
                    pc += codeAttr.getShort() & 0xffff;
                    Type type = parseType(classFile, codeAttr);
                    if (type.isClass2Type()) {
                        newFrame = new Append2Frame(pc, oldFrame, type, Type.VOID);
                    } else {
                        newFrame = new Append1Frame(pc, oldFrame, type);
                    }
                } else if (tag == 253) {
                    // APPEND (2)
                    pc += codeAttr.getShort() & 0xffff;
                    Type type1 = parseType(classFile, codeAttr);
                    Type type2 = parseType(classFile, codeAttr);
                    if (type1.isClass2Type()) {
                        if (type2.isClass2Type()) {
                            newFrame = new Append4Frame(pc, oldFrame, type1, Type.VOID, type2, Type.VOID);
                        } else {
                            newFrame = new Append3Frame(pc, oldFrame, type1, Type.VOID, type2);
                        }
                    } else {
                        if (type2.isClass2Type()) {
                            newFrame = new Append3Frame(pc, oldFrame, type1, type2, Type.VOID);
                        } else {
                            newFrame = new Append2Frame(pc, oldFrame, type1, type2);
                        }
                    }
                } else if (tag == 254) {
                    // APPEND (3)
                    pc += codeAttr.getShort() & 0xffff;
                    Type type1 = parseType(classFile, codeAttr);
                    Type type2 = parseType(classFile, codeAttr);
                    Type type3 = parseType(classFile, codeAttr);
                    if (type1.isClass2Type()) {
                        if (type2.isClass2Type()) {
                            if (type3.isClass2Type()) {
                                newFrame = new Append6Frame(pc, oldFrame, type1, Type.VOID, type2, Type.VOID, type3, Type.VOID);
                            } else {
                                newFrame = new Append5Frame(pc, oldFrame, type1, Type.VOID, type2, Type.VOID, type3);
                            }
                        } else {
                            if (type3.isClass2Type()) {
                                newFrame = new Append5Frame(pc, oldFrame, type1, Type.VOID, type2, type3, Type.VOID);
                            } else {
                                newFrame = new Append4Frame(pc, oldFrame, type1, Type.VOID, type2, type3);
                            }
                        }
                    } else {
                        if (type2.isClass2Type()) {
                            if (type3.isClass2Type()) {
                                newFrame = new Append5Frame(pc, oldFrame, type1, type2, Type.VOID, type3, Type.VOID);
                            } else {
                                newFrame = new Append4Frame(pc, oldFrame, type1, type2, Type.VOID, type3);
                            }
                        } else {
                            if (type3.isClass2Type()) {
                                newFrame = new Append4Frame(pc, oldFrame, type1, type2, type3, Type.VOID);
                            } else {
                                newFrame = new Append3Frame(pc, oldFrame, type1, type2, type3);
                            }
                        }
                    }
                } else if (tag == 255) {
                    // FULL_FRAME
                    pc += codeAttr.getShort() & 0xffff;
                    int nLocals = codeAttr.getShort() & 0xffff;
                    Type[] localTypes = buildLocalsArray(classFile, codeAttr, 0, nLocals, 0);
                    int nStack = codeAttr.getShort() & 0xffff;
                    Type[] stackTypes = nStack == 0 ? Type.NO_TYPES : new Type[nStack];
                    for (int j = 0; j < nStack; j ++) {
                        stackTypes[j] = parseType(classFile, codeAttr);
                    }
                    newFrame = new FullFrame(pc, localTypes, stackTypes);
                } else {
                    throw new InvalidStackMapFrameEntry();
                }
                if (pc == oldFrame.getPc()) {
                    // replace
                    frames[fp - 1] = newFrame;
                } else {
                    // add
                    frames[fp ++] = newFrame;
                }
            }
            stackMap = frames;
        } finally {
            codeAttr.position(savePos);
        }
    }

    private static Type[] buildLocalsArray(ClassFileImpl classFile, final Type[] original, int idx, int cnt, int realIdx) {
        if (idx == cnt) {
            return realIdx == 0 ? Type.NO_TYPES : new Type[realIdx];
        }
        Type t = original[idx];
        boolean class2Type = t.isClass2Type();
        Type[] array = buildLocalsArray(classFile, original, idx + 1, cnt, class2Type ? realIdx + 2 : realIdx + 1);
        array[realIdx] = t;
        if (class2Type) {
            array[realIdx + 1] = Type.VOID;
        }
        return array;
    }

    private static Type[] buildLocalsArray(ClassFileImpl classFile, final ByteBuffer codeAttr, int idx, int cnt, int realIdx) {
        if (idx == cnt) {
            return realIdx == 0 ? Type.NO_TYPES : new Type[realIdx];
        }
        Type t = parseType(classFile, codeAttr);
        boolean class2Type = t.isClass2Type();
        Type[] array = buildLocalsArray(classFile, codeAttr, idx + 1, cnt, class2Type ? realIdx + 2 : realIdx + 1);
        array[realIdx] = t;
        if (class2Type) {
            array[realIdx + 1] = Type.VOID;
        }
        return array;
    }

    private static Type parseType(final ClassFileImpl classFile, final ByteBuffer codeAttr) {
        int tag = codeAttr.get() & 0xff;
        switch (tag) {
            case 0: return Type.VOID;
            case 1: return Type.S32;
            case 2: return Type.F32;
            case 3: return Type.F64;
            case 4: return Type.S64;
            case 5: return Type.NULL_TYPE;
            case 6: return classFile.resolveType().uninitialized();
            case 7: return classFile.loadClass(codeAttr.getShort() & 0xffff);
            case 8: return ((ClassType)classFile.resolveSingleDescriptor(codeAttr.getShort() & 0xffff)).uninitialized();
            default: throw new InvalidStackMapFrameEntry();
        }
    }

    public int getStackMapCount() {
        return stackMap.length;
    }

    public StackMapFrame getFrame(int index) {
        return stackMap[index];
    }

    static abstract class StackMapFrame {
        final short pc;

        StackMapFrame(final int pc) {
            assert 0 <= pc && pc <= 0xffff;
            this.pc = (short) pc;
        }

        final int getPc() {
            return pc & 0xffff;
        }

        abstract int getStackDepth();

        final Type getStackTypeFromTop(int depth) {
            return getStackTypeFromBottom(getStackDepth() - depth - 1);
        }

        abstract Type getStackTypeFromBottom(int height);

        abstract int getLocalCount();

        Type getLocalType(int index) {
            return Type.VOID;
        }
    }

    static abstract class DelegatingFrame extends StackMapFrame {
        final StackMapFrame delegate;

        DelegatingFrame(final int pc, final StackMapFrame delegate) {
            super(pc);
            this.delegate = delegate;
        }

        int getStackDepth() {
            return delegate.getStackDepth();
        }

        Type getStackTypeFromBottom(final int height) {
            return delegate.getStackTypeFromBottom(height);
        }

        int getLocalCount() {
            return delegate.getLocalCount();
        }

        Type getLocalType(final int index) {
            return delegate.getLocalType(index);
        }
    }

    static final class SameFrame extends DelegatingFrame {
        SameFrame(final int pc, final StackMapFrame delegate) {
            super(pc, delegate);
        }
    }

    static final class SameLocals1StackItemFrame extends DelegatingFrame {
        private final Type stackItem;

        SameLocals1StackItemFrame(final int pc, final StackMapFrame delegate, final Type stackItem) {
            super(pc, delegate);
            this.stackItem = stackItem;
        }

        int getStackDepth() {
            return 1;
        }

        Type getStackTypeFromBottom(final int height) {
            if (height != 0) {
                throw new IndexOutOfBoundsException(height);
            }
            return stackItem;
        }
    }

    static final class ChopFrame extends DelegatingFrame {
        private final int localCnt;

        ChopFrame(final int pc, final StackMapFrame delegate, final int localCnt) {
            super(pc, delegate);
            this.localCnt = localCnt;
        }

        int getLocalCount() {
            return localCnt;
        }

        Type getLocalType(final int index) {
            return index >= localCnt ? Type.VOID : super.getLocalType(index);
        }
    }

    static final class Append1Frame extends DelegatingFrame {
        final Type type;

        Append1Frame(final int pc, final StackMapFrame delegate, final Type type) {
            super(pc, delegate);
            this.type = type;
        }

        int getLocalCount() {
            return super.getLocalCount() + 1;
        }

        Type getLocalType(final int index) {
            int superCnt = super.getLocalCount();
            if (index < superCnt) {
                return super.getLocalType(index);
            }
            //noinspection SwitchStatementWithTooFewBranches
            switch (index - superCnt) {
                case 0: return type;
                default: return Type.VOID;
            }
        }
    }

    static final class Append2Frame extends DelegatingFrame {
        final Type type0;
        final Type type1;

        Append2Frame(final int pc, final StackMapFrame delegate, final Type type0, final Type type1) {
            super(pc, delegate);
            this.type0 = type0;
            this.type1 = type1;
        }

        int getLocalCount() {
            return super.getLocalCount() + 2;
        }

        Type getLocalType(final int index) {
            int superCnt = super.getLocalCount();
            if (index < superCnt) {
                return super.getLocalType(index);
            }
            switch (index - superCnt) {
                case 0: return type0;
                case 1: return type1;
                default: return Type.VOID;
            }
        }
    }

    static final class Append3Frame extends DelegatingFrame {
        final Type type0;
        final Type type1;
        final Type type2;

        Append3Frame(final int pc, final StackMapFrame delegate, final Type type0, final Type type1, final Type type2) {
            super(pc, delegate);
            this.type0 = type0;
            this.type1 = type1;
            this.type2 = type2;
        }

        int getLocalCount() {
            return super.getLocalCount() + 3;
        }

        Type getLocalType(final int index) {
            int superCnt = super.getLocalCount();
            if (index < superCnt) {
                return super.getLocalType(index);
            }
            switch (index - superCnt) {
                case 0: return type0;
                case 1: return type1;
                case 2: return type2;
                default: return Type.VOID;
            }
        }
    }

    static final class Append4Frame extends DelegatingFrame {
        final Type type0;
        final Type type1;
        final Type type2;
        final Type type3;

        Append4Frame(final int pc, final StackMapFrame delegate, final Type type0, final Type type1, final Type type2, final Type type3) {
            super(pc, delegate);
            this.type0 = type0;
            this.type1 = type1;
            this.type2 = type2;
            this.type3 = type3;
        }

        int getLocalCount() {
            return super.getLocalCount() + 4;
        }

        Type getLocalType(final int index) {
            int superCnt = super.getLocalCount();
            if (index < superCnt) {
                return super.getLocalType(index);
            }
            switch (index - superCnt) {
                case 0: return type0;
                case 1: return type1;
                case 2: return type2;
                case 3: return type3;
                default: return Type.VOID;
            }
        }
    }

    static final class Append5Frame extends DelegatingFrame {
        final Type type0;
        final Type type1;
        final Type type2;
        final Type type3;
        final Type type4;

        Append5Frame(final int pc, final StackMapFrame delegate, final Type type0, final Type type1, final Type type2, final Type type3, final Type type4) {
            super(pc, delegate);
            this.type0 = type0;
            this.type1 = type1;
            this.type2 = type2;
            this.type3 = type3;
            this.type4 = type4;
        }

        int getLocalCount() {
            return super.getLocalCount() + 5;
        }

        Type getLocalType(final int index) {
            int superCnt = super.getLocalCount();
            if (index < superCnt) {
                return super.getLocalType(index);
            }
            switch (index - superCnt) {
                case 0: return type0;
                case 1: return type1;
                case 2: return type2;
                case 3: return type3;
                case 4: return type4;
                default: return Type.VOID;
            }
        }
    }

    static final class Append6Frame extends DelegatingFrame {
        final Type type0;
        final Type type1;
        final Type type2;
        final Type type3;
        final Type type4;
        final Type type5;

        Append6Frame(final int pc, final StackMapFrame delegate, final Type type0, final Type type1, final Type type2, final Type type3, final Type type4, final Type type5) {
            super(pc, delegate);
            this.type0 = type0;
            this.type1 = type1;
            this.type2 = type2;
            this.type3 = type3;
            this.type4 = type4;
            this.type5 = type5;
        }

        int getLocalCount() {
            return super.getLocalCount() + 6;
        }

        Type getLocalType(final int index) {
            int superCnt = super.getLocalCount();
            if (index < superCnt) {
                return super.getLocalType(index);
            }
            switch (index - superCnt) {
                case 0: return type0;
                case 1: return type1;
                case 2: return type2;
                case 3: return type3;
                case 4: return type4;
                case 5: return type5;
                default: return Type.VOID;
            }
        }
    }

    static final class FullFrame extends StackMapFrame {
        private final Type[] locals;
        private final Type[] stack;

        FullFrame(final int pc, final Type[] locals, final Type[] stack) {
            super(pc);
            this.locals = locals;
            this.stack = stack;
        }

        int getStackDepth() {
            return stack.length;
        }

        Type getStackTypeFromBottom(final int height) {
            return stack[height];
        }

        int getLocalCount() {
            return locals.length;
        }

        Type getLocalType(final int index) {
            return locals[index];
        }
    }
}
