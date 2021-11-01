package org.qbicc.runtime;

/**
 * The inlining condition.
 */
public enum InlineCondition {
    /**
     * The method must always be inlined if inlining is available.
     */
    ALWAYS,
    /**
     * The method should be considered for inlining.  This is the default.
     */
    HINT,
    /**
     * The method should never be inlined under any conditions.
     */
    NEVER,
    ;
}
