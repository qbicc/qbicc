package org.qbicc.runtime.main;

import org.qbicc.runtime.CNative.*;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.stdc.Stdint.*;

/**
 * Intrinsics for emitting inline-asm like code sequences and
 * for accessing implementation-dependent object header fields and
 * compiler-generated global structures.
 */
@SuppressWarnings("unused")
public class CompilerIntrinsics {

    @Hidden
    public static native Object emitNewReferenceArray(Class<?> elementType, int dimensions, int size);

    @Hidden
    public static native Object emitNew(Class<?> clazz);

    @Hidden
    @NoSafePoint
    public static native void copyInstanceFields(Class<?> clazz, Object src, Object dst);

    /**
     * Get the dimensionality for the represented type from a java.lang.Class instance.
     * Classes, interfaces, and primitive arrays have dimensionality 0.
     */
    @Hidden
    @NoSafePoint
    public static native uint8_t getDimensionsFromClass(Class<?> cls);

    /**
     * Get the concrete type ID for the represented type from a java.lang.Class instance.
     */
    @Hidden
    @NoSafePoint
    public static native type_id getTypeIdFromClass(Class<?> cls);

    /**
     * Get the java.lang.Class instance for the type ID.
     */
    @Hidden
    public static native Class<?> getClassFromTypeId(type_id typeId, uint8_t dimensions);

    /**
     * Get the java.lang.Class instance for the type ID for non-array classes.
     */
    @Hidden
    @NoSafePoint
    public static native Class<?> getClassFromTypeIdSimple(type_id typeId);

    /**
     * Returns java.lang.Class instance representing array class of a given class
     *
     * @param componentClass
     * @return instance of java.lang.Class
     */
    @Hidden
    @NoSafePoint
    public static native Class<?> getArrayClassOf(Class<?> componentClass);

    /**
     * Tries to atomically set the java.lang.Class#arrayClass field of a component class
     * to a given array class
     *
     * @param componentClass
     * @param arrayClass
     * @return boolean true if atomic operation succeeds, false otherwise
     */
    @Hidden
    @NoSafePoint
    public static native boolean setArrayClass(Class<?> componentClass, Class<?> arrayClass);

    /**
     * Is the argument typeId the typeId for java.lang.Object?
     */
    @Hidden
    @NoSafePoint
    public static native boolean isJavaLangObject(type_id typeId);

    /**
     * Is the argument typeId the typeId for java.lang.Cloneable?
     */
    @Hidden
    @NoSafePoint
    public static native boolean isJavaLangCloneable(type_id typeId);

    /**
     * Is the argument typeId the typeId for java.io.Serializable?
     */
    @Hidden
    @NoSafePoint
    public static native boolean isJavaIoSerializable(type_id typeId);

    /**
     * Is the argument typeId the typeId of a Class?
     */
    @Hidden
    @NoSafePoint
    public static native boolean isClass(type_id typeId);

    /**
     * Is the argument typeId the typeId of an Interface?
     */
    @Hidden
    @NoSafePoint
    public static native boolean isInterface(type_id typeId);

    /**
     * Is the argument typeId the typeId of a primitive array?
     */
    @Hidden
    @NoSafePoint
    public static native boolean isPrimArray(type_id typeId);

    /**
     * Is the argument typeId the typeId of a primitive?
     */
    @Hidden
    @NoSafePoint
    public static native boolean isPrimitive(type_id typeId);

    /**
     * Is the argument typeId the typeId use for reference arrays?
     */
    @NoSafePoint
    @Hidden
    public static native boolean isReferenceArray(type_id typeId);

    /**
     * Returns the typeId used for reference arrays
     */
    @Hidden
    @NoSafePoint
    public static native type_id getReferenceArrayTypeId();

    /**
     * Get the number of typeIds in the system.
     * This will be 1 higher than the highest typeid
     */
    @Hidden
    @NoSafePoint
    public static native type_id getNumberOfTypeIds();

    /**
     * Allocates an instance of java.lang.Class in the runtime heap
     *
     * @param name class name
     * @param id value to store in the id field (typeId of class itself, or the leafElemTypeId if a reference array)
     * @param dimension array dimension if the class is a reference array class, 0 otherwise
     * @param componentType the Class object for the componentType if the class is an array class, null otherwise
     * @return instance of java.lang.Class
     */
    @Hidden
    @NoSafePoint
    public static native Class<?> createClass(String name, type_id id, uint8_t dimension, Class<?> componentType);

    /**
     * Get the concrete type ID value from the referenced object.  Note that all reference arrays will have the same
     * type ID, which does not reflect the element type.
     *
     * @param reference the object reference (must not be {@code null})
     * @return the type ID of the object
     */
    @Hidden
    @NoSafePoint
    public static native type_id typeIdOf(Object reference);

    /**
     * Get the dimensionality of the referenced array.
     *
     * @param arrayReference the array reference (must not be {@code null} and must be an Object[])
     * @return the dimensionality of the array
     */
    @Hidden
    @NoSafePoint
    public static native uint8_t dimensionsOf(Object arrayReference); // Object not Object[] because we use this in the impl of cast

    /**
     * Get the element type ID value of the referenced array.
     *
     * @param arrayReference the array reference (must not be {@code null} and must be an Object[])
     * @return the array element type ID
     */
    @Hidden
    @NoSafePoint
    public static native type_id elementTypeIdOf(Object arrayReference); // Object not Object[] because we use this in the impl of cast

    /**
     * Get the length field of the argument array
     *
     * @param array the array (must not be {@code null} and must be an array)
     * @return the array length
     */
    @Hidden
    @NoSafePoint
    public static native int lengthOf(Object array);

    /**
     * Get the maxTypeId assigned to subclasses of the argument typeId
     */
    @Hidden
    @NoSafePoint
    public static native type_id maxSubClassTypeIdOf(type_id typeId);

    /**
     * Does a typeId implement the argument interface?
     */
    @Hidden
    @NoSafePoint
    public static native boolean doesImplement(type_id valueTypeId, type_id interfaceTypeId);

    @Hidden
    @NoSafePoint
    public static native void callRuntimeInitializer(int initID);

    /**
     * Fetch the superclass `type_id` from the current `type_id`
     * @param typeId an existing type_id, don't call this on Object's typeid
     * @return superclass's type_id
     */
    @Hidden
    @NoSafePoint
    public static native type_id getSuperClassTypeId(type_id typeId);

    @Hidden
    @NoSafePoint
    public static native type_id getFirstInterfaceTypeId();

    @Hidden
    @NoSafePoint
    public static native int getNumberOfBytesInInterfaceBitsArray();

    @Hidden
    @NoSafePoint
    public static native byte getByteOfInterfaceBits(type_id typeId, int index);
}
