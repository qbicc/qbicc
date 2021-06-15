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

    private final CompilationContext ctxt;
    private final Layout layout;
    private final HashMap<String, SymbolLiteral>  stringLiterals = new HashMap<>();
    private final IdentityHashMap<Object, SymbolLiteral> objects = new IdentityHashMap<>();
    private final HashMap<SymbolLiteral, Literal> initialHeap = new HashMap<>();
    private int literalCounter = 0;

    private BuildtimeHeap(CompilationContext ctxt) {
        this.ctxt = ctxt;
        this.layout = Layout.get(ctxt);
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

    public Map<SymbolLiteral, Literal> getHeap() {
        return initialHeap;
    }

    public synchronized SymbolLiteral serializeStringLiteral(String value) {
        // String literals are interned via equals, not ==
        if (stringLiterals.containsKey(value)) {
            return stringLiterals.get(value);
        }
        LoadedTypeDefinition jls = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load();
        SymbolLiteral sl = serializeObject(jls, value);
        stringLiterals.put(value, sl);
        return sl;
    }

    public synchronized SymbolLiteral serializeObject(Object obj) {
        if (objects.containsKey(obj)) {
            return objects.get(obj);
        }

        Class<?> cls = obj.getClass();
        SymbolLiteral lit;
        if (cls.isArray()) {
            if (obj instanceof byte[]) {
                lit = serializeArray((byte[]) obj);
            } else if (obj instanceof boolean[]) {
                lit = serializeArray((boolean[]) obj);
            } else if (obj instanceof char[]) {
                lit =  serializeArray((char[]) obj);
            } else if (obj instanceof short[]) {
                lit =  serializeArray((short[]) obj);
            } else if (obj instanceof int[]) {
                lit = serializeArray((int[]) obj);
            } else if (obj instanceof float[]) {
                lit = serializeArray((float[]) obj);
            } else if (obj instanceof long[]) {
                lit = serializeArray((long[]) obj);
            } else if (obj instanceof double[]) {
                lit = serializeArray((double[]) obj);
            } else {
                lit = serializeArray((Object[]) obj);
            }
        } else {
            LoadedTypeDefinition ltd = ctxt.getBootstrapClassContext().findDefinedType(cls.getName()).load();
            lit = serializeObject(ltd, obj);
        }

        objects.put(obj, lit);
        return lit;
    }

    private String nextLiteralName() {
        int lc = this.literalCounter++;
        return "qbicc_initial_heap_obj_"+lc;
    }

    private SymbolLiteral serializeObject(LoadedTypeDefinition concreteType, Object instance) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        Layout.LayoutInfo objLayout = layout.getInstanceLayoutInfo(concreteType);
        CompoundType objType = objLayout.getCompoundType();
        String myName = nextLiteralName();
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
                                SymbolLiteral contents = serializeObject(fieldContents);
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

        Literal objLiteral = ctxt.getLiteralFactory().literalOf(objType, memberMap);
        SymbolLiteral objName = lf.literalOfSymbol(myName, objType);
        initialHeap.put(objName, objLiteral);

        return objName;
    }

    private SymbolLiteral serializeArray(byte[] array) {
        TypeSystem ts = ctxt.getTypeSystem();
        LiteralFactory lf = ctxt.getLiteralFactory();
        String myName = nextLiteralName();

        LoadedTypeDefinition arrayLTD = layout.getByteArrayContentField().getEnclosingType().load();
        Layout.LayoutInfo objLayout = layout.getInstanceLayoutInfo(arrayLTD);
        CompoundType arrayCT = objLayout.getCompoundType();

        CompoundType.Member contentMem = objLayout.getMember(layout.getByteArrayContentField());
        ArrayType s8ArrayType = ts.getArrayType(ts.getSignedInteger8Type(), array.length);
        CompoundType.Member realContentMem = ts.getCompoundTypeMember(contentMem.getName(), s8ArrayType, contentMem.getOffset(), contentMem.getAlign());

        CompoundType literalCT = ts.getCompoundType(CompoundType.Tag.STRUCT, myName+"_type",arrayCT.getSize() + s8ArrayType.getSize(),
            arrayCT.getAlign(), () -> List.of(arrayCT.getMember(0), arrayCT.getMember(1), realContentMem));

        Literal arrayLiteral = lf.literalOf(literalCT, Map.of(
            literalCT.getMember(0), lf.literalOf(arrayLTD.getTypeId()),
            literalCT.getMember(1), lf.literalOf(array.length),
            realContentMem, lf.literalOf(s8ArrayType, array)
        ));
        SymbolLiteral arrayName = lf.literalOfSymbol(myName, literalCT);
        initialHeap.put(arrayName, arrayLiteral);

        return arrayName;
    }

    private SymbolLiteral serializeArray(boolean[] array) {
        // TODO:
        return null;
    }

    private SymbolLiteral serializeArray(char[] array) {
        // TODO:
        return null;
    }

    private SymbolLiteral serializeArray(short[] array) {
        // TODO:
        return null;
    }

    private SymbolLiteral serializeArray(int[] array) {
        // TODO:
        return null;
    }

    private SymbolLiteral serializeArray(float[] array) {
        // TODO:
        return null;
    }

    private SymbolLiteral serializeArray(long[] array) {
        // TODO:
        return null;
    }

    private SymbolLiteral serializeArray(double[] array) {
        // TODO:
        return null;
    }

    private SymbolLiteral serializeArray(Object[] array) {
        // TODO:
        return null;
    }
}
