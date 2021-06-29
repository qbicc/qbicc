package org.qbicc.plugin.serialization;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.object.Data;
import org.qbicc.object.Linkage;
import org.qbicc.object.Section;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;

public class BuildtimeHeap {
    private static final AttachmentKey<BuildtimeHeap> KEY = new AttachmentKey<>();
    private static final String prefix = "qbicc_initial_heap_obj_";

    private final CompilationContext ctxt;
    private final Layout layout;
    /** For lazy definition of native array types for literals */
    private final HashMap<String, CompoundType> arrayTypes = new HashMap<>();
    /** For interning string literals */
    private final HashMap<String, Data>  stringLiterals = new HashMap<>();
    /** For interning objects */
    private final IdentityHashMap<Object, Data> objects = new IdentityHashMap<>();
    /** The initial heap */
    private final Section heapSection;

    private int literalCounter = 0;

    private BuildtimeHeap(CompilationContext ctxt) {
        this.ctxt = ctxt;
        this.layout = Layout.get(ctxt);

        LoadedTypeDefinition ih = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap").load();
        this.heapSection = ctxt.getOrAddProgramModule(ih).getOrAddSection(ctxt.IMPLICIT_SECTION_NAME); // TODO: use ctxt.INITIAL_HEAP_SECTION_NAME
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

    public synchronized Data serializeStringLiteral(String value) {
        // String literals are interned via equals, not ==
        if (stringLiterals.containsKey(value)) {
            return stringLiterals.get(value);
        }
        LoadedTypeDefinition jls = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load();
        Data sl = serializeObject(jls, value);
        stringLiterals.put(value, sl);
        return sl;
    }

    public synchronized Data serializeObject(Object obj) {
        if (objects.containsKey(obj)) {
            return objects.get(obj);
        }

        Class<?> cls = obj.getClass();
        Data data;
        if (cls.isArray()) {
            if (obj instanceof byte[]) {
                data = serializeArray((byte[]) obj);
            } else if (obj instanceof boolean[]) {
                data = serializeArray((boolean[]) obj);
            } else if (obj instanceof char[]) {
                data =  serializeArray((char[]) obj);
            } else if (obj instanceof short[]) {
                data =  serializeArray((short[]) obj);
            } else if (obj instanceof int[]) {
                data = serializeArray((int[]) obj);
            } else if (obj instanceof float[]) {
                data = serializeArray((float[]) obj);
            } else if (obj instanceof long[]) {
                data = serializeArray((long[]) obj);
            } else if (obj instanceof double[]) {
                data = serializeArray((double[]) obj);
            } else {
                data = serializeArray((Object[]) obj);
            }
        } else {
            LoadedTypeDefinition ltd = ctxt.getBootstrapClassContext().findDefinedType(cls.getName()).load();
            data = serializeObject(ltd, obj);
        }

        objects.put(obj, data);
        return data;
    }

    private String nextLiteralName() {
        return prefix+(this.literalCounter++);
    }

    private Data defineData(String name, Literal value) {
        Data d = heapSection.addData(null,name, value);
        d.setLinkage(Linkage.EXTERNAL);
        d.setAddrspace(1);
        return d;
    }

    private Data serializeObject(LoadedTypeDefinition concreteType, Object instance) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        Layout.LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
        CompoundType objType = objLayout.getCompoundType();
        HashMap<CompoundType.Member, Literal> memberMap = new HashMap<>();

        // Object header
        memberMap.put(objLayout.getMember(layout.getObjectTypeIdField()), lf.literalOf(concreteType.getTypeId()));

        // Instance fields
        Class<?> jClass = instance.getClass();
        LoadedTypeDefinition qClass = concreteType;
        while (qClass.hasSuperClass()) {
            for (Field jf : jClass.getDeclaredFields()) {
                FieldElement qf = qClass.findField(jf.getName());
                CompoundType.Member member = objLayout.getMember(qf);
                jf.setAccessible(true);
                if (qf != null && !qf.isStatic()) {
                    try {
                        if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.Z)) {
                            memberMap.put(member, lf.literalOf(jf.getBoolean(instance)));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.B)) {
                            memberMap.put(member, lf.literalOf(jf.getByte(instance)));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.S)) {
                            memberMap.put(member, lf.literalOf(jf.getShort(instance)));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.C)) {
                            memberMap.put(member, lf.literalOf(jf.getChar(instance)));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.I)) {
                            memberMap.put(member, lf.literalOf(jf.getInt(instance)));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.F)) {
                            memberMap.put(member, lf.literalOf(jf.getFloat(instance)));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.J)) {
                            memberMap.put(member, lf.literalOf(jf.getLong(instance)));
                        } else if (qf.getTypeDescriptor().equals(BaseTypeDescriptor.D)) {
                            memberMap.put(member, lf.literalOf(jf.getDouble(instance)));
                        } else {
                            Object fieldContents = jf.get(instance);
                            if (fieldContents == null) {
                                memberMap.put(member, lf.zeroInitializerLiteralOfType(member.getType()));
                            } else {
                                Data contents = serializeObject(fieldContents);
                                SymbolLiteral refToContents = lf.literalOfSymbol(contents.getName(), contents.getType().getPointer().asCollected());
                                memberMap.put(member, lf.bitcastLiteral(refToContents, (WordType)member.getType()));
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

        return defineData(nextLiteralName(), ctxt.getLiteralFactory().literalOf(objType, memberMap));
    }

    private CompoundType arrayLiteralType(FieldElement contents, int length) {
        LoadedTypeDefinition ltd = contents.getEnclosingType().load();
        String typeName = ltd.getInternalName() + "_"+length;
        CompoundType sizedArrayType = arrayTypes.get(typeName);
        if (sizedArrayType == null) {
            TypeSystem ts = ctxt.getTypeSystem();
            Layout.LayoutInfo objLayout = layout.getInstanceLayoutInfo(ltd);
            CompoundType arrayCT = objLayout.getCompoundType();

            CompoundType.Member contentMem = objLayout.getMember(contents);
            ArrayType sizedContentMem = ts.getArrayType(((ArrayType)contents.getType()).getElementType(), length);
            CompoundType.Member realContentMem = ts.getCompoundTypeMember(contentMem.getName(), sizedContentMem, contentMem.getOffset(), contentMem.getAlign());

            sizedArrayType = ts.getCompoundType(CompoundType.Tag.STRUCT, typeName,arrayCT.getSize() + sizedContentMem.getSize(),
                arrayCT.getAlign(), () -> List.of(arrayCT.getMember(0), arrayCT.getMember(1), realContentMem));

            arrayTypes.put(typeName, sizedArrayType);
        }
        return sizedArrayType;
    }

    private Data serializeArray(byte[] array) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        FieldElement contentsField = CoreClasses.get(ctxt).getByteArrayContentField();
        CompoundType literalCT = arrayLiteralType(contentsField, array.length);
        Literal arrayLiteral = lf.literalOf(literalCT, Map.of(
            literalCT.getMember(0), lf.literalOf(contentsField.getEnclosingType().load().getTypeId()),
            literalCT.getMember(1), lf.literalOf(array.length),
            literalCT.getMember(2), lf.literalOf(ctxt.getTypeSystem().getArrayType(ctxt.getTypeSystem().getSignedInteger8Type(), array.length), array)
        ));
        return defineData(nextLiteralName(), arrayLiteral);
    }

    private Data serializeArray(boolean[] array) {
        // TODO:
        return null;
    }

    private Data serializeArray(char[] array) {
        // TODO:
        return null;
    }

    private Data serializeArray(short[] array) {
        // TODO:
        return null;
    }

    private Data serializeArray(int[] array) {
        // TODO:
        return null;
    }

    private Data serializeArray(float[] array) {
        // TODO:
        return null;
    }

    private Data serializeArray(long[] array) {
        // TODO:
        return null;
    }

    private Data serializeArray(double[] array) {
        // TODO:
        return null;
    }

    private Data serializeArray(Object[] array) {
        // TODO:
        return null;
    }
}
