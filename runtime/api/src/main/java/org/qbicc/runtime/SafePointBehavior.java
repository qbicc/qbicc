package org.qbicc.runtime;

/**
 * A description of safepoint behavior.
 */
public enum SafePointBehavior {
    /**
     * Disallow calling from methods or functions which may be in safepoint.
     * Disallow calls to methods or functions which may enter safepoint.
     * The method itself will not poll for safepoints.
     * May call:
     * <ul>
     *     <li>{@link #FORBIDDEN}</li>
     *     <li>{@link #ALLOWED}</li>
     * </ul>
     */
    FORBIDDEN,
    /**
     * Disallow calling from methods or functions which may be in safepoint.
     * Allow calls to methods or functions which may enter safepoint.
     * The method itself will not poll for safepoints.
     * May call:
     * <ul>
     *     <li>{@link #FORBIDDEN}</li>
     *     <li>{@link #NONE}</li>
     *     <li>{@link #POLLING}</li>
     *     <li>{@link #ENTER}</li>
     *     <li>{@link #ALLOWED}</li>
     * </ul>
     */
    NONE,
    /**
     * Disallow calling from methods or functions which may be in safepoint.
     * Attempting to call a method with this mode will otherwise result in a compile error.
     * The method may poll for a thread safepoint on exit and/or at other appropriate times.
     * This is the default for regular methods.
     * May call:
     * <ul>
     *     <li>{@link #FORBIDDEN}</li>
     *     <li>{@link #NONE}</li>
     *     <li>{@link #POLLING}</li>
     *     <li>{@link #ENTER}</li>
     *     <li>{@link #ALLOWED}</li>
     * </ul>
     */
    POLLING,
    /**
     * Enter a safepoint for a duration of calls to the method or function.
     * This is the default for external (native) functions.
     * Heap objects must not be accessed.
     */
    ENTER,
    /**
     * Only allow calling from methods which are definitely in safepoint.
     * Attempting to call a method with this mode will otherwise result in a compile error.
     */
    REQUIRED,
    /**
     * Allow calling from methods that are or are not definitely in safepoint.
     * Heap objects may only be safely accessed if the caller is definitely not in safepoint.
     */
    ALLOWED,
    ;
}
