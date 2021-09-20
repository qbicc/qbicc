package org.qbicc.interpreter;

import java.util.List;

import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

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
}
