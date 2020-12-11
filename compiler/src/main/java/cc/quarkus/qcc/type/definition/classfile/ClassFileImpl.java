package cc.quarkus.qcc.type.definition.classfile;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ClassFileUtil;
import cc.quarkus.qcc.type.definition.DefineFailedException;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.InvokableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterElement;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.Descriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.ClassSignature;
import cc.quarkus.qcc.type.generic.ClassTypeSignature;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.TypeSignature;

final class ClassFileImpl extends AbstractBufferBacked implements ClassFile {
    private static final VarHandle intArrayHandle = MethodHandles.arrayElementVarHandle(int[].class);
    private static final VarHandle intArrayArrayHandle = MethodHandles.arrayElementVarHandle(int[][].class);
    private static final VarHandle literalArrayHandle = MethodHandles.arrayElementVarHandle(Literal[].class);
    private static final VarHandle stringArrayHandle = MethodHandles.arrayElementVarHandle(String[].class);
    private static final VarHandle annotationArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[].class);
    private static final VarHandle annotationArrayArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[][].class);
    private static final VarHandle descriptorArrayHandle = MethodHandles.arrayElementVarHandle(Descriptor[].class);

    private static final int[] NO_INTS = new int[0];
    private static final int[][] NO_INT_ARRAYS = new int[0][];

    private final int[] cpOffsets;
    private final String[] strings;
    private final Literal[] literals;
    private final Descriptor[] descriptors;
    /**
     * This is the method parameter part of every method descriptor.
     */
    private final boolean[][] methodParamClass2;
    /**
     * This is the type of every field descriptor or the return type of every method descriptor.
     */
    private final ValueType[] types;
    private final int interfacesOffset;
    private final int[] fieldOffsets;
    private final int[][] fieldAttributeOffsets;
    private final int[] methodOffsets;
    private final int[][] methodAttributeOffsets;
    private final int[] attributeOffsets;
    private final int thisClassIdx;
    private final TypeSystem typeSystem;
    private final LiteralFactory literalFactory;
    private final ClassContext ctxt;
    private final String sourceFile;

    ClassFileImpl(final ClassContext ctxt, final ByteBuffer buffer) {
        super(buffer);
        this.ctxt = ctxt;
        typeSystem = ctxt.getTypeSystem();
        literalFactory = ctxt.getLiteralFactory();
        // scan the file to build up offset tables
        ByteBuffer scanBuf = buffer.duplicate();
        // do some basic pieces of verification
        if (scanBuf.order() != ByteOrder.BIG_ENDIAN) {
            throw new DefineFailedException("Wrong byte buffer order");
        }
        int magic = scanBuf.getInt();
        if (magic != 0xcafebabe) {
            throw new DefineFailedException("Bad magic number");
        }
        int minor = scanBuf.getShort() & 0xffff;
        int major = scanBuf.getShort() & 0xffff;
        // todo fix up
        if (major < 45 || major == 45 && minor < 3 || major > 55 || major == 55 && minor > 0) {
            throw new DefineFailedException("Unsupported class version " + major + "." + minor);
        }
        int cpCount = (scanBuf.getShort() & 0xffff);
        // one extra slot because the constant pool is one-based, so just leave a hole at the beginning
        int[] cpOffsets = new int[cpCount];
        for (int i = 1; i < cpCount; i ++) {
            cpOffsets[i] = scanBuf.position();
            int tag = scanBuf.get() & 0xff;
            switch (tag) {
                case ClassFile.CONSTANT_Utf8: {
                    int size = scanBuf.getShort() & 0xffff;
                    scanBuf.position(scanBuf.position() + size);
                    break;
                }
                case ClassFile.CONSTANT_Integer:
                case ClassFile.CONSTANT_Float:
                case ClassFile.CONSTANT_Fieldref:
                case ClassFile.CONSTANT_Methodref:
                case ClassFile.CONSTANT_InterfaceMethodref:
                case ClassFile.CONSTANT_NameAndType:
                case ClassFile.CONSTANT_Dynamic:
                case ClassFile.CONSTANT_InvokeDynamic: {
                    scanBuf.position(scanBuf.position() + 4);
                    break;
                }
                case ClassFile.CONSTANT_Class:
                case ClassFile.CONSTANT_String:
                case ClassFile.CONSTANT_MethodType:
                case ClassFile.CONSTANT_Module:
                case ClassFile.CONSTANT_Package: {
                    scanBuf.position(scanBuf.position() + 2);
                    break;
                }
                case ClassFile.CONSTANT_Long:
                case ClassFile.CONSTANT_Double: {
                    scanBuf.position(scanBuf.position() + 8);
                    i++; // two slots
                    break;
                }
                case ClassFile.CONSTANT_MethodHandle: {
                    scanBuf.position(scanBuf.position() + 3);
                    break;
                }
                default: {
                    throw new DefineFailedException("Unknown constant pool tag " + Integer.toHexString(tag) + " at index " + i);
                }
            }
        }
        StringBuilder b = new StringBuilder(64);
        int access = scanBuf.getShort() & 0xffff;
        int thisClassIdx = scanBuf.getShort() & 0xffff;
        int superClassIdx = scanBuf.getShort() & 0xffff;
        int interfacesCount = scanBuf.getShort() & 0xffff;
        int interfacesOffset = scanBuf.position();
        for (int i = 0; i < interfacesCount; i ++) {
            scanBuf.getShort();
        }
        int fieldsCnt = scanBuf.getShort() & 0xffff;
        int[] fieldOffsets = new int[fieldsCnt];
        int[][] fieldAttributeOffsets = new int[fieldsCnt][];
        for (int i = 0; i < fieldsCnt; i ++) {
            fieldOffsets[i] = scanBuf.position();
            int fieldAccess = scanBuf.getShort() & 0xffff;
            scanBuf.getShort(); // name index
            scanBuf.getShort(); // descriptor index
            // skip attributes
            int attrCnt = scanBuf.getShort() & 0xffff;
            fieldAttributeOffsets[i] = new int[attrCnt];
            for (int j = 0; j < attrCnt; j ++) {
                fieldAttributeOffsets[i][j] = scanBuf.position();
                scanBuf.getShort(); // name index
                int size = scanBuf.getInt();
                scanBuf.position(scanBuf.position() + size);
            }
        }
        int methodsCnt = scanBuf.getShort() & 0xffff;
        int[] methodOffsets = new int[methodsCnt];
        int[][] methodAttributeOffsets = new int[methodsCnt][];
        for (int i = 0; i < methodsCnt; i ++) {
            methodOffsets[i] = scanBuf.position();
            int methodAccess = scanBuf.getShort() & 0xffff;
            scanBuf.getShort(); // name index
            scanBuf.getShort(); // descriptor index
            // skip attributes - except for code (for now)
            int attrCnt = scanBuf.getShort() & 0xffff;
            methodAttributeOffsets[i] = new int[attrCnt];
            for (int j = 0; j < attrCnt; j ++) {
                methodAttributeOffsets[i][j] = scanBuf.position();
                scanBuf.getShort(); // name index
                int size = scanBuf.getInt();
                scanBuf.position(scanBuf.position() + size);
            }
        }
        int attrCnt = scanBuf.getShort() & 0xffff;
        int[] attributeOffsets = new int[attrCnt];
        for (int i = 0; i < attrCnt; i ++) {
            attributeOffsets[i] = scanBuf.position();
            scanBuf.getShort(); // name index
            int size = scanBuf.getInt();
            scanBuf.position(scanBuf.position() + size);
        }
        if (scanBuf.hasRemaining()) {
            throw new DefineFailedException("Extra data at end of class file");
        }

        this.interfacesOffset = interfacesOffset;
        this.fieldOffsets = fieldOffsets;
        this.fieldAttributeOffsets = fieldAttributeOffsets;
        this.methodOffsets = methodOffsets;
        this.methodAttributeOffsets = methodAttributeOffsets;
        this.attributeOffsets = attributeOffsets;
        this.cpOffsets = cpOffsets;
        this.thisClassIdx = thisClassIdx;
        strings = new String[cpOffsets.length];
        literals = new Literal[cpOffsets.length];
        descriptors = new Descriptor[cpOffsets.length];
        methodParamClass2 = new boolean[cpOffsets.length][];
        types = new ValueType[cpOffsets.length];
        // read globally-relevant attributes
        String sourceFile = null;
        int cnt = getAttributeCount();
        for (int i = 0; i < cnt; i ++) {
            if (attributeNameEquals(i, "SourceFile")) {
                sourceFile = getUtf8Constant(getRawAttributeShort(i, 0));
                break;
            }
        }
        this.sourceFile = sourceFile;
    }

    public ClassFile getClassFile() {
        return this;
    }

    public ClassContext getClassContext() {
        return ctxt;
    }

    public int getMajorVersion() {
        return getShort(6);
    }

    public int getMinorVersion() {
        return getShort(4);
    }

    public int getConstantCount() {
        return cpOffsets.length;
    }

    public int getConstantType(final int poolIndex) {
        int cpOffset = cpOffsets[poolIndex];
        return cpOffset == 0 ? 0 : getByte(cpOffset);
    }

    public boolean utf8ConstantEquals(final int idx, final String expected) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, ClassFileUtil.CONSTANT_Utf8);
        int offs = cpOffsets[idx];
        return utf8TextEquals(offs + 3, getShort(offs + 1), expected);
    }

    public String getUtf8Constant(final int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        if (idx == 0) {
            return null;
        }
        // TODO: deduplication
        String result = getVolatile(strings, idx);
        if (result != null) {
            return result;
        }
        checkConstantType(idx, ClassFileUtil.CONSTANT_Utf8);
        int offs = cpOffsets[idx];
        int len = getShort(offs + 1);
        return setIfNull(strings, idx, getUtf8Text(offs + 3, len, new StringBuilder(len)));
    }

    public ByteBuffer getUtf8ConstantAsBuffer(final int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        if (idx == 0) {
            return null;
        }
        checkConstantType(idx, ClassFileUtil.CONSTANT_Utf8);
        int offs = cpOffsets[idx];
        int len = getShort(offs + 1);
        return slice(offs + 3, len);
    }

    int utf8ConstantByteAt(final int idx, final int offset) {
        int offs = cpOffsets[idx];
        if (offset >= getShort(offs + 1)) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getByte(offs + 3 + offset);
    }

    public void checkConstantType(final int idx, final int expectedType) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        // also validates that the constant exists
        int cpOffset = cpOffsets[idx];
        if (cpOffset == 0 || getByte(cpOffset) != expectedType) {
            throw new ConstantTypeMismatchException();
        }
    }

    public void checkConstantType(final int idx, final int expectedType1, final int expectedType2) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        // also validates that the constant exists
        int cpOffset = cpOffsets[idx];
        int actual = getByte(cpOffset);
        if (cpOffset == 0 || actual != expectedType1 && actual != expectedType2) {
            throw new ConstantTypeMismatchException();
        }
    }

    public Literal getConstantValue(int idx) {
        if (idx == 0) {
            return null;
        }
        Literal lit = getVolatile(literals, idx);
        if (lit != null) {
            return lit;
        }
        int constantType = getConstantType(idx);
        switch (constantType) {
            case CONSTANT_Class: return setIfNull(literals, idx, getClassConstant(idx));
            case CONSTANT_String:
                return setIfNull(literals, idx, literalFactory.literalOf(getStringConstant(idx)));
            case CONSTANT_Integer:
                return setIfNull(literals, idx, literalFactory.literalOf(getIntConstant(idx)));
            case CONSTANT_Float:
                return setIfNull(literals, idx, literalFactory.literalOf(getFloatConstant(idx)));
            case CONSTANT_Long:
                return setIfNull(literals, idx, literalFactory.literalOf(getLongConstant(idx)));
            case CONSTANT_Double:
                return setIfNull(literals, idx, literalFactory.literalOf(getDoubleConstant(idx)));
            default: {
                throw new IllegalArgumentException("Unexpected constant type at index " + idx);
            }
        }
    }

    TypeIdLiteral getClassConstant(int idx) {
        int nameIdx = getClassConstantNameIdx(idx);
        String name = getUtf8Constant(nameIdx);
        assert name != null;
        if (name.startsWith("[")) {
            ArrayTypeDescriptor desc = (ArrayTypeDescriptor) getDescriptorConstant(nameIdx);
            ValueType type = ctxt.resolveTypeFromDescriptor(desc, List.of(), TypeSignature.synthesize(ctxt, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
            return ((ReferenceType) type).getUpperBound();
        } else {
            DefinedTypeDefinition definedType = ctxt.findDefinedType(name);
            if (definedType == null) {
                throw new DefineFailedException("No class named " + name + " found");
            }
            return definedType.validate().getTypeId();
        }
    }

    public int getRawConstantByte(final int idx, final int offset) throws IndexOutOfBoundsException {
        int cpOffset = cpOffsets[idx];
        return cpOffset == 0 ? 0 : getByte(cpOffset + offset);
    }

    public int getRawConstantShort(final int idx, final int offset) throws IndexOutOfBoundsException {
        int cpOffset = cpOffsets[idx];
        return cpOffset == 0 ? 0 : getShort(cpOffset + offset);
    }

    public int getRawConstantInt(final int idx, final int offset) throws IndexOutOfBoundsException {
        int cpOffset = cpOffsets[idx];
        return cpOffset == 0 ? 0 : getInt(cpOffset + offset);
    }

    public long getRawConstantLong(final int idx, final int offset) throws IndexOutOfBoundsException {
        int cpOffset = cpOffsets[idx];
        return cpOffset == 0 ? 0 : getLong(cpOffset + offset);
    }

    TypeIdLiteral resolveSingleType(int cpIdx) {
        int cpOffset = cpOffsets[cpIdx];
        return resolveSingleType(cpOffset + 3, getShort(cpOffset + 1));
    }

    TypeIdLiteral resolveSingleType(int offs, int maxLen) {
        return loadClass(offs + 1, maxLen - 1, false);
    }

    TypeIdLiteral resolveSingleType(String name) {
        DefinedTypeDefinition definedType = ctxt.findDefinedType(name);
        return definedType == null ? null : definedType.validate().getTypeId();
    }

    public TypeIdLiteral resolveType() {
        int nameIdx = getShort(cpOffsets[thisClassIdx] + 1);
        int offset = cpOffsets[nameIdx];
        return loadClass(offset + 3, getShort(offset + 1), false);
    }

    private ValueType loadClassAsReferenceType(final int offs, final int len, final boolean expectTerminator) {
        TypeIdLiteral typeId = loadClass(offs, len, expectTerminator);
        if (typeId != null) {
            return typeSystem.getReferenceType(typeId);
        } else {
            String str = ctxt.deduplicate(buffer, offs, len);
            ctxt.getCompilationContext().error("Failed to link %s (class %s not found)", getName(), str);
            return typeSystem.getPoisonType();
        }
    }

    private TypeIdLiteral loadClass(final int offs, final int len, final boolean expectTerminator) {
        DefinedTypeDefinition definedType = ctxt.findDefinedType(ctxt.deduplicate(buffer, offs, len));
        return definedType == null ? null : definedType.validate().getTypeId();
    }

    public int getAccess() {
        return getShort(interfacesOffset - 8);
    }

    public String getName() {
        return getClassConstantName(getShort(interfacesOffset - 6));
    }

    public String getSuperClassName() {
        return getClassConstantName(getShort(interfacesOffset - 4));
    }

    public int getInterfaceNameCount() {
        return getShort(interfacesOffset - 2);
    }

    public String getInterfaceName(final int idx) {
        if (idx < 0 || idx >= getInterfaceNameCount()) {
            throw new IndexOutOfBoundsException(idx);
        }
        return getClassConstantName(getShort(interfacesOffset + (idx << 1)));
    }

    public Descriptor getDescriptorConstant(final int idx) {
        if (idx == 0) {
            return null;
        }
        Descriptor descriptor = getVolatile(descriptors, idx);
        if (descriptor != null) {
            return descriptor;
        } else {
            return setIfNull(descriptors, idx, Descriptor.parse(ctxt, getUtf8ConstantAsBuffer(idx)));
        }
    }

    public int getFieldCount() {
        return fieldOffsets.length;
    }

    public int getFieldAttributeCount(final int idx) throws IndexOutOfBoundsException {
        return fieldAttributeOffsets[idx].length;
    }

    public boolean fieldAttributeNameEquals(final int fieldIdx, final int attrIdx, final String expected) throws IndexOutOfBoundsException {
        return utf8ConstantEquals(getShort(fieldAttributeOffsets[fieldIdx][attrIdx]), expected);
    }

    public int getFieldRawAttributeByte(final int fieldIdx, final int attrIdx, final int offset) throws IndexOutOfBoundsException {
        int base = fieldAttributeOffsets[fieldIdx][attrIdx];
        if (offset >= getInt(base + 2)) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getByte(base + 6 + offset);
    }

    public int getFieldRawAttributeShort(final int fieldIdx, final int attrIdx, final int offset) throws IndexOutOfBoundsException {
        int base = fieldAttributeOffsets[fieldIdx][attrIdx];
        if (offset >= getInt(base + 2) - 1) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getShort(base + 6 + offset);
    }

    public int getFieldRawAttributeInt(final int fieldIdx, final int attrIdx, final int offset) throws IndexOutOfBoundsException {
        int base = fieldAttributeOffsets[fieldIdx][attrIdx];
        if (offset >= getInt(base + 2) - 3) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getInt(base + 6 + offset);
    }

    public long getFieldRawAttributeLong(final int fieldIdx, final int attrIdx, final int offset) throws IndexOutOfBoundsException {
        int base = fieldAttributeOffsets[fieldIdx][attrIdx];
        if (offset >= getInt(base + 2) - 7) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getLong(base + 6 + offset);
    }

    public ByteBuffer getFieldRawAttributeContent(final int fieldIdx, final int attrIdx) throws IndexOutOfBoundsException {
        int base = fieldAttributeOffsets[fieldIdx][attrIdx];
        return slice(base + 6, getInt(base + 2));
    }

    public int getFieldAttributeContentLength(final int fieldIdx, final int attrIdx) throws IndexOutOfBoundsException {
        return getInt(fieldAttributeOffsets[fieldIdx][attrIdx] + 2);
    }

    public int getMethodCount() {
        return methodOffsets.length;
    }

    public int getMethodAttributeCount(final int idx) throws IndexOutOfBoundsException {
        return methodAttributeOffsets[idx].length;
    }

    public boolean methodAttributeNameEquals(final int methodIdx, final int attrIdx, final String expected) throws IndexOutOfBoundsException {
        return utf8ConstantEquals(getShort(methodAttributeOffsets[methodIdx][attrIdx]), expected);
    }

    public int getMethodRawAttributeByte(final int methodIdx, final int attrIdx, final int offset) throws IndexOutOfBoundsException {
        int base = methodAttributeOffsets[methodIdx][attrIdx];
        if (offset >= getInt(base + 2)) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getByte(base + 6 + offset);
    }

    public int getMethodRawAttributeShort(final int methodIdx, final int attrIdx, final int offset) throws IndexOutOfBoundsException {
        int base = methodAttributeOffsets[methodIdx][attrIdx];
        if (offset >= getInt(base + 2) - 1) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getShort(base + 6 + offset);
    }

    public int getMethodRawAttributeInt(final int methodIdx, final int attrIdx, final int offset) throws IndexOutOfBoundsException {
        int base = methodAttributeOffsets[methodIdx][attrIdx];
        if (offset >= getInt(base + 2) - 3) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getInt(base + 6 + offset);
    }

    public long getMethodRawAttributeLong(final int methodIdx, final int attrIdx, final int offset) throws IndexOutOfBoundsException {
        int base = methodAttributeOffsets[methodIdx][attrIdx];
        if (offset >= getInt(base + 2) - 7) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getLong(base + 6 + offset);
    }

    public ByteBuffer getMethodRawAttributeContent(final int methodIdx, final int attrIdx) throws IndexOutOfBoundsException {
        int base = methodAttributeOffsets[methodIdx][attrIdx];
        return slice(base + 6, getInt(base + 2));
    }

    public int getMethodAttributeContentLength(final int methodIdx, final int attrIdx) throws IndexOutOfBoundsException {
        return getInt(methodAttributeOffsets[methodIdx][attrIdx] + 2);
    }

    public int getAttributeCount() {
        return attributeOffsets.length;
    }

    public boolean attributeNameEquals(final int idx, final String expected) throws IndexOutOfBoundsException {
        return utf8ConstantEquals(getShort(attributeOffsets[idx]), expected);
    }

    public int getAttributeContentLength(final int idx) throws IndexOutOfBoundsException {
        return getInt(attributeOffsets[idx] + 2);
    }

    public ByteBuffer getRawAttributeContent(final int idx) throws IndexOutOfBoundsException {
        int base = attributeOffsets[idx];
        return slice(base + 6, getInt(base + 2));
    }

    public int getRawAttributeByte(final int idx, final int offset) throws IndexOutOfBoundsException {
        int base = attributeOffsets[idx];
        if (offset >= getInt(base + 2)) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getByte(base + 6 + offset);
    }

    public int getRawAttributeShort(final int idx, final int offset) throws IndexOutOfBoundsException {
        int base = attributeOffsets[idx];
        if (offset >= getInt(base + 2) - 1) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getShort(base + 6 + offset);
    }

    public int getRawAttributeInt(final int idx, final int offset) throws IndexOutOfBoundsException {
        int base = attributeOffsets[idx];
        if (offset >= getInt(base + 2) - 3) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getInt(base + 6 + offset);
    }

    public long getRawAttributeLong(final int idx, final int offset) throws IndexOutOfBoundsException {
        int base = attributeOffsets[idx];
        if (offset >= getInt(base + 2) - 7) {
            throw new IndexOutOfBoundsException(offset);
        }
        return getLong(base + 6 + offset);
    }

    public void accept(final DefinedTypeDefinition.Builder builder) throws ClassFormatException {
        builder.setName(getName());
        builder.setContext(ctxt);
        int access = getAccess();
        String superClassName = getSuperClassName();
        builder.setSuperClassName(getSuperClassName());
        int cnt = getInterfaceNameCount();
        for (int i = 0; i < cnt; i ++) {
            builder.addInterfaceName(getInterfaceName(i));
        }
        // make sure that annotations are added first for convenience
        int acnt = getAttributeCount();
        ClassSignature signature = null;
        for (int i = 0; i < acnt; i ++) {
            if (attributeNameEquals(i, "RuntimeVisibleAnnotations")) {
                ByteBuffer data = getRawAttributeContent(i);
                int ac = data.getShort() & 0xffff;
                Annotation[] annotations = new Annotation[ac];
                for (int j = 0; j < ac; j ++) {
                    annotations[j] = Annotation.parse(this, ctxt, data);
                }
                builder.setVisibleAnnotations(List.of(annotations));
            } else if (attributeNameEquals(i, "RuntimeInvisibleAnnotations")) {
                ByteBuffer data = getRawAttributeContent(i);
                int ac = data.getShort() & 0xffff;
                Annotation[] annotations = new Annotation[ac];
                for (int j = 0; j < ac; j++) {
                    annotations[j] = Annotation.parse(this, ctxt, data);
                }
                builder.setInvisibleAnnotations(List.of(annotations));
            } else if (attributeNameEquals(i, "RuntimeVisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getRawAttributeContent(i));
                builder.setVisibleTypeAnnotations(list);
            } else if (attributeNameEquals(i, "RuntimeInvisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getRawAttributeContent(i));
                builder.setInvisibleTypeAnnotations(list);
            } else if (attributeNameEquals(i, "Deprecated")) {
                access |= I_ACC_DEPRECATED;
            } else if (attributeNameEquals(i, "Synthetic")) {
                access |= ACC_SYNTHETIC;
            } else if (attributeNameEquals(i, "Signature")) {
                int sigIdx = getRawAttributeShort(i, 0);
                signature = ClassSignature.parse(ctxt, getUtf8ConstantAsBuffer(sigIdx));
            } else if (attributeNameEquals(i, "SourceFile")) {
                // todo
            } else if (attributeNameEquals(i, "BootstrapMethods")) {
                // todo
            } else if (attributeNameEquals(i, "NestHost")) {
                // todo
            } else if (attributeNameEquals(i, "NestMembers")) {
                // todo
            }
        }
        if (signature == null) {
            ClassTypeSignature superClassSig = superClassName == null ? null : (ClassTypeSignature) TypeSignature.synthesize(ctxt, ClassTypeDescriptor.synthesize(ctxt, superClassName));
            ClassTypeSignature[] interfaceSigs = new ClassTypeSignature[cnt];
            for (int i = 0; i < cnt; i ++) {
                interfaceSigs[i] = (ClassTypeSignature) TypeSignature.synthesize(ctxt, ClassTypeDescriptor.synthesize(ctxt, getInterfaceName(i)));
            }
            signature = ClassSignature.synthesize(ctxt, superClassSig, List.of(interfaceSigs));
        }
        builder.setSignature(signature);
        boolean foundInitializer = false;
        acnt = getMethodCount();
        for (int i = 0; i < acnt; i ++) {
            int base = methodOffsets[i];
            int nameIdx = getShort(base + 2);
            if (utf8ConstantEquals(nameIdx, "<clinit>")) {
                builder.setInitializer(this, i);
                foundInitializer = true;
            } else {
                if (utf8ConstantEquals(nameIdx, "<init>")) {
                    builder.addConstructor(this, i);
                } else {
                    builder.addMethod(this, i);
                }
            }
        }
        if (! foundInitializer) {
            // synthesize an empty one
            builder.setInitializer(this, 0);
        }
        acnt = getFieldCount();
        for (int i = 0; i < acnt; i ++) {
            builder.addField(this, i);
        }
        builder.setModifiers(access);
    }

    public FieldElement resolveField(final int index, final DefinedTypeDefinition enclosing) {
        FieldElement.Builder builder = FieldElement.builder();
        builder.setEnclosingType(enclosing);
        TypeDescriptor typeDescriptor = (TypeDescriptor) getDescriptorConstant(getShort(fieldOffsets[index] + 4));
        builder.setDescriptor(typeDescriptor);
        builder.setModifiers(getShort(fieldOffsets[index]));
        builder.setName(getUtf8Constant(getShort(fieldOffsets[index] + 2)));
        // process attributes
        TypeSignature signature = null;
        int cnt = getFieldAttributeCount(index);
        for (int i = 0; i < cnt; i ++) {
            if (fieldAttributeNameEquals(index, i, "RuntimeVisibleAnnotations")) {
                ByteBuffer data = getFieldRawAttributeContent(index, i);
                int ac = data.getShort() & 0xffff;
                Annotation[] annotations = new Annotation[ac];
                for (int j = 0; j < ac; j ++) {
                    annotations[j] = Annotation.parse(this, ctxt, data);
                }
                builder.setVisibleAnnotations(List.of(annotations));
            } else if (fieldAttributeNameEquals(index, i, "RuntimeInvisibleAnnotations")) {
                ByteBuffer data = getFieldRawAttributeContent(index, i);
                int ac = data.getShort() & 0xffff;
                Annotation[] annotations = new Annotation[ac];
                for (int j = 0; j < ac; j ++) {
                    annotations[j] = Annotation.parse(this, ctxt, data);
                }
                builder.setInvisibleAnnotations(List.of(annotations));
            } else if (fieldAttributeNameEquals(index, i, "Signature")) {
                int sigIdx = getFieldRawAttributeShort(index, i, 0);
                signature = TypeSignature.parse(ctxt, getUtf8ConstantAsBuffer(sigIdx));
            } else if (fieldAttributeNameEquals(index, i, "RuntimeVisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getFieldRawAttributeContent(index, i));
                builder.setVisibleTypeAnnotations(list);
            } else if (fieldAttributeNameEquals(index, i, "RuntimeInvisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getFieldRawAttributeContent(index, i));
                builder.setInvisibleTypeAnnotations(list);
            }
        }
        if (signature == null) {
            signature = TypeSignature.synthesize(ctxt, typeDescriptor);
        }
        builder.setSignature(signature);
        builder.setSourceFileName(sourceFile);
        builder.setIndex(index);
        return builder.build();
    }

    public MethodElement resolveMethod(final int index, final DefinedTypeDefinition enclosing) {
        MethodElement.Builder builder = MethodElement.builder();
        builder.setEnclosingType(enclosing);
        int methodModifiers = getShort(methodOffsets[index]);
        builder.setModifiers(methodModifiers);
        builder.setName(getUtf8Constant(getShort(methodOffsets[index] + 2)));
        boolean mayHaveExact = (methodModifiers & ACC_ABSTRACT) == 0;
        boolean hasVirtual = (methodModifiers & (ACC_STATIC | ACC_PRIVATE)) == 0;
        if (mayHaveExact) {
            addMethodBody(builder, index, enclosing);
        }
        addParameters(builder, index, enclosing);
        addMethodAnnotations(index, builder);
        builder.setIndex(index);
        builder.setSourceFileName(sourceFile);
        return builder.build();
    }

    public ConstructorElement resolveConstructor(final int index, final DefinedTypeDefinition enclosing) {
        ConstructorElement.Builder builder = ConstructorElement.builder();
        builder.setEnclosingType(enclosing);
        int methodModifiers = getShort(methodOffsets[index]);
        builder.setModifiers(methodModifiers);
        addMethodBody(builder, index, enclosing);
        addParameters(builder, index, enclosing);
        addMethodAnnotations(index, builder);
        builder.setIndex(index);
        builder.setSourceFileName(sourceFile);
        return builder.build();
    }

    public InitializerElement resolveInitializer(final int index, final DefinedTypeDefinition enclosing) {
        InitializerElement.Builder builder = InitializerElement.builder();
        builder.setEnclosingType(enclosing);
        builder.setModifiers(ACC_STATIC);
        if (index != 0) {
            addMethodBody(builder, index, enclosing);
        }
        builder.setSourceFileName(sourceFile);
        builder.setIndex(index);
        return builder.build();
    }

    private void addMethodBody(ExecutableElement.Builder builder, int index, final DefinedTypeDefinition enclosing) {
        int attrCount = getMethodAttributeCount(index);
        for (int i = 0; i < attrCount; i ++) {
            if (methodAttributeNameEquals(index, i, "Code")) {
                addExactBody(builder, index, getMethodRawAttributeContent(index, i), enclosing);
                return;
            }
        }
    }

    private void addExactBody(final ExecutableElement.Builder builder, final int index, final ByteBuffer codeAttr, final DefinedTypeDefinition enclosing) {
        int modifiers = getShort(methodOffsets[index]);
        builder.setMethodBody(new ExactMethodHandleImpl(this, modifiers, index, codeAttr, enclosing));
    }

    private void addParameters(InvokableElement.Builder builder, int index, final DefinedTypeDefinition enclosing) {
        int base = methodOffsets[index];
        int descIdx = getShort(base + 4);
        MethodDescriptor methodDescriptor = (MethodDescriptor) getDescriptorConstant(descIdx);
        int attrCnt = getMethodAttributeCount(index);
        assert methodDescriptor != null;
        int realCnt = methodDescriptor.getParameterTypes().size();
        ByteBuffer visibleAnn = null;
        ByteBuffer invisibleAnn = null;
        ByteBuffer visibleTypeAnn = null;
        ByteBuffer invisibleTypeAnn = null;
        ByteBuffer methodParams = null;
        MethodSignature signature = null;
        for (int i = 0; i < attrCnt; i ++) {
            if (methodAttributeNameEquals(index, i, "RuntimeVisibleParameterAnnotations")) {
                visibleAnn = getMethodRawAttributeContent(index, i);
            } else if (methodAttributeNameEquals(index, i, "RuntimeInvisibleParameterAnnotations")) {
                invisibleAnn = getMethodRawAttributeContent(index, i);
            } else if (methodAttributeNameEquals(index, i, "RuntimeVisibleParameterTypeAnnotations")) {
                visibleTypeAnn = getMethodRawAttributeContent(index, i);
            } else if (methodAttributeNameEquals(index, i, "RuntimeInvisibleParameterTypeAnnotations")) {
                invisibleTypeAnn = getMethodRawAttributeContent(index, i);
            } else if (methodAttributeNameEquals(index, i, "MethodParameters")) {
                methodParams = getMethodRawAttributeContent(index, i);
            } else if (methodAttributeNameEquals(index, i, "Signature")) {
                // todo: variant which accepts a MethodDescriptor to account for differing param lengths
                int sigIdx = getMethodRawAttributeShort(index, i, 0);
                signature = MethodSignature.parse(ctxt, getUtf8ConstantAsBuffer(sigIdx));
            }
        }
        if (signature == null || signature.getParameterTypes().size() != methodDescriptor.getParameterTypes().size()) {
            signature = MethodSignature.synthesize(ctxt, methodDescriptor);
        }
        // we're making an assumption that annotations and params match the end of the list (due to inner classes);
        // if this doesn't work, we might need to use an alt. strategy e.g. skip ACC_MANDATED params
        int vaCnt = visibleAnn == null ? 0 : visibleAnn.get() & 0xff;
        int vaOffs = realCnt - vaCnt;
        int vtaCnt = visibleTypeAnn == null ? 0 : visibleTypeAnn.get() & 0xff;
        int vtaOffs = realCnt - vtaCnt;
        int iaCnt = invisibleAnn == null ? 0 : invisibleAnn.get() & 0xff;
        int iaOffs = realCnt - iaCnt;
        int itaCnt = invisibleTypeAnn == null ? 0 : invisibleTypeAnn.get() & 0xff;
        int itaOffs = realCnt - itaCnt;
        int mpCnt = methodParams == null ? 0 : methodParams.get() & 0xff;
        int mpOffs = realCnt - mpCnt;
        ParameterElement[] parameters = new ParameterElement[realCnt];
        for (int i = 0; i < realCnt; i ++) {
            ParameterElement.Builder paramBuilder = ParameterElement.builder();
            paramBuilder.setIndex(i);
            paramBuilder.setDescriptor(methodDescriptor.getParameterTypes().get(i));
            paramBuilder.setSignature(signature.getParameterTypes().get(i));
            if (i >= vaOffs && i < vaOffs + vaCnt) {
                int annCnt = visibleAnn.getShort() & 0xffff;
                Annotation[] annotations = new Annotation[annCnt];
                for (int j = 0; j < annCnt; j ++) {
                    annotations[j] = Annotation.parse(this, ctxt, visibleAnn);
                }
                paramBuilder.setVisibleAnnotations(List.of(annotations));
            }
            if (i >= vtaOffs && i < vtaOffs + vtaCnt) {
                paramBuilder.setVisibleTypeAnnotations(TypeAnnotationList.parse(this, ctxt, visibleTypeAnn));
            }
            if (i >= iaOffs && i < iaOffs + iaCnt) {
                int annCnt = invisibleAnn.getShort() & 0xffff;
                Annotation[] annotations = new Annotation[annCnt];
                for (int j = 0; j < annCnt; j ++) {
                    annotations[j] = Annotation.parse(this, ctxt, invisibleAnn);
                }
                paramBuilder.setInvisibleAnnotations(List.of(annotations));
            }
            if (i >= itaOffs && i < itaOffs + itaCnt) {
                paramBuilder.setInvisibleTypeAnnotations(TypeAnnotationList.parse(this, ctxt, invisibleTypeAnn));
            }
            if (i >= mpOffs && i < mpOffs + mpCnt) {
                int nameIdx = methodParams.getShort() & 0xffff;
                if (nameIdx != 0) {
                    paramBuilder.setName(getUtf8Constant(nameIdx));
                }
                paramBuilder.setModifiers(methodParams.getShort() & 0xffff);
            }
            parameters[i] = paramBuilder.build();
        }
        builder.setDescriptor(methodDescriptor);
        builder.setSignature(signature);
        builder.setParameters(List.of(parameters));
    }

    private void addMethodAnnotations(final int index, InvokableElement.Builder builder) {
        int cnt = getMethodAttributeCount(index);
        for (int i = 0; i < cnt; i ++) {
            if (methodAttributeNameEquals(index, i, "RuntimeVisibleAnnotations")) {
                ByteBuffer data = getMethodRawAttributeContent(index, i);
                int ac = data.getShort() & 0xffff;
                Annotation[] annotations = new Annotation[ac];
                for (int j = 0; j < ac; j ++) {
                    annotations[j] = Annotation.parse(this, ctxt, data);
                }
                builder.setVisibleAnnotations(List.of(annotations));
            } else if (methodAttributeNameEquals(index, i, "RuntimeInvisibleAnnotations")) {
                ByteBuffer data = getMethodRawAttributeContent(index, i);
                int ac = data.getShort() & 0xffff;
                Annotation[] annotations = new Annotation[ac];
                for (int j = 0; j < ac; j ++) {
                    annotations[j] = Annotation.parse(this, ctxt, data);
                }
                builder.setInvisibleAnnotations(List.of(annotations));
            } else if (methodAttributeNameEquals(index, i, "RuntimeVisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getFieldRawAttributeContent(index, i));
                builder.setReturnVisibleTypeAnnotations(list);
            } else if (methodAttributeNameEquals(index, i, "RuntimeInvisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getFieldRawAttributeContent(index, i));
                builder.setReturnInvisibleTypeAnnotations(list);
            }
        }
    }

    // concurrency

    private static Literal getVolatile(Literal[] array, int index) {
        return (Literal) literalArrayHandle.getVolatile(array, index);
    }

    private static Descriptor getVolatile(Descriptor[] array, int index) {
        return (Descriptor) descriptorArrayHandle.getVolatile(array, index);
    }

    private static String getVolatile(String[] array, int index) {
        return (String) stringArrayHandle.getVolatile(array, index);
    }

    private static int getVolatile(int[] array, int index) {
        return (int) intArrayHandle.getVolatile(array, index);
    }

    private static int[] getVolatile(int[][] array, int index) {
        return (int[]) intArrayArrayHandle.getVolatile(array, index);
    }

    private static Annotation[] getVolatile(Annotation[][] array, int index) {
        return (Annotation[]) annotationArrayArrayHandle.getVolatile(array, index);
    }

    private static Annotation getVolatile(Annotation[] array, int index) {
        return (Annotation) annotationArrayHandle.getVolatile(array, index);
    }

    private static Literal setIfNull(Literal[] array, int index, Literal newVal) {
        while (! literalArrayHandle.compareAndSet(array, index, null, newVal)) {
            Literal appearing = getVolatile(array, index);
            if (appearing != null) {
                return appearing;
            }
        }
        return newVal;
    }

    private static Descriptor setIfNull(Descriptor[] array, int index, Descriptor newVal) {
        while (! descriptorArrayHandle.compareAndSet(array, index, null, newVal)) {
            Descriptor appearing = getVolatile(array, index);
            if (appearing != null) {
                return appearing;
            }
        }
        return newVal;
    }

    private static String setIfNull(String[] array, int index, String newVal) {
        while (! stringArrayHandle.compareAndSet(array, index, null, newVal)) {
            String appearing = getVolatile(array, index);
            if (appearing != null) {
                return appearing;
            }
        }
        return newVal;
    }

    private static int[] setIfNull(int[][] array, int index, int[] newVal) {
        while (! intArrayArrayHandle.compareAndSet(array, index, null, newVal)) {
            int[] appearing = getVolatile(array, index);
            if (appearing != null) {
                return appearing;
            }
        }
        return newVal;
    }

    private static Annotation[] setIfNull(Annotation[][] array, int index, Annotation[] newVal) {
        while (! annotationArrayArrayHandle.compareAndSet(array, index, null, newVal)) {
            Annotation[] appearing = getVolatile(array, index);
            if (appearing != null) {
                return appearing;
            }
        }
        return newVal;
    }

    private static Annotation setIfNull(Annotation[] array, int index, Annotation newVal) {
        while (! annotationArrayHandle.compareAndSet(array, index, null, newVal)) {
            Annotation appearing = getVolatile(array, index);
            if (appearing != null) {
                return appearing;
            }
        }
        return newVal;
    }
}
