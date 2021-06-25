package org.qbicc.runtime.main;

import org.qbicc.runtime.CNative;
import org.qbicc.runtime.stdc.Stdint;

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
    public static native Stdint.uint8_t get_dimensions_from_class(Class<?> cls);

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
    public static native Stdint.uint8_t dimensions_of(Object arrayReference); // Object not Object[] because we use this in the impl of cast

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

    static final int Flag_typeid_has_clinit = 1;
    static final int Flag_typeid_declares_default_methods = 2;
    static final int Flag_typeid_has_default_methods = 4;

    /**
     * Get the `flags` field from the qbicc_typeid_array for the given
     * typeid.
     * 
     * Flags are:
     * 1 - has clinit method
     * 2 - declares default methods
     * 4 - has default methods
     * See SupersDisplayTables.calculateTypeIdFlags() for definitive list.
     * 
     * @param typeID the class to read the flags for
     * @return the flags value
     */
    public static native int get_typeid_flags(CNative.type_id typeId);

    public static boolean has_class_initializer(CNative.type_id typeId) {
        return (get_typeid_flags(typeId) & Flag_typeid_has_clinit) == Flag_typeid_has_clinit;
    }

    public static boolean declares_default_methods(CNative.type_id typeId) {
        return (get_typeid_flags(typeId) & Flag_typeid_declares_default_methods) == Flag_typeid_declares_default_methods;
    }

    public static boolean has_default_methods(CNative.type_id typeId) {
        return (get_typeid_flags(typeId) & Flag_typeid_has_default_methods) == Flag_typeid_has_default_methods;
    }

    /** 
     * Fetch the superclass `type_id` from the current `type_id`
     * @param an existing type_id, don't call this on Object's typeid
     * @return superclass's type_id
     */
    public static native CNative.type_id get_superclass_typeid(CNative.type_id typeId);

    public static native CNative.type_id get_first_interface_typeid();

    public static native int get_number_of_bytes_in_interface_bits_array();

    public static native byte get_byte_of_interface_bits(CNative.type_id typeId, int index);

    /**
     * Check the `clinit_states` native structure to see if this typeid is initialized.
     * 
     * This is a fast check reading a bit in the structure.  A "true" value can be trusted
     * as a fast path check while a "false" value requires the state-machine defined in
     * VMHelpers.initialize_class() to validate the result and handle the transition.
     * 
     * @return true if initialized.  False if the state machine needs to validate.
     */
    public static native boolean is_initialized(CNative.type_id typdId);

    /**
     * Set the class initialized.  
     * 
     * This should only be done by the MHelpers.initialize_class() statemachine
     * @param typdId the class to mark initialized
     */
    public static native void set_initialized(CNative.type_id typdId);
}
