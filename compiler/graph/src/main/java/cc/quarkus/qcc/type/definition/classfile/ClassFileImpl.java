package cc.quarkus.qcc.type.definition.classfile;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import cc.quarkus.qcc.graph.literal.ArrayTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.AnnotationValue;
import cc.quarkus.qcc.type.annotation.BooleanAnnotationValue;
import cc.quarkus.qcc.type.annotation.ByteAnnotationValue;
import cc.quarkus.qcc.type.annotation.CharAnnotationValue;
import cc.quarkus.qcc.type.annotation.DoubleAnnotationValue;
import cc.quarkus.qcc.type.annotation.EnumConstantAnnotationValue;
import cc.quarkus.qcc.type.annotation.FloatAnnotationValue;
import cc.quarkus.qcc.type.annotation.IntAnnotationValue;
import cc.quarkus.qcc.type.annotation.LongAnnotationValue;
import cc.quarkus.qcc.type.annotation.ShortAnnotationValue;
import cc.quarkus.qcc.type.annotation.StringAnnotationValue;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ClassFileUtil;
import cc.quarkus.qcc.type.definition.DefineFailedException;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import cc.quarkus.qcc.type.definition.element.AnnotatedElement;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;
import cc.quarkus.qcc.type.descriptor.ConstructorDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.ParameterizedExecutableDescriptor;

final class ClassFileImpl extends AbstractBufferBacked implements ClassFile,
                                                                  ConstructorElement.TypeResolver,
                                                                  FieldElement.TypeResolver,
                                                                  MethodElement.TypeResolver,
                                                                  ParameterElement.TypeResolver {
    private static final VarHandle intArrayHandle = MethodHandles.arrayElementVarHandle(int[].class);
    private static final VarHandle intArrayArrayHandle = MethodHandles.arrayElementVarHandle(int[][].class);
    private static final VarHandle literalArrayHandle = MethodHandles.arrayElementVarHandle(Literal[].class);
    private static final VarHandle stringArrayHandle = MethodHandles.arrayElementVarHandle(String[].class);
    private static final VarHandle annotationArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[].class);
    private static final VarHandle annotationArrayArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[][].class);

    private static final int[] NO_INTS = new int[0];
    private static final int[][] NO_INT_ARRAYS = new int[0][];

    private final int[] cpOffsets;
    private final String[] strings;
    private final Literal[] literals;
    /**
     * This is the method parameter part of every method descriptor.
     */
    private final ParameterizedExecutableDescriptor[] methodParamInfo;
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
        methodParamInfo = new ParameterizedExecutableDescriptor[cpOffsets.length];
        types = new ValueType[cpOffsets.length];
    }

    public ClassFile getClassFile() {
        return this;
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

    public Literal getConstantValue(int idx) {
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
            ValueType type = resolveSingleDescriptor(nameIdx);
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

    private void populateTypeInfo(final int descIdx) {
        checkConstantType(descIdx, CONSTANT_Utf8);
        if (types[descIdx] != null) {
            // already populated
            return;
        }
        int b = getRawConstantByte(descIdx, 3);
        int len = getUtf8ConstantLength(descIdx);
        int i;
        if (b == '(') {
            // it's a method
            // scan it twice
            int cnt = getTypesArrayLength(descIdx, 1, 0);
            // first get the number of parameters
            if (cnt == 0) {
                methodParamInfo[descIdx] = ParameterizedExecutableDescriptor.of();
                i = 2; // "()"
            } else {
                ValueType[] types = new ValueType[cnt];
                i = populateTypesArray(descIdx, types, 1, 0) + 1;
                methodParamInfo[descIdx] = ParameterizedExecutableDescriptor.of(types);
            }
        } else {
            i = 0;
        }
        // now the remaining string (starting at i) is the type (or return type)
        int descOffs = cpOffsets[descIdx];
        types[descIdx] = resolveSingleDescriptor(descOffs + 3 + i, getShort(descOffs + 1) - i);
    }

    public ConstructorDescriptor resolveConstructorDescriptor(final int argument) throws ResolutionFailedException {
        return getConstructorDescriptor(getShort(methodOffsets[argument] + 4));
    }

    public ConstructorDescriptor getConstructorDescriptor(final int descIdx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        populateTypeInfo(descIdx);
        return ConstructorDescriptor.of(methodParamInfo[descIdx]);
    }

    public MethodDescriptor resolveMethodDescriptor(final int argument) throws ResolutionFailedException {
        // we have the method index but we have to get the descriptor index
        return getMethodDescriptor(getShort(methodOffsets[argument] + 4));
    }

    public MethodDescriptor getMethodDescriptor(final int descIdx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        populateTypeInfo(descIdx);
        return MethodDescriptor.of(methodParamInfo[descIdx], types[descIdx]);
    }

    int getTypesArrayLength(int descIdx, int pos, int idx) {
        int b = getRawConstantByte(descIdx, 3 + pos);
        switch (b) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'V':
            case 'Z': {
                return getTypesArrayLength(descIdx, pos + 1, idx + 1);
            }
            case '[': {
                return getTypesArrayLength(descIdx, pos + 1, idx);
            }
            case 'L': {
                while (getRawConstantByte(descIdx, ++pos + 3) != ';');
                return getTypesArrayLength(descIdx, pos + 1, idx + 1);
            }
            case ')': {
                return idx;
            }
            default: {
                throw new InvalidTypeDescriptorException("Invalid type descriptor character '" + (char) b + "'");
            }
        }
    }

    int populateTypesArray(int descIdx, ValueType[] types, int pos, int idx) {
        if (idx == types.length) {
            return pos;
        }
        int b = getRawConstantByte(descIdx, 3 + pos);
        switch (b) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'V':
            case 'Z': {
                types[idx] = forSimpleDescriptor(b);
                pos ++;
                break;
            }
            case '[': {
                int res = populateTypesArray(descIdx, types, pos + 1, idx);
                ArrayTypeIdLiteral arrayType;
                if (types[idx] instanceof ReferenceType) {
                    arrayType = literalFactory.literalOfArrayType((ReferenceType) types[idx]);
                } else {
                    arrayType = literalFactory.literalOfArrayType(types[idx]);
                }
                types[idx] = typeSystem.getReferenceType(arrayType);
                return res;
            }
            case 'L': {
                int start = ++pos;
                while (getRawConstantByte(descIdx, 3 + pos++) != ';');
                types[idx] = typeSystem.getReferenceType(loadClass(cpOffsets[descIdx] + 3 + start, pos - start - 1, true));
                break;
            }
            default: {
                throw new InvalidTypeDescriptorException("Invalid type descriptor character '" + (char) b + "'");
            }
        }
        return populateTypesArray(descIdx, types, pos, idx + 1);
    }

    ValueType resolveSingleDescriptor(int cpIdx) {
        checkConstantType(cpIdx, CONSTANT_Utf8);
        int cpOffset = cpOffsets[cpIdx];
        return resolveSingleDescriptor(cpOffset + 3, getShort(cpOffset + 1));
    }

    ValueType forSimpleDescriptor(int ch) {
        switch (ch) {
            case 'B': return typeSystem.getSignedInteger8Type();
            case 'C': return typeSystem.getUnsignedInteger16Type();
            case 'D': return typeSystem.getFloat64Type();
            case 'F': return typeSystem.getFloat32Type();
            case 'I': return typeSystem.getSignedInteger32Type();
            case 'J': return typeSystem.getSignedInteger64Type();
            case 'S': return typeSystem.getSignedInteger16Type();
            case 'V': return typeSystem.getVoidType();
            case 'Z': return typeSystem.getBooleanType();
            default: throw new InvalidTypeDescriptorException("Invalid type descriptor character '" + (char) ch + "'");
        }
    }

    ValueType resolveSingleDescriptor(final int offs, final int maxLen) {
        if (maxLen < 1) {
            throw new InvalidTypeDescriptorException("Invalid empty type descriptor");
        }
        int b = getByte(offs);
        switch (b) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'V':
            case 'Z': return forSimpleDescriptor(b);
            //
            case '[': {
                ValueType elementType = resolveSingleDescriptor(offs + 1, maxLen - 1);
                ArrayTypeIdLiteral arrayType;
                if (elementType instanceof ReferenceType) {
                    arrayType = literalFactory.literalOfArrayType((ReferenceType) elementType);
                } else {
                    arrayType = literalFactory.literalOfArrayType(elementType);
                }
                return typeSystem.getReferenceType(arrayType);
            }
            case 'L': {
                for (int i = 0; i < maxLen; i ++) {
                    if (getByte(offs + 1 + i) == ';') {
                        return typeSystem.getReferenceType(loadClass(offs + 1, i, true));
                    }
                }
                // fall thru
            }
            default: throw new InvalidTypeDescriptorException("Invalid type descriptor character '" + (char) b + "'");
        }
    }

    TypeIdLiteral resolveSingleType(int cpIdx) {
        int cpOffset = cpOffsets[cpIdx];
        return resolveSingleType(cpOffset + 3, getShort(cpOffset + 1));
    }

    TypeIdLiteral resolveSingleType(int offs, int maxLen) {
        return loadClass(offs + 1, maxLen - 1, false);
    }

    TypeIdLiteral resolveSingleType(String name) {
        return ctxt.findDefinedType(name).validate().getTypeId();
    }

    public TypeIdLiteral resolveType() {
        int nameIdx = getShort(cpOffsets[thisClassIdx] + 1);
        int offset = cpOffsets[nameIdx];
        return loadClass(offset + 3, getShort(offset + 1), false);
    }

    private TypeIdLiteral loadClass(final int offs, final int len, final boolean expectTerminator) {
        return ctxt.findDefinedType(ctxt.deduplicate(buffer, offs, len)).validate().getTypeId();
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
        builder.setSuperClassName(getSuperClassName());
        int cnt = getInterfaceNameCount();
        for (int i = 0; i < cnt; i ++) {
            builder.addInterfaceName(getInterfaceName(i));
        }
        boolean foundInitializer = false;
        cnt = getMethodCount();
        for (int i = 0; i < cnt; i ++) {
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
        cnt = getFieldCount();
        for (int i = 0; i < cnt; i ++) {
            builder.addField(this, i);
        }
        cnt = getAttributeCount();
        for (int i = 0; i < cnt; i ++) {
            if (attributeNameEquals(i, "RuntimeVisibleAnnotations")) {
                ByteBuffer data = getRawAttributeContent(i);
                int ac = data.getShort() & 0xffff;
                for (int j = 0; j < ac; j ++) {
                    builder.addVisibleAnnotation(buildAnnotation(data));
                }
            } else if (attributeNameEquals(i, "RuntimeInvisibleAnnotations")) {
                ByteBuffer data = getRawAttributeContent(i);
                int ac = data.getShort() & 0xffff;
                for (int j = 0; j < ac; j ++) {
                    builder.addInvisibleAnnotation(buildAnnotation(data));
                }
            } else if (attributeNameEquals(i, "Deprecated")) {
                access |= I_ACC_DEPRECATED;
            } else if (attributeNameEquals(i, "Synthetic")) {
                access |= ACC_SYNTHETIC;
            } else if (attributeNameEquals(i, "Signature")) {
                // todo
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
        builder.setModifiers(access);
    }

    public FieldElement resolveField(final int index, final DefinedTypeDefinition enclosing) {
        FieldElement.Builder builder = FieldElement.builder();
        builder.setEnclosingType(enclosing);
        builder.setTypeResolver(this, index);
        builder.setModifiers(getShort(fieldOffsets[index]));
        builder.setName(getUtf8Constant(getShort(fieldOffsets[index] + 2)));
        addFieldAnnotations(index, builder);
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
            addExactBody(builder, index, enclosing);
        }
        if (hasVirtual) {
            builder.setVirtualMethodBody(new VirtualMethodHandleImpl(this, index));
        }
        addParameters(builder, index, enclosing);
        addMethodAnnotations(index, builder);
        builder.setMethodTypeResolver(this, index);
        return builder.build();
    }

    public ConstructorElement resolveConstructor(final int index, final DefinedTypeDefinition enclosing) {
        ConstructorElement.Builder builder = ConstructorElement.builder();
        builder.setEnclosingType(enclosing);
        int methodModifiers = getShort(methodOffsets[index]);
        builder.setModifiers(methodModifiers);
        addExactBody(builder, index, enclosing);
        addParameters(builder, index, enclosing);
        addMethodAnnotations(index, builder);
        builder.setConstructorTypeResolver(this, index);
        return builder.build();
    }

    public InitializerElement resolveInitializer(final int index, final DefinedTypeDefinition enclosing) {
        InitializerElement.Builder builder = InitializerElement.builder();
        builder.setEnclosingType(enclosing);
        builder.setModifiers(ACC_STATIC);
        if (index == 0) {
            builder.setExactMethodBody(null);
        } else {
            addExactBody(builder, index, enclosing);
        }
        return builder.build();
    }

    public ValueType resolveFieldType(final long argument) throws ResolutionFailedException {
        int fo = fieldOffsets[(int) argument];
        return resolveSingleDescriptor(getShort(fo + 4));
    }

    public ValueType resolveParameterType(final int methodIdx, final int paramIdx) throws ResolutionFailedException {
        return getMethodDescriptor(getShort(methodOffsets[methodIdx] + 4)).getParameterType(paramIdx);
    }

    private void addExactBody(ExecutableElement.Builder builder, int index, final DefinedTypeDefinition enclosing) {
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
        builder.setExactMethodBody(new ExactMethodHandleImpl(this, modifiers, index, codeAttr, enclosing));
    }

    private void addParameters(ParameterizedExecutableElement.Builder builder, int index, final DefinedTypeDefinition enclosing) {
        int base = methodOffsets[index];
        int descIdx = getShort(base + 4);
        int descLen = getShort(cpOffsets[descIdx] + 1);
        int attrCnt = getMethodAttributeCount(index);
        int realCnt = getTypesArrayLength(descIdx, 1, 0);
        ByteBuffer visibleAnn = null;
        ByteBuffer invisibleAnn = null;
        ByteBuffer methodParams = null;
        for (int i = 0; i < attrCnt; i ++) {
            if (methodAttributeNameEquals(index, i, "RuntimeVisibleParameterAnnotations")) {
                visibleAnn = getMethodRawAttributeContent(index, i);
            } else if (methodAttributeNameEquals(index, i, "RuntimeInvisibleParameterAnnotations")) {
                invisibleAnn = getMethodRawAttributeContent(index, i);
            } else if (methodAttributeNameEquals(index, i, "MethodParameters")) {
                methodParams = getMethodRawAttributeContent(index, i);
            }
        }
        // we're making an assumption that annotations and params match the end of the list (due to inner classes);
        // if this doesn't work, we might need to use an alt. strategy e.g. skip ACC_MANDATED params
        int vaCnt = visibleAnn == null ? 0 : visibleAnn.get() & 0xff;
        int vaOffs = realCnt - vaCnt;
        int iaCnt = invisibleAnn == null ? 0 : invisibleAnn.get() & 0xff;
        int iaOffs = realCnt - iaCnt;
        int mpCnt = methodParams == null ? 0 : methodParams.get() & 0xff;
        int mpOffs = realCnt - mpCnt;
        for (int i = 0; i < realCnt; i ++) {
            ParameterElement.Builder paramBuilder = ParameterElement.builder();
            paramBuilder.setEnclosingType(enclosing);
            paramBuilder.setIndex(i);
            paramBuilder.setResolver(this, index, i);
            if (i >= vaOffs && i < vaOffs + vaCnt) {
                int annCnt = visibleAnn.getShort() & 0xffff;
                paramBuilder.expectVisibleAnnotationCount(annCnt);
                for (int j = 0; j < annCnt; j ++) {
                    paramBuilder.addVisibleAnnotation(buildAnnotation(visibleAnn));
                }
            }
            if (i >= iaOffs && i < iaOffs + iaCnt) {
                int annCnt = invisibleAnn.getShort() & 0xffff;
                paramBuilder.expectInvisibleAnnotationCount(annCnt);
                for (int j = 0; j < annCnt; j ++) {
                    paramBuilder.addInvisibleAnnotation(buildAnnotation(invisibleAnn));
                }
            }
            if (i >= mpOffs && i < mpOffs + mpCnt) {
                int nameIdx = methodParams.getShort() & 0xffff;
                if (nameIdx != 0) {
                    paramBuilder.setName(getUtf8Constant(nameIdx));
                }
                paramBuilder.setModifiers(methodParams.getShort() & 0xffff);
            }
            builder.addParameter(paramBuilder.build());
        }
    }

    private void addMethodAnnotations(final int index, AnnotatedElement.Builder builder) {
        int cnt = getMethodAttributeCount(index);
        for (int i = 0; i < cnt; i ++) {
            if (methodAttributeNameEquals(index, i, "RuntimeVisibleAnnotations")) {
                ByteBuffer data = getMethodRawAttributeContent(index, i);
                int ac = data.getShort() & 0xffff;
                for (int j = 0; j < ac; j ++) {
                    builder.addVisibleAnnotation(buildAnnotation(data));
                }
            } else if (methodAttributeNameEquals(index, i, "RuntimeInvisibleAnnotations")) {
                ByteBuffer data = getMethodRawAttributeContent(index, i);
                int ac = data.getShort() & 0xffff;
                for (int j = 0; j < ac; j ++) {
                    builder.addInvisibleAnnotation(buildAnnotation(data));
                }
            }
        }
    }

    private void addFieldAnnotations(final int index, AnnotatedElement.Builder builder) {
        int cnt = getFieldAttributeCount(index);
        for (int i = 0; i < cnt; i ++) {
            if (fieldAttributeNameEquals(index, i, "RuntimeVisibleAnnotations")) {
                ByteBuffer data = getFieldRawAttributeContent(index, i);
                int ac = data.getShort() & 0xffff;
                for (int j = 0; j < ac; j ++) {
                    builder.addVisibleAnnotation(buildAnnotation(data));
                }
            } else if (fieldAttributeNameEquals(index, i, "RuntimeInvisibleAnnotations")) {
                ByteBuffer data = getFieldRawAttributeContent(index, i);
                int ac = data.getShort() & 0xffff;
                for (int j = 0; j < ac; j ++) {
                    builder.addInvisibleAnnotation(buildAnnotation(data));
                }
            }
        }
    }

    // general

    private Annotation buildAnnotation(ByteBuffer buffer) {
        Annotation.Builder builder = Annotation.builder();
        int typeIndex = buffer.getShort() & 0xffff;
        int ch = getRawConstantByte(typeIndex, 3); // first byte of the string
        if (ch != 'L') {
            throw new InvalidTypeDescriptorException("Invalid annotation type descriptor");
        }
        int typeLen = getRawConstantShort(typeIndex, 1);
        ch = getRawConstantByte(typeIndex, 3 + typeLen - 1); // last byte
        if (ch != ';') {
            throw new InvalidTypeDescriptorException("Unterminated annotation type descriptor");
        }
        String name = ctxt.deduplicate(buffer, cpOffsets[typeIndex] + 3, typeLen);
        builder.setClassName(name);
        int cnt = buffer.getShort() & 0xffff;
        for (int i = 0; i < cnt; i ++) {
            builder.addValue(getUtf8Constant(buffer.getShort() & 0xffff), buildAnnotationValue(buffer));
        }
        return builder.build();
    }

    private AnnotationValue buildAnnotationValue(ByteBuffer buffer) {
        // tag
        switch (buffer.get() & 0xff) {
            case 'B': {
                return ByteAnnotationValue.of(getIntConstant(buffer.getShort() & 0xffff));
            }
            case 'C': {
                return CharAnnotationValue.of(getIntConstant(buffer.getShort() & 0xffff));
            }
            case 'D': {
                return DoubleAnnotationValue.of(getDoubleConstant(buffer.getShort() & 0xffff));
            }
            case 'F': {
                return FloatAnnotationValue.of(getFloatConstant(buffer.getShort() & 0xffff));
            }
            case 'I': {
                return IntAnnotationValue.of(getIntConstant(buffer.getShort() & 0xffff));
            }
            case 'J': {
                return LongAnnotationValue.of(getLongConstant(buffer.getShort() & 0xffff));
            }
            case 'S': {
                return ShortAnnotationValue.of(getIntConstant(buffer.getShort() & 0xffff));
            }
            case 'Z': {
                return BooleanAnnotationValue.of(getIntConstant(buffer.getShort() & 0xffff) != 0);
            }
            case 's': {
                return StringAnnotationValue.of(getUtf8Constant(buffer.getShort() & 0xffff));
            }
            case 'e': {
                return EnumConstantAnnotationValue.of(getUtf8Constant(buffer.getShort() & 0xffff), getUtf8Constant(buffer.getShort() & 0xffff));
            }
            case '@': {
                return buildAnnotation(buffer);
            }
            case '[': {
                int count = buffer.getShort() & 0xffff;
                AnnotationValue[] array = new AnnotationValue[count];
                for (int j = 0; j < count; j ++) {
                    array[j] = buildAnnotationValue(buffer);
                }
                return AnnotationValue.array(array);
            }
            default: {
                throw new InvalidAnnotationValueException("Invalid annotation value tag");
            }
        }
    }

    private Annotation buildAnnotation(int offset, int length) {
        return buildAnnotation(slice(offset, length));
    }

    // concurrency

    private static Literal getVolatile(Literal[] array, int index) {
        return (Literal) literalArrayHandle.getVolatile(array, index);
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
