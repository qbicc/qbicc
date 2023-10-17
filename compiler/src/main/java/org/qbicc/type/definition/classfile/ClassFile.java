package org.qbicc.type.definition.classfile;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.qbicc.context.ClassContext;
import org.qbicc.runtime.ExtModifier;
import org.qbicc.type.definition.ClassFileUtil;
import org.qbicc.type.definition.ConstructorResolver;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.descriptor.Descriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.methodhandle.MethodHandleConstant;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * A class file that was defined, with basic validation performed.  The constant pool is available.
 */
public interface ClassFile extends FieldResolver,
                                   MethodResolver,
                                   ConstructorResolver,
                                   InitializerResolver {

    int CONSTANT_Utf8 = 1;
    int CONSTANT_Integer = 3;
    int CONSTANT_Float = 4;
    int CONSTANT_Long = 5; // Double size!
    int CONSTANT_Double = 6; // Double size!
    int CONSTANT_Class = 7;
    int CONSTANT_String = 8;
    int CONSTANT_Fieldref = 9;
    int CONSTANT_Methodref = 10;
    int CONSTANT_InterfaceMethodref = 11;
    int CONSTANT_NameAndType = 12;
    int CONSTANT_MethodHandle = 15;
    int CONSTANT_MethodType = 16;
    int CONSTANT_Dynamic = 17;
    int CONSTANT_InvokeDynamic = 18;
    int CONSTANT_Module = 19;
    int CONSTANT_Package = 20;

    int ACC_PUBLIC = ExtModifier.ACC_PUBLIC;
    int ACC_PRIVATE = ExtModifier.ACC_PRIVATE;
    int ACC_PROTECTED = ExtModifier.ACC_PROTECTED;
    int ACC_STATIC = ExtModifier.ACC_STATIC;
    int ACC_FINAL = ExtModifier.ACC_FINAL;
    int ACC_OPEN = ExtModifier.ACC_OPEN; // same as ACC_FINAL
    int ACC_SYNCHRONIZED = ExtModifier.ACC_SYNCHRONIZED;
    int ACC_SUPER = ExtModifier.ACC_SUPER; // same as ACC_SYNCHRONIZED
    int ACC_BRIDGE = ExtModifier.ACC_BRIDGE;
    int ACC_VOLATILE = ExtModifier.ACC_VOLATILE; // same as ACC_BRIDGE
    int ACC_STATIC_PHASE = ExtModifier.ACC_STATIC_PHASE; // same as ACC_BRIDGE
    int ACC_VARARGS = ExtModifier.ACC_VARARGS;
    int ACC_TRANSIENT = ExtModifier.ACC_TRANSIENT; // same as ACC_VARARGS
    int ACC_NATIVE = ExtModifier.ACC_NATIVE;
    int ACC_INTERFACE = ExtModifier.ACC_INTERFACE;
    int ACC_ABSTRACT = ExtModifier.ACC_ABSTRACT;
    int ACC_STRICT = ExtModifier.ACC_STRICT;
    int ACC_SYNTHETIC = ExtModifier.ACC_SYNTHETIC;
    int ACC_ANNOTATION = ExtModifier.ACC_ANNOTATION;
    int ACC_ENUM = ExtModifier.ACC_ENUM;
    int ACC_MODULE = ExtModifier.ACC_MODULE;
    int ACC_MANDATED = ExtModifier.ACC_MANDATED; // same as ACC_MODULE

    /**
     * Quoting from OpenJDK:
     *
     * A signature-polymorphic method (JLS 15.12.3) is a method that
     *   (i) is declared in the java.lang.invoke.MethodHandle/VarHandle classes;
     *  (ii) takes a single variable arity parameter;
     * (iii) whose declared type is Object[];
     *  (iv) has any return type, Object signifying a polymorphic return type; and
     *   (v) is native.
     */
    int I_ACC_SIGNATURE_POLYMORPHIC = ExtModifier.I_ACC_SIGNATURE_POLYMORPHIC;
    /**
     * For fields which are declared as {@code final} but are actually mutable, including:
     * <ul>
     *     <li>{@code System.in}/{@code .out}/{@code .err}</li>
     *     <li>fields that are reflected upon or mutated via {@code Unsafe}</li>
     * </ul>
     */
    int I_ACC_NOT_REALLY_FINAL = ExtModifier.I_ACC_NOT_REALLY_FINAL;
    /*
     * Bit 16 not used for classes.
     */
    // reserved = 1 << 16;
    /**
     * For classes which represent value-based (aka primitive) types.
     */
    int I_ACC_PRIMITIVE = ExtModifier.I_ACC_PRIMITIVE;
    /**
     * On methods, hide from stack traces.  On classes, defined as a JEP 371 "hidden class".
     */
    int I_ACC_HIDDEN = ExtModifier.I_ACC_HIDDEN;
    /**
     * For static fields that are thread-local.
     */
    int I_ACC_THREAD_LOCAL = ExtModifier.I_ACC_THREAD_LOCAL;
    int I_ACC_ALWAYS_INLINE = ExtModifier.I_ACC_ALWAYS_INLINE;
    int I_ACC_NEVER_INLINE = ExtModifier.I_ACC_NEVER_INLINE;
    /**
     * For methods which have no side-effects.
     */
    int I_ACC_NO_SIDE_EFFECTS = ExtModifier.I_ACC_NO_SIDE_EFFECTS;
    /**
     * For members and types which should not appear to reflection.
     */
    int I_ACC_NO_REFLECT = ExtModifier.I_ACC_NO_REFLECT;
    /**
     * For members which should never be symbolically resolvable and classes that should not be registered to the class loader.
     */
    int I_ACC_NO_RESOLVE = ExtModifier.I_ACC_NO_RESOLVE;
    /**
     * For executable members which never return normally.
     */
    int I_ACC_NO_RETURN = ExtModifier.I_ACC_NO_RETURN;
    /**
     * For executable members which never throw - not even {@link StackOverflowError} or {@link OutOfMemoryError}.
     */
    int I_ACC_NO_THROW = ExtModifier.I_ACC_NO_THROW;
    /**
     * For executable members which should be evaluated during compilation.
     */
    int I_ACC_FOLD = ExtModifier.I_ACC_FOLD;
    /**
     * For members which are visible at run time.  Members with this annotation are not accessible during build.
     * Fields which are available at both build time and run time do <em>not</em> have this modifier, even if they
     * are associated with an initializer that does have this modifier.
     */
    int I_ACC_RUN_TIME = ExtModifier.I_ACC_RUN_TIME;
    /**
     * For methods which have the JDK {@code @CallerSensitive} annotation.
     */
    int I_ACC_CALLER_SENSITIVE = ExtModifier.I_ACC_CALLER_SENSITIVE;
    /**
     * For methods which are only invokable at build time.  Members with this annotation are not invokable during runtime.
     */
    int I_ACC_BUILD_TIME_ONLY = ExtModifier.I_ACC_BUILD_TIME_ONLY;
    /**
     * For classes. Indicates that the GC bitmap is a pointer to the full bitmap, rather than a {@code long} value.
     */
    int I_ACC_EXTENDED_BITMAP = ExtModifier.I_ACC_EXTENDED_BITMAP;

    int OP_NOP = 0x00;
    int OP_ACONST_NULL = 0x01;
    int OP_ICONST_M1 = 0x02;
    int OP_ICONST_0 = 0x03;
    int OP_ICONST_1 = 0x04;
    int OP_ICONST_2 = 0x05;
    int OP_ICONST_3 = 0x06;
    int OP_ICONST_4 = 0x07;
    int OP_ICONST_5 = 0x08;
    int OP_LCONST_0 = 0x09;
    int OP_LCONST_1 = 0x0a;
    int OP_FCONST_0 = 0x0b;
    int OP_FCONST_1 = 0x0c;
    int OP_FCONST_2 = 0x0d;
    int OP_DCONST_0 = 0x0e;
    int OP_DCONST_1 = 0x0f;
    int OP_BIPUSH = 0x10;
    int OP_SIPUSH = 0x11;
    int OP_LDC = 0x12;
    int OP_LDC_W = 0x13;
    int OP_LDC2_W = 0x14;
    int OP_ILOAD = 0x15;
    int OP_LLOAD = 0x16;
    int OP_FLOAD = 0x17;
    int OP_DLOAD = 0x18;
    int OP_ALOAD = 0x19;
    int OP_ILOAD_0 = 0x1a;
    int OP_ILOAD_1 = 0x1b;
    int OP_ILOAD_2 = 0x1c;
    int OP_ILOAD_3 = 0x1d;
    int OP_LLOAD_0 = 0x1e;
    int OP_LLOAD_1 = 0x1f;
    int OP_LLOAD_2 = 0x20;
    int OP_LLOAD_3 = 0x21;
    int OP_FLOAD_0 = 0x22;
    int OP_FLOAD_1 = 0x23;
    int OP_FLOAD_2 = 0x24;
    int OP_FLOAD_3 = 0x25;
    int OP_DLOAD_0 = 0x26;
    int OP_DLOAD_1 = 0x27;
    int OP_DLOAD_2 = 0x28;
    int OP_DLOAD_3 = 0x29;
    int OP_ALOAD_0 = 0x2a;
    int OP_ALOAD_1 = 0x2b;
    int OP_ALOAD_2 = 0x2c;
    int OP_ALOAD_3 = 0x2d;
    int OP_IALOAD = 0x2e;
    int OP_LALOAD = 0x2f;
    int OP_FALOAD = 0x30;
    int OP_DALOAD = 0x31;
    int OP_AALOAD = 0x32;
    int OP_BALOAD = 0x33;
    int OP_CALOAD = 0x34;
    int OP_SALOAD = 0x35;
    int OP_ISTORE = 0x36;
    int OP_LSTORE = 0x37;
    int OP_FSTORE = 0x38;
    int OP_DSTORE = 0x39;
    int OP_ASTORE = 0x3a;
    int OP_ISTORE_0 = 0x3b;
    int OP_ISTORE_1 = 0x3c;
    int OP_ISTORE_2 = 0x3d;
    int OP_ISTORE_3 = 0x3e;
    int OP_LSTORE_0 = 0x3f;
    int OP_LSTORE_1 = 0x40;
    int OP_LSTORE_2 = 0x41;
    int OP_LSTORE_3 = 0x42;
    int OP_FSTORE_0 = 0x43;
    int OP_FSTORE_1 = 0x44;
    int OP_FSTORE_2 = 0x45;
    int OP_FSTORE_3 = 0x46;
    int OP_DSTORE_0 = 0x47;
    int OP_DSTORE_1 = 0x48;
    int OP_DSTORE_2 = 0x49;
    int OP_DSTORE_3 = 0x4a;
    int OP_ASTORE_0 = 0x4b;
    int OP_ASTORE_1 = 0x4c;
    int OP_ASTORE_2 = 0x4d;
    int OP_ASTORE_3 = 0x4e;
    int OP_IASTORE = 0x4f;
    int OP_LASTORE = 0x50;
    int OP_FASTORE = 0x51;
    int OP_DASTORE = 0x52;
    int OP_AASTORE = 0x53;
    int OP_BASTORE = 0x54;
    int OP_CASTORE = 0x55;
    int OP_SASTORE = 0x56;
    int OP_POP = 0x57;
    int OP_POP2 = 0x58;
    int OP_DUP = 0x59;
    int OP_DUP_X1 = 0x5a;
    int OP_DUP_X2 = 0x5b;
    int OP_DUP2 = 0x5c;
    int OP_DUP2_X1 = 0x5d;
    int OP_DUP2_X2 = 0x5e;
    int OP_SWAP = 0x5f;
    int OP_IADD = 0x60;
    int OP_LADD = 0x61;
    int OP_FADD = 0x62;
    int OP_DADD = 0x63;
    int OP_ISUB = 0x64;
    int OP_LSUB = 0x65;
    int OP_FSUB = 0x66;
    int OP_DSUB = 0x67;
    int OP_IMUL = 0x68;
    int OP_LMUL = 0x69;
    int OP_FMUL = 0x6a;
    int OP_DMUL = 0x6b;
    int OP_IDIV = 0x6c;
    int OP_LDIV = 0x6d;
    int OP_FDIV = 0x6e;
    int OP_DDIV = 0x6f;
    int OP_IREM = 0x70;
    int OP_LREM = 0x71;
    int OP_FREM = 0x72;
    int OP_DREM = 0x73;
    int OP_INEG = 0x74;
    int OP_LNEG = 0x75;
    int OP_FNEG = 0x76;
    int OP_DNEG = 0x77;
    int OP_ISHL = 0x78;
    int OP_LSHL = 0x79;
    int OP_ISHR = 0x7a;
    int OP_LSHR = 0x7b;
    int OP_IUSHR = 0x7c;
    int OP_LUSHR = 0x7d;
    int OP_IAND = 0x7e;
    int OP_LAND = 0x7f;
    int OP_IOR = 0x80;
    int OP_LOR = 0x81;
    int OP_IXOR = 0x82;
    int OP_LXOR = 0x83;
    int OP_IINC = 0x84;
    int OP_I2L = 0x85;
    int OP_I2F = 0x86;
    int OP_I2D = 0x87;
    int OP_L2I = 0x88;
    int OP_L2F = 0x89;
    int OP_L2D = 0x8a;
    int OP_F2I = 0x8b;
    int OP_F2L = 0x8c;
    int OP_F2D = 0x8d;
    int OP_D2I = 0x8e;
    int OP_D2L = 0x8f;
    int OP_D2F = 0x90;
    int OP_I2B = 0x91;
    int OP_I2C = 0x92;
    int OP_I2S = 0x93;
    int OP_LCMP = 0x94;
    int OP_FCMPL = 0x95;
    int OP_FCMPG = 0x96;
    int OP_DCMPL = 0x97;
    int OP_DCMPG = 0x98;
    int OP_IFEQ = 0x99;
    int OP_IFNE = 0x9a;
    int OP_IFLT = 0x9b;
    int OP_IFGE = 0x9c;
    int OP_IFGT = 0x9d;
    int OP_IFLE = 0x9e;
    int OP_IF_ICMPEQ = 0x9f;
    int OP_IF_ICMPNE = 0xa0;
    int OP_IF_ICMPLT = 0xa1;
    int OP_IF_ICMPGE = 0xa2;
    int OP_IF_ICMPGT = 0xa3;
    int OP_IF_ICMPLE = 0xa4;
    int OP_IF_ACMPEQ = 0xa5;
    int OP_IF_ACMPNE = 0xa6;
    int OP_GOTO = 0xa7;
    int OP_JSR = 0xa8;
    int OP_RET = 0xa9;
    int OP_TABLESWITCH = 0xaa;
    int OP_LOOKUPSWITCH = 0xab;
    int OP_IRETURN = 0xac;
    int OP_LRETURN = 0xad;
    int OP_FRETURN = 0xae;
    int OP_DRETURN = 0xaf;
    int OP_ARETURN = 0xb0;
    int OP_RETURN = 0xb1;
    int OP_GETSTATIC = 0xb2;
    int OP_PUTSTATIC = 0xb3;
    int OP_GETFIELD = 0xb4;
    int OP_PUTFIELD = 0xb5;
    int OP_INVOKEVIRTUAL = 0xb6;
    int OP_INVOKESPECIAL = 0xb7;
    int OP_INVOKESTATIC = 0xb8;
    int OP_INVOKEINTERFACE = 0xb9;
    int OP_INVOKEDYNAMIC = 0xba;
    int OP_NEW = 0xbb;
    int OP_NEWARRAY = 0xbc;
    int OP_ANEWARRAY = 0xbd;
    int OP_ARRAYLENGTH = 0xbe;
    int OP_ATHROW = 0xbf;
    int OP_CHECKCAST = 0xc0;
    int OP_INSTANCEOF = 0xc1;
    int OP_MONITORENTER = 0xc2;
    int OP_MONITOREXIT = 0xc3;
    int OP_WIDE = 0xc4;
    int OP_MULTIANEWARRAY = 0xc5;
    int OP_IFNULL = 0xc6;
    int OP_IFNONNULL = 0xc7;
    int OP_GOTO_W = 0xc8;
    int OP_JSR_W = 0xc9;

    int OP_BREAKPOINT = 0xca;

    int OP_IMPDEP1 = 0xfe;
    int OP_IMPDEP2 = 0xff;

    int T_BOOLEAN = 4;
    int T_CHAR = 5;
    int T_FLOAT = 6;
    int T_DOUBLE = 7;
    int T_BYTE = 8;
    int T_SHORT = 9;
    int T_INT = 10;
    int T_LONG = 11;

    ClassContext getClassContext();

    ByteBuffer getBackingBuffer();

    int getMajorVersion();

    int getMinorVersion();

    default int compareVersion(int major, int minor) {
        int res = Integer.compare(getMajorVersion(), major);
        if (res == 0) {
            res = Integer.compare(getMinorVersion(), minor);
        }
        return res;
    }

    /**
     * Get the number of constants, including the invisible "null" constant at index zero.
     *
     * @return the number of constants in the constant pool
     */
    int getConstantCount();

    int getConstantType(int poolIndex);

    // get<constant-type>Constant[<field>][<java-type>]

    /**
     * Get a class name from the constant pool.
     *
     * @param idx the index of the {@link ClassFileUtil#CONSTANT_Class} entry
     * @return the name of the class referenced at this index
     * @throws IndexOutOfBoundsException     if the index does not fall within the valid range for the constant pool of
     *                                       this class
     * @throws ConstantTypeMismatchException if the constant pool entry is of the wrong type
     */
    default String getClassConstantName(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        return getUtf8Constant(getClassConstantNameIdx(idx));
    }

    default boolean classConstantNameEquals(int idx, String expect) {
        return utf8ConstantEquals(getClassConstantNameIdx(idx), expect);
    }

    default int getClassConstantNameIdx(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        if (idx == 0) {
            return 0;
        }
        checkConstantType(idx, CONSTANT_Class);
        return getRawConstantShort(idx, 1);
    }

    default String getStringConstant(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        if (idx == 0) {
            return null;
        }
        checkConstantType(idx, CONSTANT_String);
        return getUtf8Constant(getRawConstantShort(idx, 1));
    }

    default long getLongConstant(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Long);
        return getRawConstantLong(idx, 1);
    }

    default int getIntConstant(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Integer);
        return getRawConstantInt(idx, 1);
    }

    default float getFloatConstant(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Float);
        return Float.intBitsToFloat(getRawConstantInt(idx, 1));
    }

    default double getDoubleConstant(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Double);
        return Double.longBitsToDouble(getRawConstantLong(idx, 1));
    }

    default String getFieldrefConstantClassName(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Fieldref);
        return getClassConstantName(getRawConstantShort(idx, 1));
    }

    default int getFieldrefConstantClassIndex(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Fieldref);
        return getRawConstantShort(idx, 1);
    }

    default int getFieldrefNameAndTypeIndex(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Fieldref);
        return getRawConstantShort(idx, 3);
    }

    default int getFieldrefConstantDescriptorIdx(final int idx) {
        checkConstantType(idx, CONSTANT_Fieldref);
        return getNameAndTypeConstantDescriptorIdx(getRawConstantShort(idx, 3));
    }

    default String getMethodrefConstantClassName(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Methodref, CONSTANT_InterfaceMethodref);
        return getClassConstantName(getRawConstantShort(idx, 1));
    }

    default int getMethodrefConstantClassIndex(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Methodref, CONSTANT_InterfaceMethodref);
        return getRawConstantShort(idx, 1);
    }

    default int getMethodrefNameAndTypeIndex(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Methodref, CONSTANT_InterfaceMethodref);
        return getRawConstantShort(idx, 3);
    }

    default String getMethodrefConstantName(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Methodref, CONSTANT_InterfaceMethodref);
        return getNameAndTypeConstantName(getRawConstantShort(idx, 3));
    }

    default boolean methodrefConstantNameEquals(int idx, String expected) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Methodref, CONSTANT_InterfaceMethodref);
        return nameAndTypeConstantNameEquals(getRawConstantShort(idx, 3), expected);
    }

    default int getInterfaceMethodrefConstantClassIndex(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_InterfaceMethodref, CONSTANT_InterfaceMethodref);
        return getRawConstantShort(idx, 1);
    }

    default int getInterfaceMethodrefNameAndTypeIndex(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_InterfaceMethodref);
        return getRawConstantShort(idx, 3);
    }

    default String getInterfaceMethodrefConstantName(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_InterfaceMethodref);
        return getNameAndTypeConstantName(getRawConstantShort(idx, 3));
    }

    default boolean interfaceMethodrefConstantNameEquals(int idx, String expected) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_InterfaceMethodref);
        return nameAndTypeConstantNameEquals(getRawConstantShort(idx, 3), expected);
    }

    default String getNameAndTypeConstantName(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_NameAndType);
        return getUtf8Constant(getRawConstantShort(idx, 1));
    }

    default boolean nameAndTypeConstantNameEquals(int idx, String expected) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_NameAndType);
        return utf8ConstantEquals(getRawConstantShort(idx, 1), expected);
    }

    default int getNameAndTypeConstantDescriptorIdx(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_NameAndType);
        return getRawConstantShort(idx, 3);
    }

    default Descriptor getNameAndTypeConstantDescriptor(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_NameAndType);
        return getDescriptorConstant(getRawConstantShort(idx, 3));
    }

    default int getInvokeDynamicBootstrapMethodIndex(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_InvokeDynamic);
        return getRawConstantShort(idx, 1);
    }

    default int getInvokeDynamicNameAndTypeIndex(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_InvokeDynamic);
        return getRawConstantShort(idx, 3);
    }

    default int getMethodHandleReferenceKind(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_MethodHandle);
        return getRawConstantByte(idx, 1);
    }

    default int getMethodHandleReferenceIndex(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_MethodHandle);
        return getRawConstantShort(idx, 3);
    }

    default int getMethodTypeDescriptorIndex(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_MethodType);
        return getRawConstantShort(idx, 1);
    }

    /**
     * Get the constant table index of the {@code MethodHandle} constant corresponding to the bootstrap method with
     * the given index.
     *
     * @param idx the bootstrap method index
     * @return the method handle index
     * @throws IndexOutOfBoundsException if the index is not valid
     * @throws ConstantTypeMismatchException if a constant is of an unexpected type
     */
    int getBootstrapMethodHandleRef(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException;

    /**
     * Get the number of explicit arguments for the bootstrap method with the given index.
     *
     * @param idx the bootstrap method index
     * @return the argument count
     * @throws IndexOutOfBoundsException if the index is not valid
     * @throws ConstantTypeMismatchException if a constant is of an unexpected type
     */
    int getBootstrapMethodArgumentCount(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException;

    /**
     * Get the index of the constant table entry for the argument with the given index to the corresponding bootstrap
     * method with the corresponding index.
     *
     * @param idx the bootstrap method index
     * @param argIdx the argument index
     * @return the method handle index
     * @throws IndexOutOfBoundsException if an index is not valid
     * @throws ConstantTypeMismatchException if a constant is of an unexpected type
     */
    int getBootstrapMethodArgumentConstantIndex(int idx, int argIdx) throws IndexOutOfBoundsException, ConstantTypeMismatchException;

    // todo: Module
    // todo: Package

    // raw constant pool access

    boolean utf8ConstantEquals(int idx, String expected) throws IndexOutOfBoundsException, ConstantTypeMismatchException;

    String getUtf8Constant(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException;

    ByteBuffer getUtf8ConstantAsBuffer(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException;

    default int getUtf8ConstantLength(int idx) throws IndexOutOfBoundsException, ConstantTypeMismatchException {
        checkConstantType(idx, CONSTANT_Utf8);
        return getRawConstantShort(idx, 1);
    }

    void checkConstantType(int idx, int expectedType) throws IndexOutOfBoundsException, ConstantTypeMismatchException;

    void checkConstantType(int idx, int expectedType1, int expectedType2) throws IndexOutOfBoundsException, ConstantTypeMismatchException;

    int getRawConstantByte(int idx, int offset) throws IndexOutOfBoundsException;

    int getRawConstantShort(int idx, int offset) throws IndexOutOfBoundsException;

    int getRawConstantInt(int idx, int offset) throws IndexOutOfBoundsException;

    long getRawConstantLong(int idx, int offset) throws IndexOutOfBoundsException;

    int getAccess();

    String getName();

    String getSuperClassName();

    int getInterfaceNameCount();

    String getInterfaceName(int idx);

    Descriptor getDescriptorConstant(int idx);

    TypeDescriptor getClassConstantAsDescriptor(int idx);

    MethodHandleConstant getMethodHandleConstant(final int idx);

    /**
     * Get the number of fields physically present in the class file.
     *
     * @return the number of fields physically present in the class file
     */
    int getFieldCount();

    int getFieldModifiers(int idx);

    String getFieldName(int idx);

    TypeDescriptor getFieldDescriptor(int idx);

    int getFieldAttributeCount(int idx) throws IndexOutOfBoundsException;

    boolean fieldAttributeNameEquals(int fieldIdx, int attrIdx, String expected) throws IndexOutOfBoundsException;

    int getFieldRawAttributeByte(int fieldIdx, int attrIdx, int offset) throws IndexOutOfBoundsException;
    int getFieldRawAttributeShort(int fieldIdx, int attrIdx, int offset) throws IndexOutOfBoundsException;
    int getFieldRawAttributeInt(int fieldIdx, int attrIdx, int offset) throws IndexOutOfBoundsException;
    long getFieldRawAttributeLong(int fieldIdx, int attrIdx, int offset) throws IndexOutOfBoundsException;
    ByteBuffer getFieldRawAttributeContent(int fieldIdx, int attrIdx) throws IndexOutOfBoundsException;
    int getFieldAttributeContentLength(int fieldIdx, int attrIdx) throws IndexOutOfBoundsException;

    /**
     * Get the number of methods physically present in the class file.  This includes
     * initializers and constructors.
     *
     * @return the number of methods physically present in the class file
     */
    int getMethodCount();

    int getMethodModifiers(int idx);
    String getMethodName(int idx);
    MethodDescriptor getMethodDescriptor(int idx);

    int getMethodAttributeCount(int idx) throws IndexOutOfBoundsException;

    boolean methodAttributeNameEquals(int methodIdx, int attrIdx, String expected) throws IndexOutOfBoundsException;

    int getMethodRawAttributeByte(int methodIdx, int attrIdx, int offset) throws IndexOutOfBoundsException;
    int getMethodRawAttributeShort(int methodIdx, int attrIdx, int offset) throws IndexOutOfBoundsException;
    int getMethodRawAttributeInt(int methodIdx, int attrIdx, int offset) throws IndexOutOfBoundsException;
    long getMethodRawAttributeLong(int methodIdx, int attrIdx, int offset) throws IndexOutOfBoundsException;
    ByteBuffer getMethodRawAttributeContent(int methodIdx, int attrIdx) throws IndexOutOfBoundsException;
    int getMethodAttributeContentLength(int methodIdx, int attrIdx) throws IndexOutOfBoundsException;

    /**
     * Get the number of top-level attributes physically present in the class file.
     *
     * @return the number of attributes
     */
    int getAttributeCount();

    boolean attributeNameEquals(int idx, String expected) throws IndexOutOfBoundsException;

    int getAttributeContentLength(int idx) throws IndexOutOfBoundsException;

    ByteBuffer getRawAttributeContent(int idx) throws IndexOutOfBoundsException;

    int getRawAttributeByte(int idx, int offset) throws IndexOutOfBoundsException;

    int getRawAttributeShort(int idx, int offset) throws IndexOutOfBoundsException;

    int getRawAttributeInt(int idx, int offset) throws IndexOutOfBoundsException;

    long getRawAttributeLong(int idx, int offset) throws IndexOutOfBoundsException;

    /**
     * Pass this class file through the given type definition builder.
     *
     * @param builder the type definition builder
     * @throws ClassFormatException if a class format problem is discovered
     */
    void accept(DefinedTypeDefinition.Builder builder) throws ClassFormatException;

    static ClassFile of(final ClassContext ctxt, ByteBuffer orig) {
        orig.order(ByteOrder.BIG_ENDIAN);
        ByteBuffer buffer = orig.duplicate();
        return ClassFileImpl.make(ctxt, buffer);
    }
}
