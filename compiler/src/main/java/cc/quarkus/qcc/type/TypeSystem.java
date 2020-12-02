package cc.quarkus.qcc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import io.smallrye.common.constraint.Assert;

/**
 * The type system of the target machine and VM configuration.
 */
public final class TypeSystem {
    private final int byteBits;
    private final int pointerSize;
    private final int pointerAlign;
    private final int funcAlign;
    private final int referenceSize;
    private final int referenceAlign;
    private final PoisonType poisonType = new PoisonType(this);
    private final VoidType voidType = new VoidType(this, false);
    private final NullType nullType = new NullType(this);
    private final BlockType blockType = new BlockType(this);
    private final StringType stringType = new StringType(this);
    private final BooleanType booleanType;
    private final TypeIdType typeIdType;
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
    private final Map<TypeIdLiteral, ReferenceType> referenceTypeCache = new ConcurrentHashMap<>();
    private final TypeCache<FunctionType> functionTypeCache = new TypeCache<>();

    TypeSystem(final Builder builder) {
        int byteBits = builder.getByteBits();
        this.byteBits = byteBits;
        pointerSize = builder.getPointerSize();
        pointerAlign = builder.getPointerAlignment();
        funcAlign = builder.getPointerAlignment();
        referenceSize = builder.getReferenceSize();
        referenceAlign = builder.getReferenceAlignment();
        booleanType = new BooleanType(this, builder.getBoolSize(), builder.getBoolAlignment(), false);
        typeIdType = new TypeIdType(this, builder.getTypeIdSize(), builder.getTypeIdAlignment(), false);
        int float32Size = builder.getFloat32Size();
        if (float32Size * byteBits < 32) {
            throw typeTooSmall("float32");
        }
        float32Type = new FloatType(this, float32Size, 32, builder.getFloat32Alignment(), false);
        int float64Size = builder.getFloat64Size();
        if (float64Size * byteBits < 64) {
            throw typeTooSmall("float64");
        }
        float64Type = new FloatType(this, float64Size, 64, builder.getFloat64Alignment(), false);
        // always OK
        signedInteger8Type = new SignedIntegerType(this, builder.getInt8Size(), builder.getInt8Alignment(), 8, false);
        unsignedInteger8Type = new UnsignedIntegerType(this, builder.getInt8Size(), builder.getInt8Alignment(), 8, false);
        int int16Size = builder.getInt16Size();
        if (int16Size * byteBits < 16) {
            throw typeTooSmall("int16");
        }
        signedInteger16Type = new SignedIntegerType(this, builder.getInt16Size(), builder.getInt16Alignment(), 16, false);
        unsignedInteger16Type = new UnsignedIntegerType(this, builder.getInt16Size(), builder.getInt16Alignment(), 16, false);
        int int32Size = builder.getInt32Size();
        if (int32Size * byteBits < 32) {
            throw typeTooSmall("int32");
        }
        signedInteger32Type = new SignedIntegerType(this, builder.getInt32Size(), builder.getInt32Alignment(), 32, false);
        unsignedInteger32Type = new UnsignedIntegerType(this, builder.getInt32Size(), builder.getInt32Alignment(), 32, false);
        int int64Size = builder.getInt64Size();
        if (int64Size * byteBits < 64) {
            throw typeTooSmall("int64");
        }
        signedInteger64Type = new SignedIntegerType(this, builder.getInt64Size(), builder.getInt64Alignment(), 64, false);
        unsignedInteger64Type = new UnsignedIntegerType(this, builder.getInt64Size(), builder.getInt64Alignment(), 64, false);
    }

    private static IllegalArgumentException typeTooSmall(String name) {
        return new IllegalArgumentException("Type " + name + " does not contain enough bits");
    }

    public VoidType getVoidType() {
        return voidType;
    }

    public PoisonType getPoisonType() {
        return poisonType;
    }

    public NullType getNullType() {
        return nullType;
    }

    public BooleanType getBooleanType() {
        return booleanType;
    }

    public int getReferenceSize() {
        return referenceSize;
    }

    public StringType getStringType() {
        return stringType;
    }

    public ReferenceType getReferenceType(TypeIdLiteral typeId) {
        Assert.checkNotNullParam("typeId", typeId);
        return referenceTypeCache.computeIfAbsent(typeId, id -> new ReferenceType(this, id, false, referenceSize, referenceAlign, false));
    }

    /**
     * Get the pointer size for this type system.
     *
     * @return the size of a pointer
     */
    public int getPointerSize() {
        return pointerSize;
    }

    /**
     * Get the pointer alignment for this type system.
     *
     * @return the pointer alignment for this type system
     */
    public int getPointerAlignment() {
        return pointerAlign;
    }

    /**
     * Get the function alignment for this type system.
     *
     * @return the function alignment for this type system
     */
    public int getFunctionAlignment() {
        return funcAlign;
    }

    public CompoundType.Member getCompoundTypeMember(String name, ValueType type, int offset, int align) {
        Assert.checkNotNullParam("name", name);
        if (Integer.bitCount(align) != 1) {
            throw new IllegalArgumentException("Invalid alignment (must be a power of 2; specify 1 for minimum alignment)");
        }
        align = Math.max(type.getAlign(), align);
        Assert.checkMinimumParameter("offset", 0, offset);
        if ((offset & (align - 1)) != 0) {
            throw new IllegalArgumentException("Invalid offset (not sufficiently aligned)");
        }
        return new CompoundType.Member(name, type, offset, align);
    }

    public FunctionType getFunctionType(ValueType returnType, ValueType... argTypes) {
        Assert.checkNotNullParam("returnType", returnType);
        TypeCache<FunctionType> current = functionTypeCache.computeIfAbsent(returnType, TypeCache::new);
        for (int i = 0; i < argTypes.length; i++) {
            Assert.checkNotNullArrayParam("argTypes", i, argTypes);
            final ValueType argType = argTypes[i];
            if (! argType.isComplete()) {
                throw new IllegalArgumentException("Function argument types must be complete");
            }
            current = current.computeIfAbsent(argType, TypeCache::new);
        }
        FunctionType functionType = current.getValue();
        if (functionType == null) {
            functionType = new FunctionType(this, returnType, argTypes);
            while (! current.compareAndSet(null, functionType)) {
                FunctionType appearing = current.getValue();
                if (appearing != null) {
                    return appearing;
                }
            }
        }
        return functionType;
    }

    public CompoundType getCompoundType(final CompoundType.Tag tag, String name, long size, int align, CompoundType.Member... members) {
        Assert.checkNotNullParam("tag", tag);
        Assert.checkNotNullParam("name", name);
        Assert.checkMinimumParameter("size", 1, size);
        TypeUtil.checkAlignmentParameter("align", align);
        return new CompoundType(this, tag, name, members, size, align, false);
    }

    public ArrayType getArrayType(ValueType memberType, long elements) {
        Assert.checkNotNullParam("memberType", memberType);
        Assert.checkMinimumParameter("elements", 0, elements);
        if (! memberType.isComplete()) {
            throw new IllegalArgumentException("Arrays of incomplete type are not allowed");
        }
        return new ArrayType(this, memberType, elements);
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
     * Get the type of identifier for class, interface, or array references.
     *
     * @return the class identifier type
     */
    public TypeIdType getTypeIdType() {
        return typeIdType;
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

    PointerType createPointer(Type type) {
        return new PointerType(this, type, false, false);
    }

    public static Builder builder() {
        return new Builder() {
            int byteBits = 8;
            int pointerSize = 8;
            int pointerAlignment = 8;
            int functionAlignment = 1;
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

            public int getByteBits() {
                return byteBits;
            }

            public void setByteBits(final int byteBits) {
                Assert.checkMinimumParameter("byteBits", 8, byteBits);
                this.byteBits = byteBits;
            }

            public int getPointerSize() {
                return pointerSize;
            }

            public void setPointerSize(final int pointerSize) {
                Assert.checkMinimumParameter("pointerSize", 1, pointerSize);
                this.pointerSize = pointerSize;
            }

            public int getPointerAlignment() {
                return pointerAlignment;
            }

            public void setPointerAlignment(final int pointerAlignment) {
                TypeUtil.checkAlignmentParameter("pointerAlignment", pointerAlignment);
                this.pointerAlignment = pointerAlignment;
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

            public TypeSystem build() {
                return new TypeSystem(this);
            }
        };
    }

    /**
     * The type system builder interface.
     */
    public interface Builder {
        int getByteBits();

        void setByteBits(int byteBits);

        int getPointerSize();

        void setPointerSize(int pointerSize);

        int getPointerAlignment();

        void setPointerAlignment(int pointerAlignment);

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

        @SuppressWarnings("unchecked")
        public T putValue(T newValue) {
            return (T) valueHandle.getAndSet(this, value);
        }

        public boolean compareAndSet(T expect, T update) {
            return valueHandle.compareAndSet(this, expect, update);
        }
    }
}
