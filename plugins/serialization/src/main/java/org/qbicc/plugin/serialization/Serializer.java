package org.qbicc.plugin.serialization;

import org.qbicc.context.CompilationContext;
import org.qbicc.runtime.deserialization.SerializationConstants;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.Set;

public class Serializer implements SerializationConstants {
    private final CompilationContext ctxt;
    private final IdentityHashMap<Object, Integer> objects = new IdentityHashMap<>();
    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final DataOutputStream out = new DataOutputStream(outBytes);
    private int lastIndex = 0;

    public Serializer(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    private int getBackref(Object obj) {
        if (objects.containsKey(obj)) {
            Integer prevIdx = objects.get(obj);
            return lastIndex - prevIdx;
        } else {
            lastIndex += 1;
            objects.put(obj, lastIndex);
            return -1;
        }
    }

    public void flush() throws IOException {
        out.flush();
    }

    public byte[] getBytes() throws IOException {
        flush();
        return outBytes.toByteArray();
    }

    public int getNumberOfObjects() {
        return lastIndex;
    }

    public void writeObject(Object obj, Set<LoadedTypeDefinition> writtenClasses) throws IOException {
        if (obj == null) {
            out.writeByte(NULL);
            return;
        }

        int possibleBackRef = getBackref(obj);
        if (possibleBackRef >= 0) {
            if (possibleBackRef < 128) {
                out.writeByte(TINY_REF_TAG_BIT | possibleBackRef);
            } else if (possibleBackRef <= 0xFFFF) {
                out.writeByte(BACKREF_SMALL);
                out.writeShort(possibleBackRef);
            } else {
                out.writeByte(BACKREF_LARGE);
                out.writeInt(possibleBackRef);
            }
            return;
        }

        if (obj instanceof String) {
            String str = (String)obj;
            // TODO: Check to see if the String really can be encoded as a LATIN-1 string!
            byte[] bytes = str.getBytes(StandardCharsets.ISO_8859_1);
            if (bytes.length < 128) {
                out.writeByte(STRING_SMALL_L1);
                out.writeByte(bytes.length);
            } else {
                out.writeByte(STRING_LARGE_L1);
                out.writeInt(bytes.length);
            }
            out.write(bytes);
            return;
        }

        if (obj instanceof boolean[]) {
            boolean[] array = (boolean[])obj;
            if (array.length < 128) {
                out.writeByte(ARRAY_SMALL_BOOLEAN);
                out.writeByte(array.length);
            } else {
                out.writeByte(ARRAY_LARGE_BOOLEAN);
                out.writeInt(array.length);
            }
            for (boolean b : array) {
                out.writeBoolean(b);
            }
            return;
        }
        if (obj instanceof byte[]) {
            byte[] array = (byte[])obj;
            if (array.length < 128) {
                out.writeByte(ARRAY_SMALL_BYTE);
                out.writeByte(array.length);
            } else {
                out.writeByte(ARRAY_LARGE_BYTE);
                out.writeInt(array.length);
            }
            for (byte b : array) {
                out.writeByte(b);
            }
            return;
        }
        if (obj instanceof short[]) {
            short[] array = (short[])obj;
            if (array.length < 128) {
                out.writeByte(ARRAY_SMALL_SHORT);
                out.writeByte(array.length);
            } else {
                out.writeByte(ARRAY_LARGE_SHORT);
                out.writeInt(array.length);
            }
            for (short value : array) {
                out.writeShort(value);
            }
            return;
        }
        if (obj instanceof char[]) {
            char[] array = (char[])obj;
            if (array.length < 128) {
                out.writeByte(ARRAY_SMALL_CHAR);
                out.writeByte(array.length);
            } else {
                out.writeByte(ARRAY_LARGE_CHAR);
                out.writeInt(array.length);
            }
            for (char c : array) {
                out.writeChar(c);
            }
            return;
        }
        if (obj instanceof int[]) {
            int[] array = (int[])obj;
            if (array.length < 128) {
                out.writeByte(ARRAY_SMALL_INT);
                out.writeByte(array.length);
            } else {
                out.writeByte(ARRAY_LARGE_INT);
                out.writeInt(array.length);
            }
            for (int j : array) {
                out.writeInt(j);
            }
            return;
        }
        if (obj instanceof float[]) {
            float[] array = (float[])obj;
            if (array.length < 128) {
                out.writeByte(ARRAY_SMALL_FLOAT);
                out.writeByte(array.length);
            } else {
                out.writeByte(ARRAY_LARGE_FLOAT);
                out.writeInt(array.length);
            }
            for (float v : array) {
                out.writeFloat(v);
            }
            return;
        }
        if (obj instanceof long[]) {
            long[] array = (long[])obj;
            if (array.length < 128) {
                out.writeByte(ARRAY_SMALL_LONG);
                out.writeByte(array.length);
            } else {
                out.writeByte(ARRAY_LARGE_LONG);
                out.writeInt(array.length);
            }
            for (long l : array) {
                out.writeLong(l);
            }
            return;
        }
        if (obj instanceof double[]) {
            double[] array = (double[])obj;
            if (array.length < 128) {
                out.writeByte(ARRAY_SMALL_DOUBLE);
                out.writeByte(array.length);
            } else {
                out.writeByte(ARRAY_LARGE_DOUBLE);
                out.writeInt(array.length);
            }
            for (double v : array) {
                out.writeDouble(v);
            }
            return;
        }

        if (obj instanceof Object[]) {
            Object[] array = (Object[])obj;
            if (obj instanceof String[]) {
                if (array.length < 128) {
                    out.writeByte(ARRAY_SMALL_STRING);
                    out.writeByte(array.length);
                } else {
                    out.writeLong(ARRAY_LARGE_STRING);
                    out.writeInt(array.length);
                }
            } else if (obj.getClass().getComponentType().equals(Object.class)) {
                if (array.length < 128) {
                    out.writeByte(ARRAY_SMALL_OBJECT);
                    out.writeByte(array.length);
                } else {
                    out.writeLong(ARRAY_LARGE_OBJECT);
                    out.writeInt(array.length);
                }
            } else {
                if (array.length < 128) {
                    out.writeByte(ARRAY_SMALL_CLASS);
                    out.writeByte(array.length);
                } else {
                    out.writeLong(ARRAY_LARGE_CLASS);
                    out.writeInt(array.length);
                }
            }
            for (Object elem : array) {
                writeObject(elem, writtenClasses);
            }
            return;
        }

        if (obj instanceof Class) {
            throw new UnsupportedOperationException("TODO: serialize java.lang.Class");
        }

        serializeInstance(obj, writtenClasses);
    }

    private void serializeInstance(Object obj, Set<LoadedTypeDefinition> writtenClasses) throws IOException {
        Class<?> cls = obj.getClass();
        LoadedTypeDefinition ltd = ctxt.getBootstrapClassContext().findDefinedType(cls.getName()).load();
        out.writeByte(OBJECT);
        out.writeShort(ltd.getTypeId());
        try {
            serializeInstanceFields(obj, ltd, writtenClasses);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            ctxt.error(e, "Error serializing instance fields of "+ltd.getInternalName());
        }
    }

    private void serializeInstanceFields(Object obj, LoadedTypeDefinition cur, Set<LoadedTypeDefinition> writtenClasses) throws IllegalAccessException, NoSuchFieldException, IOException {
        if (cur.hasSuperClass()) {
            serializeInstanceFields(obj, cur.getSuperClass(), writtenClasses);
        }
        writtenClasses.add(cur);
        for (int i=0; i<cur.getFieldCount(); i++) {
            FieldElement qf = cur.getField(i);
            if (!qf.isStatic()) {
                Field jf = obj.getClass().getField(qf.getName());
                if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.Z)){
                    out.writeBoolean(jf.getBoolean(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.B)) {
                    out.writeByte(jf.getByte(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.S)) {
                    out.writeShort(jf.getShort(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.C)) {
                    out.writeChar(jf.getChar(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.I)) {
                    out.writeInt(jf.getInt(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.F)) {
                    out.writeFloat(jf.getFloat(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.J)) {
                    out.writeLong(jf.getLong(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.D)) {
                    out.writeDouble(jf.getDouble(obj));
                } else {
                    writeObject(jf.get(obj), writtenClasses);
                }
            }
        }
    }

}
