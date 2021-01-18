package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.ValueType;

/**
 * A primitive class (or {@code void}).
 */
public interface VmPrimitiveClass extends VmClass {
    ValueType getRealType();
}
