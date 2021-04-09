package org.qbicc.interpreter;

import org.qbicc.type.ValueType;

/**
 * A primitive class (or {@code void}).
 */
public interface VmPrimitiveClass extends VmClass {
    ValueType getRealType();
}
