package org.qbicc.runtime;

import java.lang.ref.Reference;

/**
 * A Java object which is always allocated on the stack and is not allowed to escape a stack frame, but is accessed
 * by (stack) reference. Stack references are a distinct type from heap references.
 * <p>
 * Heap objects may not contain references to stack objects.
 * <p>
 * Arrays of stack objects are also stack objects.
 */
public abstract class StackObject {
    @SafePoint(SafePointBehavior.ALLOWED)
    protected StackObject() {
    }

    /**
     * Destructor for this object, which is <em>always</em> called when instances go out of scope, which happens
     * implicitly at the point where the last reference to the object is used.
     * <p>
     * Note that an object may be considered to be unreachable even while a method on that object is still executing, as
     * long as no further accesses to the object's data are necessary. This is more likely to occur in conjunction with
     * method inlining but is allowed to happen at any time at the compiler's discretion.
     * <p>
     * To extend the reachability of a stack object, use {@link Reference#reachabilityFence(Object)}.
     * The reachability of a stack object may not be extended beyond its allocating method.
     * <p>
     * This method must not leak {@code this}, otherwise a compilation error will result.
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    protected void destroy() {
    }
}
