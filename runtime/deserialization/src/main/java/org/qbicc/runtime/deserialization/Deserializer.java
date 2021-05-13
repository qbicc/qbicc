package org.qbicc.runtime.deserialization;

/**
 * The main Deserializer engine.
 *
 * Given an input ByteBuffer, parse it to create an object graph.
 * We expect the compiler to generate a top-level heap initialization method that
 * looks something like:
 * <pre>
 *     deser = new Deserializer(bytes, qbiccObjectDeserializer)
 *     C1.f1 = deser.readObject()
 *     C1.f2 = deser.readObject()
 *     C2.f1 = deser.readObject()
 *     ...
 * </pre>
 */
public final class Deserializer implements SerializationConstants {
    private final DeserializationBuffer buf;
    private final ObjectGraph objects;
    private final ObjectDeserializer thunk;

    public Deserializer(byte[] buf, int expectedObjects, ObjectDeserializer thunk) {
        this.buf = new DeserializationBuffer(buf);
        this.objects = new ObjectGraph(expectedObjects);
        this.thunk = thunk;
    }

    /**
     * Deserialize the entire buffer and return the constructed object graph.
     * This method is only intended to be used for writing test cases.
     */
    ObjectGraph readAll() {
        while (!buf.isEmpty()) {
            readObject();
        }
        return objects;
    }

    /*
     * With the exception of readObject, the other readX methods are only intended to be
     * called from the compiler-generated deserialization routines. They are public to
     * expose them to the compiler generated code.
     */

    public boolean readBoolean() {
        byte data = buf.get();
        return (data & 1) != 0;
    }

    public byte readByte() {
        return buf.get();
    }

    public char readChar() {
        return buf.getChar();
    }

    public short readShort() {
        return buf.getShort();
    }

    public float readFloat()  {
        return buf.getFloat();
    }

    public int readInt()  {
        return buf.getInt();
    }

    public double readDouble() {
        return buf.getDouble();
    }

    public long readLong() {
        return buf.getLong();
    }

    public int readU8() {
        return 0xFF & buf.get();
    }

    public int readU16() {
        return 0xFFFF & buf.getShort();
    }

    /**
     * Fully deserialize (including transitively reachable objects) an object and return it.
     * @return The deserialized object.
     */
    public Object readObject() {
        int tag = readU8();
        if (tag == NULL) {
            return null;
        }
        if ((tag & TINY_REF_TAG_BIT) != 0) {
            int tinyBackRef = tag & TINY_REF_MASK;
            return objects.resolveBackref(tinyBackRef);
        }

        switch (tag) {
            case OBJECT:
            case OBJECT_LONG_LIVED:
            case OBJECT_IMMORTAL: {
                int typeId = readU16();
                Object uninitialized = thunk.allocateClass(typeId, tag == OBJECT_LONG_LIVED, tag == OBJECT_IMMORTAL);
                objects.recordObject(uninitialized); // MUST call recordObject before calling readInstanceFields fields; otherwise cyclic graphs will be off by 1!
                thunk.deserializeInstanceFields(typeId, uninitialized, this);
                return uninitialized;
            }

            case STRING_SMALL_L1:
            case STRING_LARGE_L1: {
                int length = tag == STRING_SMALL_L1 ? readU8() : readInt();
                byte[] data = new byte[length];
                buf.get(data);
                String str = thunk.createString(data, (byte)0, false, false);
                objects.recordObject(str);
                return str;
            }

            case STRING_SMALL_U16: {
                int length = readU8();
                byte[] data = new byte[length * 2];
                buf.get(data);
                String str = thunk.createString(data, (byte)1, false, false);
                objects.recordObject(str);
                return str;
            }

            case STRING_LARGE_U16: {
                int length = readInt();
                if (length < Integer.MAX_VALUE / 2) {
                    byte[] data = new byte[length * 2];
                    buf.get(data);
                    String str = thunk.createString(data, (byte)1, false, false);
                    objects.recordObject(str);
                    return str;
                } else {
                    // TODO: This is going to be very slow...do we really have to support this?  Could we fail at build time instead?
                    char[] data = new char[length];
                    for (int i = 0; i < length; i++) {
                        data[i] = readChar();
                    }
                    String str = new String(data);
                    objects.recordObject(str);
                    return str;
                }
            }

            case ARRAY_SMALL_BOOLEAN:
            case ARRAY_SMALL_BYTE:
            case ARRAY_SMALL_CHAR:
            case ARRAY_SMALL_SHORT:
            case ARRAY_SMALL_INT:
            case ARRAY_SMALL_FLOAT:
            case ARRAY_SMALL_LONG:
            case ARRAY_SMALL_DOUBLE: {
                int length = readU8();
                int typeId = 10 + (~TAG_MASK & tag);
                Object theArray = thunk.allocatePrimitiveArray(typeId, length, false, false);
                objects.recordObject(theArray);
                thunk.deserializePrimitiveArrayData(typeId, length, theArray, this);
                return theArray;
            }

            case ARRAY_SMALL_OBJECT: {
                int length = readU8();
                Object[] theArray = new Object[length];
                objects.recordObject(theArray);
                for (int i = 0; i < length; i++) {
                    theArray[i] = readObject();
                }
                return theArray;
            }
            case ARRAY_SMALL_STRING: {
                int length = readU8();
                String[] theArray = new String[length];
                objects.recordObject(theArray);
                for (int i = 0; i < length; i++) {
                    theArray[i] = (String)readObject();
                }
                return theArray;
            }
            case ARRAY_SMALL_CLASS: {
                int length = readU8();
                int typeId = readU16();
                Object[] theArray = thunk.allocateReferenceArray(typeId, length, false, false);
                objects.recordObject(theArray);
                for (int i = 0; i < length; i++) {
                    theArray[i] = readObject();
                }
                return theArray;
            }

            case ARRAY_LARGE_BOOLEAN:
            case ARRAY_LARGE_BYTE:
            case ARRAY_LARGE_SHORT:
            case ARRAY_LARGE_CHAR:
            case ARRAY_LARGE_INT:
            case ARRAY_LARGE_FLOAT:
            case ARRAY_LARGE_LONG:
            case ARRAY_LARGE_DOUBLE: {
                int length = readInt();
                int typeId = 10 + (~TAG_MASK & tag);
                Object theArray = thunk.allocatePrimitiveArray(typeId, length, false, false);
                objects.recordObject(theArray);
                thunk.deserializePrimitiveArrayData(typeId, length, theArray, this);
                return theArray;
            }

            case ARRAY_LARGE_OBJECT: {
                int length = readInt();
                Object[] theArray = new Object[length];
                objects.recordObject(theArray);
                for (int i = 0; i < length; i++) {
                    theArray[i] = readObject();
                }
                return theArray;
            }
            case ARRAY_LARGE_STRING: {
                int length = readInt();
                String[] theArray = new String[length];
                objects.recordObject(theArray);
                for (int i = 0; i < length; i++) {
                    theArray[i] = (String)readObject();
                }
                return theArray;
            }
            case ARRAY_LARGE_CLASS: {
                int length = readInt();
                int typeId = readU16();
                Object[] theArray = thunk.allocateReferenceArray(typeId, length, false, false);
                objects.recordObject(theArray);
                for (int i = 0; i < length; i++) {
                    theArray[i] = readObject();
                }
                return theArray;
            }

            case CLASS_LITERAL_TAG: {
                throw new RuntimeException("TODO");
            }

            case BACKREF_SMALL: {
                int backref = readU16();
                return objects.resolveBackref(backref);
            }
            case BACKREF_LARGE: {
                int backref = readInt(); // NOTE: Not supporting more than 2^31 objects in an object graph.
                return objects.resolveBackref(backref);
            }

            default:
                throw new RuntimeException("Unexpected TAG " + Integer.toHexString(tag));
        }
    }
}
