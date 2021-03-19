package cc.quarkus.qcc.runtime.main;

import cc.quarkus.qcc.runtime.CNative;

/**
 * Intrinsics for accessing implementation-dependent object header fields
 * and compiler-generated global object model structures.
 *
 * These APIs are primarily intended for use in methods of VMHelper
 * and are subject to change as the runtime object model evolves.
 */
@SuppressWarnings("unused")
public class ObjectModel {

    /**
     * Get the concrete type ID value from the referenced object.  Note that all reference arrays will have the same
     * type ID, which does not reflect the element type.
     *
     * @param reference the object reference (must not be {@code null})
     * @return the type ID of the object
     */
    public static native CNative.type_id type_id_of(Object reference);

    /**
     * Get the element type ID value of the referenced array.
     *
     * @param arrayReference the array reference (must not be {@code null})
     * @return the array element type ID
     */
    public static native CNative.type_id element_type_id_of(Object[] arrayReference);

    /**
     * Get the dimensionality of the referenced array.
     *
     * @param arrayReference the array reference (must not be {@code null})
     * @return the dimensionality of the array
     */
    public static native CNative.c_int dimensions_of(Object[] arrayReference);

}
