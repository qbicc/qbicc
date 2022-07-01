package org.qbicc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

import org.qbicc.type.definition.DefinedTypeDefinition;

/**
 * An object type whose elements are primitive type values.
 * There are exactly 8 such object types in Java: [Z, [B, [C, [S, [I, [J, [F, [D.
 */
public final class PrimitiveArrayObjectType extends ArrayObjectType {
    private static final VarHandle definitionHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "definition", VarHandle.class, PrimitiveArrayObjectType.class, DefinedTypeDefinition.class);

    private final WordType elementType;
    private volatile DefinedTypeDefinition definition;

    PrimitiveArrayObjectType(final TypeSystem typeSystem, final ClassObjectType objectClass, final WordType elementType) {
        super(typeSystem, Objects.hash(elementType), objectClass);
        this.elementType = elementType;
    }

    public DefinedTypeDefinition getDefinition() {
        DefinedTypeDefinition definition = this.definition;
        if (definition == null) {
            return super.getDefinition();
        }
        return definition;
    }

    public void initializeDefinition(DefinedTypeDefinition definition) {
        if (! definitionHandle.compareAndSet(this, null, definition)) {
            throw new IllegalStateException("Type definition set twice");
        }
    }

    public long getSize() throws IllegalStateException {
        return 0;
    }

    public boolean isSubtypeOf(final ObjectType other) {
        return super.isSubtypeOf(other)
            || other instanceof PrimitiveArrayObjectType && isSubtypeOf((PrimitiveArrayObjectType) other);
    }

    public boolean isSubtypeOf(final PrimitiveArrayObjectType other) {
        return this == other;
    }

    public WordType getElementType() {
        return elementType;
    }

    public ObjectType getCommonSupertype(final ObjectType other) {
        return equals(other) ? this : super.getCommonSupertype(other);
    }

    @Override
    public final boolean equals(ObjectType other) {
        return other instanceof PrimitiveArrayObjectType && equals((PrimitiveArrayObjectType) other);
    }

    public boolean equals(PrimitiveArrayObjectType other) {
        return super.equals(other) && elementType.equals(other.elementType);
    }
}
