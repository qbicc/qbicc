package org.qbicc.graph;

import org.qbicc.type.BooleanType;

/**
 * A value with a boolean type.  All nodes which implement this interface will have boolean type; however, some nodes
 * may have boolean type without implementing this interface (for example, a method call which returns a boolean value).
 * <p>
 * This interface allows inputs to be constrained depending on control flow.  For example, if the condition represented
 * by this value is a {@code null} check, then the value-if-true will be a {@code null} literal, and the value-if-false
 * will be a non-nullable constraint wrapping the input.
 */
public interface BooleanValue extends Value {
    BooleanType getType();
}
