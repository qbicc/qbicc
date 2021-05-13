package org.qbicc.runtime.deserialization;

/**
 * The compiler-generated deserialization code and low-level qbicc runtime functions
 * will be exposed to the Deserializer engine via a class that implements this interface.
 */
public abstract class ObjectDeserializer {
    /**
     * Create and initialize a String from the given byte array and coder.
     * @param data the bytes from which to make the string
     * @param coder which Charset to use: 0 means ISO_8859_1 and 1 means UTF_16BE
     * @param longLived is the instance predicted to be long lived?
     * @param immortal is the instance predicted to be immortal?
     * @return the uninitialized object
     */
    public abstract String createString(byte[] data, byte coder, boolean longLived, boolean immortal);

    /**
     * Allocate memory for instance of the given typeId and initialize its object header
     * @param typeId the typeId of the Class to allocate
     * @param longLived is the instance predicted to be long lived?
     * @param immortal is the instance predicted to be immortal?
     * @return the uninitialized object
     */
    public abstract Object allocateClass(int typeId, boolean longLived, boolean immortal);

    /**
     * Allocate memory for a primitive array of the given typeId and initialize its object header
     * @param typeId the typeId of the primitive array to allocate
     * @param length the length of the array (in elements)
     * @param longLived is the instance predicted to be long lived?
     * @param immortal is the instance predicted to be immortal?
     * @return the uninitialized array
     */
    public abstract Object allocatePrimitiveArray(int typeId, int length, boolean longLived, boolean immortal);

    /**
     * Allocate memory for a reference array containing elements of the given typeId and initialize its object header
     * @param elementTypeId the typeId of the array's elements
     * @param length the length of the array (in elements)
     * @param longLived is the instance predicted to be long lived?
     * @param immortal is the instance predicted to be immortal?
     * @return the uninitialized array
     */
    public abstract Object[] allocateReferenceArray(int elementTypeId, int length, boolean longLived, boolean immortal);

    /**
     * Deserialize the instance fields of an object
     * @param typeId the typeId of the class whose fields should be read
     * @param obj the object into which the deserialized fields should be stored
     * @param deserializer the Deserializer from which to read the fields
     */
    public abstract void deserializeInstanceFields(int typeId, Object obj, Deserializer deserializer);

    /**
     * Deserialize the elements of a primitive array
     * @param typeId the typeId of the array whose data should be read
     * @param length the length of the array
     * @param array the array object into which the deserialized elements should be stored
     * @param deserializer the Deserializer from which to read the fields
     */
    public abstract void deserializePrimitiveArrayData(int typeId, int length, Object array, Deserializer deserializer);
}
