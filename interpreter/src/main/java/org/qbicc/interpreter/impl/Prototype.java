package org.qbicc.interpreter.impl;

import org.qbicc.type.definition.FieldContainer;

public interface Prototype {
    byte[] getBytecode();
    String getClassName();
    Class<? extends FieldContainer> getPrototypeClass();
    FieldContainer construct();
}
