package org.qbicc.runtime.main;

import org.qbicc.runtime.CNative;

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
     * Get the dimensionality for the represented type from a java.lang.Class instance.
     * Classes and interfaces have dimensionality 0.
     */
    public static native int get_dimensions_from_class(Class<?> cls);

    /**
     * Get the concrete type ID for the represented type from a java.lang.Class instance.
     */
    public static native CNative.type_id get_type_id_from_class(Class<?> cls);

    /**
     * Get the concrete type ID value from the referenced object.  Note that all reference arrays will have the same
     * type ID, which does not reflect the element type.
     *
     * @param reference the object reference (must not be {@code null})
     * @return the type ID of the object
     */
    public static native CNative.type_id type_id_of(Object reference);

    /**
     * Get the dimensionality of the referenced array.
     *
     * @param arrayReference the array reference (must not be {@code null} and must be an Object[])
     * @return the dimensionality of the array
     */
    public static native int dimensions_of(Object arrayReference); // Object not Object[] because we use this in the impl of cast

    /**
     * Get the element type ID value of the referenced array.
     *
     * @param arrayReference the array reference (must not be {@code null} and must be an Object[])
     * @return the array element type ID
     */
    public static native CNative.type_id element_type_id_of(Object arrayReference); // Object not Object[] because we use this in the impl of cast

    /**
     * Get the maxTypeId assigned to subclasses of the argument typeId
     */
    public static native CNative.type_id max_subclass_type_id_of(CNative.type_id typeId);

    /**
     * Is the argument typeId the typeId for java.lang.Object?
     */
    public static native boolean is_java_lang_object(CNative.type_id typeId);

    /**
     * Is the argument typeId the typeId for java.lang.Cloneable?
     */
    public static native boolean is_java_lang_cloneable(CNative.type_id typeId);

    /**
     * Is the argument typeId the typeId for java.io.Serializable?
     */
    public static native boolean is_java_io_serializable(CNative.type_id typeId);

    /**
     * Is the argument typeId the typeId of a Class?
     */
    public static native boolean is_class(CNative.type_id typeId);

    /**
     * Is the argument typeId the typeId of an Interface?
     */
    public static native boolean is_interface(CNative.type_id typeId);

    /**
     * Is the argument typeId the typeId of a primitive array?
     */
    public static native boolean is_prim_array(CNative.type_id typeId);

    /**
     * Is the argument typeId the typeId use for reference arrays?
     */
    public static native boolean is_reference_array(CNative.type_id typeId);

    /**
     * Does a typeId implement the argument interface?
     */
    public static native boolean does_implement(CNative.type_id valueTypeId, CNative.type_id interfaceTypeId);

    /**
     * Get the number of typeIds in the system.
     * This will be 1 higher than the highest typeid
     */
    public static native int get_number_of_typeids();

    /**
     * Call the class initializer for this class if it hasn't already been
     * called.
     * 
     * This operation is racy as the locking is managed by the ClinitState
     * object in VMHelpers#initialize_class and should only be called by
     * that method.
     * 
     * @param typeId the class to initialize
     */
    public static native void call_class_initializer(CNative.type_id typeId);
}
