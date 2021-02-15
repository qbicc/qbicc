package org.qbicc.interpreter;


import org.qbicc.context.ClassContext;

/**
 *
 */
public interface VmClassLoader extends VmObject {
    ClassContext getClassContext();

    VmClass loadClass(String name) throws Thrown;

    VmClass defineClass(VmString name, VmArray content, VmObject protectionDomain) throws Thrown;
}
