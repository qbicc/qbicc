package org.qbicc.plugin.serialization;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.CompoundType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;

public class BuildtimeHeap {
    private static final AttachmentKey<BuildtimeHeap> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Layout layout;
    private final GrowableByteBuffer heap;
    private final HashMap<String, Integer>  stringLiterals = new HashMap<>();
    private final IdentityHashMap<Object, Integer> objects = new IdentityHashMap<>();
    private final ArrayList<Literal> relocateOffsets = new ArrayList<>();

    private BuildtimeHeap(CompilationContext ctxt) {
        this.ctxt = ctxt;
        this.layout = Layout.get(ctxt);
        this.heap = new GrowableByteBuffer(ctxt.getTypeSystem().getEndianness(), ctxt.getTypeSystem().getPointerSize());
    }

    public static BuildtimeHeap get(CompilationContext ctxt) {
        BuildtimeHeap heap = ctxt.getAttachment(KEY);
        if (heap == null) {
            heap = new BuildtimeHeap(ctxt);
            BuildtimeHeap appearing = ctxt.putAttachmentIfAbsent(KEY, heap);
            if (appearing != null) {
                heap = appearing;
            }
        }
        return heap;
    }

    public byte[] getHeapBytes() {
        return heap.getBytes();
    }

    public List<Literal> getHeapRelocations() {
        return relocateOffsets;
    }

    public synchronized int serializeStringLiteral(String value) {
        // String literals are interned via equals, not ==
        if (stringLiterals.containsKey(value)) {
            return stringLiterals.get(value);
        }
        LoadedTypeDefinition jls = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load();
        int offset = serializeObject(jls, value);
        stringLiterals.put(value, offset);
        return offset;
    }

    public synchronized int serializeObject(Object obj) {
        if (objects.containsKey(obj)) {
            return objects.get(obj);
        }

        Class<?> cls = obj.getClass();
        int offset;
        if (cls.isArray()) {
            if (obj instanceof byte[]) {
                offset = serializeArray((byte[]) obj);
            } else if (obj instanceof boolean[]) {
                offset = serializeArray((boolean[]) obj);
            } else if (obj instanceof char[]) {
                offset =  serializeArray((char[]) obj);
            } else if (obj instanceof short[]) {
                offset =  serializeArray((short[]) obj);
            } else if (obj instanceof int[]) {
                offset = serializeArray((int[]) obj);
            } else if (obj instanceof float[]) {
                offset = serializeArray((float[]) obj);
            } else if (obj instanceof long[]) {
                offset = serializeArray((long[]) obj);
            } else if (obj instanceof double[]) {
                offset = serializeArray((double[]) obj);
            } else {
                offset = serializeArray((Object[]) obj);
            }
        } else {
            LoadedTypeDefinition ltd = ctxt.getBootstrapClassContext().findDefinedType(cls.getName()).load();
            offset = serializeObject(ltd, obj);
        }

        objects.put(obj, offset);
        return offset;
    }

    private void writeObjectHeader(int startOffset, Layout.LayoutInfo li, int typeId) {
        heap.putInt(startOffset + li.getMember(layout.getObjectTypeIdField()).getOffset(), typeId);
    }

    private void writePrimArrayHeader(int startOffset, Layout.LayoutInfo li, int arrayTypeId, int length) {
        heap.putInt(startOffset + li.getMember(layout.getObjectTypeIdField()).getOffset(), arrayTypeId);
        heap.putInt(startOffset + li.getMember(layout.getArrayLengthField()).getOffset(), length);
    }

    private void writeRefArrayHeader(int startOffset, Layout.LayoutInfo li, int arrayTypeId, int dimensions, int leafElemTypeId, int length) {
        heap.putInt(startOffset + li.getMember(layout.getObjectTypeIdField()).getOffset(), arrayTypeId);
        heap.putInt(startOffset + li.getMember(layout.getArrayLengthField()).getOffset(), length);
        heap.putInt(startOffset + li.getMember(layout.getRefArrayElementTypeIdField()).getOffset(), leafElemTypeId);
        heap.putInt(startOffset + li.getMember(layout.getRefArrayDimensionsField()).getOffset(), dimensions);
    }

    private int serializeObject(LoadedTypeDefinition concreteType, Object instance) {
        Layout.LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
        CompoundType objType = objLayout.getCompoundType();
        int startOffset = heap.allocate(objType, 0);
        writeObjectHeader(startOffset, objLayout, concreteType.getTypeId());

        Class<?> jClass = instance.getClass();
        LoadedTypeDefinition qClass = concreteType;
        while (qClass.hasSuperClass()) {
            for (Field jf : jClass.getDeclaredFields()) {
                FieldElement qf = qClass.findField(jf.getName());
                jf.setAccessible(true);
                if (qf != null && !qf.isStatic()) {
                    try {
                        int offset = startOffset + objLayout.getMember(qf).getOffset();
                        if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.Z)) {
                            heap.putBoolean(offset, jf.getBoolean(instance));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.B)) {
                            heap.putByte(offset, jf.getByte(instance));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.S)) {
                            heap.putShort(offset, jf.getShort(instance));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.C)) {
                            heap.putChar(offset, jf.getChar(instance));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.I)) {
                            heap.putInt(offset, jf.getInt(instance));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.F)) {
                            heap.putFloat(offset, jf.getFloat(instance));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.J)) {
                            heap.putLong(offset, jf.getLong(instance));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.D)) {
                            heap.putDouble(offset, jf.getDouble(instance));
                        } else {
                            Object fieldContents = jf.get(instance);
                            if (fieldContents != null) {
                                int targetOffset = serializeObject(fieldContents);
                                heap.putPointer(offset, targetOffset);
                                relocateOffsets.add(ctxt.getLiteralFactory().literalOf(offset));
                            }
                        }
                    } catch (IllegalAccessException e) {
                        ctxt.error("Heap Serialization: denied access to field %s of %s", jf.getName(), jClass);
                    }
                }
            }
            qClass = qClass.getSuperClass();
            jClass = jClass.getSuperclass();
        }
        return startOffset;
    }

    private int serializeArray(byte[] array) {
        Layout.LayoutInfo objLayout = layout.getInstanceLayoutInfo(layout.getByteArrayContentField().getEnclosingType());
        int startOffset = heap.allocate(objLayout.getCompoundType(), array.length);
        writePrimArrayHeader(startOffset, objLayout, layout.getByteArrayContentField().getEnclosingType().load().getTypeId(), array.length);
        int contentsBaseOffset = startOffset +  objLayout.getMember(layout.getByteArrayContentField()).getOffset();
        for (int i=0; i<array.length; i++) {
            heap.putByte(contentsBaseOffset + i, array[i]);
        }
        return startOffset;
    }

    private int serializeArray(boolean[] array) {
        // TODO:
        return -1;
    }

    private int serializeArray(char[] array) {
        // TODO:
        return -1;
    }

    private int serializeArray(short[] array) {
        // TODO:
        return -1;
    }

    private int serializeArray(int[] array) {
        // TODO:
        return -1;
    }

    private int serializeArray(float[] array) {
        // TODO:
        return -1;
    }

    private int serializeArray(long[] array) {
        // TODO:
        return -1;
    }

    private int serializeArray(double[] array) {
        // TODO:
        return -1;
    }

    private int serializeArray(Object[] array) {
        // TODO:
        return -1;
    }
}
