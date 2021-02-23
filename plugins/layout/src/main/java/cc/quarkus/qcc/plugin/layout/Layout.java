package cc.quarkus.qcc.plugin.layout;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.PrimitiveArrayObjectType;
import cc.quarkus.qcc.type.ReferenceArrayObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.InitializerResolver;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.generic.BaseTypeSignature;
import cc.quarkus.qcc.type.generic.ClassSignature;
import cc.quarkus.qcc.type.generic.ClassTypeSignature;
import cc.quarkus.qcc.type.generic.TypeSignature;
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

    private final Map<ValidatedTypeDefinition, LayoutInfo> instanceLayouts = new ConcurrentHashMap<>();
    private final CompilationContext ctxt;
    private final FieldElement objectTypeIdField;
    private final FieldElement classTypeIdField;

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
        ValidatedTypeDefinition jlo = jloDef.validate();
        ValidatedTypeDefinition jlc = jlcDef.validate();

        // inject a 16bit unsigned int field to hold the object typeId
        FieldElement.Builder builder = FieldElement.builder();
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL | ClassFile.I_ACC_HIDDEN);
        builder.setName("typeId");
        builder.setEnclosingType(jloDef);
        // typeId is a 16 bit unsigned int value, Char is the closest descriptor
        builder.setDescriptor(BaseTypeDescriptor.C);
        builder.setSignature(BaseTypeSignature.C);
        builder.setType(jlo.getClassType().getTypeType());
        FieldElement field = builder.build();
        jlo.injectField(field);
        objectTypeIdField = field;

        // now inject a field of ClassObjectType into Class to hold the corresponding run time type
        builder = FieldElement.builder();
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL | ClassFile.I_ACC_HIDDEN);
        builder.setName("id");
        builder.setEnclosingType(jlcDef);
        // void for now, but this is cheating terribly
        builder.setDescriptor(BaseTypeDescriptor.V);
        builder.setSignature(BaseTypeSignature.V);
        builder.setType(jlo.getClassType().getTypeType());
        field = builder.build();
        jlc.injectField(field);
        classTypeIdField = field;

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
        typeBuilder.setSimpleName("base array type");
        typeBuilder.setContext(classContext);
        typeBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PUBLIC | ClassFile.I_ACC_HIDDEN);
        typeBuilder.setName("base array type");
        typeBuilder.addField(Layout::makeLengthField, 0);
        typeBuilder.setInitializer(EMPTY_INIT, 0);
        DefinedTypeDefinition baseType = typeBuilder.build();

        arrayLengthField = baseType.validate().getField(0);

        // primitives first
        // booleans are special
        booleanArrayContentField = defineArrayType(classContext, baseType, ts.getUnsignedInteger8Type(), "[Z").validate().getField(0);

        byteArrayContentField = defineArrayType(classContext, baseType, ts.getSignedInteger8Type(), "[B").validate().getField(0);
        shortArrayContentField = defineArrayType(classContext, baseType, ts.getSignedInteger16Type(), "[S").validate().getField(0);
        intArrayContentField = defineArrayType(classContext, baseType, ts.getSignedInteger32Type(), "[I").validate().getField(0);
        longArrayContentField = defineArrayType(classContext, baseType, ts.getSignedInteger64Type(), "[J").validate().getField(0);

        charArrayContentField = defineArrayType(classContext, baseType, ts.getUnsignedInteger16Type(), "[C").validate().getField(0);

        floatArrayContentField = defineArrayType(classContext, baseType, ts.getFloat32Type(), "[F").validate().getField(0);
        doubleArrayContentField = defineArrayType(classContext, baseType, ts.getFloat64Type(), "[D").validate().getField(0);

        // now the reference array class

        ValidatedTypeDefinition refArrayType = defineArrayType(classContext, baseType, jlo.getClassType().getReference().asNullable(), "[L").validate();
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
        typeBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PUBLIC | ClassFile.I_ACC_HIDDEN);
        typeBuilder.setName(internalName);
        // add fields in this order, which is relied upon up above
        int idx = 0;
        if (realMemberType instanceof ReferenceType) {
            // also need a dimensions field
            typeBuilder.addField(Layout::makeDimensionsField, idx++);
            // also need a type ID field
            typeBuilder.addField((index, encl) -> makeTypeIdField(index, superClass, encl), idx++);
        }
        typeBuilder.addField((index, enclosing) -> makeContentField(index, enclosing, realMemberType), idx);
        typeBuilder.setInitializer(EMPTY_INIT, 0);
        return typeBuilder.build();
    }

    private static FieldElement makeDimensionsField(final int index, final DefinedTypeDefinition enclosing) {
        FieldElement.Builder fieldBuilder = FieldElement.builder();
        fieldBuilder.setEnclosingType(enclosing);
        fieldBuilder.setDescriptor(BaseTypeDescriptor.I);
        fieldBuilder.setSignature(BaseTypeSignature.I);
        fieldBuilder.setIndex(index);
        fieldBuilder.setName("dims");
        fieldBuilder.setType(enclosing.getContext().getTypeSystem().getSignedInteger32Type());
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_HIDDEN);
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
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_HIDDEN);
        return fieldBuilder.build();
    }

    private static FieldElement makeTypeIdField(final int index, final DefinedTypeDefinition jlo, final DefinedTypeDefinition enclosing) {
        FieldElement.Builder fieldBuilder = FieldElement.builder();
        fieldBuilder.setEnclosingType(enclosing);
        fieldBuilder.setDescriptor(BaseTypeDescriptor.V);
        fieldBuilder.setSignature(BaseTypeSignature.V);
        fieldBuilder.setIndex(index);
        fieldBuilder.setName("elementType");
        fieldBuilder.setType(jlo.validate().getClassType().getReference().getTypeType());
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_HIDDEN);
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
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_HIDDEN);
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

    public ValidatedTypeDefinition getArrayValidatedTypeDefinition(String arrayType) {
        switch(arrayType) {
        case "[Z":
            return booleanArrayContentField.getEnclosingType().validate();
        case "[B":
            return byteArrayContentField.getEnclosingType().validate();
        case "[S":
            return shortArrayContentField.getEnclosingType().validate();
        case "[C":
            return charArrayContentField.getEnclosingType().validate();
        case "[I":
            return intArrayContentField.getEnclosingType().validate();
        case "[F":
            return floatArrayContentField.getEnclosingType().validate();
        case "[J":
            return longArrayContentField.getEnclosingType().validate();
        case "[D":
            return doubleArrayContentField.getEnclosingType().validate();
        case "[ref":
            return refArrayContentField.getEnclosingType().validate();
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
        ValidatedTypeDefinition validated = type.validate();
        LayoutInfo layoutInfo = instanceLayouts.get(validated);
        if (layoutInfo != null) {
            return layoutInfo;
        }
        ValidatedTypeDefinition superClass = validated.getSuperClass();
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

    private CompoundType.Member computeMember(final BitSet allocated, final FieldElement field) {
        TypeSystem ts = ctxt.getTypeSystem();
        ValueType fieldType = field.getType(List.of(/* todo */));
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
