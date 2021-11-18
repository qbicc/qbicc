package org.qbicc.graph;

import org.qbicc.type.BooleanType;

/**
 * A value with a boolean type.  All nodes which implement this interface will have boolean type; however, some nodes
 * may have boolean type without implementing this interface (for example, a method call which returns a boolean value).
 * <p>
 * In conjunction with {@link PhiValue#getValueForInput(Terminator)} and {@link If#getOutboundValue(PhiValue)}, this
 * interface allows inputs to be constrained depending on control flow.  For example, if the condition represented
 * by this value is a {@code null} check, then the value-if-true will be a {@code null} literal, and the value-if-false
 * will be a non-nullable constraint wrapping the input.
 */
public interface BooleanValue extends Value {
    BooleanType getType();

    /**
     * Get the actual value of the given input if this value evaluates to {@code true}.
     * If the input is equal to this value, then the result must be the {@code true} literal.
     *
     * @param input the input value (must not be {@code null})
     * @return the value if {@code true} (not {@code null})
     */
    Value getValueIfTrue(Value input);

    /**
     * Get the actual value of the given input if this value evaluates to {@code false}.
     * If the input is equal to this value, then the result must be the {@code false} literal.
     *
     * @param input the input value (must not be {@code null})
     * @return the value if {@code false} (not {@code null})
     */
    Value getValueIfFalse(Value input);
}
