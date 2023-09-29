package org.qbicc.runtime;

/**
 * A description of safepoint behavior for a given method.
 */
@SuppressWarnings("JavadocDeclaration")
public enum SafePointBehavior {
    /**
     * Disallow calling from methods or functions which may be in safepoint.
     * Disallow calls to methods or functions which may enter safepoint.
     * The method itself will not poll for safepoints.
     * <p>
     * May be called from:
     * <ul>
     *     <li>{@link #FORBIDDEN}</li>
     *     <li>{@link #NONE}</li>
     *     <li>{@link #POLLING}</li>
     * </ul>
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
     * <p>
     * May be called from:
     * <ul>
     *     <li>{@link #NONE}</li>
     *     <li>{@link #POLLING}</li>
     * </ul>
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
     * <p>
     * May be called from:
     * <ul>
     *     <li>{@link #NONE}</li>
     *     <li>{@link #POLLING}</li>
     * </ul>
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
     * <p>
     * May be called from:
     * <ul>
     *     <li>{@link #NONE}</li>
     *     <li>{@link #POLLING}</li>
     *     <li>{@link #ENTER}</li>
     *     <li>{@link #REQUIRED}</li>
     * </ul>
     * May call:
     * <ul>
     *     <li>{@link #ENTER}</li>
     *     <li>{@link #REQUIRED}</li>
     *     <li>{@link #ALLOWED}</li>
     * </ul>
     */
    ENTER,
    /**
     * Only allow calling from call sites which are definitely in safepoint.
     * Attempting to call a method with this mode will otherwise result in a compile error.
     * <p>
     * May be called from:
     * <ul>
     *     <li>{@link #ENTER}</li>
     *     <li>{@link #REQUIRED}</li>
     * </ul>
     * May call:
     * <ul>
     *     <li>{@link #ENTER}</li>
     *     <li>{@link #REQUIRED}</li>
     *     <li>{@link #ALLOWED}</li>
     * </ul>
     */
    REQUIRED,
    /**
     * Allow calling from call sites that are or are not in safepoint.
     * Heap objects may only be safely accessed if the caller is definitely not in safepoint.
     * <p>
     * May be called from:
     * <ul>
     *     <li><em>all modes</em></li>
     * </ul>
     * May call:
     * <ul>
     *     <li>{@link #ALLOWED}</li>
     * </ul>
     */
    ALLOWED,
    ;
}
