package org.qbicc.runtime.main;

import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.NoReflect;
import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.NoThrow;

/**
 * Methods which facilitate the handling and implementation of safe points.
 */
public final class SafePoint {
    private SafePoint() {}

    /**
     * Perform safe point work.
     * <p>
     * The actual safepoint tasks are registered by plugins.
     */
    @NoSafePoint
    @NoThrow
    @NoReflect
    @Hidden
    static native void enterSafePoint();

    /**
     * Request a global safepoint.
     * Returns immediately.
     * <p>
     * The actual implementation depends on the selected safepoint strategy.
     */
    @NoSafePoint
    @NoThrow
    @NoReflect
    @Hidden
    public static native void requestGlobalSafePoint();

    /**
     * Clear the global safepoint request.
     * Returns immediately.
     * This should only be called from within the safepoint handler mechanism.
     * <p>
     * The actual implementation depends on the selected safepoint strategy.
     */
    @NoSafePoint
    @NoThrow
    @NoReflect
    @Hidden
    public static native void clearGlobalSafePoint();

    /**
     * Poll for a safepoint.
     * <p>
     * The actual implementation (if any) depends on the selected safepoint strategy.
     * This method exists as a placeholder for safepoint strategies that rely on a method call
     * so that they do not have to inject a method for that purpose.
     */
    @NoReflect
    @Hidden
    @NoSafePoint
    @NoThrow
    static native void pollSafePoint();
}
