package cc.quarkus.qcc.runtime.api;

/**
 * A Java object which is always handled by value. Inline objects cannot be {@code null}, unlike
 * Java records. Language-level "references" to inline objects are not actually references; they
 * are literal values.
 * <p>
 * The compiler may transform instances of other classes to be inline classes if they are only referenced in an
 * inline-safe manner.
 * <p>
 * Given a class {@code A} which is an inline object class, and a subtype of that class {@code B}, it is not usually
 * legal to assign a value of type {@code B} to a variable of type {@code A}, as it would be with
 * a reference object. This is because heap reference types are implicitly <em>covariant</em> whereas values of
 * inline object types are implicitly <em>invariant</em>. As a result, inline object types are not normally usable as
 * generic type parameter values in many situations.
 */
public abstract class InlineObject {
    /**
     * Construct a new instance.
     */
    protected InlineObject() {
    }
}
