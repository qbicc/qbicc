package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InstanceMethodElement;

/**
 * A value that represents a lookup of a method's pointer (virtual or interface).
 */
public abstract class MethodLookup extends AbstractValue {
    private final InstanceMethodElement lookupMethod;
    private final Value instanceTypeId;

    MethodLookup(Node callSite, ExecutableElement element, int line, int bci, InstanceMethodElement lookupMethod, Value instanceTypeId) {
        super(callSite, element, line, bci);
        this.lookupMethod = lookupMethod;
        this.instanceTypeId = instanceTypeId;
    }

    /**
     * Get the type of this node, which is "pointer-to-{@code getMethodType()}".
     *
     * @return the type of this node (not {@code null})
     */
    @Override
    public PointerType getType() {
        return getMethodType().getPointer();
    }

    /**
     * Get the type of the method being resolved.
     *
     * @return the type of the method being resolved (not {@code null})
     */
    public InstanceMethodType getMethodType() {
        return getLookupMethod().getType();
    }

    /**
     * Get the lookup method.
     * This is the base type method to resolve.
     *
     * @return the lookup method (not {@code null})
     */
    public InstanceMethodElement getLookupMethod() {
        return lookupMethod;
    }

    public Value getInstanceTypeId() {
        return instanceTypeId;
    }

    @Override
    public boolean isConstant() {
        return instanceTypeId.isConstant();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MethodLookup ml && equals(ml);
    }

    public boolean equals(MethodLookup other) {
        return this == other || other != null && lookupMethod == other.lookupMethod && getLookupContext() == other.getLookupContext();
    }

    /**
     * Get the lookup context for this lookup.
     * Some methods (e.g. {@code private} methods) are only visible from the nestmates of the target class.
     *
     * @return the lookup context
     */
    public DefinedTypeDefinition getLookupContext() {
        return getElement().getEnclosingType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(getLookupContext(), lookupMethod, instanceTypeId);
    }
}
