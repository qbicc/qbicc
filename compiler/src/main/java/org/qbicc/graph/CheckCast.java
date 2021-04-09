package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Validate the soundness of a casting operation on a reference type, raising a runtime exception if unsound.
 * This node is used to implement two distinct kinds of dynamic checks:
 * arraystorechecks and checkcasts.
 */
public final class CheckCast extends AbstractValue implements CastValue {
    /**
     * The input value, which must be of type ReferenceType
     */
    private final Value input;
    /**
     * The type to cast to (may be a TypeLiteral).
     * The upper bound of the type must be ClassObjectType, PrimitiveArrayObjectType, or InterfaceObjectType.
     */
    private final Value toType;
    /**
     * The number of array dimensions (may be an IntegerLiteral, must be zero if the argument type is not a reference array).
     */
    private final Value toDimensions;
    /**
     * The type of the value produced by the CheckCast node (when an exception is not raised).
     */
    private final ReferenceType type;
    /**
     * The kind of exception to throw on cast failure
     */
    private final CastType kind;

    public enum CastType {
        ArrayStore, Cast;

        public String toString() {
            return this.equals(ArrayStore) ? "storecheck" : "checkcast";
        }
    }

    CheckCast(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value input, final Value toType,
              final Value toDimensions, CastType kind, ReferenceType type) {
        super(callSite, element, line, bci);
        this.input = input;
        this.toType = toType;
        this.toDimensions = toDimensions;
        this.type = type;
        this.kind = kind;
    }

    public Value getInput() {
        return input;
    }

    public Value getToType() {
        return toType;
    }

    public Value getToDimensions() {
        return toDimensions;
    }

    public ReferenceType getType() {
        return type;
    }

    public CastType getKind() {
        return kind;
    }

    public int getValueDependencyCount() {
        return 3;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        switch(index) {
            case 0: return input;
            case 1: return toType;
            case 2: return toDimensions;
            default: return Util.throwIndexOutOfBounds(index);
        }
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(CheckCast.class, input, toType, toDimensions, kind, type);
    }

    public boolean equals(final Object other) {
        return other instanceof CheckCast && equals((CheckCast) other);
    }

    public boolean equals(final CheckCast other) {
        return this == other || other != null
            && input.equals(other.input)
            && toType.equals(other.toType)
            && toDimensions.equals(other.toDimensions)
            && kind.equals(other.kind)
            && type.equals(other.type);
    }
}
