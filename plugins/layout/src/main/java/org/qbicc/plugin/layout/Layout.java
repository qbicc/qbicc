package org.qbicc.plugin.layout;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;
import org.qbicc.type.generic.ClassSignature;
import org.qbicc.type.generic.ClassTypeSignature;
import org.qbicc.type.generic.TypeSignature;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class Layout {
    private static final AttachmentKey<Layout> KEY = new AttachmentKey<>();
    private static final InitializerResolver EMPTY_INIT = (index, enclosing) -> {
        InitializerElement.Builder builder = InitializerElement.builder();
        builder.setEnclosingType(enclosing);
        return builder.build();
    };
    private static final String INTERNAL_ARRAY = "internal_array";
    private static final TypeDescriptor typeTypeDescriptor = BaseTypeDescriptor.V; // TODO: Should be C (16 bit unsigned)
    private static final TypeSignature typeTypeSignature = BaseTypeSignature.V;    // TODO: Should be C (16 bit unsigned)

    private final Map<LoadedTypeDefinition, LayoutInfo> instanceLayouts = new ConcurrentHashMap<>();
    private final CompilationContext ctxt;
    private final FieldElement objectTypeIdField;
    private final FieldElement classTypeIdField;
    private final FieldElement classDimensionField;

    private final FieldElement arrayLengthField;

    private final FieldElement refArrayElementTypeIdField;
    private final FieldElement refArrayDimensionsField;
    private final FieldElement refArrayContentField;

    private final FieldElement booleanArrayContentField;

    private final FieldElement byteArrayContentField;
    private final FieldElement shortArrayContentField;
    private final FieldElement intArrayContentField;
    private final FieldElement longArrayContentField;

    private final FieldElement charArrayContentField;

    private final FieldElement floatArrayContentField;
    private final FieldElement doubleArrayContentField;

    private Layout(final CompilationContext ctxt) {
        this.ctxt = ctxt;
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        DefinedTypeDefinition jlcDef = classContext.findDefinedType("java/lang/Class");
        LoadedTypeDefinition jlo = jloDef.load();
        LoadedTypeDefinition jlc = jlcDef.load();

        // inject a field to hold the object typeId
        // TODO: This should be a 16 bit unsigned field.  It is being generated as an i32 currently.
        FieldElement.Builder builder = FieldElement.builder();
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        builder.setName("typeId");
        builder.setEnclosingType(jloDef);
        builder.setDescriptor(typeTypeDescriptor);
        builder.setSignature(typeTypeSignature);
        builder.setType(jlo.getClassType().getTypeType());
        FieldElement field = builder.build();
        jlo.injectField(field);
        objectTypeIdField = field;

        // now inject a field of ClassObjectType into Class to hold the corresponding run time type
        // TODO: This should be a 16 bit unsigned field.  It is being generated as an i32 currently.
        builder = FieldElement.builder();
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        builder.setName("id");
        builder.setEnclosingType(jlcDef);
        builder.setDescriptor(typeTypeDescriptor);
        builder.setSignature(typeTypeSignature);
        builder.setType(jlo.getClassType().getTypeType());
        field = builder.build();
        jlc.injectField(field);
        classTypeIdField = field;

        // now inject a field of int into Class to hold the corresponding run time dimensionality
        // TODO: This could be a 8 bit unsigned field.  It is being generated as an i32 currently.
        builder = FieldElement.builder();
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        builder.setName("dimension");
        builder.setEnclosingType(jlcDef);
        builder.setDescriptor(BaseTypeDescriptor.I);
        builder.setSignature(BaseTypeSignature.I);
        builder.setType(ctxt.getTypeSystem().getSignedInteger32Type());
        field = builder.build();
        jlc.injectField(field);
        classDimensionField = field;

        // now define classes for arrays
        // todo: assign special type ID values to array types
        TypeSystem ts = classContext.getTypeSystem();

        // define an array base type so that the length is always in the same place
        DefinedTypeDefinition.Builder typeBuilder = DefinedTypeDefinition.Builder.basic();
        ClassTypeDescriptor desc = ClassTypeDescriptor.synthesize(classContext, INTERNAL_ARRAY);
        typeBuilder.setDescriptor(desc);
        ClassTypeSignature superClassSig = (ClassTypeSignature) TypeSignature.synthesize(classContext, jlo.getDescriptor());
        typeBuilder.setSignature(ClassSignature.synthesize(classContext, superClassSig, List.of()));
        typeBuilder.setSuperClassName("java/lang/Object");
        typeBuilder.expectInterfaceNameCount(2);
        typeBuilder.addInterfaceName("java/lang/Cloneable");
        typeBuilder.addInterfaceName("java/io/Serializable");
        typeBuilder.setSimpleName("base_array_type");
        typeBuilder.setContext(classContext);
        typeBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PUBLIC | ClassFile.I_ACC_NO_REFLECT);
        typeBuilder.setName("base_array_type");
        typeBuilder.addField(Layout::makeLengthField, 0);
        typeBuilder.setInitializer(EMPTY_INIT, 0);
        DefinedTypeDefinition baseType = typeBuilder.build();

        arrayLengthField = baseType.load().getField(0);

        // primitives first

        booleanArrayContentField = defineArrayType(classContext, baseType, ts.getBooleanType(), "[Z").load().getField(0);

        byteArrayContentField = defineArrayType(classContext, baseType, ts.getSignedInteger8Type(), "[B").load().getField(0);
        shortArrayContentField = defineArrayType(classContext, baseType, ts.getSignedInteger16Type(), "[S").load().getField(0);
        intArrayContentField = defineArrayType(classContext, baseType, ts.getSignedInteger32Type(), "[I").load().getField(0);
        longArrayContentField = defineArrayType(classContext, baseType, ts.getSignedInteger64Type(), "[J").load().getField(0);

        charArrayContentField = defineArrayType(classContext, baseType, ts.getUnsignedInteger16Type(), "[C").load().getField(0);

        floatArrayContentField = defineArrayType(classContext, baseType, ts.getFloat32Type(), "[F").load().getField(0);
        doubleArrayContentField = defineArrayType(classContext, baseType, ts.getFloat64Type(), "[D").load().getField(0);

        // now the reference array class

        LoadedTypeDefinition refArrayType = defineArrayType(classContext, baseType, jlo.getClassType().getReference().asNullable(), "[L").load();
        refArrayDimensionsField = refArrayType.getField(0);
        refArrayElementTypeIdField = refArrayType.getField(1);
        refArrayContentField = refArrayType.getField(2);
    }

    private static DefinedTypeDefinition defineArrayType(ClassContext classContext, DefinedTypeDefinition superClass, ValueType realMemberType, String simpleName) {
        DefinedTypeDefinition.Builder typeBuilder = DefinedTypeDefinition.Builder.basic();
        String internalName = INTERNAL_ARRAY + "_" + simpleName.charAt(1);
        ClassTypeDescriptor desc = ClassTypeDescriptor.synthesize(classContext, internalName);
        typeBuilder.setDescriptor(desc);
        ClassTypeSignature superClassSig = (ClassTypeSignature) TypeSignature.synthesize(classContext, superClass.getDescriptor());
        typeBuilder.setSignature(ClassSignature.synthesize(classContext, superClassSig, List.of()));
        typeBuilder.setSuperClass(superClass);
        typeBuilder.setSuperClassName(superClass.getInternalName());
        typeBuilder.setSimpleName(simpleName);
        typeBuilder.setContext(classContext);
        typeBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PUBLIC | ClassFile.I_ACC_NO_REFLECT);
        typeBuilder.setName(internalName);
        // add fields in this order, which is relied upon up above
        int idx = 0;
        if (realMemberType instanceof ReferenceType) {
            // also need a dimensions field
            typeBuilder.addField(Layout::makeDimensionsField, idx++);
            // also need a type ID field
            typeBuilder.addField((index, encl) -> makeElementTypeIdField(index, superClass, encl), idx++);
        }
        typeBuilder.addField((index, enclosing) -> makeContentField(index, enclosing, realMemberType), idx);
        typeBuilder.setInitializer(EMPTY_INIT, 0);
        return typeBuilder.build();
    }

    private static FieldElement makeDimensionsField(final int index, final DefinedTypeDefinition enclosing) {
        FieldElement.Builder fieldBuilder = FieldElement.builder();
        fieldBuilder.setEnclosingType(enclosing);
        // TODO: This should be a 8 bit unsigned field. (max dimensions is 255 from multianewarray)
        fieldBuilder.setDescriptor(BaseTypeDescriptor.I);
        fieldBuilder.setSignature(BaseTypeSignature.I);
        fieldBuilder.setIndex(index);
        fieldBuilder.setName("dims");
        fieldBuilder.setType(enclosing.getContext().getTypeSystem().getSignedInteger32Type());
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        return fieldBuilder.build();
    }

    private static FieldElement makeLengthField(final int index, final DefinedTypeDefinition enclosing) {
        FieldElement.Builder fieldBuilder = FieldElement.builder();
        fieldBuilder.setEnclosingType(enclosing);
        fieldBuilder.setDescriptor(BaseTypeDescriptor.I);
        fieldBuilder.setSignature(BaseTypeSignature.I);
        fieldBuilder.setIndex(index);
        fieldBuilder.setName("length");
        fieldBuilder.setType(enclosing.getContext().getTypeSystem().getSignedInteger32Type());
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        return fieldBuilder.build();
    }

    private static FieldElement makeElementTypeIdField(final int index, final DefinedTypeDefinition jlo, final DefinedTypeDefinition enclosing) {
        FieldElement.Builder fieldBuilder = FieldElement.builder();
        fieldBuilder.setEnclosingType(enclosing);
        // TODO: This should be a 16 bit unsigned field.  It is being generated as an i32 currently.
        fieldBuilder.setDescriptor(typeTypeDescriptor);
        fieldBuilder.setSignature(typeTypeSignature);
        fieldBuilder.setIndex(index);
        fieldBuilder.setName("elementType");
        fieldBuilder.setType(jlo.load().getClassType().getReference().getTypeType());
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        return fieldBuilder.build();
    }

    private static FieldElement makeContentField(final int index, final DefinedTypeDefinition enclosing, final ValueType realMemberType) {
        FieldElement.Builder fieldBuilder = FieldElement.builder();
        fieldBuilder.setEnclosingType(enclosing);
        fieldBuilder.setDescriptor(BaseTypeDescriptor.V);
        fieldBuilder.setSignature(BaseTypeSignature.V);
        fieldBuilder.setIndex(index);
        fieldBuilder.setName("content");
        fieldBuilder.setType(enclosing.getContext().getTypeSystem().getArrayType(realMemberType, 0));
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        return fieldBuilder.build();
    }

    public static Layout get(CompilationContext ctxt) {
        Layout layout = ctxt.getAttachment(KEY);
        if (layout == null) {
            layout = new Layout(ctxt);
            Layout appearing = ctxt.putAttachmentIfAbsent(KEY, layout);
            if (appearing != null) {
                layout = appearing;
            }
        }
        return layout;
    }

    public FieldElement getArrayContentField(final ObjectType arrayObjType) {
        if (arrayObjType instanceof PrimitiveArrayObjectType) {
            // read value from primitive array; have to select on the type
            WordType elementType = ((PrimitiveArrayObjectType) arrayObjType).getElementType();
            boolean signed = elementType instanceof SignedIntegerType;
            boolean floating = elementType instanceof FloatType;
            int size = elementType.getMinBits();
            if (signed) {
                if (size == 8) {
                    // byte
                    return getByteArrayContentField();
                } else if (size == 16) {
                    // short
                    return getShortArrayContentField();
                } else if (size == 32) {
                    // int
                    return getIntArrayContentField();
                } else if (size == 64) {
                    // long
                    return getLongArrayContentField();
                } else {
                    return null;
                }
            } else if (floating) {
                if (size == 32) {
                    // float
                    return getFloatArrayContentField();
                } else if (size == 64) {
                    // double
                    return getDoubleArrayContentField();
                } else {
                    return null;
                }
            } else {
                if (size == 16) {
                    // char
                    return getCharArrayContentField();
                } else if (elementType instanceof BooleanType) {
                    return getBooleanArrayContentField();
                } else {
                    return null;
                }
            }
        } else if (arrayObjType instanceof ReferenceArrayObjectType) {
            // read value from reference array
            return getRefArrayContentField();
        } else {
            return null;
        }
    }

    public LoadedTypeDefinition getArrayLoadedTypeDefinition(String arrayType) {
        switch(arrayType) {
        case "[Z":
            return booleanArrayContentField.getEnclosingType().load();
        case "[B":
            return byteArrayContentField.getEnclosingType().load();
        case "[S":
            return shortArrayContentField.getEnclosingType().load();
        case "[C":
            return charArrayContentField.getEnclosingType().load();
        case "[I":
            return intArrayContentField.getEnclosingType().load();
        case "[F":
            return floatArrayContentField.getEnclosingType().load();
        case "[J":
            return longArrayContentField.getEnclosingType().load();
        case "[D":
            return doubleArrayContentField.getEnclosingType().load();
        case "[ref":
            return refArrayContentField.getEnclosingType().load();
        default:
            throw Assert.impossibleSwitchCase("arrayType");
        }
    }

    /**
     * Get the object field which holds the run time type identifier.
     *
     * @return the type identifier field
     */
    public FieldElement getObjectTypeIdField() {
        return objectTypeIdField;
    }

    /**
     * Get the field on {@code Class} which holds the type identifier of its corresponding instance type.
     *
     * @return the class type identifier field
     */
    public FieldElement getClassTypeIdField() {
        return classTypeIdField;
    }

    public FieldElement getClassDimensionField() {
        return classDimensionField;
    }

    public FieldElement getArrayLengthField() {
        return arrayLengthField;
    }

    public FieldElement getRefArrayElementTypeIdField() {
        return refArrayElementTypeIdField;
    }

    public FieldElement getRefArrayDimensionsField() {
        return refArrayDimensionsField;
    }

    public FieldElement getRefArrayContentField() {
        return refArrayContentField;
    }

    public FieldElement getBooleanArrayContentField() {
        return booleanArrayContentField;
    }

    public FieldElement getByteArrayContentField() {
        return byteArrayContentField;
    }

    public FieldElement getShortArrayContentField() {
        return shortArrayContentField;
    }

    public FieldElement getIntArrayContentField() {
        return intArrayContentField;
    }

    public FieldElement getLongArrayContentField() {
        return longArrayContentField;
    }

    public FieldElement getCharArrayContentField() {
        return charArrayContentField;
    }

    public FieldElement getFloatArrayContentField() {
        return floatArrayContentField;
    }

    public FieldElement getDoubleArrayContentField() {
        return doubleArrayContentField;
    }

    public LayoutInfo getInstanceLayoutInfo(DefinedTypeDefinition type) {
        if (type.isInterface()) {
            throw new IllegalArgumentException("Interfaces have no instance layout");
        }
        LoadedTypeDefinition validated = type.load();
        LayoutInfo layoutInfo = instanceLayouts.get(validated);
        if (layoutInfo != null) {
            return layoutInfo;
        }
        LoadedTypeDefinition superClass = validated.getSuperClass();
        LayoutInfo superLayout;
        if (superClass != null) {
            superLayout = getInstanceLayoutInfo(superClass);
        } else {
            superLayout = null;
        }
        BitSet allocated = new BitSet();
        if (superLayout != null) {
            allocated.or(superLayout.allocated);
        }
        int cnt = validated.getFieldCount();
        Map<FieldElement, CompoundType.Member> fieldToMember = superLayout == null ? new HashMap<>(cnt) : new HashMap<>(superLayout.fieldToMember);
        for (int i = 0; i < cnt; i ++) {
            // todo: skip unused fields?
            FieldElement field = validated.getField(i);
            if (field.isStatic()) {
                continue;
            }
            fieldToMember.put(field, computeMember(allocated, field));
        }
        int size = allocated.length();
        CompoundType.Member[] membersArray = fieldToMember.values().toArray(CompoundType.Member[]::new);
        Arrays.sort(membersArray);
        List<CompoundType.Member> membersList = List.of(membersArray);
        CompoundType compoundType = ctxt.getTypeSystem().getCompoundType(CompoundType.Tag.NONE, type.getInternalName().replace('/', '.'), size, 1, () -> membersList);
        layoutInfo = new LayoutInfo(allocated, compoundType, fieldToMember);
        LayoutInfo appearing = instanceLayouts.putIfAbsent(validated, layoutInfo);
        return appearing != null ? appearing : layoutInfo;
    }

    private ValueType widenBoolean(ValueType type) {
        if (type instanceof BooleanType) {
            return ctxt.getTypeSystem().getUnsignedInteger8Type();
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            ValueType elementType = arrayType.getElementType();
            ValueType widened = widenBoolean(elementType);
            return elementType == widened ? type : ctxt.getTypeSystem().getArrayType(widened, arrayType.getElementCount());
        } else {
            return type;
        }
    }

    private CompoundType.Member computeMember(final BitSet allocated, final FieldElement field) {
        TypeSystem ts = ctxt.getTypeSystem();
        ValueType fieldType = widenBoolean(field.getType());
        if (fieldType instanceof BooleanType) {
            // widen booleans to 8 bits
            fieldType = ts.getUnsignedInteger8Type();
        }
        int size = (int) fieldType.getSize();
        int align = fieldType.getAlign();
        int idx = find(allocated, align, size);
        allocated.set(idx, idx + size);
        return ts.getCompoundTypeMember(field.getName(), fieldType, idx, align);
    }

    /**
     * Find a sequence of consecutive zero bits with the given alignment and count.  Current implementation finds the
     * first fit.
     *
     * @param bitSet the bit set to search
     * @param alignment the alignment
     * @param size the size
     * @return the bit index
     */
    private int find(BitSet bitSet, int alignment, int size) {
        assert Integer.bitCount(alignment) == 1;
        int mask = alignment - 1;
        int i = bitSet.nextClearBit(0);
        int n;
        for (;;) {
            // adjust for alignment
            int amt = mask - (i - 1 & mask);
            while (amt > 0) {
                i = bitSet.nextClearBit(i + amt);
                amt = mask - (i - 1 & mask);
            }
            // check the size
            n = bitSet.nextSetBit(i);
            if (n == -1 || n - i >= size) {
                // found a fit
                return i;
            }
            // try the next spot
            i = bitSet.nextClearBit(n);
        }
    }

    public static final class LayoutInfo {
        private final BitSet allocated;
        private final CompoundType compoundType;
        private final Map<FieldElement, CompoundType.Member> fieldToMember;

        LayoutInfo(final BitSet allocated, final CompoundType compoundType, final Map<FieldElement, CompoundType.Member> fieldToMember) {
            this.allocated = allocated;
            this.compoundType = compoundType;
            this.fieldToMember = fieldToMember;
        }

        public CompoundType getCompoundType() {
            return compoundType;
        }

        public CompoundType.Member getMember(FieldElement element) {
            return fieldToMember.get(element);
        }
    }
}
