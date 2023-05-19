package org.qbicc.plugin.verification;

import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Dereference;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.ConstantLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.StructType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

/**
 * A block builder that resolves member references to their elements.
 */
public class MemberResolvingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final AttachmentKey<Info> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;

    public MemberResolvingBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    public Value instanceFieldOf(Value instance, TypeDescriptor owner, String name, TypeDescriptor type) {
        return instanceFieldOf(instance, resolveInstanceField(owner, name, type));
    }

    public Value resolveStaticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        FieldElement field = resolveField(owner, name, type);
        if (field instanceof StaticFieldElement sfe) {
            return getLiteralFactory().literalOf(sfe);
        } else {
            return super.resolveStaticField(owner, name, type);
        }
    }

    @Override
    public Value resolveInstanceMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            MethodElement element;
            if (definedType.isInterface()) {
                // use 5.4.3.4 rules
                element = definedType.load().resolveMethodElementInterface(name, descriptor);
            } else {
                // use 5.4.3.3 rules
                element = definedType.load().resolveMethodElementVirtual(getClassContext(), name, descriptor);
            }
            if (element == null) {
                throw new BlockEarlyTermination(nsme(name));
            } else {
                return getLiteralFactory().literalOf(element);
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme(name));
        }
    }

    @Override
    public Value lookupVirtualMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            MethodElement element = definedType.load().resolveMethodElementVirtual(getClassContext(), name, descriptor);
            if (element == null) {
                throw new BlockEarlyTermination(nsme(name));
            } else {
                return getFirstBuilder().lookupVirtualMethod(reference, (InstanceMethodElement) element);
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme(name));
        }
    }

    @Override
    public Value lookupInterfaceMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            MethodElement element = definedType.load().resolveMethodElementInterface(name, descriptor);
            if (element == null) {
                throw new BlockEarlyTermination(nsme(name));
            } else {
                return getFirstBuilder().lookupInterfaceMethod(reference, (InstanceMethodElement) element);
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme(name));
        }
    }

    @Override
    public Value resolveStaticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed.
            MethodElement element;
            if (definedType.isInterface()) {
                // use 5.4.3.4 rules
                element = definedType.load().resolveMethodElementInterface(name, descriptor);
            } else {
                // use 5.4.3.3 rules
                element = definedType.load().resolveMethodElementVirtual(getClassContext(), name, descriptor);
            }
            if (element == null) {
                throw new BlockEarlyTermination(nsme(name));
            } else {
                return getLiteralFactory().literalOf(element);
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme(name));
        }
    }

    @Override
    public Value resolveConstructor(TypeDescriptor owner, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            ConstructorElement element = definedType.load().resolveConstructorElement(descriptor);
            if (element == null) {
                throw new BlockEarlyTermination(nsme("<init>"));
            } else {
                // no sig-poly constructors exist
                return getLiteralFactory().literalOf(element);
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme("<init>"));
        }
    }

    public Value extractInstanceField(Value valueObj, TypeDescriptor owner, String name, TypeDescriptor type) {
        return extractInstanceField(valueObj, resolveField(owner, name, type));
    }

    public Value checkcast(final Value value, final TypeDescriptor desc) {
        ClassContext cc = getClassContext();
        // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
        ValueType castType = cc.resolveTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc));
        ValueType valueType = value.getType();
        if (value instanceof ConstantLiteral) {
            // it may be something we can't really cast.
            return ctxt.getLiteralFactory().constantLiteralOfType(castType);
        } else if (value instanceof UndefinedLiteral) {
            // it may be something we can't really cast.
            return ctxt.getLiteralFactory().undefinedLiteralOfType(castType);
        } else if (castType instanceof ObjectType originalToType) {
            ObjectType toType = originalToType;
            int toDimensions = 0;
            if (toType instanceof ReferenceArrayObjectType) {
                toDimensions = ((ReferenceArrayObjectType) toType).getDimensionCount();
                toType = ((ReferenceArrayObjectType) toType).getLeafElementType();
            }
            return checkcast(value, cc.getLiteralFactory().literalOfType(toType), cc.getLiteralFactory().literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), toDimensions), CheckCast.CastType.Cast, originalToType);
        } else if (castType instanceof WordType toType) {
            // A checkcast in the bytecodes, but it is actually a WordType coming from some native magic.
            WordType fromType = (WordType) valueType;
            // Zeros are always convertible to any word type.
            if (value.isDefEq(ctxt.getLiteralFactory().zeroInitializerLiteralOfType(fromType))) {
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(toType);
            }
            Value sizedValue;
            if (fromType instanceof IntegerType it) {
                if (toType instanceof PointerType) {
                    return super.valueConvert(value, toType);
                } else if (toType.getMinBits() < fromType.getMinBits()) {
                    sizedValue = super.truncate(value, it.asSized(toType.getMinBits()));
                } else if (toType.getMinBits() > fromType.getMinBits()) {
                    sizedValue = super.extend(value, it.asSized(toType.getMinBits()));
                } else {
                    sizedValue = value;
                }
            } else {
                sizedValue = value;
            }
            // handle casts between integer and pointer types
            if (fromType instanceof PointerType && toType instanceof IntegerType
                || fromType instanceof IntegerType && toType instanceof PointerType) {
                return super.valueConvert(sizedValue, toType);
            } else {
                return super.bitCast(sizedValue, toType);
            }
        } else if (castType instanceof StructType) {
            // A checkcast in the bytecodes but it's really casting to a structure or compound type;
            // we can't just bitcast it really, in fact it's an error unless the actual value is zero
            if (value instanceof Literal && ((Literal) value).isZero()) {
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(castType);
            } else if (castType.equals(valueType)) {
                return value;
            } else if (value instanceof Dereference){
                return value;
            } else {
                ctxt.error(getLocation(), "Disallowed cast of value from %s to %s", valueType, castType);
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(castType);
            }
        } else if (valueType instanceof PointerType && castType instanceof ArrayType) {
            // narrowing a pointer to an array is actually an array view of a pointer
            return value;
        } else {
            ctxt.error(getLocation(), "Disallowed cast of value from %s to %s", valueType, castType);
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(castType);
        }
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        ClassContext cc = getClassContext(); 
        // fetch the classfile's view of the type (or as close as we can synthesize) to save in the InstanceOf node
        ObjectType ot;
        int dimensions = 0;
        if (desc instanceof ArrayTypeDescriptor) {
            ot = cc.resolveArrayObjectTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc));
            if (ot instanceof ReferenceArrayObjectType) {
                dimensions = ((ReferenceArrayObjectType) ot).getDimensionCount();
                ot = ((ReferenceArrayObjectType) ot).getLeafElementType();
            }
        } else if (desc instanceof ClassTypeDescriptor classDesc) {
            String className = (classDesc.getPackageName().isEmpty() ? "" : classDesc.getPackageName() + "/") + classDesc.getClassName();
            DefinedTypeDefinition definedType = cc.findDefinedType(className);
            ot = definedType.load().getObjectType();
        } else {
            // this comes from the classfile - it better be something the verifier allows in instanceof/checkcast expressions
            throw Assert.unreachableCode();
        }
        return instanceOf(input, ot, dimensions);
    }

    public Value new_(final ClassTypeDescriptor desc) {
        ClassContext cc = getClassContext();
        ValueType type;
        DefinedTypeDefinition enclosingType = getCurrentElement().getEnclosingType();
        if (desc == enclosingType.getDescriptor()) {
            // always the current class
            type = enclosingType.load().getObjectType();
        } else {
            type = cc.resolveTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc));
        }
        if (type instanceof ClassObjectType cot) {
            Layout layout = Layout.get(ctxt);
            StructType structType = layout.getInstanceLayoutInfo(cot.getDefinition()).getStructType();
            LiteralFactory lf = ctxt.getLiteralFactory();
             return super.new_(cot, lf.literalOfType(cot), lf.literalOf(structType.getSize()), lf.literalOf(structType.getAlign()));
        }
        return super.new_(desc);
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        ClassContext cc = getClassContext();
        ValueType type = cc.resolveTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc));
        if (type instanceof PrimitiveArrayObjectType pat) {
            return super.newArray(pat, size);
        } else if (type instanceof ReferenceArrayObjectType rat) {
            Value dimensions =  ctxt.getLiteralFactory().literalOf((IntegerType) CoreClasses.get(ctxt).getRefArrayDimensionsField().getType(), rat.getDimensionCount());
            Value elemTypeId =  ctxt.getLiteralFactory().literalOfType(rat.getLeafElementType());
            return super.newReferenceArray(rat, elemTypeId, dimensions, size);
        } else if (type instanceof ArrayType at) {
            // it's a native array
            if (size instanceof IntegerLiteral il) {
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getArrayType(at.getElementType(), il.longValue()));
            } else {
                ctxt.error(getLocation(), "Native arrays must have a literal length");
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(at);
            }
        }
        return super.newArray(desc, size);
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        ClassContext cc = getClassContext();
        ValueType type = cc.resolveTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc));
        if (type instanceof ArrayObjectType) {
            return super.multiNewArray((ArrayObjectType) type, dimensions);
        }
        return super.multiNewArray(desc, dimensions);
    }

    private InstanceFieldElement resolveInstanceField(final TypeDescriptor owner, final String name, final TypeDescriptor desc) {
        FieldElement field = resolveField(owner, name, desc);
        if (field instanceof InstanceFieldElement ife) {
            return ife;
        }
        ctxt.warning(getLocation(), "Incompatible class change: non-static to static field %s#%s", field.getEnclosingType().getInternalName().replace('/', '.'), name);
        throw new BlockEarlyTermination(icce(name));
    }

    private FieldElement resolveField(final TypeDescriptor owner, final String name, final TypeDescriptor desc) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            FieldElement element = definedType.load().resolveField(desc, name);
            if (element == null) {
                throw new BlockEarlyTermination(nsfe(name));
            } else {
                // todo: compare descriptor
                return element;
            }
        } else {
            ctxt.error(getLocation(), "Resolve field on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsfe(name));
        }
    }

    private DefinedTypeDefinition resolveDescriptor(final TypeDescriptor owner) {
        DefinedTypeDefinition enclosingType = getCurrentElement().getEnclosingType();
        if (owner == enclosingType.getDescriptor()) {
            // special case - resolving on a hidden class
            return enclosingType;
        }
        if (owner instanceof ClassTypeDescriptor) {
            final String typeName;
            if (((ClassTypeDescriptor) owner).getPackageName().isEmpty()) {
                typeName = ((ClassTypeDescriptor) owner).getClassName();
            } else {
                typeName = ((ClassTypeDescriptor) owner).getPackageName() + "/" + ((ClassTypeDescriptor) owner).getClassName();
            }
            return getClassContext().findDefinedType(typeName);
        } else if (owner instanceof ArrayTypeDescriptor atd) {
            CoreClasses coreClasses = CoreClasses.get(ctxt);
            TypeDescriptor elementDesc = atd.getElementTypeDescriptor();
            if (elementDesc instanceof BaseTypeDescriptor) {
                // find out which one it belongs to
                if (elementDesc == BaseTypeDescriptor.B) {
                    return coreClasses.getByteArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.C) {
                    return coreClasses.getCharArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.D) {
                    return coreClasses.getDoubleArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.F) {
                    return coreClasses.getFloatArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.I) {
                    return coreClasses.getIntArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.J) {
                    return coreClasses.getLongArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.S) {
                    return coreClasses.getShortArrayContentField().getEnclosingType();
                } else {
                    assert elementDesc == BaseTypeDescriptor.Z;
                    return coreClasses.getShortArrayContentField().getEnclosingType();
                }
            } else if (elementDesc instanceof ClassTypeDescriptor || elementDesc instanceof ArrayTypeDescriptor) {
                return coreClasses.getRefArrayContentField().getEnclosingType();
            }
        }
        return null;
    }

    private BasicBlock nsfe(String name) {
        Info info = Info.get(ctxt);
        Value nsfe = new_(info.nsfeClass);
        Literal l = ctxt.getLiteralFactory().literalOf(name, ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load().getObjectType().getReference());
        call(resolveConstructor(info.nsfeClass, info.cd), nsfe, List.of(l));
        return throw_(nsfe);
    }

    private BasicBlock nsme(String name) {
        Info info = Info.get(ctxt);
        Value nsme = new_(info.nsmeClass);
        Literal l = ctxt.getLiteralFactory().literalOf(name, ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load().getObjectType().getReference());
        call(resolveConstructor(info.nsmeClass, info.cd), nsme, List.of(l));
        return throw_(nsme);
    }

    private BasicBlock icce(String name) {
        Info info = Info.get(ctxt);
        Value icce = new_(info.icceClass);
        Literal l = ctxt.getLiteralFactory().literalOf(name, ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load().getObjectType().getReference());
        call(resolveConstructor(info.icceClass, info.cd), icce, List.of(l));
        return throw_(icce);
    }

    private ClassContext getClassContext() {
        return getCurrentElement().getEnclosingType().getContext();
    }

    static final class Info {
        final ClassTypeDescriptor nsmeClass;
        final ClassTypeDescriptor nsfeClass;
        final ClassTypeDescriptor icceClass;
        final MethodDescriptor cd; // void (String)

        private Info(final CompilationContext ctxt) {
            DefinedTypeDefinition type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/NoSuchMethodError");
            nsmeClass = (ClassTypeDescriptor) type.getDescriptor();
            type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/NoSuchFieldError");
            nsfeClass = (ClassTypeDescriptor) type.getDescriptor();
            type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/IncompatibleClassChangeError");
            icceClass = (ClassTypeDescriptor) type.getDescriptor();
            ClassTypeDescriptor string = ClassTypeDescriptor.synthesize(ctxt.getBootstrapClassContext(), "java/lang/String");
            cd = MethodDescriptor.synthesize(ctxt.getBootstrapClassContext(), BaseTypeDescriptor.V, List.of(string));
        }

        static Info get(CompilationContext ctxt) {
            return ctxt.computeAttachmentIfAbsent(KEY, () -> new Info(ctxt));
        }
    }
}
