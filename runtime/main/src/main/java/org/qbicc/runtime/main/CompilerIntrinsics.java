package org.qbicc.runtime.main;

import org.qbicc.runtime.CNative.*;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.posix.PThread;
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
    public static native void copyInstanceFields(Class<?> clazz, Object src, Object dst);


    /**
     * TODO
     * @param thread
     * @param pthreadPtr
     * @return
     */
    public static native boolean saveNativeThread(void_ptr thread, PThread.pthread_t_ptr pthreadPtr);

    /**
     * This intrinsic sets up and executes the `public void run()` of threadParam.
     * @param threadParam - java.lang.Thread object that has been cast to a void pointer to be compatible with pthread_create
     * @return null - this return value will not be used
     */
    public static native void_ptr threadWrapperNative(void_ptr threadParam);

    /**
     * Get the dimensionality for the represented type from a java.lang.Class instance.
     * Classes, interfaces, and primitive arrays have dimensionality 0.
     */
    @Hidden
    public static native uint8_t getDimensionsFromClass(Class<?> cls);

    /**
     * Get the concrete type ID for the represented type from a java.lang.Class instance.
     */
    @Hidden
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
    public static native Class<?> getClassFromTypeIdSimple(type_id typeId);

    /**
     * Returns java.lang.Class instance representing array class of a given class
     *
     * @param componentClass
     * @return instance of java.lang.Class
     */
    @Hidden
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
    public static native boolean setArrayClass(Class<?> componentClass, Class<?> arrayClass);

    /**
     * Is the argument typeId the typeId for java.lang.Object?
     */
    @Hidden
    public static native boolean isJavaLangObject(type_id typeId);

    /**
     * Is the argument typeId the typeId for java.lang.Cloneable?
     */
    @Hidden
    public static native boolean isJavaLangCloneable(type_id typeId);

    /**
     * Is the argument typeId the typeId for java.io.Serializable?
     */
    @Hidden
    public static native boolean isJavaIoSerializable(type_id typeId);

    /**
     * Is the argument typeId the typeId of a Class?
     */
    @Hidden
    public static native boolean isClass(type_id typeId);

    /**
     * Is the argument typeId the typeId of an Interface?
     */
    @Hidden
    public static native boolean isInterface(type_id typeId);

    /**
     * Is the argument typeId the typeId of a primitive array?
     */
    @Hidden
    public static native boolean isPrimArray(type_id typeId);

    /**
     * Is the argument typeId the typeId of a primitive?
     */
    @Hidden
    public static native boolean isPrimitive(type_id typeId);

    /**
     * Is the argument typeId the typeId use for reference arrays?
     */
    @Hidden
    public static native boolean isReferenceArray(type_id typeId);

    /**
     * Returns the typeId used for reference arrays
     */
    @Hidden
    public static native type_id getReferenceArrayTypeId();

    /**
     * Get the number of typeIds in the system.
     * This will be 1 higher than the highest typeid
     */
    @Hidden
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
    public static native Class<?> createClass(String name, type_id id, uint8_t dimension, Class<?> componentType);

    /**
     * Get the concrete type ID value from the referenced object.  Note that all reference arrays will have the same
     * type ID, which does not reflect the element type.
     *
     * @param reference the object reference (must not be {@code null})
     * @return the type ID of the object
     */
    @Hidden
    public static native type_id typeIdOf(Object reference);

    /**
     * Get the dimensionality of the referenced array.
     *
     * @param arrayReference the array reference (must not be {@code null} and must be an Object[])
     * @return the dimensionality of the array
     */
    @Hidden
    public static native uint8_t dimensionsOf(Object arrayReference); // Object not Object[] because we use this in the impl of cast

    /**
     * Get the element type ID value of the referenced array.
     *
     * @param arrayReference the array reference (must not be {@code null} and must be an Object[])
     * @return the array element type ID
     */
    @Hidden
    public static native type_id elementTypeIdOf(Object arrayReference); // Object not Object[] because we use this in the impl of cast

    /**
     * Get the length field of the argument array
     *
     * @param array the array (must not be {@code null} and must be an array)
     * @return the array length
     */
    @Hidden
    public static native int lengthOf(Object array);

    /**
     * Get the maxTypeId assigned to subclasses of the argument typeId
     */
    @Hidden
    public static native type_id maxSubClassTypeIdOf(type_id typeId);

    /**
     * Does a typeId implement the argument interface?
     */
    @Hidden
    public static native boolean doesImplement(type_id valueTypeId, type_id interfaceTypeId);

    @Hidden
    public static native void callRuntimeInitializer(int initID);

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
     * @param typeId the class to read the flags for
     * @return the flags value
     */
    @Hidden
    public static native int getTypeIdFlags(type_id typeId);

    /**
     * Fetch the superclass `type_id` from the current `type_id`
     * @param typeId an existing type_id, don't call this on Object's typeid
     * @return superclass's type_id
     */
    @Hidden
    public static native type_id getSuperClassTypeId(type_id typeId);

    @Hidden
    public static native type_id getFirstInterfaceTypeId();

    @Hidden
    public static native int getNumberOfBytesInInterfaceBitsArray();

    @Hidden
    public static native byte getByteOfInterfaceBits(type_id typeId, int index);
}
