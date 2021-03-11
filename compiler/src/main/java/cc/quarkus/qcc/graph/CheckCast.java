package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * Validate the soundness of a casting operation on a reference type, raising a runtime exception if unsound.
 * This node is used to implement two distinct kinds of dynamic checks:
 * arraystorechecks and checkcasts.
 */
public final class CheckCast extends AbstractValue implements CastValue {
    private final Value input;
    /**
     * If the kind is Cast, then narrowInput can be a typeLiteral (checkcast bytecode),
     * or a reference to a java.lang.Class instance (Class.cast intrinsic).
     *
     * If the kind is ArrayStore, then narrowInput will be a reference to the array
     * in which input is being stored.
     */
    private final Value narrowInput;
    private final ReferenceType type;
    private final CastType kind;

    public enum CastType {
        ArrayStore, Cast;

        public String toString() {
            return this.equals(ArrayStore) ? "storecheck" : "checkcast";
        }
    }

    CheckCast(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value input, final Value narrowInput, ReferenceType type, CastType kind) {
        super(callSite, element, line, bci);
        this.input = input;
        this.narrowInput = narrowInput;
        this.type = type;
        this.kind = kind;
    }

    public Value getInput() {
        return input;
    }

    public Value getNarrowInput() {
        return narrowInput;
    }

    public ReferenceType getType() {
        return type;
    }

    public CastType getKind() {
        return kind;
    }

    public int getValueDependencyCount() {
        return 2;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? input : index == 1 ? narrowInput : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(CheckCast.class, input, narrowInput, kind, type);
    }

    public boolean equals(final Object other) {
        return other instanceof CheckCast && equals((CheckCast) other);
    }

    public boolean equals(final CheckCast other) {
        return this == other || other != null
            && input.equals(other.input)
            && narrowInput.equals(other.narrowInput)
            && kind.equals(other.kind)
            && type.equals(other.type);
    }
}
