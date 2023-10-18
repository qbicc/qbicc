package org.qbicc.runtime;

import java.util.Map;
import java.util.Set;

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
    FORBIDDEN(false, true),
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
     *     <li>{@link #EXIT}</li>
     *     <li>{@link #ALLOWED}</li>
     * </ul>
     */
    NONE(false, true),
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
     *     <li>{@link #EXIT}</li>
     *     <li>{@link #ALLOWED}</li>
     * </ul>
     */
    POLLING(false, true),
    /**
     * Enter a safepoint for a duration of calls to the method or function.
     * This is the default for external (native) functions.
     * Heap objects generally must not be accessed from safepoint methods (other than GC itself).
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
     *     <li>{@link #EXIT}</li>
     *     <li>{@link #REQUIRED}</li>
     *     <li>{@link #ALLOWED}</li>
     * </ul>
     */
    ENTER(true, false),
    /**
     * Exit a safepoint for the duration of calls to the method or function, re-entering
     * the safepoint on return (if the caller was in a safepoint already).
     * <p>
     * May be called from:
     * <ul>
     *     <li>{@link #NONE}</li>
     *     <li>{@link #POLLING}</li>
     *     <li>{@link #ENTER}</li>
     *     <li>{@link #EXIT}</li>
     *     <li>{@link #REQUIRED}</li>
     * </ul>
     * May call:
     * <ul>
     *     <li>{@link #FORBIDDEN}</li>
     *     <li>{@link #NONE}</li>
     *     <li>{@link #POLLING}</li>
     *     <li>{@link #ENTER}</li>
     *     <li>{@link #EXIT}</li>
     *     <li>{@link #ALLOWED}</li>
     * </ul>
     */
    EXIT(false, true),
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
    REQUIRED(true, false),
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
    ALLOWED(false, false),
    ;

    private static final Map<SafePointBehavior, Set<SafePointBehavior>> calls = Map.of(
        FORBIDDEN, Set.of(ALLOWED, FORBIDDEN),
        NONE, Set.of(ALLOWED, ENTER, EXIT, FORBIDDEN, NONE, POLLING),
        POLLING, Set.of(ALLOWED, ENTER, EXIT, FORBIDDEN, NONE, POLLING),
        ENTER, Set.of(ALLOWED, ENTER, EXIT, REQUIRED),
        EXIT, Set.of(ALLOWED, ENTER, EXIT, FORBIDDEN, NONE, POLLING),
        REQUIRED, Set.of(ALLOWED, ENTER, EXIT, REQUIRED),
        ALLOWED, Set.of(ALLOWED)
    );

    private final boolean inSafePoint;
    private final boolean notInSafePoint;

    SafePointBehavior(boolean inSafePoint, boolean notInSafePoint) {
        this.inSafePoint = inSafePoint;
        this.notInSafePoint = notInSafePoint;
    }

    /**
     * {@return <code>true</code> if members with this safepoint behavior may call members with the given behavior}
     * @param other the target's safepoint behavior
     */
    public boolean mayCall(SafePointBehavior other) {
        return other != null && calls.get(this).contains(other);
    }

    /**
     * {@return <code>true</code> if members with the given safepoint behavior may call members with this behavior}
     * @param other the source's safepoint behavior
     */
    public boolean mayBeCalledBy(SafePointBehavior other) {
        return other.mayCall(this);
    }

    /** {@return <code>true</code> when callers with this behavior are definitely always in a safe point} */
    public boolean isInSafePoint() {
        return inSafePoint;
    }

    /** {@return <code>true</code> when callers with this behavior are definitely never in a safe point} */
    public boolean isNotInSafePoint() {
        return notInSafePoint;
    }

    /** {@return <code>true</code> when callers with this behavior are allowed to be in a safe point} */
    public boolean mayBeInSafePoint() {
        return ! isNotInSafePoint();
    }

    /** {@return <code>true</code> when callers with this behavior are allowed to not be in a safe point} */
    public boolean mayNotBeInSafePoint() {
        return ! isInSafePoint();
    }
}
