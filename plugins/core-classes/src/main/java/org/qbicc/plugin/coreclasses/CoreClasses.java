package org.qbicc.plugin.coreclasses;

import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.patcher.Patcher;
import org.qbicc.type.BooleanType;
import org.qbicc.type.FloatType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;
import org.qbicc.type.generic.ClassSignature;
import org.qbicc.type.generic.ClassTypeSignature;
import org.qbicc.type.generic.TypeSignature;

/**
 * The core objects plugin entry point.  This plugin manages special classes and fields used in the implementation
 * of the VM.
 */
public final class CoreClasses {
    private static final AttachmentKey<CoreClasses> KEY = new AttachmentKey<>();

    private static final String INTERNAL_ARRAY = "internal_array";

    private static final InitializerResolver EMPTY_INIT = (index, enclosing, builder) -> {
        builder.setEnclosingType(enclosing);
        return builder.build();
    };
    private static final String OBJECT_INT_NAME = "java/lang/Object";
    private static final String CLASS_INT_NAME = "java/lang/Class";
    private static final String THREAD_INT_NAME = "java/lang/Thread";
    private static final String THROWABLE_INT_NAME = "java/lang/Throwable";

    private final CompilationContext ctxt;

    private final FieldElement objectHeaderField;
    private final FieldElement objectTypeIdField;
    private final FieldElement objectNativeObjectMonitorField;
    private final FieldElement classTypeIdField;
    private final FieldElement classDimensionField;
    private final FieldElement arrayClassField;

    private final FieldElement thrownField;

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

    private CoreClasses(final CompilationContext ctxt) {
        this.ctxt = ctxt;
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType(OBJECT_INT_NAME);
        DefinedTypeDefinition jlcDef = classContext.findDefinedType(CLASS_INT_NAME);
        DefinedTypeDefinition jltDef = classContext.findDefinedType(THREAD_INT_NAME);
        ClassTypeDescriptor jlcDesc = ClassTypeDescriptor.synthesize(classContext, CLASS_INT_NAME);
        LoadedTypeDefinition jlo = jloDef.load();
        LoadedTypeDefinition jlc = jlcDef.load();
        LoadedTypeDefinition jlt = jltDef.load();
        final TypeSystem ts = ctxt.getTypeSystem();

        objectHeaderField = jlo.resolveField(BaseTypeDescriptor.V, "header", true);
        objectTypeIdField = jlo.resolveField(BaseTypeDescriptor.V, "typeId", true);
        objectNativeObjectMonitorField = jlo.resolveField(BaseTypeDescriptor.J, "nativeObjectMonitor", true);

        classTypeIdField = jlc.resolveField(BaseTypeDescriptor.V, "id", true);
        classDimensionField = jlc.resolveField(BaseTypeDescriptor.V, "dimension", true);
        arrayClassField = jlc.resolveField(jlcDesc, "arrayClass", true);

        thrownField = jlt.resolveField(ClassTypeDescriptor.synthesize(classContext, THROWABLE_INT_NAME), "thrown", true);

        // now define classes for arrays
        // todo: assign special type ID values to array types

        // define an array base type so that the length is always in the same place
        DefinedTypeDefinition.Builder typeBuilder = DefinedTypeDefinition.Builder.basic();
        ClassTypeDescriptor desc = ClassTypeDescriptor.synthesize(classContext, INTERNAL_ARRAY);
        typeBuilder.setDescriptor(desc);
        ClassTypeSignature superClassSig = (ClassTypeSignature) TypeSignature.synthesize(classContext, jlo.getDescriptor());
        typeBuilder.setSignature(ClassSignature.synthesize(classContext, superClassSig, List.of()));
        typeBuilder.setSuperClassName(OBJECT_INT_NAME);
        typeBuilder.expectInterfaceNameCount(2);
        typeBuilder.addInterfaceName("java/lang/Cloneable");
        typeBuilder.addInterfaceName("java/io/Serializable");
        typeBuilder.setSimpleName("base_array_type");
        typeBuilder.setContext(classContext);
        typeBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PUBLIC | ClassFile.I_ACC_NO_REFLECT);
        typeBuilder.setName("base_array_type");
        typeBuilder.addField(CoreClasses::makeLengthField, 0, "length", BaseTypeDescriptor.I);
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

        LoadedTypeDefinition refArrayType = defineArrayType(classContext, baseType, jlo.getClassType().getReference(), "[L").load();
        refArrayDimensionsField = refArrayType.getField(0);
        refArrayElementTypeIdField = refArrayType.getField(1);
        refArrayContentField = refArrayType.getField(2);

    }

    private static DefinedTypeDefinition defineArrayType(ClassContext classContext, DefinedTypeDefinition superClass, ValueType realMemberType, String simpleName) {
        DefinedTypeDefinition.Builder typeBuilder = DefinedTypeDefinition.Builder.basic();
        String internalName = "[" + simpleName.charAt(1);
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
        DefinedTypeDefinition jlo = classContext.findDefinedType(OBJECT_INT_NAME);
        if (realMemberType instanceof ReferenceType) {
            // also need a dimensions field
            typeBuilder.addField(CoreClasses::makeDimensionsField, idx++, "dims", BaseTypeDescriptor.V);
            // also need a type ID field
            typeBuilder.addField((index, encl, builder) -> makeElementTypeIdField(index, jlo, encl, builder), idx++, "elementType", BaseTypeDescriptor.V);
        }
        typeBuilder.addField((index, enclosing, builder) -> makeContentField(index, enclosing, realMemberType, builder), idx, "content", BaseTypeDescriptor.V);
        typeBuilder.setInitializer(EMPTY_INIT, 0);
        return typeBuilder.build();
    }

    private static FieldElement makeDimensionsField(final int index, final DefinedTypeDefinition enclosing, FieldElement.Builder fieldBuilder) {
        fieldBuilder.setEnclosingType(enclosing);
        fieldBuilder.setSignature(BaseTypeSignature.V);
        fieldBuilder.setIndex(index);
        fieldBuilder.setType(enclosing.getContext().getTypeSystem().getUnsignedInteger8Type());
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        return fieldBuilder.build();
    }

    private static FieldElement makeLengthField(final int index, final DefinedTypeDefinition enclosing, FieldElement.Builder fieldBuilder) {
        fieldBuilder.setEnclosingType(enclosing);
        fieldBuilder.setSignature(BaseTypeSignature.I);
        fieldBuilder.setIndex(index);
        fieldBuilder.setType(enclosing.getContext().getTypeSystem().getSignedInteger32Type());
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        return fieldBuilder.build();
    }

    private static FieldElement makeElementTypeIdField(final int index, final DefinedTypeDefinition jlo, final DefinedTypeDefinition enclosing, FieldElement.Builder fieldBuilder) {
        fieldBuilder.setEnclosingType(enclosing);
        fieldBuilder.setSignature(BaseTypeSignature.V);
        fieldBuilder.setIndex(index);
        fieldBuilder.setType(jlo.load().getClassType().getTypeType());
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        return fieldBuilder.build();
    }

    private static FieldElement makeContentField(final int index, final DefinedTypeDefinition enclosing, final ValueType realMemberType, FieldElement.Builder fieldBuilder) {
        fieldBuilder.setEnclosingType(enclosing);
        fieldBuilder.setSignature(BaseTypeSignature.V);
        fieldBuilder.setIndex(index);
        fieldBuilder.setType(enclosing.getContext().getTypeSystem().getArrayType(realMemberType, 0));
        fieldBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
        return fieldBuilder.build();
    }

    /**
     * Initialize all of our fields ahead of time using patcher.
     *
     * @param ctxt the compilation context (must not be {@code null})
     */
    public static void init(CompilationContext ctxt) {
        Patcher patcher = Patcher.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // inject a field to hold the object header bits
        patcher.addField(classContext, OBJECT_INT_NAME, "header", BaseTypeDescriptor.V, new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_VOLATILE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
                builder.setEnclosingType(enclosing);
                builder.setSignature(BaseTypeSignature.V);
                builder.setType(HeaderBits.get(ctxt).getHeaderType());
                return builder.build();
            }
        }, 0, 0, null, 0);

        // inject a field to hold the object typeId
        patcher.addField(classContext, OBJECT_INT_NAME, "typeId", BaseTypeDescriptor.V, new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
                builder.setEnclosingType(enclosing);
                builder.setSignature(BaseTypeSignature.V);
                builder.setTypeResolver(e -> e.getEnclosingType().load().getClassType().getTypeType());
                return builder.build();
            }
        }, 0, 0, null, 0);

        // inject a field to hold the object monitor
        patcher.addField(classContext, OBJECT_INT_NAME, "nativeObjectMonitor", BaseTypeDescriptor.J, new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
                builder.setEnclosingType(enclosing);
                builder.setSignature(BaseTypeSignature.J);
                return builder.build();
            }
        }, 0, 0, null, 0);

        // now inject a field of ClassObjectType into Class to hold the corresponding run time type
        patcher.addField(classContext, CLASS_INT_NAME, "id", BaseTypeDescriptor.V, new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
                builder.setEnclosingType(enclosing);
                builder.setSignature(BaseTypeSignature.V);
                builder.setTypeResolver(e -> e.getEnclosingType().load().getSuperClass().getClassType().getTypeType());
                return builder.build();
            }
        }, 0, 0, null, 0);

        // now inject a field of int into Class to hold the corresponding run time dimensionality
        patcher.addField(classContext, CLASS_INT_NAME, "dimension", BaseTypeDescriptor.V, new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
                builder.setEnclosingType(enclosing);
                builder.setSignature(BaseTypeSignature.V);
                ClassContext classContext = enclosing.getContext();
                builder.setType(classContext.getTypeSystem().getUnsignedInteger8Type());
                return builder.build();
            }
        }, 0, 0, null, 0);

        // now inject a field of type java/lang/Class into Class to hold reference to array class of this class
        patcher.addField(classContext, CLASS_INT_NAME, "arrayClass", ClassTypeDescriptor.synthesize(classContext, CLASS_INT_NAME), new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_VOLATILE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
                builder.setEnclosingType(enclosing);
                builder.setSignature(TypeSignature.synthesize(classContext, enclosing.getDescriptor()));
                builder.setTypeResolver(f -> f.getEnclosingType().load().getClassType().getReference());
                return builder.build();
            }
        }, 0, 0, null, 0);

        // inject the thrown exception field
        ClassTypeDescriptor throwableDesc = ClassTypeDescriptor.synthesize(classContext, THROWABLE_INT_NAME);
        patcher.addField(classContext, THREAD_INT_NAME, "thrown", throwableDesc, new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
                builder.setEnclosingType(enclosing);
                builder.setSignature(TypeSignature.synthesize(classContext, throwableDesc));
                return builder.build();
            }
        }, 0, 0, null, 0);
    }

    public static CoreClasses get(CompilationContext ctxt) {
        CoreClasses co = ctxt.getAttachment(KEY);
        if (co == null) {
            co = new CoreClasses(ctxt);
            CoreClasses appearing = ctxt.putAttachmentIfAbsent(KEY, co);
            if (appearing != null) {
                co = appearing;
            }
        }
        return co;
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
        return switch (arrayType) {
            case "[Z" -> booleanArrayContentField.getEnclosingType().load();
            case "[B" -> byteArrayContentField.getEnclosingType().load();
            case "[S" -> shortArrayContentField.getEnclosingType().load();
            case "[C" -> charArrayContentField.getEnclosingType().load();
            case "[I" -> intArrayContentField.getEnclosingType().load();
            case "[F" -> floatArrayContentField.getEnclosingType().load();
            case "[J" -> longArrayContentField.getEnclosingType().load();
            case "[D" -> doubleArrayContentField.getEnclosingType().load();
            case "[ref", "[L" -> refArrayContentField.getEnclosingType().load();
            default -> throw Assert.impossibleSwitchCase(arrayType);
        };
    }

    /**
     * Get the object header field.
     *
     * @return the object header field
     */
    public FieldElement getObjectHeaderField() {
        return objectHeaderField;
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
     * A convenience method to get the type definition for {@code Object}.
     *
     * @return the type definition of {@code Object} (not {@code null})
     */
    public LoadedTypeDefinition getObjectTypeDefinition() {
        return objectTypeIdField.getEnclosingType().load();
    }

    /**
     * Get the object field which holds the synchronization information (mutex)
     *
     * @return the native object monitor field
     */
    public FieldElement getObjectNativeObjectMonitorField() { 
        return objectNativeObjectMonitorField;
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

    public FieldElement getArrayClassField() { return arrayClassField; }

    public LoadedTypeDefinition getClassTypeDefinition() {
        return classTypeIdField.getEnclosingType().load();
    }

    public FieldElement getThrownField() {
        return thrownField;
    }

    public LoadedTypeDefinition getArrayBaseTypeDefinition() {
        return getArrayLengthField().getEnclosingType().load();
    }

    public FieldElement getArrayLengthField() {
        return arrayLengthField;
    }

    public LoadedTypeDefinition getReferenceArrayTypeDefinition() {
        return getRefArrayContentField().getEnclosingType().load();
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

    public LoadedTypeDefinition getBooleanArrayTypeDefinition() {
        return getBooleanArrayContentField().getEnclosingType().load();
    }

    public FieldElement getBooleanArrayContentField() {
        return booleanArrayContentField;
    }

    public LoadedTypeDefinition getByteArrayTypeDefinition() {
        return getByteArrayContentField().getEnclosingType().load();
    }

    public FieldElement getByteArrayContentField() {
        return byteArrayContentField;
    }

    public LoadedTypeDefinition getShortArrayTypeDefinition() {
        return getShortArrayContentField().getEnclosingType().load();
    }

    public FieldElement getShortArrayContentField() {
        return shortArrayContentField;
    }

    public LoadedTypeDefinition getIntArrayTypeDefinition() {
        return getIntArrayContentField().getEnclosingType().load();
    }

    public FieldElement getIntArrayContentField() {
        return intArrayContentField;
    }

    public LoadedTypeDefinition getLongArrayTypeDefinition() {
        return getLongArrayContentField().getEnclosingType().load();
    }

    public FieldElement getLongArrayContentField() {
        return longArrayContentField;
    }

    public LoadedTypeDefinition getCharArrayTypeDefinition() {
        return getCharArrayContentField().getEnclosingType().load();
    }

    public FieldElement getCharArrayContentField() {
        return charArrayContentField;
    }

    public LoadedTypeDefinition getFloatArrayTypeDefinition() {
        return getFloatArrayContentField().getEnclosingType().load();
    }

    public FieldElement getFloatArrayContentField() {
        return floatArrayContentField;
    }

    public LoadedTypeDefinition getDoubleArrayTypeDefinition() {
        return getDoubleArrayContentField().getEnclosingType().load();
    }

    public FieldElement getDoubleArrayContentField() {
        return doubleArrayContentField;
    }

}
