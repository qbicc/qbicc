package org.qbicc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.eclipse.collections.api.factory.primitive.IntIntMaps;
import org.eclipse.collections.api.map.primitive.ImmutableIntIntMap;
import org.qbicc.type.definition.DefinedTypeDefinition;
import io.smallrye.common.constraint.Assert;

/**
 * The type system of the target machine and VM configuration.
 */
public final class TypeSystem {
    private static final VarHandle referenceArrayDefinitionHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "referenceArrayDefinition", VarHandle.class, TypeSystem.class, DefinedTypeDefinition.class);

    private final int byteBits;
    private final ImmutableIntIntMap pointerSizes;
    private final ImmutableIntIntMap pointerAlignments;
    private final int referenceSize;
    private final int referenceAlign;
    private final int typeIdSize;
    private final int typeIdAlign;
    private final int maxAlign;
    private final boolean signedChar;
    private final ByteOrder endianness;
    private final VariadicType variadicType = new VariadicType(this);
    private final PoisonType poisonType = new PoisonType(this);
    private final VoidType voidType = new VoidType(this);
    private final BlockType blockType = new BlockType(this);
    private final UnresolvedType unresolvedType = new UnresolvedType(this);
    private final BooleanType booleanType;
    private final FloatType float32Type;
    private final FloatType float64Type;
    private final SignedIntegerType signedInteger8Type;
    private final SignedIntegerType signedInteger16Type;
    private final SignedIntegerType signedInteger32Type;
    private final SignedIntegerType signedInteger64Type;
    private final UnsignedIntegerType unsignedInteger8Type;
    private final UnsignedIntegerType unsignedInteger16Type;
    private final UnsignedIntegerType unsignedInteger32Type;
    private final UnsignedIntegerType unsignedInteger64Type;
    private final TypeCache<FunctionType> functionTypeCache = new TypeCache<>();
    private final TypeCache<StaticMethodType> staticMethodTypeCache = new TypeCache<>();
    private final TypeCache<InstanceMethodType> instanceMethodTypeCache = new TypeCache<>();

    private volatile ClassObjectType objectClass;
    @SuppressWarnings("unused")
    private volatile DefinedTypeDefinition referenceArrayDefinition;

    TypeSystem(final Builder builder) {
        int byteBits = builder.getByteBits();
        this.byteBits = byteBits;
        maxAlign = builder.getMaxAlignment();
        signedChar = builder.isSignedChar();
        pointerSizes = builder.getPointerSizes();
        pointerAlignments = builder.getPointerAlignments();
        referenceSize = builder.getReferenceSize();
        referenceAlign = builder.getReferenceAlignment();
        booleanType = new BooleanType(this, builder.getBoolSize(), builder.getBoolAlignment());
        typeIdSize = builder.getTypeIdSize();
        typeIdAlign = builder.getTypeIdAlignment();
        endianness = builder.getEndianness();
        int float32Size = builder.getFloat32Size();
        if (float32Size * byteBits < 32) {
            throw typeTooSmall("float32");
        }
        float32Type = new FloatType(this, float32Size, 32, builder.getFloat32Alignment());
        int float64Size = builder.getFloat64Size();
        if (float64Size * byteBits < 64) {
            throw typeTooSmall("float64");
        }
        float64Type = new FloatType(this, float64Size, 64, builder.getFloat64Alignment());
        // always OK
        signedInteger8Type = new SignedIntegerType(this, builder.getInt8Size(), builder.getInt8Alignment(), 8);
        unsignedInteger8Type = new UnsignedIntegerType(this, builder.getInt8Size(), builder.getInt8Alignment(), 8);
        int int16Size = builder.getInt16Size();
        if (int16Size * byteBits < 16) {
            throw typeTooSmall("int16");
        }
        signedInteger16Type = new SignedIntegerType(this, builder.getInt16Size(), builder.getInt16Alignment(), 16);
        unsignedInteger16Type = new UnsignedIntegerType(this, builder.getInt16Size(), builder.getInt16Alignment(), 16);
        int int32Size = builder.getInt32Size();
        if (int32Size * byteBits < 32) {
            throw typeTooSmall("int32");
        }
        signedInteger32Type = new SignedIntegerType(this, builder.getInt32Size(), builder.getInt32Alignment(), 32);
        unsignedInteger32Type = new UnsignedIntegerType(this, builder.getInt32Size(), builder.getInt32Alignment(), 32);
        int int64Size = builder.getInt64Size();
        if (int64Size * byteBits < 64) {
            throw typeTooSmall("int64");
        }
        signedInteger64Type = new SignedIntegerType(this, builder.getInt64Size(), builder.getInt64Alignment(), 64);
        unsignedInteger64Type = new UnsignedIntegerType(this, builder.getInt64Size(), builder.getInt64Alignment(), 64);
        Primitive.BOOLEAN.setType(booleanType);
        Primitive.BYTE.setType(signedInteger8Type);
        Primitive.SHORT.setType(signedInteger16Type);
        Primitive.CHAR.setType(unsignedInteger16Type);
        Primitive.INT.setType(signedInteger32Type);
        Primitive.FLOAT.setType(float32Type);
        Primitive.LONG.setType(signedInteger64Type);
        Primitive.DOUBLE.setType(float64Type);
        Primitive.VOID.setType(voidType);
    }

    private static IllegalArgumentException typeTooSmall(String name) {
        return new IllegalArgumentException("Type " + name + " does not contain enough bits");
    }

    public VariadicType getVariadicType() {
        return variadicType;
    }

    public VoidType getVoidType() {
        return voidType;
    }

    public PoisonType getPoisonType() {
        return poisonType;
    }

    public BooleanType getBooleanType() {
        return booleanType;
    }

    public int getReferenceSize() {
        return referenceSize;
    }

    public int getReferenceAlignment() {
        return referenceAlign;
    }

    public UnresolvedType getUnresolvedType() {
        return unresolvedType;
    }

    /**
     * Get the pointer size for the default address space of this type system.
     *
     * @return the size of a pointer
     */
    public int getPointerSize() {
        return pointerSize(0);
    }

    /**
     * {@return the pointer size for the given address space}
     * @param addrSpace the address space (must not be negative)
     */
    public int pointerSize(int addrSpace) {
        return pointerSizes.getOrThrow(addrSpace);
    }

    /**
     * Get the pointer alignment for this type system.
     *
     * @return the pointer alignment for this type system
     */
    public int getPointerAlignment() {
        return pointerAlignment(0);
    }

    /**
     * {@return the pointer alignment for the given address space}
     * @param addrSpace the address space (must not be negative)
     */
    public int pointerAlignment(int addrSpace) {
        return pointerAlignments.getOrThrow(addrSpace);
    }

    /**
     * Get the maximal alignment for any type defined by this type system
     *
     * @return the maximal alignment for any type of this type system
     */
    public int getMaxAlignment() {
        return maxAlign;
    }

    /**
     * Get the function alignment for this type system.
     *
     * @return the function alignment for this type system
     */
    public int getFunctionAlignment() {
        return pointerAlignment(0);
    }

    /**
     * Get the size of type ID values (in bytes) for this type system.
     *
     * @return the size of type ID values for this type system
     */
    public int getTypeIdSize() {
        return typeIdSize;
    }

    /**
     * Get the integer type to use when creating typeId literals.
     */
    public IntegerType getTypeIdLiteralType() {
        switch (getTypeIdSize()) {
            case 1: return getUnsignedInteger8Type();
            case 2: return getUnsignedInteger16Type();
            case 4: return getUnsignedInteger32Type();
            default: throw new IllegalStateException("Unsupported size for type IDs: " + getTypeIdSize());
        }
    }

    /**
     * Get the alignment of type ID values for this type system.
     *
     * @return the alignment of type ID values for this type system
     */
    public int getTypeIdAlignment() {
        return typeIdAlign;
    }

    /**
     * Get the type of {@code char} on this system.
     * Most of the time it will be 8 bits, but it may be signed or unsigned depending on the platform.
     *
     * @return the type of {@code char} (not {@code null})
     */
    public IntegerType getNativeCharType() {
        return signedChar ? getSignedInteger8Type() : getUnsignedInteger8Type();
    }

    public ByteOrder getEndianness() {
        return endianness;
    }

    public void initializeReferenceArrayClass(DefinedTypeDefinition arrayClassDefinition) {
        if (! referenceArrayDefinitionHandle.compareAndSet(this, null, arrayClassDefinition)) {
            throw new IllegalStateException("Reference array class definition initialized twice");
        }
    }

    DefinedTypeDefinition getReferenceArrayTypeDefinition() {
        return referenceArrayDefinition;
    }

    public StructType.Member getProbedStructTypeMember(String name, ValueType type, int offset) {
        Assert.checkNotNullParam("name", name);
        Assert.checkMinimumParameter("offset", 0, offset);
        return new StructType.Member(name, type, offset, 1);
    }

    public StructType.Member getStructTypeMember(String name, ValueType type, int offset, int align) {
        Assert.checkNotNullParam("name", name);
        TypeUtil.checkAlignmentParameter("align", align);
        align = Math.max(type.getAlign(), align);
        Assert.checkMinimumParameter("offset", 0, offset);
        if ((offset & (align - 1)) != 0) {
            throw new IllegalArgumentException("Invalid offset (not sufficiently aligned)");
        }
        return new StructType.Member(name, type, offset, align);
    }

    /**
     * Get an unaligned compound type member. This is useful when creating structures that are not stored in memory
     * or that LLVM must understand.
     *
     * @param name the member name (must not be {@code null})
     * @param type the member type (must not be {@code null})
     * @param offset the member offset (must be at least 0)
     * @return the member (not {@code null})
     */
    public StructType.Member getUnalignedStructTypeMember(String name, ValueType type, int offset) {
        Assert.checkNotNullParam("name", name);
        Assert.checkMinimumParameter("offset", 0, offset);
        return new StructType.Member(name, type, offset, type.getAlign());
    }

    public FunctionType getFunctionType(final ValueType returnType, final List<ValueType> parameterTypes) {
        Assert.checkNotNullParam("returnType", returnType);
        Assert.checkNotNullParam("parameterTypes", parameterTypes);
        TypeCache<FunctionType> current = functionTypeCache.computeIfAbsent(returnType, TypeCache::new);
        int size = parameterTypes.size();
        for (int i = 0; i < size; i++) {
            final ValueType argType = parameterTypes.get(i);
            if (argType instanceof VariadicType) {
                if (i < size - 1) {
                    throw new IllegalArgumentException("Only last argument may be variadic");
                }
            } else if (! argType.isComplete()) {
                throw new IllegalArgumentException("Function argument types must be complete");
            }
            current = current.computeIfAbsent(argType, TypeCache::new);
        }
        FunctionType functionType = current.getValue();
        if (functionType == null) {
            functionType = new FunctionType(this, returnType, parameterTypes);
            while (! current.compareAndSet(null, functionType)) {
                FunctionType appearing = current.getValue();
                if (appearing != null) {
                    return appearing;
                }
            }
        }
        return functionType;
    }

    @Deprecated
    public FunctionType getFunctionType(ValueType returnType, ValueType... argTypes) {
        return getFunctionType(returnType, List.of(argTypes));
    }

    public StaticMethodType getStaticMethodType(final ValueType returnType, final List<ValueType> parameterTypes) {
        Assert.checkNotNullParam("returnType", returnType);
        Assert.checkNotNullParam("parameterTypes", parameterTypes);
        TypeCache<StaticMethodType> current = staticMethodTypeCache.computeIfAbsent(returnType, TypeCache::new);
        for (ValueType parameterType : parameterTypes) {
            current = current.computeIfAbsent(parameterType, TypeCache::new);
        }
        StaticMethodType type = current.getValue();
        if (type == null) {
            type = new StaticMethodType(this, returnType, parameterTypes);
            while (! current.compareAndSet(null, type)) {
                StaticMethodType appearing = current.getValue();
                if (appearing != null) {
                    return appearing;
                }
            }
        }
        return type;
    }

    public InstanceMethodType getInstanceMethodType(final ValueType receiverType, final ValueType returnType, final List<ValueType> parameterTypes) {
        Assert.checkNotNullParam("receiverType", receiverType);
        Assert.checkNotNullParam("returnType", returnType);
        Assert.checkNotNullParam("parameterTypes", parameterTypes);
        TypeCache<InstanceMethodType> current = instanceMethodTypeCache.computeIfAbsent(receiverType, TypeCache::new);
        current = current.computeIfAbsent(returnType, TypeCache::new);
        for (ValueType parameterType : parameterTypes) {
            current = current.computeIfAbsent(parameterType, TypeCache::new);
        }
        InstanceMethodType type = current.getValue();
        if (type == null) {
            type = new InstanceMethodType(this, receiverType, returnType, parameterTypes);
            while (! current.compareAndSet(null, type)) {
                InstanceMethodType appearing = current.getValue();
                if (appearing != null) {
                    return appearing;
                }
            }
        }
        return type;
    }

    public StructType getStructType(final StructType.Tag tag, String name, long size, int align, Supplier<List<StructType.Member>> memberResolver) {
        Assert.checkNotNullParam("tag", tag);
        Assert.checkMinimumParameter("size", 0, size);
        TypeUtil.checkAlignmentParameter("align", align);
        return new StructType(this, tag, name, memberResolver, size, align);
    }

    public StructType getStructType(final StructType.Tag tag, String name, Supplier<List<StructType.Member>> memberResolver) {
        Assert.checkNotNullParam("tag", tag);
        return new StructType(this, tag, name, memberResolver);
    }

    public StructType getIncompleteStructType(final StructType.Tag tag, final String name) {
        Assert.checkNotNullParam("tag", tag);
        Assert.checkNotNullParam("name", name);
        return new StructType(this, tag, name);
    }

    public ArrayType getArrayType(ValueType memberType, long elements) {
        Assert.checkNotNullParam("memberType", memberType);
        Assert.checkMinimumParameter("elements", 0, elements);
        return new ArrayType(this, memberType, elements);
    }

    public UnionType.Member getUnionTypeMember(String name, ValueType type) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        return new UnionType.Member(name, type);
    }

    public UnionType getUnionType(final UnionType.Tag tag, String name, Supplier<List<UnionType.Member>> memberResolver) {
        Assert.checkNotNullParam("tag", tag);
        Assert.checkNotNullParam("memberResolver", memberResolver);
        return new UnionType(this, tag, name, memberResolver);
    }

    /**
     * Get the number of bits in a byte for this platform (guaranteed to be at least 8).
     *
     * @return the number of bits in a byte, ≥ 8
     */
    public int getByteBits() {
        return byteBits;
    }

    /**
     * Get the type representing a block label.
     *
     * @return the type representing a block label
     */
    public BlockType getBlockType() {
        return blockType;
    }

    /**
     * Get the floating-point type that should be used for {@code float} values.  Will be a class 1 type.
     *
     * @return the {@code float} value type
     */
    public FloatType getFloat32Type() {
        return float32Type;
    }

    /**
     * Get the floating-point type that should be used for {@code double} values.  Will be a class 2 type.
     *
     * @return the {@code double} value type
     */
    public FloatType getFloat64Type() {
        return float64Type;
    }

    /**
     * Get the signed integer type that has at least 8 bits.  On some systems, it may be larger.
     *
     * @return the type
     */
    public SignedIntegerType getSignedInteger8Type() {
        return signedInteger8Type;
    }

    /**
     * Get the signed integer type that has at least 16 bits.  On some systems, it may be larger.
     *
     * @return the type
     */
    public SignedIntegerType getSignedInteger16Type() {
        return signedInteger16Type;
    }

    /**
     * Get the signed integer type that has at least 32 bits.  On some systems, it may be larger.
     *
     * @return the type
     */
    public SignedIntegerType getSignedInteger32Type() {
        return signedInteger32Type;
    }

    /**
     * Get the signed integer type that has at least 64 bits.  On some systems, it may be larger.  Will be a class 2
     * type.
     *
     * @return the type
     */
    public SignedIntegerType getSignedInteger64Type() {
        return signedInteger64Type;
    }

    /**
     * Get the unsigned integer type that has at least 8 bits.  On some systems, it may be larger.
     *
     * @return the type
     */
    public UnsignedIntegerType getUnsignedInteger8Type() {
        return unsignedInteger8Type;
    }

    /**
     * Get the unsigned integer type that has at least 16 bits.  On some systems, it may be larger.
     *
     * @return the type
     */
    public UnsignedIntegerType getUnsignedInteger16Type() {
        return unsignedInteger16Type;
    }

    /**
     * Get the unsigned integer type that has at least 32 bits.  On some systems, it may be larger.
     *
     * @return the type
     */
    public UnsignedIntegerType getUnsignedInteger32Type() {
        return unsignedInteger32Type;
    }

    /**
     * Get the unsigned integer type that has at least 64 bits.  On some systems, it may be larger.
     *
     * @return the type
     */
    public UnsignedIntegerType getUnsignedInteger64Type() {
        return unsignedInteger64Type;
    }

    /**
     * Generate a unique class object type for the given defined type.  Should only be called during class loading.
     *
     * @param definedType the defined type (must not be {@code null})
     * @param superClass the superclass type (must not be {@code null} except for the {@code Object} base class)
     * @param interfaces the interfaces list (must not be {@code null}, entries must not be {@code null})
     * @return the class object type
     */
    public ClassObjectType generateClassObjectType(DefinedTypeDefinition definedType, ClassObjectType superClass, List<InterfaceObjectType> interfaces) {
        if (definedType.isInterface()) {
            throw new IllegalArgumentException("Not a class");
        }
        if (definedType.internalNameEquals("java/lang/Object")) {
            // special case: store this one
            return this.objectClass = new ClassObjectType(this, definedType, null, List.of());
        }
        return new ClassObjectType(this, definedType, superClass, interfaces);
    }

    /**
     * Generate a unique interface object type for the given defined type.  Should only be called during class loading.
     *
     * @param definedType the defined type (must not be {@code null})
     * @param interfaces the interfaces list (must not be {@code null}, entries must not be {@code null})
     * @return the interface object type
     */
    public InterfaceObjectType generateInterfaceObjectType(DefinedTypeDefinition definedType, List<InterfaceObjectType> interfaces) {
        if (! definedType.isInterface()) {
            throw new IllegalArgumentException("Not an interface");
        }
        return new InterfaceObjectType(this, definedType, interfaces);
    }

    PointerType createPointer(ValueType type, int addrSpace) {
        return new PointerType(this, type, false, false, pointerSize(addrSpace), addrSpace);
    }

    ReferenceType createReference(PhysicalObjectType objectType, Set<InterfaceObjectType> interfaceBounds) {
        return new ReferenceType(this, objectType, interfaceBounds, referenceAlign);
    }

    ReferenceArrayObjectType createReferenceArrayObject(final ObjectType elementType) {
        return new ReferenceArrayObjectType(this, objectClass, elementType);
    }

    PrimitiveArrayObjectType createPrimitiveArrayObject(final WordType elementType) {
        return new PrimitiveArrayObjectType(this, objectClass, elementType);
    }

    TypeIdType createTypeType(final ValueType upperBound) {
        return new TypeIdType(this, upperBound);
    }

    public static Builder builder() {
        return new Builder() {
            int byteBits = 8;
            ImmutableIntIntMap pointerSizes = IntIntMaps.immutable.of(0, 8);
            ImmutableIntIntMap pointerAlignments = IntIntMaps.immutable.of(0, 8);
            int functionAlignment = 1;
            boolean signedChar = true;
            int boolSize = 1;
            int boolAlignment = 1;
            int int8Size = 1;
            int int8Alignment = 1;
            int int16Size = 2;
            int int16Alignment = 2;
            int int32Size = 4;
            int int32Alignment = 4;
            int int64Size = 8;
            int int64Alignment = 8;
            int float32Size = 4;
            int float32Alignment = 4;
            int float64Size = 8;
            int float64Alignment = 8;
            int typeIdSize = 4;
            int typeIdAlignment = 4;
            int referenceSize = 4;
            int referenceAlignment = 4;
            int maxAlignment = 16;
            ByteOrder endianness = ByteOrder.nativeOrder();

            public int getByteBits() {
                return byteBits;
            }

            public void setByteBits(final int byteBits) {
                Assert.checkMinimumParameter("byteBits", 8, byteBits);
                this.byteBits = byteBits;
            }

            public ImmutableIntIntMap getPointerSizes() {
                return pointerSizes;
            }

            public void setPointerSizes(final ImmutableIntIntMap pointerSizes) {
                Assert.checkNotNullParam("pointerSizes", pointerSizes);
                this.pointerSizes = pointerSizes;
            }

            public ImmutableIntIntMap getPointerAlignments() {
                return pointerAlignments;
            }

            public void setPointerAlignments(final ImmutableIntIntMap pointerAlignments) {
                Assert.checkNotNullParam("pointerAlignments", pointerAlignments);
                this.pointerAlignments = pointerAlignments;
            }

            public void setMaxAlignment(final int maxAlignment) {
                TypeUtil.checkAlignmentParameter("maxAlignment", maxAlignment);
                this.maxAlignment = maxAlignment;
            }

            public int getMaxAlignment() {
                return maxAlignment;
            }

            public int getFunctionAlignment() {
                return functionAlignment;
            }

            public void setFunctionAlignment(final int functionAlignment) {
                TypeUtil.checkAlignmentParameter("functionAlignment", functionAlignment);
                this.functionAlignment = functionAlignment;
            }

            public int getInt8Size() {
                return int8Size;
            }

            public void setInt8Size(final int size) {
                Assert.checkMinimumParameter("size", 1, size);
                int8Size = size;
            }

            public int getInt16Size() {
                return int16Size;
            }

            public void setInt16Size(final int size) {
                Assert.checkMinimumParameter("size", 1, size);
                int16Size = size;
            }

            public int getInt32Size() {
                return int32Size;
            }

            public void setInt32Size(final int size) {
                Assert.checkMinimumParameter("size", 1, size);
                int32Size = size;
            }

            public int getInt64Size() {
                return int64Size;
            }

            public void setInt64Size(final int size) {
                Assert.checkMinimumParameter("size", 1, size);
                int64Size = size;
            }

            public int getBoolSize() {
                return boolSize;
            }

            public void setBoolSize(final int size) {
                Assert.checkMinimumParameter("size", 1, size);
                boolSize = size;
            }

            public int getFloat32Size() {
                return float32Size;
            }

            public void setFloat32Size(final int size) {
                Assert.checkMinimumParameter("size", 1, size);
                float32Size = size;
            }

            public int getFloat64Size() {
                return float64Size;
            }

            public void setFloat64Size(final int size) {
                Assert.checkMinimumParameter("size", 1, size);
                float64Size = size;
            }

            public int getTypeIdSize() {
                return typeIdSize;
            }

            public void setTypeIdSize(final int size) {
                Assert.checkMinimumParameter("size", 1, size);
                this.typeIdSize = size;
            }

            public int getBoolAlignment() {
                return boolAlignment;
            }

            public void setBoolAlignment(final int alignment) {
                TypeUtil.checkAlignmentParameter("alignment", alignment);
                boolAlignment = alignment;
            }

            public int getInt8Alignment() {
                return int8Alignment;
            }

            public void setInt8Alignment(final int alignment) {
                TypeUtil.checkAlignmentParameter("alignment", alignment);
                int8Alignment = alignment;
            }

            public int getInt16Alignment() {
                return int16Alignment;
            }

            public void setInt16Alignment(final int alignment) {
                TypeUtil.checkAlignmentParameter("alignment", alignment);
                int16Alignment = alignment;
            }

            public int getInt32Alignment() {
                return int32Alignment;
            }

            public void setInt32Alignment(final int alignment) {
                TypeUtil.checkAlignmentParameter("alignment", alignment);
                int32Alignment = alignment;
            }

            public int getInt64Alignment() {
                return int64Alignment;
            }

            public void setInt64Alignment(final int alignment) {
                TypeUtil.checkAlignmentParameter("alignment", alignment);
                int64Alignment = alignment;
            }

            public int getFloat32Alignment() {
                return float32Alignment;
            }

            public void setFloat32Alignment(final int alignment) {
                TypeUtil.checkAlignmentParameter("alignment", alignment);
                float32Alignment = alignment;
            }

            public int getFloat64Alignment() {
                return float64Alignment;
            }

            public void setFloat64Alignment(final int alignment) {
                TypeUtil.checkAlignmentParameter("alignment", alignment);
                float64Alignment = alignment;
            }

            public int getTypeIdAlignment() {
                return typeIdAlignment;
            }

            public void setTypeIdAlignment(final int alignment) {
                TypeUtil.checkAlignmentParameter("alignment", alignment);
                typeIdAlignment = alignment;
            }

            public int getReferenceSize() {
                return referenceSize;
            }

            public void setReferenceSize(final int size) {
                Assert.checkMinimumParameter("size", 1, size);
                this.referenceSize = size;
            }

            public int getReferenceAlignment() {
                return referenceAlignment;
            }

            public void setReferenceAlignment(final int alignment) {
                TypeUtil.checkAlignmentParameter("alignment", alignment);
                this.referenceAlignment = alignment;
            }

            public ByteOrder getEndianness() {
                return endianness;
            }

            public void setEndianness(ByteOrder endianness) {
                this.endianness = endianness;
            }

            public TypeSystem build() {
                return new TypeSystem(this);
            }

            public boolean isSignedChar() {
                return signedChar;
            }

            public void setSignedChar(boolean signedChar) {
                this.signedChar = signedChar;
            }
        };
    }

    /**
     * The type system builder interface.
     */
    public interface Builder {
        int getByteBits();

        void setByteBits(int byteBits);

        ImmutableIntIntMap getPointerSizes();

        void setPointerSizes(ImmutableIntIntMap pointerSizes);

        ImmutableIntIntMap getPointerAlignments();

        void setPointerAlignments(ImmutableIntIntMap pointerAlignments);

        int getMaxAlignment();

        void setMaxAlignment(int maxAlignment);

        int getFunctionAlignment();

        void setFunctionAlignment(int functionAlignment);

        int getInt8Size();

        void setInt8Size(int size);

        int getInt16Size();

        void setInt16Size(int size);

        int getInt32Size();

        void setInt32Size(int size);

        int getInt64Size();

        void setInt64Size(int size);

        int getBoolSize();

        void setBoolSize(int size);

        int getFloat32Size();

        void setFloat32Size(int size);

        int getFloat64Size();

        void setFloat64Size(int size);

        int getTypeIdSize();

        void setTypeIdSize(int size);

        int getBoolAlignment();

        void setBoolAlignment(int alignment);

        int getInt8Alignment();

        void setInt8Alignment(int alignment);

        int getInt16Alignment();

        void setInt16Alignment(int alignment);

        int getInt32Alignment();

        void setInt32Alignment(int alignment);

        int getInt64Alignment();

        void setInt64Alignment(int alignment);

        int getFloat32Alignment();

        void setFloat32Alignment(int alignment);

        int getFloat64Alignment();

        void setFloat64Alignment(int alignment);

        int getTypeIdAlignment();

        void setTypeIdAlignment(int alignment);

        int getReferenceSize();

        void setReferenceSize(int size);

        int getReferenceAlignment();

        void setReferenceAlignment(int alignment);

        ByteOrder getEndianness();

        void setEndianness(ByteOrder endianness);

        boolean isSignedChar();

        void setSignedChar(boolean signedChar);

        TypeSystem build();
    }

    @SuppressWarnings("serial")
    static final class TypeCache<T> extends ConcurrentHashMap<ValueType, TypeCache<T>> {
        static final VarHandle valueHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "value", VarHandle.class, TypeCache.class, Object.class);

        TypeCache() {
        }

        TypeCache(Object ignored) {
        }

        volatile T value;

        public T getValue() {
            return value;
        }

        public boolean compareAndSet(T expect, T update) {
            return valueHandle.compareAndSet(this, expect, update);
        }
    }
}
