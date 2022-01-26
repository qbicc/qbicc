package org.qbicc.graph;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class CmpAndSwap extends AbstractValue implements OrderedNode {
    private static final AttachmentKey<Map<ValueType, CompoundType>> RESULT_TYPE_MAP_KEY = new AttachmentKey<>();

    private final Node dependency;
    private final ValueHandle target;
    private final Value expectedValue;
    private final Value updateValue;
    private final CompoundType resultType;
    private final ReadAccessMode readAccessMode;
    private final WriteAccessMode writeAccessMode;
    private final Strength strength;

    CmpAndSwap(final Node callSite, final ExecutableElement element, final int line, final int bci, final CompoundType resultType, final Node dependency, final ValueHandle target, final Value expectedValue, final Value updateValue, final ReadAccessMode readAccessMode, final WriteAccessMode writeAccessMode, Strength strength) {
        super(callSite, element, line, bci);
        this.resultType = Assert.checkNotNullParam("resultType", resultType);
        this.dependency = Assert.checkNotNullParam("dependency", dependency);
        this.target = Assert.checkNotNullParam("target", target);
        this.expectedValue = Assert.checkNotNullParam("expectedValue", expectedValue);
        this.updateValue = Assert.checkNotNullParam("updateValue", updateValue);
        this.readAccessMode = Assert.checkNotNullParam("readAccessMode", readAccessMode);
        this.writeAccessMode = Assert.checkNotNullParam("writeAccessMode", writeAccessMode);
        this.strength = Assert.checkNotNullParam("strength", strength);
        if (! target.isWritable()) {
            throw new IllegalArgumentException("Handle is not writable");
        }
        if (! target.isReadable()) {
            throw new IllegalArgumentException("Handle is not readable");
        }

        ValueType targetType = target.getValueType();
        /* expected and update value both be assignable to the handle. */
        if (!(expectedValue instanceof NullLiteral || targetType.isImplicitlyConvertibleFrom(expectedValue.getType()))) {
            throw new IllegalArgumentException("The target and expected value types must agree.");
        }
        if (!(updateValue instanceof NullLiteral || targetType.isImplicitlyConvertibleFrom(updateValue.getType()))) {
            throw new IllegalArgumentException("The target and update value types must agree.");
        }
    }

    int calcHashCode() {
        return Objects.hash(CmpAndSwap.class, dependency, target, expectedValue, updateValue, resultType, readAccessMode, writeAccessMode);
    }

    @Override
    String getNodeName() {
        return "CmpAndSwap";
    }

    public CompoundType getType() {
        return resultType;
    }

    public Node getDependency() {
        return dependency;
    }

    public ValueHandle getValueHandle() {
        return target;
    }

    public boolean hasValueHandleDependency() {
        return true;
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
        b.append(expectedValue);
        b.append(',');
        b.append(updateValue);
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
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target)
            && expectedValue.equals(other.expectedValue) && updateValue.equals(other.updateValue)
            && resultType.equals(other.resultType) && readAccessMode == other.readAccessMode
            && writeAccessMode == other.writeAccessMode && strength == other.strength;
    }

    public int getValueDependencyCount() {
        return 2;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? expectedValue : index == 1 ? updateValue : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isConstant() {
        // we could possibly make this more exact in the future
        return false;
    }

    public static CompoundType getResultType(CompilationContext ctxt, ValueType valueType) {
        Map<ValueType, CompoundType> map = ctxt.getAttachment(RESULT_TYPE_MAP_KEY);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            Map<ValueType, CompoundType> appearing = ctxt.putAttachmentIfAbsent(RESULT_TYPE_MAP_KEY, map);
            if (appearing != null) {
                map = appearing;
            }
        }
        CompoundType compoundType = map.get(valueType);
        if (compoundType == null) {
            TypeSystem ts = ctxt.getTypeSystem();
            compoundType = CompoundType.builder(ts)
                .setTag(CompoundType.Tag.NONE)
                .setName(null)
                .addNextMember(valueType)
                .addNextMember(ts.getBooleanType())
                .setOverallAlignment(1)
                .build();
            CompoundType appearing = map.putIfAbsent(valueType, compoundType);
            if (appearing != null) {
                compoundType = appearing;
            }
        }
        return compoundType;
    }

    /**
     * Original value found in the CAS target
     * @return value found in CAS target
     */
    public CompoundType.Member getResultValueType() {
        return this.resultType.getMember(0);
    }

    /**
     * Result flag where true indicates success, if the operation is marked as strong (default).
     * @return result flag
     */
    public CompoundType.Member getResultFlagType() {
        return this.resultType.getMember(1);
    }

    public enum Strength {
        WEAK,
        STRONG,
        ;
    }
}
