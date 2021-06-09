package org.qbicc.plugin.serialization;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.qbicc.context.CompilationContext;
import org.qbicc.runtime.deserialization.SerializationConstants;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;

public class Serializer implements SerializationConstants {
    private final CompilationContext ctxt;
    private final HeapOutputStream out;

    public Serializer(CompilationContext ctxt) {
        this.ctxt = ctxt;
        out  = new HeapOutputStream(ctxt.getTypeSystem().getEndianness());
    }

    public byte[] getBytes() {
        return out.getBytes();
    }

    public int getNumberOfObjects() {
        return out.getNumberOfObjects();
    }

    public void writeObject(Object obj, Set<LoadedTypeDefinition> writtenClasses) {
        if (obj == null) {
            out.putByte(NULL);
            return;
        }

        int possibleBackRef = out.getBackref(obj);
        if (possibleBackRef >= 0) {
            if (possibleBackRef < 128) {
                out.putByte((byte)(TINY_REF_TAG_BIT | possibleBackRef));
            } else if (possibleBackRef <= 0xFFFF) {
                out.putByte(BACKREF_SMALL);
                out.putShort((short)possibleBackRef);
            } else {
                out.putByte(BACKREF_LARGE);
                out.putInt(possibleBackRef);
            }
            return;
        }

        if (obj instanceof String) {
            String str = (String)obj;
            // TODO: Check to see if the String really can be encoded as a LATIN-1 string!
            byte[] bytes = str.getBytes(StandardCharsets.ISO_8859_1);
            if (bytes.length < 128) {
                out.putByte(STRING_SMALL_L1);
                out.putByte((byte)bytes.length);
            } else {
                out.putByte(STRING_LARGE_L1);
                out.putInt(bytes.length);
            }
            out.put(bytes);
            return;
        }

        if (obj instanceof boolean[]) {
            boolean[] array = (boolean[])obj;
            if (array.length < 128) {
                out.putByte(ARRAY_SMALL_BOOLEAN);
                out.putByte((byte)array.length);
            } else {
                out.putByte(ARRAY_LARGE_BOOLEAN);
                out.putInt(array.length);
            }
            for (boolean b : array) {
                out.putBoolean(b);
            }
            return;
        }
        if (obj instanceof byte[]) {
            byte[] array = (byte[])obj;
            if (array.length < 128) {
                out.putByte(ARRAY_SMALL_BYTE);
                out.putByte((byte)array.length);
            } else {
                out.putByte(ARRAY_LARGE_BYTE);
                out.putInt(array.length);
            }
            for (byte b : array) {
                out.putByte(b);
            }
            return;
        }
        if (obj instanceof short[]) {
            short[] array = (short[])obj;
            if (array.length < 128) {
                out.putByte(ARRAY_SMALL_SHORT);
                out.putByte((byte)array.length);
            } else {
                out.putByte(ARRAY_LARGE_SHORT);
                out.putInt(array.length);
            }
            for (short value : array) {
                out.putShort(value);
            }
            return;
        }
        if (obj instanceof char[]) {
            char[] array = (char[])obj;
            if (array.length < 128) {
                out.putByte(ARRAY_SMALL_CHAR);
                out.putByte((byte)array.length);
            } else {
                out.putByte(ARRAY_LARGE_CHAR);
                out.putInt(array.length);
            }
            for (char c : array) {
                out.putChar(c);
            }
            return;
        }
        if (obj instanceof int[]) {
            int[] array = (int[])obj;
            if (array.length < 128) {
                out.putByte(ARRAY_SMALL_INT);
                out.putByte((byte)array.length);
            } else {
                out.putByte(ARRAY_LARGE_INT);
                out.putInt(array.length);
            }
            for (int j : array) {
                out.putInt(j);
            }
            return;
        }
        if (obj instanceof float[]) {
            float[] array = (float[])obj;
            if (array.length < 128) {
                out.putByte(ARRAY_SMALL_FLOAT);
                out.putByte((byte)array.length);
            } else {
                out.putByte(ARRAY_LARGE_FLOAT);
                out.putInt(array.length);
            }
            for (float v : array) {
                out.putFloat(v);
            }
            return;
        }
        if (obj instanceof long[]) {
            long[] array = (long[])obj;
            if (array.length < 128) {
                out.putByte(ARRAY_SMALL_LONG);
                out.putByte((byte)array.length);
            } else {
                out.putByte(ARRAY_LARGE_LONG);
                out.putInt(array.length);
            }
            for (long l : array) {
                out.putLong(l);
            }
            return;
        }
        if (obj instanceof double[]) {
            double[] array = (double[])obj;
            if (array.length < 128) {
                out.putByte(ARRAY_SMALL_DOUBLE);
                out.putByte((byte)array.length);
            } else {
                out.putByte(ARRAY_LARGE_DOUBLE);
                out.putInt(array.length);
            }
            for (double v : array) {
                out.putDouble(v);
            }
            return;
        }

        if (obj instanceof Object[]) {
            Object[] array = (Object[])obj;
            if (obj instanceof String[]) {
                if (array.length < 128) {
                    out.putByte(ARRAY_SMALL_STRING);
                    out.putByte((byte)array.length);
                } else {
                    out.putLong(ARRAY_LARGE_STRING);
                    out.putInt(array.length);
                }
            } else if (obj.getClass().getComponentType().equals(Object.class)) {
                if (array.length < 128) {
                    out.putByte(ARRAY_SMALL_OBJECT);
                    out.putByte((byte)array.length);
                } else {
                    out.putLong(ARRAY_LARGE_OBJECT);
                    out.putInt(array.length);
                }
            } else {
                if (array.length < 128) {
                    out.putByte(ARRAY_SMALL_CLASS);
                    out.putByte((byte)array.length);
                } else {
                    out.putLong(ARRAY_LARGE_CLASS);
                    out.putInt(array.length);
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

    private void serializeInstance(Object obj, Set<LoadedTypeDefinition> writtenClasses) {
        Class<?> cls = obj.getClass();
        LoadedTypeDefinition ltd = ctxt.getBootstrapClassContext().findDefinedType(cls.getName()).load();
        out.putByte(OBJECT);
        out.putShort((short)ltd.getTypeId());
        try {
            serializeInstanceFields(obj, ltd, writtenClasses);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            ctxt.error(e, "Error serializing instance fields of "+ltd.getInternalName());
        }
    }

    private void serializeInstanceFields(Object obj, LoadedTypeDefinition cur, Set<LoadedTypeDefinition> writtenClasses) throws IllegalAccessException, NoSuchFieldException {
        if (cur.hasSuperClass()) {
            serializeInstanceFields(obj, cur.getSuperClass(), writtenClasses);
        }
        writtenClasses.add(cur);
        for (int i=0; i<cur.getFieldCount(); i++) {
            FieldElement qf = cur.getField(i);
            if (!qf.isStatic()) {
                Field jf = obj.getClass().getField(qf.getName());
                if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.Z)){
                    out.putBoolean(jf.getBoolean(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.B)) {
                    out.putByte(jf.getByte(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.S)) {
                    out.putShort(jf.getShort(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.C)) {
                    out.putChar(jf.getChar(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.I)) {
                    out.putInt(jf.getInt(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.F)) {
                    out.putFloat(jf.getFloat(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.J)) {
                    out.putLong(jf.getLong(obj));
                } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.D)) {
                    out.putDouble(jf.getDouble(obj));
                } else {
                    writeObject(jf.get(obj), writtenClasses);
                }
            }
        }
    }

}
