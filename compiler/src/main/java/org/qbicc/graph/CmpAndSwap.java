package org.qbicc.graph;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
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
    private final MemoryAtomicityMode successAtomicityMode;
    private final MemoryAtomicityMode failureAtomicityMode;

    CmpAndSwap(final Node callSite, final ExecutableElement element, final int line, final int bci, final CompoundType resultType, final Node dependency, final ValueHandle target, final Value expectedValue, final Value updateValue, final MemoryAtomicityMode successAtomicityMode, final MemoryAtomicityMode failureAtomicityMode) {
        super(callSite, element, line, bci);
        this.resultType = resultType;
        this.dependency = dependency;
        this.target = target;
        this.expectedValue = expectedValue;
        this.updateValue = updateValue;
        this.successAtomicityMode = successAtomicityMode;
        this.failureAtomicityMode = failureAtomicityMode;
        if (! target.isWritable()) {
            throw new IllegalArgumentException("Handle is not writable");
        }
        if (! target.isReadable()) {
            throw new IllegalArgumentException("Handle is not readable");
        }

        /* expected and update value must have the same type, target must be a pointer to that type. */
        if (!expectedValue.getType().equals(updateValue.getType())
             || !expectedValue.getType().equals(target.getPointerType().getPointeeType())
        ) {
            throw new IllegalArgumentException("The target, expected and new value types must agree.");
        };

        /* ordering params must be at least MONOTONIC */
        if ((0 > successAtomicityMode.compareTo(MemoryAtomicityMode.MONOTONIC))
            || (0 > failureAtomicityMode.compareTo(MemoryAtomicityMode.MONOTONIC))
        ) {
            throw new IllegalArgumentException("Mode must be at least monotonic");
        }
        /* volatile does not map to an ordering constraint */
        if (successAtomicityMode.equals(MemoryAtomicityMode.VOLATILE) || failureAtomicityMode.equals(MemoryAtomicityMode.VOLATILE)) {
            throw new IllegalArgumentException("volatile does not map to a LLVM ordering constraint");
        }
        /* failure ordering must not be RELEASE or ACQUIRE_RELEASE */
        if (failureAtomicityMode.equals(MemoryAtomicityMode.RELEASE) || failureAtomicityMode.equals(MemoryAtomicityMode.ACQUIRE_RELEASE)) {
            throw new IllegalArgumentException("Failure mode cannot be release or acquire_release");
        }

    }

    int calcHashCode() {
        return Objects.hash(CmpAndSwap.class, dependency, target, expectedValue, updateValue, resultType, successAtomicityMode, failureAtomicityMode);
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

    public MemoryAtomicityMode getSuccessAtomicityMode() {
        return successAtomicityMode;
    }

    public MemoryAtomicityMode getFailureAtomicityMode() {
        return failureAtomicityMode;
    }

    public boolean equals(final Object other) {
        return other instanceof CmpAndSwap && equals((CmpAndSwap) other);
    }

    public boolean equals(final CmpAndSwap other) {
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target)
            && expectedValue.equals(other.expectedValue) && updateValue.equals(other.updateValue)
            && resultType.equals(other.resultType) && successAtomicityMode == other.successAtomicityMode
            && failureAtomicityMode == other.failureAtomicityMode;
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
            compoundType = ts.getCompoundType(CompoundType.Tag.NONE, null, -1, -1, () -> List.of(ts.getCompoundTypeMember(null, valueType, -1, -1), ts.getCompoundTypeMember(null, ts.getBooleanType(), -1, -1)));
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
}
