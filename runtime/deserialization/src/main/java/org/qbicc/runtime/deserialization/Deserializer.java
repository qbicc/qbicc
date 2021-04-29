package org.qbicc.runtime.deserialization;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The main Deserializer engine.
 *
 * Given an input stream of bytes, process it and create an object graph.
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
public class Deserializer implements SerializationConstants {
    private final DataInputStream in;
    private final ObjectGraph objects;
    private final ObjectDeserializer thunk;

    public Deserializer(InputStream is, ObjectDeserializer thunk) {
        this.in = new DataInputStream(is);
        this.objects = new ObjectGraph();
        this.thunk = thunk;
    }

    /**
     * Deserialize the entire buffer and return the constructed object graph.
     * This method is only intended to be used for writing test cases.
     */
    ObjectGraph readAll() throws IOException {
        try {
            while (true) {
                readObject();
            }
        } catch (EOFException ignored) {
        }
        return objects;
    }

    /*
     * With the exception of readObject, the other readX methods are only intended to be
     * called from the compiler-generated deserialization routines. They are public to
     * expose them to the compiler generated code.
     */

    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    public byte readByte() throws IOException {
        return in.readByte();
    }

    public char readChar() throws IOException {
        return in.readChar();
    }

    public short readShort() throws IOException {
        return in.readShort();
    }

    public float readFloat() throws IOException {
        return in.readFloat();
    }

    public int readInt() throws IOException {
        return in.readInt();
    }

    public double readDouble() throws IOException {
        return in.readDouble();
    }

    public long readLong() throws IOException {
        return in.readLong();
    }

    public int readU8() throws IOException {
        return in.readUnsignedByte();
    }

    public int readU16() throws IOException {
        return in.readUnsignedShort();
    }

    /**
     * Fully deserialize (including transitively reachable objects) an object and return it.
     * @return The deserialized object.
     */
    public Object readObject() throws IOException {
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
                thunk.readInstanceFields(typeId, uninitialized, this);
                return uninitialized;
            }

            case STRING_SMALL_L1:
            case STRING_LARGE_L1: {
                int length = tag == STRING_SMALL_L1 ? readU8() : readInt();
                byte[] data = new byte[length];
                in.readFully(data);
                String str = new String(data, StandardCharsets.ISO_8859_1);
                objects.recordObject(str);
                return str;
            }

            case STRING_SMALL_U16: {
                int length = readU8();
                byte[] data = new byte[length * 2];
                in.readFully(data);
                String str = new String(data, StandardCharsets.UTF_16);
                objects.recordObject(str);
                return str;
            }

            case STRING_LARGE_U16: {
                int length = readInt();
                if (length < Integer.MAX_VALUE / 2) {
                    byte[] data = new byte[length * 2];
                    in.readFully(data);
                    String str = new String(data, StandardCharsets.UTF_16);
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
                thunk.readPrimitiveArrayData(typeId, length, theArray, this);
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
                thunk.readPrimitiveArrayData(typeId, length, theArray, this);
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
