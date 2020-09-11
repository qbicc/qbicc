package cc.quarkus.qcc.type.definition.classfile;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.interpreter.JavaObject;
import cc.quarkus.qcc.interpreter.JavaVM;
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
import cc.quarkus.qcc.type.definition.ClassFileUtil;
import cc.quarkus.qcc.type.definition.DefineFailedException;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import cc.quarkus.qcc.type.definition.element.AnnotatedElement;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ExactExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;

final class ClassFileImpl extends AbstractBufferBacked implements ClassFile,
                                                                  FieldElement.TypeResolver,
                                                                  MethodElement.TypeResolver,
                                                                  ParameterElement.TypeResolver {
    private static final VarHandle intArrayHandle = MethodHandles.arrayElementVarHandle(int[].class);
    private static final VarHandle intArrayArrayHandle = MethodHandles.arrayElementVarHandle(int[][].class);
    private static final VarHandle stringArrayHandle = MethodHandles.arrayElementVarHandle(String[].class);
    private static final VarHandle annotationArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[].class);
    private static final VarHandle annotationArrayArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[][].class);

    private static final int[] NO_INTS = new int[0];
    private static final int[][] NO_INT_ARRAYS = new int[0][];

    private final JavaObject definingClassLoader;
    private final int[] cpOffsets;
    private final String[] strings;
    private final int interfacesOffset;
    private final int[] fieldOffsets;
    private final int[][] fieldAttributeOffsets;
    private final int[] methodOffsets;
    private final int[][] methodAttributeOffsets;
    private final int[] attributeOffsets;

    ClassFileImpl(final JavaObject definingClassLoader, final ByteBuffer buffer) {
        super(buffer);
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
        int cpCount = (scanBuf.getShort() & 0xffff) - 1;
        // one extra slot because the constant pool is one-based, so just leave a hole at the beginning
        int[] cpOffsets = new int[cpCount + 1];
        for (int i = 1; i < cpCount + 1; i ++) {
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
            // skip attributes - except for code (for now)
            int attrCnt = scanBuf.getShort() & 0xffff;
            methodAttributeOffsets[i] = new int[attrCnt];
            for (int j = 0; j < attrCnt; j ++) {
                methodAttributeOffsets[i][j] = scanBuf.position();
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
        this.definingClassLoader = definingClassLoader;
        this.attributeOffsets = attributeOffsets;
        this.cpOffsets = cpOffsets;
        strings = new String[cpOffsets.length];
    }

    public ClassFile getClassFile() {
        return this;
    }

    public int getMajorVersion() {
        return getShort(4);
    }

    public int getMinorVersion() {
        return getShort(6);
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

    private Type resolveSingleDescriptor(int cpIdx) {
        int cpOffset = cpOffsets[cpIdx];
        return resolveSingleDescriptor(cpOffset + 3, getShort(cpOffset + 1));
    }

    private Type resolveSingleDescriptor(final int offs, final int maxLen) {
        if (maxLen < 1) {
            throw new InvalidTypeDescriptorException("Invalid empty type descriptor");
        }
        int b = getByte(offs);
        switch (b) {
            case 'B': return Type.S8;
            case 'C': return Type.U16;
            case 'D': return Type.F64;
            case 'F': return Type.F32;
            case 'I': return Type.S32;
            case 'J': return Type.S64;
            case 'S': return Type.S16;
            case 'V': return Type.VOID;
            case 'Z': return Type.BOOL;
            //
            case '[': return Type.arrayOf(resolveSingleDescriptor(offs + 1, maxLen - 1));
            //
            case 'L': return loadClass(offs + 1, maxLen - 1, true);
            default: throw new InvalidTypeDescriptorException("Invalid type descriptor character '" + (char) b + "'");
        }
    }

    private ClassType loadClass(final int offs, final int maxLen, final boolean expectTerminator) {
        JavaVM vm = JavaVM.requireCurrentThread().getVM();
        return vm.loadClass(definingClassLoader, vm.deduplicate(definingClassLoader, buffer, offs, maxLen, expectTerminator)).getTypeDefinition().verify().getClassType();
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
        builder.setDefiningClassLoader(definingClassLoader);
        int access = getAccess();
        builder.setSuperClassName(getSuperClassName());
        int cnt = getInterfaceNameCount();
        for (int i = 0; i < cnt; i ++) {
            builder.addInterfaceName(getInterfaceName(i));
        }
        cnt = getMethodCount();
        for (int i = 0; i < cnt; i ++) {
            int base = methodOffsets[i];
            int nameIdx = getShort(base + 2);
            if (utf8ConstantEquals(nameIdx, "<clinit>")) {
                builder.setInitializer(this, i);
            } else {
                if (utf8ConstantEquals(nameIdx, "<init>")) {
                    builder.addConstructor(this, i);
                } else {
                    builder.addMethod(this, i);
                }
            }
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

    public FieldElement resolveField(final int index) {
        FieldElement.Builder builder = FieldElement.builder();
        builder.setTypeResolver(this, index);
        builder.setModifiers(getShort(fieldOffsets[index]));
        builder.setName(getUtf8Constant(getShort(fieldOffsets[index] + 2)));
        addAnnotations(builder);
        return builder.build();
    }

    public MethodElement resolveMethod(final int index) {
        MethodElement.Builder builder = MethodElement.builder();
        builder.setReturnTypeResolver(this, index);
        int methodModifiers = getShort(methodOffsets[index]);
        builder.setModifiers(methodModifiers);
        builder.setName(getUtf8Constant(getShort(methodOffsets[index] + 2)));
        boolean mayHaveExact = (methodModifiers & ACC_ABSTRACT) == 0;
        boolean hasVirtual = (methodModifiers & (ACC_STATIC | ACC_PRIVATE)) == 0;
        if (mayHaveExact) {
            addExactBody(builder, index);
        }
        if (hasVirtual) {
            builder.setVirtualMethodBody(new VirtualMethodHandleImpl(this, index));
        }
        addParameters(builder, index);
        addAnnotations(builder);
        return builder.build();
    }

    public ConstructorElement resolveConstructor(final int index) {
        ConstructorElement.Builder builder = ConstructorElement.builder();
        int methodModifiers = getShort(methodOffsets[index]);
        builder.setModifiers(methodModifiers);
        addExactBody(builder, index);
        addParameters(builder, index);
        addAnnotations(builder);
        return builder.build();
    }

    public InitializerElement resolveInitializer(final int index) {
        InitializerElement.Builder builder = InitializerElement.builder();
        builder.setModifiers(ACC_STATIC);
        addExactBody(builder, index);
        return builder.build();
    }

    public Type resolveFieldType(final long argument) throws ResolutionFailedException {
        // todo: encode length too?
        return resolveSingleDescriptor((int) (argument & 0x7fff_ffff), 1024);
    }

    public Type resolveMethodReturnType(final long argument) throws ResolutionFailedException {
        // todo: encode length too?
        return resolveSingleDescriptor((int) (argument & 0x7fff_ffff), 1024);
    }

    public Type resolveParameterType(final long argument) throws ResolutionFailedException {
        // todo: encode length too?
        return resolveSingleDescriptor((int) (argument & 0x7fff_ffff), 1024);
    }

    private void addExactBody(ExactExecutableElement.Builder builder, int index) {
        int attrCount = getMethodAttributeCount(index);
        for (int i = 0; i < attrCount; i ++) {
            if (methodAttributeNameEquals(index, i, "Code")) {
                addExactBody(builder, index, getMethodRawAttributeContent(index, i));
                return;
            }
        }
    }

    private void addExactBody(final ExactExecutableElement.Builder builder, final int index, final ByteBuffer codeAttr) {
        int modifiers = getShort(methodOffsets[index]);
        builder.setExactMethodBody(new ExactMethodHandleImpl(modifiers, index, codeAttr));
    }

    private void addParameters(ParameterizedExecutableElement.Builder builder, int index) {
        int base = methodOffsets[index];
        int descIdx = getShort(base + 4);
        int attrCnt = getMethodAttributeCount(index);
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
        int actualCnt = scanDescriptorParameters(descIdx);
        if (methodParams != null) {
            int cnt = methodParams.get() & 0xff;
            if (cnt != actualCnt) {
                // can't parse; ignore
                methodParams = null;
            }
        }
        if (visibleAnn != null) {
            int cnt = visibleAnn.get() & 0xff;
            if (cnt != actualCnt) {
                // can't parse; ignore
                visibleAnn = null;
            }
        }
        if (invisibleAnn != null) {
            int cnt = invisibleAnn.get() & 0xff;
            if (cnt != actualCnt) {
                // can't parse; ignore
                invisibleAnn = null;
            }
        }

        int descOffs = 1;
        for (int i = 0; i < actualCnt; i ++) {
            ParameterElement.Builder paramBuilder = ParameterElement.builder();
            if (methodParams != null) {
                int nameIdx = methodParams.getShort() & 0xffff;
                if (nameIdx != 0) {
                    paramBuilder.setName(getUtf8Constant(nameIdx));
                }
                paramBuilder.setModifiers(methodParams.getShort() & 0xffff);
            }
            if (visibleAnn != null) {
                int annCnt = visibleAnn.getShort() & 0xffff;
                for (int j = 0; j < annCnt; j ++) {
                    paramBuilder.addVisibleAnnotation(buildAnnotation(visibleAnn));
                }
            }
            if (invisibleAnn != null) {
                int annCnt = invisibleAnn.getShort() & 0xffff;
                for (int j = 0; j < annCnt; j ++) {
                    paramBuilder.addInvisibleAnnotation(buildAnnotation(invisibleAnn));
                }
            }
            paramBuilder.setResolver(this, (long)index << 8 | i);
            addAnnotations(paramBuilder);
            builder.addParameter(paramBuilder.build());
        }
    }

    private int scanSingleParameter(int idx, int offs) {
        switch (utf8ConstantByteAt(idx, offs)) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z': {
                return offs + 1;
            }
            case ')': {
                return 0;
            }
            case '[': {
                return 1 + scanSingleParameter(idx, offs + 1);
            }
            case 'L': {
                for (;;) {
                    offs++;
                    if (utf8ConstantByteAt(idx, offs) == ';') {
                        return offs + 1;
                    }
                }
            }
            default: {
                throw new InvalidTypeDescriptorException("Unexpected character in method descriptor");
            }
        }
    }

    private int scanDescriptorParameters(int idx) {
        if (utf8ConstantByteAt(idx, 0) != '(') {
            throw new InvalidTypeDescriptorException("Expected ( in method descriptor");
        }
        return scanDescriptorParameters(idx, 1);
    }

    private int scanDescriptorParameters(int idx, int offs) {
        int n = scanSingleParameter(idx, offs);
        if (n == 0) {
            return 0;
        } else {
            return 1 + scanDescriptorParameters(idx, n);
        }
    }

    private void addAnnotations(AnnotatedElement.Builder builder) {
        // todo
    }

    // general

    private Annotation buildAnnotation(ByteBuffer buffer) {
        Annotation.Builder builder = Annotation.builder();
        int typeIndex = getShort(buffer.getShort() & 0xffff);
        int avPairCnt = getShort(buffer.getShort() & 0xffff);
        int ch = getRawConstantByte(typeIndex, 4); // first byte of the string
        if (ch != 'L') {
            throw new InvalidTypeDescriptorException("Invalid annotation type descriptor");
        }
        int typeLen = getRawConstantShort(typeIndex, 1);
        ch = getRawConstantByte(typeIndex, 3 + typeLen); // last byte
        if (ch != ';') {
            throw new InvalidTypeDescriptorException("Unterminated annotation type descriptor");
        }
        JavaVM vm = JavaVM.requireCurrentThread().getVM();
        String name = vm.deduplicate(definingClassLoader, this.buffer, cpOffsets[typeIndex] + 3, typeLen, true);
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
