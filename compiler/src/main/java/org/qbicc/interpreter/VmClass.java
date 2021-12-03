package org.qbicc.interpreter;

import java.util.List;

import org.qbicc.graph.literal.Literal;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public interface VmClass extends VmObject {

    LoadedTypeDefinition getTypeDefinition();

    VmClass getSuperClass();

    List<? extends VmClass> getInterfaces();

    VmArray getDeclaredFields(boolean publicOnly);

    VmArray getDeclaredMethods(boolean publicOnly);

    VmClassLoader getClassLoader();

    VmObject getProtectionDomain();

    /**
     * Get a {@link java.lang.invoke.MethodHandles.Lookup} instance for this class with the given flags. The
     * flags are not checked.
     *
     * @param allowedModes the flags
     * @return the lookup object
     */
    VmObject getLookupObject(int allowedModes);

    String getName();

    String getSimpleName();

    ObjectType getInstanceObjectType();

    ObjectType getInstanceObjectTypeId();

    Literal getValueForStaticField(FieldElement field);

    int indexOfStatic(FieldElement field) throws IllegalArgumentException;
}
