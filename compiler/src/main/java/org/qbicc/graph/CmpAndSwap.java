package org.qbicc.graph;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.type.StructType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;

/**
 *
 */
public final class CmpAndSwap extends AbstractValue implements OrderedNode {
    private static final AttachmentKey<Map<ValueType, StructType>> RESULT_TYPE_MAP_KEY = new AttachmentKey<>();

    private final Node dependency;
    private final Value pointer;
    private final Value expectedValue;
    private final Value updateValue;
    private final StructType resultType;
    private final ReadAccessMode readAccessMode;
    private final WriteAccessMode writeAccessMode;
    private final Strength strength;

    CmpAndSwap(final ProgramLocatable pl, final StructType resultType, final Node dependency, final Value pointer, final Value expectedValue, final Value updateValue, final ReadAccessMode readAccessMode, final WriteAccessMode writeAccessMode, Strength strength) {
        super(pl);
        this.resultType = Assert.checkNotNullParam("resultType", resultType);
        this.dependency = Assert.checkNotNullParam("dependency", dependency);
        this.pointer = Assert.checkNotNullParam("pointer", pointer);
        this.expectedValue = Assert.checkNotNullParam("expectedValue", expectedValue);
        this.updateValue = Assert.checkNotNullParam("updateValue", updateValue);
        this.readAccessMode = Assert.checkNotNullParam("readAccessMode", readAccessMode);
        this.writeAccessMode = Assert.checkNotNullParam("writeAccessMode", writeAccessMode);
        this.strength = Assert.checkNotNullParam("strength", strength);
        if (! pointer.isWritable()) {
            throw new IllegalArgumentException("Handle is not writable");
        }
        if (! pointer.isReadable()) {
            throw new IllegalArgumentException("Handle is not readable");
        }

        ValueType targetType = pointer.getPointeeType();
        /* expected and update value both be assignable to the handle. */
        if (!(expectedValue instanceof NullLiteral || targetType.isImplicitlyConvertibleFrom(expectedValue.getType()))) {
            throw new IllegalArgumentException("The target and expected value types must agree.");
        }
        if (!(updateValue instanceof NullLiteral || targetType.isImplicitlyConvertibleFrom(updateValue.getType()))) {
            throw new IllegalArgumentException("The target and update value types must agree.");
        }
    }

    int calcHashCode() {
        return Objects.hash(CmpAndSwap.class, dependency, pointer, expectedValue, updateValue, resultType, readAccessMode, writeAccessMode);
    }

    @Override
    String getNodeName() {
        return "CmpAndSwap";
    }

    public StructType getType() {
        return resultType;
    }

    public Node getDependency() {
        return dependency;
    }

    public Value getPointer() {
        return pointer;
    }

    public Value getExpectedValue() {
        return expectedValue;
    }

    public Value getUpdateValue() {
        return updateValue;
    }

    public ReadAccessMode getReadAccessMode() {
        return readAccessMode;
    }

    public WriteAccessMode getWriteAccessMode() {
        return writeAccessMode;
    }

    public Strength getStrength() {
        return strength;
    }

    public boolean equals(final Object other) {
        return other instanceof CmpAndSwap && equals((CmpAndSwap) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        expectedValue.toReferenceString(b);
        b.append(',');
        updateValue.toReferenceString(b);
        b.append(',');
        b.append(strength);
        b.append(',');
        b.append(readAccessMode);
        b.append(',');
        b.append(writeAccessMode);
        b.append(')');
        return b;
    }

    public boolean equals(final CmpAndSwap other) {
        return this == other || other != null && dependency.equals(other.dependency) && pointer.equals(other.pointer)
            && expectedValue.equals(other.expectedValue) && updateValue.equals(other.updateValue)
            && resultType.equals(other.resultType) && readAccessMode == other.readAccessMode
            && writeAccessMode == other.writeAccessMode && strength == other.strength;
    }

    public int getValueDependencyCount() {
        return 3;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> pointer;
            case 1 -> expectedValue;
            case 2 -> updateValue;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isConstant() {
        // we could possibly make this more exact in the future
        return false;
    }

    public static StructType getResultType(CompilationContext ctxt, ValueType valueType) {
        Map<ValueType, StructType> map = ctxt.getAttachment(RESULT_TYPE_MAP_KEY);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            Map<ValueType, StructType> appearing = ctxt.putAttachmentIfAbsent(RESULT_TYPE_MAP_KEY, map);
            if (appearing != null) {
                map = appearing;
            }
        }
        StructType structType = map.get(valueType);
        if (structType == null) {
            TypeSystem ts = ctxt.getTypeSystem();
            structType = StructType.builder(ts)
                .setTag(StructType.Tag.NONE)
                .setName(null)
                .addNextMember(valueType)
                .addNextMember(ts.getBooleanType())
                .setOverallAlignment(1)
                .build();
            StructType appearing = map.putIfAbsent(valueType, structType);
            if (appearing != null) {
                structType = appearing;
            }
        }
        return structType;
    }

    /**
     * Original value found in the CAS target
     * @return value found in CAS target
     */
    public StructType.Member getResultValueType() {
        return this.resultType.getMember(0);
    }

    /**
     * Result flag where true indicates success, if the operation is marked as strong (default).
     * @return result flag
     */
    public StructType.Member getResultFlagType() {
        return this.resultType.getMember(1);
    }

    public enum Strength {
        WEAK,
        STRONG,
        ;
    }
}
