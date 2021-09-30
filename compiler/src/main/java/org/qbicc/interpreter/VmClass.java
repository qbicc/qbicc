package org.qbicc.interpreter;

import java.util.List;

import org.qbicc.graph.literal.Literal;
import org.qbicc.type.ClassObjectType;
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

    VmClassLoader getClassLoader();

    VmObject getProtectionDomain();

    String getName();

    String getSimpleName();

    ObjectType getInstanceObjectType();

    ClassObjectType getInstanceObjectTypeId();

    Literal getValueForStaticField(FieldElement field);
}
