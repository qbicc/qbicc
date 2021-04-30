package org.qbicc.graph;

import org.qbicc.type.BooleanType;

/**
 * A value with a boolean type.  All nodes which implement this interface will have boolean type; however, some nodes
 * may have boolean type without implementing this interface (for example, a method call which returns a boolean value).
 */
public interface BooleanValue extends Value {
    BooleanType getType();

    /**
     * Get the actual value of the given input if this value evaluates to {@code true}.
     *
     * @param input the input value (must not be {@code null})
     * @return the value if {@code true} (not {@code null})
     */
    Value getValueIfTrue(Value input);

    /**
     * Get the actual value of the given input if this value evaluates to {@code false}.
     *
     * @param input the input value (must not be {@code null})
     * @return the value if {@code false} (not {@code null})
     */
    Value getValueIfFalse(Value input);
}
