package org.qbicc.type.definition.classfile;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.qbicc.context.ClassContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.AnnotationValue;
import org.qbicc.type.annotation.EnumConstantAnnotationValue;
import org.qbicc.type.annotation.IntAnnotationValue;
import org.qbicc.type.annotation.type.TargetInfo;
import org.qbicc.type.annotation.type.TypeAnnotation;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.annotation.type.TypePathKind;
import org.qbicc.type.definition.ByteBufferInputStream;
import org.qbicc.type.definition.ClassFileUtil;
import org.qbicc.type.definition.DefineFailedException;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.EnclosedClassResolver;
import org.qbicc.type.definition.EnclosingClassResolver;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.MethodBodyFactory;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
import org.qbicc.type.definition.element.ParameterElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.generic.ArrayTypeSignature;
import org.qbicc.type.generic.NestedClassTypeSignature;
import org.qbicc.type.methodhandle.ConstructorMethodHandleConstant;
import org.qbicc.type.descriptor.Descriptor;
import org.qbicc.type.methodhandle.FieldMethodHandleConstant;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.methodhandle.MethodHandleConstant;
import org.qbicc.type.methodhandle.MethodHandleKind;
import org.qbicc.type.methodhandle.MethodMethodHandleConstant;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.ClassSignature;
import org.qbicc.type.generic.ClassTypeSignature;
import org.qbicc.type.generic.MethodSignature;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

final class ClassFileImpl extends AbstractBufferBacked implements ClassFile, EnclosingClassResolver, EnclosedClassResolver, MethodBodyFactory {
    private static final int[] NO_INTS = new int[0];
    private static final ValueType[] NO_TYPES = new ValueType[0];
    private static final ValueType[][] NO_TYPE_ARRAYS = new ValueType[0][];

    private static final VarHandle intArrayHandle = MethodHandles.arrayElementVarHandle(int[].class);
    private static final VarHandle intArrayArrayHandle = MethodHandles.arrayElementVarHandle(int[][].class);
    private static final VarHandle literalArrayHandle = MethodHandles.arrayElementVarHandle(Literal[].class);
    private static final VarHandle stringArrayHandle = MethodHandles.arrayElementVarHandle(String[].class);
    private static final VarHandle annotationArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[].class);
    private static final VarHandle annotationArrayArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[][].class);
    private static final VarHandle descriptorArrayHandle = MethodHandles.arrayElementVarHandle(Descriptor[].class);
    private static final BlockParameter[] NO_PARAMETERS = new BlockParameter[0];

    private final int[] cpOffsets;
    private final String[] strings;
    private final Literal[] literals;
    private final Descriptor[] descriptors;
    /**
     * This is the type of every field descriptor or the return type of every method descriptor.
     */
    private final int interfacesOffset;
    private final int[] fieldOffsets;
    private final int[][] fieldAttributeOffsets;
    private final int[] methodOffsets;
    private final int[][] methodAttributeOffsets;
    private final int[] attributeOffsets;
    private final int[] bootstrapMethodOffsets;
    private final LiteralFactory literalFactory;
    private final ClassContext ctxt;
    private final String sourceFile;

    private static class ClassUpgrader extends ClassVisitor {

        protected ClassUpgrader(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            Assert.assertTrue(version < 50);
            cv.visit(50, access, name, signature, superName, interfaces);
        }
    }

    public static ClassFile make(final ClassContext ctxt, ByteBuffer buffer) {
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
        if (major < 45 || major == 45 && minor < 3 || major > 61 || major == 61 && minor > 0) {
            throw new DefineFailedException("Unsupported class version " + major + "." + minor);
        }
        if (major < 50) {
            try {
                // Rewrite old classfile versions to ensure that we have the stack map tables we need for phi nodes
                ClassReader cr = new ClassReader(new ByteBufferInputStream(buffer.duplicate()));
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                cr.accept(new ClassUpgrader(Opcodes.ASM9, cw), ClassReader.SKIP_FRAMES);
                byte[] result = cw.toByteArray();
                buffer = ByteBuffer.wrap(result);
            } catch (IOException e) {
                ctxt.getCompilationContext().warning("Failed to rewrite input class file; may be missing stack maps");
            }
        }
        return new ClassFileImpl(ctxt, buffer);
    }

    private ClassFileImpl(final ClassContext ctxt, final ByteBuffer buffer) {
        super(buffer);
        this.ctxt = ctxt;
        literalFactory = ctxt.getLiteralFactory();
        // scan the file to build up offset tables
        ByteBuffer scanBuf = buffer.duplicate();
        // we checked this above, but verify in case we used ASM to rewrite things.
        int magic = scanBuf.getInt();
        if (magic != 0xcafebabe) {
            throw new DefineFailedException("Bad magic number");
        }
        int minor = scanBuf.getShort() & 0xffff;
        int major = scanBuf.getShort() & 0xffff;
        if (major < 50 || major > 61 || major == 61 && minor > 0) {
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
        strings = new String[cpOffsets.length];
        literals = new Literal[cpOffsets.length];
        descriptors = new Descriptor[cpOffsets.length];
        // read globally-relevant attributes
        String sourceFile = null;
        int cnt = getAttributeCount();
        int[] bootstrapMethodOffsets = NO_INTS;
        for (int i = 0; i < cnt; i ++) {
            if (attributeNameEquals(i, "SourceFile")) {
                sourceFile = getUtf8Constant(getRawAttributeShort(i, 0));
            } else if (attributeNameEquals(i, "BootstrapMethods")) {
                int pos = attributeOffsets[i] + 8;
                int bmCnt = getShort(pos - 2);
                bootstrapMethodOffsets = new int[bmCnt];
                for (int j = 0; j < bmCnt; j ++) {
                    bootstrapMethodOffsets[j] = pos;
                    pos += (getShort(pos + 2) << 1) + 4;
                }
            }
        }
        this.bootstrapMethodOffsets = bootstrapMethodOffsets;
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

    public int getBootstrapMethodHandleRef(final int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        return getShort(bootstrapMethodOffsets[idx]);
    }

    public int getBootstrapMethodArgumentCount(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        return getShort(bootstrapMethodOffsets[idx] + 2);
    }

    public int getBootstrapMethodArgumentConstantIndex(int idx, int argIdx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        if (argIdx < 0 || argIdx >= getBootstrapMethodArgumentCount(idx)) {
            throw new IndexOutOfBoundsException(argIdx);
        }
        return getShort(bootstrapMethodOffsets[idx] + 4 + (argIdx << 1));
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

    public Literal getConstantValue(int idx, TypeParameterContext paramCtxt) {
        if (idx == 0) {
            return null;
        }
        Literal lit = getVolatile(literals, idx);
        if (lit != null) {
            return lit;
        }
        int constantType = getConstantType(idx);
        switch (constantType) {
            case CONSTANT_Class: return setIfNull(literals, idx, literalFactory.literalOfType(getTypeConstant(idx, paramCtxt)));
            case CONSTANT_String:
                return setIfNull(literals, idx, literalFactory.literalOf(getStringConstant(idx), ctxt.findDefinedType("java/lang/String").load().getObjectType().getReference()));
            case CONSTANT_Integer:
                return setIfNull(literals, idx, literalFactory.literalOf(getIntConstant(idx)));
            case CONSTANT_Float:
                return setIfNull(literals, idx, literalFactory.literalOf(getFloatConstant(idx)));
            case CONSTANT_Long:
                return setIfNull(literals, idx, literalFactory.literalOf(getLongConstant(idx)));
            case CONSTANT_Double:
                return setIfNull(literals, idx, literalFactory.literalOf(getDoubleConstant(idx)));
            case CONSTANT_MethodHandle:
                return setIfNull(literals, idx, getMethodHandleLiteral(idx));
            case CONSTANT_MethodType:
                return setIfNull(literals, idx, getMethodTypeConstant(idx));
            default: {
                throw new IllegalArgumentException("Unexpected constant of type " + constantType + " at index " + idx);
            }
        }
    }

    /**
     * Get a {@code ValueType} from a constant pool entry of type {@code CONSTANT_Class}.  The type might be translated
     * by way of the type resolver.
     *
     * @param idx the constant pool index (must not be 0)
     * @param paramCtxt the type parameter context (must not be {@code null})
     * @return the type
     */
    ValueType getTypeConstant(int idx, TypeParameterContext paramCtxt) {
        int nameIdx = getClassConstantNameIdx(idx);
        String name = getUtf8Constant(nameIdx);
        assert name != null;
        if (name.startsWith("[")) {
            TypeDescriptor desc = (TypeDescriptor) getDescriptorConstant(nameIdx);
            // todo: acquire the correct signature and type annotation info from the bytecode index and method info
            return ctxt.resolveTypeFromDescriptor(desc, paramCtxt, TypeSignature.synthesize(ctxt, desc));
        } else {
            int slash = name.lastIndexOf('/');
            String packageName = slash == -1 ? "" : ctxt.deduplicate(name.substring(0, slash));
            String className = slash == -1 ? name : ctxt.deduplicate(name.substring(slash + 1));
            if (paramCtxt instanceof DefinedTypeDefinition dtd && dtd.internalPackageAndNameEquals(packageName, className)) {
                return dtd.load().getObjectType();
            }
            return ctxt.resolveTypeFromClassName(packageName, className);
        }
    }

    MethodHandleLiteral getMethodHandleLiteral(int idx) {
        MethodHandleConstant methodHandleConstant = getMethodHandleConstant(idx);
        ReferenceType type = ctxt.findDefinedType("java/lang/invoke/MethodHandle").load().getClassType().getReference();
        return literalFactory.literalOfMethodHandle(methodHandleConstant, type);
    }

    ObjectLiteral getMethodTypeConstant(int idx) {
        int descIdx = getRawConstantShort(idx, 1);
        MethodDescriptor methodDescriptor = (MethodDescriptor) getDescriptorConstant(descIdx);
        VmThread thread = Vm.requireCurrentThread();
        Vm vm = thread.getVM();
        VmObject obj = vm.createMethodType(ctxt, methodDescriptor);
        return ctxt.getLiteralFactory().literalOf(obj);
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

    public TypeDescriptor getClassConstantAsDescriptor(final int idx) {
        checkConstantType(idx, CONSTANT_Class);
        if (idx == 0) {
            return null;
        }
        int strIdx = getRawConstantShort(idx, 1);
        TypeDescriptor descriptor = (TypeDescriptor) getVolatile(descriptors, strIdx);
        if (descriptor != null) {
            return descriptor;
        } else {
            return (TypeDescriptor) setIfNull(descriptors, strIdx, TypeDescriptor.parseClassConstant(ctxt, getUtf8ConstantAsBuffer(strIdx)));
        }
    }

    public MethodHandleConstant getMethodHandleConstant(final int idx) {
        checkConstantType(idx, CONSTANT_MethodHandle);
        if (idx == 0) {
            return null;
        }
        int kindVal = getRawConstantByte(idx, 1);
        MethodHandleKind kind = MethodHandleKind.forId(kindVal);
        int refIdx = getRawConstantShort(idx, 2);
        if (kind.isFieldTarget()) {
            int ownerIdx = getFieldrefConstantClassIndex(refIdx);
            int frNameAndType = getFieldrefNameAndTypeIndex(refIdx);
            TypeDescriptor desc = (TypeDescriptor) getDescriptorConstant(getNameAndTypeConstantDescriptorIdx(frNameAndType));
            String fieldName = getNameAndTypeConstantName(frNameAndType);
            return new FieldMethodHandleConstant((ClassTypeDescriptor) getClassConstantAsDescriptor(ownerIdx), fieldName, kind, desc);
        } else {
            int mrNameAndType = getMethodrefNameAndTypeIndex(refIdx);
            MethodDescriptor desc = (MethodDescriptor) getDescriptorConstant(getNameAndTypeConstantDescriptorIdx(mrNameAndType));
            if (desc == null) {
                throw new IllegalStateException("No method descriptor for method ref at index " + refIdx);
            }
            ClassTypeDescriptor ownerDesc = (ClassTypeDescriptor) getClassConstantAsDescriptor(getMethodrefConstantClassIndex(refIdx));
            if (kind == MethodHandleKind.NEW_INVOKE_SPECIAL) {
                return new ConstructorMethodHandleConstant(ownerDesc, kind, desc);
            } else {
                String methodName = getMethodrefConstantName(refIdx);
                return new MethodMethodHandleConstant(ownerDesc, methodName, kind, desc);
            }
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

    public int getMethodModifiers(int idx) {
        return getShort(methodOffsets[idx]);
    }

    public String getMethodName(int idx) {
        return getUtf8Constant(getShort(methodOffsets[idx] + 2));
    }

    public MethodDescriptor getMethodDescriptor(int idx) {
        return (MethodDescriptor) getDescriptorConstant(getShort(methodOffsets[idx] + 4));
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
        String internalName = getName();
        builder.setName(internalName);
        builder.setContext(ctxt);
        int access = getAccess();
        String superClassName = getSuperClassName();
        if ((access & ACC_INTERFACE) == 0) {
            builder.setSuperClassName(getSuperClassName());
        }
        int cnt = getInterfaceNameCount();
        for (int i = 0; i < cnt; i ++) {
            builder.addInterfaceName(getInterfaceName(i));
        }
        // make sure that annotations are added first for convenience
        int acnt = getAttributeCount();
        ClassTypeDescriptor descriptor = (ClassTypeDescriptor) getClassConstantAsDescriptor(getShort(interfacesOffset - 6));
        builder.setDescriptor(descriptor);
        ClassSignature signature = null;
        boolean foundRuntimeInvisibleAnnotations = false;
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
                foundRuntimeInvisibleAnnotations = true;
                builder.setInvisibleAnnotations(List.of(annotations));
            } else if (attributeNameEquals(i, "RuntimeVisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getRawAttributeContent(i));
                builder.setVisibleTypeAnnotations(list);
            } else if (attributeNameEquals(i, "RuntimeInvisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getRawAttributeContent(i));
                builder.setInvisibleTypeAnnotations(list);
            } else if (attributeNameEquals(i, "Synthetic")) {
                access |= ACC_SYNTHETIC;
            } else if (attributeNameEquals(i, "Signature")) {
                int sigIdx = getRawAttributeShort(i, 0);
                signature = ClassSignature.parse(ctxt, getUtf8ConstantAsBuffer(sigIdx));
            } else if (attributeNameEquals(i, "SourceFile")) {
                // todo
            } else if (attributeNameEquals(i, "NestHost")) {
                ByteBuffer data = getRawAttributeContent(i);
                int ci = data.getShort() & 0xffff;
                builder.setNestHost(getClassConstantName(ci));
            } else if (attributeNameEquals(i, "NestMembers")) {
                ByteBuffer data = getRawAttributeContent(i);
                int cc = data.getShort() & 0xffff;
                for (int j = 0; j < cc; j ++) {
                    builder.addNestMember(getClassConstantName(data.getShort() & 0xffff));
                }
            } else if (attributeNameEquals(i, "InnerClasses")) {
                ByteBuffer data = getRawAttributeContent(i);
                int innerCnt = data.getShort() & 0xffff;
                for (int j = 0; j < innerCnt; j ++) {
                    int innerClassInfoIdx = data.getShort() & 0xffff; // CONSTANT_Class
                    int outerClassInfoIdx = data.getShort() & 0xffff; // CONSTANT_Class
                    int innerNameIdx = data.getShort() & 0xffff; // CONSTANT_Utf8
                    int innerFlags = data.getShort() & 0xffff; // value
                    if (classConstantNameEquals(innerClassInfoIdx, internalName)) {
                        // this is *our* information!
                        if (innerNameIdx != 0) {
                            builder.setSimpleName(getUtf8Constant(innerNameIdx));
                        }
                        access |= (innerFlags & (ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC));
                        if (outerClassInfoIdx != 0) {
                            builder.setEnclosingClass(getClassConstantName(outerClassInfoIdx), this, attributeOffsets[i] + 8 + j * 8);
                        }
                    } else {
                        // it might be an inner class of ours...
                        if (outerClassInfoIdx != 0 && classConstantNameEquals(outerClassInfoIdx, internalName)) {
                            builder.addEnclosedClass(this, attributeOffsets[i] + 8 + j * 8);
                        }
                    }
                }
            } else if (attributeNameEquals(i, "EnclosingMethod")) {
                int classIdx = getRawAttributeShort(i, 0);
                int methodNatIdx = getRawAttributeShort(i, 2);
                String classConstantName = getClassConstantName(classIdx);
                if (methodNatIdx == 0) {
                    builder.setEnclosingMethod(classConstantName, null, null);
                } else {
                    String methodName = getNameAndTypeConstantName(methodNatIdx);
                    int mdi = getNameAndTypeConstantDescriptorIdx(methodNatIdx);
                    MethodDescriptor methodDesc = (MethodDescriptor) getDescriptorConstant(mdi);
                    builder.setEnclosingMethod(classConstantName, methodName, methodDesc);
                }
            }
        }
        if (!foundRuntimeInvisibleAnnotations) {
            builder.setInvisibleAnnotations(List.of()); // allows PatchedTypeBuilder to intercept and implement the @Annotate annotation on types
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
                MethodDescriptor methodDescriptor = (MethodDescriptor) getDescriptorConstant(getShort(base + 4));
                if (utf8ConstantEquals(nameIdx, "<init>")) {
                    builder.addConstructor(this, i, methodDescriptor);
                } else {
                    String name = getUtf8Constant(nameIdx);
                    builder.addMethod(this, i, name, methodDescriptor);
                }
            }
        }
        if (! foundInitializer) {
            // synthesize an empty one
            builder.setInitializer(this, -1);
        }
        acnt = getFieldCount();
        for (int i = 0; i < acnt; i ++) {
            String name = getUtf8Constant(getShort(fieldOffsets[i] + 2));
            TypeDescriptor typeDescriptor = (TypeDescriptor) getDescriptorConstant(getShort(fieldOffsets[i] + 4));
            builder.addField(this, i, name, typeDescriptor);
        }
        builder.setModifiers(access);
    }

    public FieldElement resolveField(final int index, final DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
        builder.setEnclosingType(enclosing);
        TypeDescriptor typeDescriptor = (TypeDescriptor) getDescriptorConstant(getShort(fieldOffsets[index] + 4));
        int modifiers = getFieldModifiers(index);
        builder.setModifiers(modifiers);
        // process attributes
        TypeSignature signature = null;
        TypeAnnotationList visibleList = null;
        TypeAnnotationList invisibleList = null;
        int cnt = getFieldAttributeCount(index);
        for (int i = 0; i < cnt; i ++) {
            if (fieldAttributeNameEquals(index, i, "RuntimeVisibleAnnotations")) {
                ByteBuffer data = getFieldRawAttributeContent(index, i);
                builder.addVisibleAnnotations(Annotation.parseList(this, ctxt, data));
            } else if (fieldAttributeNameEquals(index, i, "RuntimeInvisibleAnnotations")) {
                ByteBuffer data = getFieldRawAttributeContent(index, i);
                builder.addInvisibleAnnotations(Annotation.parseList(this, ctxt, data));
            } else if (fieldAttributeNameEquals(index, i, "Signature")) {
                int sigIdx = getFieldRawAttributeShort(index, i, 0);
                signature = TypeSignature.parse(ctxt, getUtf8ConstantAsBuffer(sigIdx));
            } else if (fieldAttributeNameEquals(index, i, "RuntimeVisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getFieldRawAttributeContent(index, i));
                builder.setVisibleTypeAnnotations(list);
                visibleList = list;
            } else if (fieldAttributeNameEquals(index, i, "RuntimeInvisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getFieldRawAttributeContent(index, i));
                builder.setInvisibleTypeAnnotations(list);
                invisibleList = list;
            } else if (fieldAttributeNameEquals(index, i, "ConstantValue")) {
                if ((modifiers & ACC_STATIC) != 0) {
                    builder.setInitialValue(getConstantValue(getFieldRawAttributeShort(index, i, 0), enclosing));
                }
            }
        }
        if (signature == null) {
            signature = TypeSignature.synthesize(ctxt, typeDescriptor);
        }
        signature = applyToFieldSignature(applyToFieldSignature(signature, visibleList), invisibleList);
        builder.setSignature(signature);
        builder.setSourceFileName(sourceFile);
        return builder.build();
    }

    private TypeSignature applyToFieldSignature(TypeSignature sig, TypeAnnotationList list) {
        if (list == null) {
            return sig;
        }
        for (TypeAnnotation typeAnnotation : list) {
            TargetInfo targetType = typeAnnotation.getTargetType();
            if (targetType instanceof TargetInfo.Empty) {
                // OK
                sig = applyNested(typeAnnotation, 0, sig);
            }
        }
        // fully transformed
        return sig;
    }

    private TypeSignature applyNested(TypeAnnotation ta, int idx, TypeSignature input) {
        if (idx == ta.getPathLength()) {
            // it applies here
            Annotation annotation = ta.getAnnotation();
            return input.withAnnotation(annotation);
        }
        TypePathKind kind = ta.getPathKind(idx);
        switch (kind) {
            case ARRAY -> {
                if (input instanceof ArrayTypeSignature ats) {
                    TypeSignature nested = applyNested(ta, idx + 1, ats.getElementTypeSignature());
                    return ats.withElementType(nested);
                }
            }
            case NESTED -> {
                if (input instanceof NestedClassTypeSignature nts) {
                    TypeSignature nested = applyNested(ta, idx + 1, nts.getEnclosing());
                    if (nested instanceof ClassTypeSignature cts) {
                        return nts.withEnclosing(cts);
                    }
                }
            }
        }
        // unknown
        return input;
    }

    public String getFieldName(final int index) {
        return getUtf8Constant(getShort(fieldOffsets[index] + 2));
    }

    public TypeDescriptor getFieldDescriptor(int idx) {
        return (TypeDescriptor) getDescriptorConstant(getShort(fieldOffsets[idx] + 4));
    }

    public int getFieldModifiers(final int fieldIndex) {
        return getShort(fieldOffsets[fieldIndex]);
    }

    public NestedClassElement resolveEnclosedNestedClass(final int index, final DefinedTypeDefinition enclosing, NestedClassElement.Builder builder) {
        int innerClassInfoIdx = getShort(index); // CONSTANT_Class == to inner class name
        int outerClassInfoIdx = getShort(index + 2); // CONSTANT_Class == to our name
        int innerNameIdx = getShort(index + 4); // CONSTANT_Utf8 == simple name
        int innerFlags = getShort(index + 6); // value == modifiers
        DefinedTypeDefinition enclosed = ctxt.findDefinedType(getClassConstantName(innerClassInfoIdx));
        if (enclosed != null) {
            builder.setEnclosingType(enclosing);
            builder.setCorrespondingType(enclosed);
            if (innerNameIdx != 0) {
                builder.setName(getUtf8Constant(innerNameIdx));
            }
            builder.setModifiers(innerFlags);
            builder.setSourceFileName(sourceFile);
            return builder.build();
        }
        return null;
    }

    public NestedClassElement resolveEnclosingNestedClass(final int index, final DefinedTypeDefinition enclosed, NestedClassElement.Builder builder) {
        int innerClassInfoIdx = getShort(index); // CONSTANT_Class == to our name
        int outerClassInfoIdx = getShort(index + 2); // CONSTANT_Class == to enclosing class name
        int innerNameIdx = getShort(index + 4); // CONSTANT_Utf8 == simple name
        int innerFlags = getShort(index + 6); // value == modifiers
        DefinedTypeDefinition outer = ctxt.findDefinedType(getClassConstantName(outerClassInfoIdx));
        if (outer == null) {
            return null;
        }
        builder.setEnclosingType(outer);
        builder.setCorrespondingType(enclosed);
        if (innerNameIdx != 0) {
            builder.setName(getUtf8Constant(innerNameIdx));
        }
        builder.setModifiers(innerFlags);
        builder.setSourceFileName(sourceFile);
        return builder.build();
    }

    public MethodElement resolveMethod(final int index, final DefinedTypeDefinition enclosing, MethodElement.Builder builder) {
        builder.setEnclosingType(enclosing);
        int methodModifiers = getShort(methodOffsets[index]);
        builder.setModifiers(methodModifiers);
        builder.setSourceFileName(sourceFile);
        boolean isNative = (methodModifiers & ACC_NATIVE) != 0;
        boolean mayHaveBody = (methodModifiers & ACC_ABSTRACT) == 0 && ! isNative;
        if (mayHaveBody) {
            int attrCount = getMethodAttributeCount(index);
            for (int i = 0; i < attrCount; i ++) {
                if (methodAttributeNameEquals(index, i, "Code")) {
                    builder.setMethodBodyFactory(this, index);

                    LineNumberTable lnt = LineNumberTable.createForCodeAttribute(this, getMethodRawAttributeContent(index, i));
                    builder.setMinimumLineNumber(lnt.getMinimumLineNumber());
                    builder.setMaximumLineNumber(lnt.getMaximumLineNumber());

                    break;
                }
            }
        } else if (isNative) {
            ctxt.getCompilationContext().getNativeMethodConfigurator().configureNativeMethod(builder, enclosing, builder.getName(), builder.getDescriptor());
        }
        addParameters(builder, index, enclosing);
        addMethodAnnotations(index, builder);
        return builder.build();
    }

    public ConstructorElement resolveConstructor(final int index, final DefinedTypeDefinition enclosing, ConstructorElement.Builder builder) {
        builder.setEnclosingType(enclosing);
        int methodModifiers = getShort(methodOffsets[index]);
        builder.setModifiers(methodModifiers);
        int attrCount = getMethodAttributeCount(index);
        for (int i = 0; i < attrCount; i ++) {
            if (methodAttributeNameEquals(index, i, "Code")) {
                builder.setMethodBodyFactory(this, index);

                LineNumberTable lnt = LineNumberTable.createForCodeAttribute(this, getMethodRawAttributeContent(index, i));
                builder.setMinimumLineNumber(lnt.getMinimumLineNumber());
                builder.setMaximumLineNumber(lnt.getMaximumLineNumber());

                break;
            }
        }
        addParameters(builder, index, enclosing);
        addMethodAnnotations(index, builder);
        builder.setSourceFileName(sourceFile);
        return builder.build();
    }

    public InitializerElement resolveInitializer(final int index, final DefinedTypeDefinition enclosing, InitializerElement.Builder builder) {
        builder.setEnclosingType(enclosing);
        builder.setModifiers(ACC_STATIC);
        if (index != -1) {
            int attrCount = getMethodAttributeCount(index);
            for (int i = 0; i < attrCount; i ++) {
                if (methodAttributeNameEquals(index, i, "Code")) {
                    builder.setMethodBodyFactory(this, index);

                    LineNumberTable lnt = LineNumberTable.createForCodeAttribute(this, getMethodRawAttributeContent(index, i));
                    builder.setMinimumLineNumber(lnt.getMinimumLineNumber());
                    builder.setMaximumLineNumber(lnt.getMaximumLineNumber());

                    break;
                }
            }
        }
        builder.setSourceFileName(sourceFile);
        return builder.build();
    }

    private void addParameters(InvokableElement.Builder builder, int index, final DefinedTypeDefinition enclosing) {
        int base = methodOffsets[index];
        int modifiers = getShort(base);
        MethodDescriptor methodDescriptor = builder.getDescriptor();
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
        TypeParameterContext tpc = TypeParameterContext.create(enclosing, signature);
        for (int i = 0; i < realCnt; i ++) {
            String name = null;
            int paramMods = 0;
            if (i >= mpOffs && i < mpOffs + mpCnt) {
                int nameIdx = methodParams.getShort() & 0xffff;
                if (nameIdx != 0) {
                    name = getUtf8Constant(nameIdx);
                }
                paramMods = methodParams.getShort() & 0xffff;
            }
            ParameterElement.Builder paramBuilder = ParameterElement.builder(name, methodDescriptor.getParameterTypes().get(i), i);
            paramBuilder.setEnclosingType(enclosing);
            paramBuilder.setModifiers(paramMods);
            paramBuilder.setTypeParameterContext(tpc);
            paramBuilder.setSignature(signature.getParameterTypes().get(i));
            if (i >= vaOffs && i < vaOffs + vaCnt) {
                int annCnt = visibleAnn.getShort() & 0xffff;
                Annotation[] annotations = new Annotation[annCnt];
                for (int j = 0; j < annCnt; j ++) {
                    annotations[j] = Annotation.parse(this, ctxt, visibleAnn);
                }
                paramBuilder.addVisibleAnnotations(List.of(annotations));
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
                paramBuilder.addInvisibleAnnotations(List.of(annotations));
            }
            if (i >= itaOffs && i < itaOffs + itaCnt) {
                paramBuilder.setInvisibleTypeAnnotations(TypeAnnotationList.parse(this, ctxt, invisibleTypeAnn));
            }
            parameters[i] = paramBuilder.build();
        }
        builder.setSignature(signature);
        builder.setParameters(List.of(parameters));
        if (parameters.length == 1 && (modifiers & (ACC_VARARGS | ACC_NATIVE)) == (ACC_VARARGS | ACC_NATIVE)) {
            TypeDescriptor d0 = parameters[0].getTypeDescriptor();
            if (d0 instanceof ArrayTypeDescriptor) {
                TypeDescriptor ed0 = ((ArrayTypeDescriptor) d0).getElementTypeDescriptor();
                if (ed0 instanceof ClassTypeDescriptor) {
                    ClassTypeDescriptor cd = (ClassTypeDescriptor) ed0;
                    if (cd.getClassName().equals("Object") && cd.getPackageName().equals("java/lang")) {
                        if (enclosing.internalPackageAndNameEquals("java/lang/invoke", "MethodHandle") || enclosing.internalPackageAndNameEquals("java/lang/invoke", "VarHandle")) {
                            builder.addModifiers(I_ACC_SIGNATURE_POLYMORPHIC);
                        }
                    }
                }
            }
        }
    }

    private void addMethodAnnotations(final int index, InvokableElement.Builder builder) {
        int cnt = getMethodAttributeCount(index);
        for (int i = 0; i < cnt; i ++) {
            if (methodAttributeNameEquals(index, i, "RuntimeVisibleAnnotations")) {
                ByteBuffer data = getMethodRawAttributeContent(index, i);
                List<Annotation> annotations = Annotation.parseList(this, ctxt, data);
                builder.addVisibleAnnotations(annotations);
            } else if (methodAttributeNameEquals(index, i, "RuntimeInvisibleAnnotations")) {
                ByteBuffer data = getMethodRawAttributeContent(index, i);
                List<Annotation> annotations = Annotation.parseList(this, ctxt, data);
                builder.addInvisibleAnnotations(annotations);
            } else if (methodAttributeNameEquals(index, i, "RuntimeVisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getMethodRawAttributeContent(index, i));
                builder.setVisibleTypeAnnotations(list);
            } else if (methodAttributeNameEquals(index, i, "RuntimeInvisibleTypeAnnotations")) {
                TypeAnnotationList list = TypeAnnotationList.parse(this, ctxt, getMethodRawAttributeContent(index, i));
                builder.setInvisibleTypeAnnotations(list);
            } else if (methodAttributeNameEquals(index, i, "AnnotationDefault")) {
                ByteBuffer data = getMethodRawAttributeContent(index, i);
                AnnotationValue dv = AnnotationValue.parse(this, ctxt, data);
                ((MethodElement.Builder)builder).setDefaultValue(dv);
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

    public MethodBody createMethodBody(final int index, final ExecutableElement element) {
        ByteBuffer codeAttr = null;
        int attrCount = getMethodAttributeCount(index);
        for (int i = 0; i < attrCount; i ++) {
            if (methodAttributeNameEquals(index, i, "Code")) {
                codeAttr = getMethodRawAttributeContent(index, i);
                break;
            }
        }
        if (codeAttr == null) {
            throw new IllegalArgumentException("Create method body with no method body");
        }
        int modifiers = element.getModifiers();
        ClassMethodInfo classMethodInfo = new ClassMethodInfo(this, element, modifiers, index, codeAttr);
        DefinedTypeDefinition enclosing = element.getEnclosingType();
        int offs = classMethodInfo.getCodeOffs();
        int pos = codeAttr.position();
        int lim = codeAttr.limit();
        codeAttr.position(offs);
        codeAttr.limit(offs + classMethodInfo.getCodeLen());
        ByteBuffer byteCode = codeAttr.slice();
        codeAttr.position(pos);
        codeAttr.limit(lim);
        MethodParser methodParser = new MethodParser(enclosing.getContext(), classMethodInfo, byteCode, element);
        BasicBlockBuilder gf = methodParser.getBlockBuilder();
        BlockParameter thisValue;
        BlockParameter[] parameters;
        boolean nonStatic = (modifiers & ClassFile.ACC_STATIC) == 0;
        ValueType[][] varTypesByEntryPoint;
        ValueType[][] stackTypesByEntryPoint;
        ValueType[] currentVarTypes;
        int[] currentVarSlotSizes;

        // set up method for initial values
        BlockLabel entryBlockHandle = methodParser.getBlockForIndexIfExists(0);
        boolean noLoop = entryBlockHandle == null;
        BlockLabel newLabel = null;
        if (noLoop) {
            // no loop to start block; just process it as a new block
            entryBlockHandle = new BlockLabel();
            gf.begin(entryBlockHandle);
        } else {
            newLabel = new BlockLabel();
            gf.begin(newLabel);
        }

        if (element instanceof InvokableElement) {
            int initialLocals = 0;
            if (nonStatic) {
                initialLocals++;
            }
            List<ParameterElement> elementParameters = ((InvokableElement) element).getParameters();
            int paramCount = elementParameters.size();
            parameters = new BlockParameter[paramCount];
            for (int i = 0; i < paramCount; i ++) {
                boolean class2 = elementParameters.get(i).hasClass2Type();
                initialLocals += class2 ? 2 : 1;
            }
            currentVarTypes = new ValueType[initialLocals];
            currentVarSlotSizes = new int[(nonStatic ? 1 : 0) + paramCount];
            int j = 0, k = 0;
            BlockEntry be = gf.getBlockEntry();
            if (nonStatic) {
                // instance method or constructor
                thisValue = gf.addParam(be.getPinnedBlockLabel(), Slot.this_(), enclosing.load().getObjectType().getReference(), false);
                currentVarTypes[j++] = thisValue.getType();
                currentVarSlotSizes[k++] = 1;
            } else {
                thisValue = null;
            }
            for (int i = 0; i < paramCount; i ++) {
                ValueType type = elementParameters.get(i).getType();
                parameters[i] = gf.addParam(be.getPinnedBlockLabel(), Slot.funcParam(i), type);
                boolean class2 = elementParameters.get(i).hasClass2Type();
                Value promoted = methodParser.promote(parameters[i], elementParameters.get(i).getTypeDescriptor());
                currentVarTypes[j] = promoted.getType();
                j += class2 ? 2 : 1;
                currentVarSlotSizes[k++] = class2 ? 2 : 1;
            }
        } else {
            thisValue = null;
            parameters = NO_PARAMETERS;
            currentVarTypes = NO_TYPES;
            currentVarSlotSizes = NO_INTS;
        }

        // create type information for phi generation
        int smtOff = classMethodInfo.getStackMapTableOffs();
        int smtLen = smtOff == -1 ? 0 : classMethodInfo.getStackMapTableLen();
        ValueType[] currentStackTypes;
        int epCnt = classMethodInfo.getEntryPointCount();
        if (smtLen > 0) {
            varTypesByEntryPoint = new ValueType[epCnt][];
            stackTypesByEntryPoint = new ValueType[epCnt][];
            ByteBuffer sm = codeAttr.duplicate();
            int epIdx = 0;
            int bcIdx = 0;
            sm.position(smtOff);
            int tag, delta;
            for (int i = 0; i < smtLen; i ++) {
                tag = sm.get() & 0xff;
                if (tag <= 63) { // SAME
                    delta = tag;
                    currentStackTypes = NO_TYPES;
                } else if (tag <= 127) { // SAME_LOCALS_1_STACK_ITEM
                    delta = tag - 64;
                    int viTag = sm.get() & 0xff;
                    currentStackTypes = new ValueType[getSlotSize(viTag)];
                    currentStackTypes[currentStackTypes.length - 1] = getTypeOfVerificationInfo(viTag, element, sm, byteCode);
                } else if (tag <= 246) { // reserved
                    throw new IllegalStateException("Invalid stack map tag " + tag);
                } else if (tag == 247) { // SAME_LOCALS_1_STACK_ITEM_EXTENDED
                    delta = sm.getShort() & 0xffff;
                    int viTag = sm.get() & 0xff;
                    currentStackTypes = new ValueType[getSlotSize(viTag)];
                    currentStackTypes[currentStackTypes.length - 1] = getTypeOfVerificationInfo(viTag, element, sm, byteCode);
                } else if (tag <= 250) { // CHOP
                    delta = sm.getShort() & 0xffff;
                    int chop = 251 - tag;
                    int total = 0;
                    for (int j = 0; j < chop; j ++) {
                        if (currentVarSlotSizes[currentVarSlotSizes.length - 1 - j] == 2) {
                            total += 2;
                        } else {
                            total++;
                        }
                    }
                    currentVarTypes = Arrays.copyOf(currentVarTypes, currentVarTypes.length - total);
                    currentVarSlotSizes = Arrays.copyOf(currentVarSlotSizes, currentVarSlotSizes.length - chop);
                    currentStackTypes = NO_TYPES;
                } else if (tag == 251) { // SAME_FRAME_EXTENDED
                    delta = sm.getShort() & 0xffff;
                    currentStackTypes = NO_TYPES;
                } else if (tag < 255) { // APPEND
                    delta = sm.getShort() & 0xffff;
                    int append = tag - 251;
                    int total = 0;
                    int save = sm.position();
                    currentVarSlotSizes = Arrays.copyOf(currentVarSlotSizes, currentVarSlotSizes.length + append);
                    for (int j = 0; j < append; j ++) {
                        int viTag = sm.get() & 0xff;
                        // consume
                        getTypeOfVerificationInfo(viTag, element, sm, byteCode);
                        int slotSize = getSlotSize(viTag);
                        currentVarSlotSizes[currentVarSlotSizes.length - append + j] = slotSize;
                        total += slotSize;
                    }
                    sm.position(save);
                    int oldLen = currentVarTypes.length;
                    currentVarTypes = Arrays.copyOf(currentVarTypes, oldLen + total);
                    for (int j = 0, k = oldLen; j < append; j ++) {
                        int viTag = sm.get() & 0xff;
                        // consume
                        currentVarTypes[k] = getTypeOfVerificationInfo(viTag, element, sm, byteCode);
                        k += getSlotSize(viTag);
                    }
                    currentStackTypes = NO_TYPES;
                } else {
                    assert tag == 255; // FULL_FRAME
                    delta = sm.getShort() & 0xffff;
                    int localCnt = sm.getShort() & 0xffff;
                    int save = sm.position();
                    int arraySize = 0;
                    currentVarSlotSizes = new int[localCnt];
                    for (int j = 0; j < localCnt; j ++) {
                        int viTag = sm.get() & 0xff;
                        // consume
                        getTypeOfVerificationInfo(viTag, element, sm, byteCode);
                        int slotSize = getSlotSize(viTag);
                        currentVarSlotSizes[j] = slotSize;
                        arraySize += slotSize;
                    }
                    currentVarTypes = arraySize == 0 ? NO_TYPES : new ValueType[arraySize];
                    sm.position(save);
                    for (int j = 0, k = 0; j < localCnt; j ++) {
                        int viTag = sm.get() & 0xff;
                        currentVarTypes[k++] = getTypeOfVerificationInfo(viTag, element, sm, byteCode);
                        if (getSlotSize(viTag) == 2) {
                            currentVarTypes[k++] = null;
                        }
                    }
                    int stackCnt = sm.getShort() & 0xffff;
                    save = sm.position();
                    arraySize = 0;
                    for (int j = 0; j < stackCnt; j ++) {
                        int viTag = sm.get() & 0xff;
                        // consume
                        getTypeOfVerificationInfo(viTag, element, sm, byteCode);
                        arraySize += getSlotSize(viTag);
                    }
                    currentStackTypes = arraySize == 0 ? NO_TYPES : new ValueType[arraySize];
                    sm.position(save);
                    for (int j = 0, k = 0; j < stackCnt; j ++) {
                        int viTag = sm.get() & 0xff;
                        if (getSlotSize(viTag) == 2) {
                            currentStackTypes[k++] = null;
                        }
                        currentStackTypes[k++] = getTypeOfVerificationInfo(viTag, element, sm, byteCode);
                    }
                }
                // the bytecode index at which it applies
                if (i == 0) {
                    bcIdx = delta;
                } else {
                    bcIdx = bcIdx + 1 + delta;
                }
                if (epIdx < classMethodInfo.getEntryPointCount()) {
                    int target = classMethodInfo.getEntryPointTarget(epIdx);
                    if (target > bcIdx) {
                        // skip this entry point
                    } else if (target == bcIdx) {
                        // map the entry point
                        varTypesByEntryPoint[epIdx] = currentVarTypes;
                        stackTypesByEntryPoint[epIdx] = currentStackTypes;
                        epIdx++;
                    } else {
                        throw new IllegalStateException("Stack map does not match entry point calculation (next EP target is "
                            + target + ", current idx is " + bcIdx + ")");
                    }
                }
            }
        } else {
            if (epCnt == 0) {
                varTypesByEntryPoint = NO_TYPE_ARRAYS;
                stackTypesByEntryPoint = NO_TYPE_ARRAYS;
            } else {
                throw new IllegalStateException("Entry points with no type information");
            }
        }
        methodParser.setTypeInformation(varTypesByEntryPoint, stackTypesByEntryPoint);
        byteCode.position(0);
        // set initial values
        if (element instanceof InvokableElement) {
            List<ParameterElement> elementParameters = ((InvokableElement) element).getParameters();
            int paramCount = elementParameters.size();
            int j = 0;
            if (nonStatic) {
                // instance method or constructor
                methodParser.setLocal1(j++, thisValue, 0);
            }
            for (int i = 0; i < paramCount; i ++) {
                boolean class2 = elementParameters.get(i).hasClass2Type();
                Value promoted = methodParser.promote(parameters[i], elementParameters.get(i).getTypeDescriptor());
                methodParser.setLocal(j, promoted, class2, 0);
                j += class2 ? 2 : 1;
            }
        }
        // process the main entry point
        if (noLoop) {
            // no loop to start block; just process it as a new block
            methodParser.processNewBlock();
        } else {
            // we have to jump into it because there is a loop that includes index 0
            gf.goto_(entryBlockHandle, methodParser.captureOutbound());
            methodParser.processBlock();
            entryBlockHandle = newLabel;
        }
        gf.finish();
        BasicBlock entryBlock = BlockLabel.getTargetOf(entryBlockHandle);
        return MethodBody.of(entryBlock, Slot.simpleArgList(parameters.length));
    }

    int getSlotSize(int viTag) {
        return viTag == 3 || viTag == 4 ? 2 : 1;
    }

    ValueType getTypeOfVerificationInfo(int viTag, ExecutableElement element, ByteBuffer sm, ByteBuffer byteCode) {
        TypeSystem ts = ctxt.getTypeSystem();
        if (viTag == 0) { // top (hole)
            return null;
        } else if (viTag == 1) { // int
            return ts.getSignedInteger32Type();
        } else if (viTag == 2) { // float
            return ts.getFloat32Type();
        } else if (viTag == 3) { // double
            return ts.getFloat64Type();
        } else if (viTag == 4) { // long
            return ts.getSignedInteger64Type();
        } else if (viTag == 5) { // null
            // todo: bottom object type?
            return ctxt.findDefinedType("java/lang/Object").load().getClassType().getReference();
        } else if (viTag == 6) { // uninitialized this
            return element.getEnclosingType().load().getObjectType().getReference();
        } else if (viTag == 7) { // object
            int cpIdx = sm.getShort() & 0xffff;
            ValueType type = getTypeConstant(cpIdx, TypeParameterContext.of(element));
            if (type instanceof ObjectType) {
                return ((ObjectType)type).getReference();
            }
            return type;
        } else if (viTag == 8) { // uninitialized object
            int newIdx = sm.getShort() & 0xffff;
            int cpIdx = byteCode.getShort(newIdx + 1) & 0xffff;
            ValueType type = getTypeConstant(cpIdx, TypeParameterContext.of(element));
            if (type instanceof ObjectType) {
                return ((ObjectType)type).getReference();
            }
            return type;
        } else {
            throw new IllegalStateException("Invalid variable info tag " + viTag);
        }
    }
}
