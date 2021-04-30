package org.qbicc.runtime.deserialization;

/**
 * A mock to enable some simple testing without qbicc.
 */
public class MockObjectDeserializer implements ObjectDeserializer {

    final static int TYPEID_OBJECT = 10;
    final static int TYPEID_STRING = 20;
    final static int TYPEID_TEST1 = 21;
    final static int TYPEID_TEST2 = 22;

    // In the real implementation, we would use the typeId to index into a compiler-generated array of instance
    // sizes.  We then allocate the raw storage from the appropriate allocator and initialize the object header.
    public Object allocateClass(int typeId, boolean longLived, boolean immortal) {
        switch (typeId) {
            case TYPEID_TEST1: return new TestDeserialization.Test1();
            case TYPEID_TEST2: return new TestDeserialization.Test2();
            default: throw new RuntimeException("Unexpected typeid "+typeId);
        }
    }

    // In the real implementation we use type typeId to determine element size, then
    // allocate the raw storage from the appropriate allocator and initialize the array header.
    public Object allocatePrimitiveArray(int typeId, int length, boolean longLived, boolean immortal) {
        switch (typeId) {
            case 11: return new boolean[length];
            case 12: return new byte[length];
            case 13: return new short[length];
            case 14: return new char[length];
            case 15: return new int[length];
            case 16: return new float[length];
            case 17: return new long[length];
            case 18: return new double[length];
            default: throw new RuntimeException("Unexpected typeid "+typeId);
        }
    }

    public Object[] allocateReferenceArray(int elementTypeId, int length, boolean longLived, boolean immortal) {
        switch (elementTypeId) {
            case TYPEID_OBJECT: return new Object[length];
            case TYPEID_STRING: return new String[length];
            case TYPEID_TEST1: return new TestDeserialization.Test1[length];
            case TYPEID_TEST2: return new TestDeserialization.Test2[length];
            default: throw new RuntimeException("Unexpected elementTypeId "+elementTypeId);
        }
    }

    // In the real implementation, this would just be a call to memcpy and an adjustment of the deserializer's cursor
    public void readPrimitiveArrayData(int typeId, int length, Object array, Deserializer deserializer) {
        switch (typeId) {
            case 11: {
                for (int i=0; i<length; i++) {
                    ((boolean[])array)[i] = deserializer.readBoolean();
                }
            }
            case 12: {
                assert array instanceof byte[];
                for (int i=0; i<length; i++) {
                    ((byte[])array)[i] = deserializer.readByte();
                }
            }
            case 13: {
                assert array instanceof short[];
                for (int i=0; i<length; i++) {
                    ((short[])array)[i] = deserializer.readShort();
                }
            }
            case 14: {
                assert array instanceof char[];
                for (int i=0; i<length; i++) {
                    ((char[])array)[i] = deserializer.readChar();
                }
            }
            case 15: {
                assert array instanceof int[];
                for (int i=0; i<length; i++) {
                    ((int[])array)[i] = deserializer.readInt();
                }
            }
            case 16: {
                assert array instanceof float[];
                for (int i=0; i<length; i++) {
                    ((float[])array)[i] = deserializer.readFloat();
                }
            }
            case 17: {
                assert array instanceof long[];
                for (int i=0; i<length; i++) {
                    ((long[])array)[i] = deserializer.readLong();
                }
            }
            case 18: {
                assert array instanceof double[];
                for (int i=0; i<length; i++) {
                    ((double[])array)[i] = deserializer.readDouble();
                }
            }
            default: throw new RuntimeException("Unexpected typeid "+typeId);
        }

    }

    // In the real implementation, this would use typeId to index into a table of function pointers.
    public void readInstanceFields(int typeId, Object obj, Deserializer deser) {
        switch (typeId) {
            case TYPEID_TEST1: deserialize_Test1((TestDeserialization.Test1) obj, deser); break;
            case TYPEID_TEST2: deserialize_Test2((TestDeserialization.Test2) obj, deser); break;
            default: throw new RuntimeException("Unexpected typeid "+typeId);
        }
    }

    // These functions mimic what the compiler would generate for each type.
    static void deserialize_Test1(TestDeserialization.Test1 obj, Deserializer deser) {
        obj.a = deser.readInt();
        obj.b = deser.readObject();
    }
    static void deserialize_Test2(TestDeserialization.Test2 obj, Deserializer deser) {
        deserialize_Test1(obj, deser);
        obj.c = deser.readObject();
    }
}
