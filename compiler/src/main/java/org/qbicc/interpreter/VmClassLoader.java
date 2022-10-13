package org.qbicc.interpreter;


import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
public interface VmClassLoader extends VmObject {
    ClassContext getClassContext();

    /**
     * Get the name of this class loader.
     *
     * @return the class loader name (not {@code null})
     */
    String getName();

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
