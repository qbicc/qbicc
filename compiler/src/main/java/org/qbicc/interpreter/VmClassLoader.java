package org.qbicc.interpreter;


import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
public interface VmClassLoader extends VmObject {
    ClassContext getClassContext();

    VmClass loadClass(String name) throws Thrown;

    VmClass defineClass(VmString name, VmArray content, VmObject protectionDomain) throws Thrown;

    /**
     * Define a loaded class.
     *
     * @param definition the class definition (must not be {@code null})
     * @return the defined class object (not {@code null})
     */
    VmClass getOrDefineClass(LoadedTypeDefinition definition);
}
