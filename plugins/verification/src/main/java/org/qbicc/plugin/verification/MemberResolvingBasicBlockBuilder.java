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
import org.qbicc.graph.Load;
import org.qbicc.graph.MemberSelector;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.ConstantLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
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

    public MemberResolvingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, TypeDescriptor owner, String name, TypeDescriptor type) {
        return instanceFieldOf(instance, resolveField(owner, name, type));
    }

    public ValueHandle staticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        return staticField(resolveField(owner, name, type));
    }

    public ValueHandle exactMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            MethodElement element;
            if (definedType.isInterface()) {
                // use 5.4.3.4 rules
                element = definedType.load().resolveMethodElementInterface(name, descriptor);
            } else {
                // use 5.4.3.3 rules
                element = definedType.load().resolveMethodElementVirtual(name, descriptor);
            }
            if (element == null) {
                throw new BlockEarlyTermination(nsme());
            } else {
                TypeParameterContext tpc = getCurrentElement() instanceof TypeParameterContext ? (TypeParameterContext) getCurrentElement() : definedType;
                FunctionType callSiteType = getClassContext().resolveMethodFunctionType(
                    descriptor,
                    tpc,
                    element.getSignature(),
                    TypeAnnotationList.empty(),
                    element.getParameterVisibleTypeAnnotations(),
                    TypeAnnotationList.empty(),
                    element.getParameterInvisibleTypeAnnotations());
                return exactMethodOf(instance, element, descriptor, callSiteType);
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme());
        }
    }

    public ValueHandle virtualMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            MethodElement element = definedType.load().resolveMethodElementVirtual(name, descriptor);
            if (element == null) {
                throw new BlockEarlyTermination(nsme());
            } else {
                TypeParameterContext tpc = getCurrentElement() instanceof TypeParameterContext ? (TypeParameterContext) getCurrentElement() : definedType;
                FunctionType callSiteType = getClassContext().resolveMethodFunctionType(
                    descriptor,
                    tpc,
                    element.getSignature(),
                    TypeAnnotationList.empty(),
                    element.getParameterVisibleTypeAnnotations(),
                    TypeAnnotationList.empty(),
                    element.getParameterInvisibleTypeAnnotations());
                return virtualMethodOf(instance, element, descriptor, callSiteType);
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme());
        }
    }

    public ValueHandle interfaceMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            MethodElement element = definedType.load().resolveMethodElementInterface(name, descriptor);
            if (element == null) {
                throw new BlockEarlyTermination(nsme());
            } else {
                TypeParameterContext tpc = getCurrentElement() instanceof TypeParameterContext ? (TypeParameterContext) getCurrentElement() : definedType;
                FunctionType callSiteType = getClassContext().resolveMethodFunctionType(
                    descriptor,
                    tpc,
                    element.getSignature(),
                    TypeAnnotationList.empty(),
                    element.getParameterVisibleTypeAnnotations(),
                    TypeAnnotationList.empty(),
                    element.getParameterInvisibleTypeAnnotations());
                return interfaceMethodOf(instance, element, descriptor, callSiteType);
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme());
        }
    }

    public ValueHandle staticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed.
            MethodElement element;
            if (definedType.isInterface()) {
                // use 5.4.3.4 rules
                element = definedType.load().resolveMethodElementInterface(name, descriptor);
            } else {
                // use 5.4.3.3 rules
                element = definedType.load().resolveMethodElementVirtual(name, descriptor);
            }
            if (element == null) {
                throw new BlockEarlyTermination(nsme());
            } else {
                TypeParameterContext tpc = getCurrentElement() instanceof TypeParameterContext ? (TypeParameterContext) getCurrentElement() : definedType;
                FunctionType callSiteType = getClassContext().resolveMethodFunctionType(
                    descriptor,
                    tpc,
                    element.getSignature(),
                    TypeAnnotationList.empty(),
                    element.getParameterVisibleTypeAnnotations(),
                    TypeAnnotationList.empty(),
                    element.getParameterInvisibleTypeAnnotations());
                return staticMethod(element, descriptor, callSiteType);
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme());
        }
    }

    public ValueHandle constructorOf(Value instance, TypeDescriptor owner, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            ConstructorElement element = definedType.load().resolveConstructorElement(descriptor);
            if (element == null) {
                throw new BlockEarlyTermination(nsme());
            } else {
                return constructorOf(instance, element, element.getDescriptor(), element.getType());
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme());
        }
    }

    public Value extractInstanceField(Value valueObj, TypeDescriptor owner, String name, TypeDescriptor type) {
        return extractInstanceField(valueObj, resolveField(owner, name, type));
    }

    public Value checkcast(final Value value, final TypeDescriptor desc) {
        ClassContext cc = getClassContext();
        // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
        ValueType castType = cc.resolveTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
        if (value instanceof ConstantLiteral) {
            // it may be something we can't really cast.
            return ctxt.getLiteralFactory().constantLiteralOfType(castType);
        } else if (value instanceof UndefinedLiteral) {
            // it may be something we can't really cast.
            return ctxt.getLiteralFactory().undefinedLiteralOfType(castType);
        } else if (castType instanceof ObjectType) {
            ObjectType originalToType = (ObjectType) castType;
            ObjectType toType = originalToType;
            int toDimensions = 0;
            if (toType instanceof ReferenceArrayObjectType) {
                toDimensions = ((ReferenceArrayObjectType) toType).getDimensionCount();
                toType = ((ReferenceArrayObjectType) toType).getLeafElementType();
            }
            return checkcast(value, cc.getLiteralFactory().literalOfType(toType), cc.getLiteralFactory().literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), toDimensions), CheckCast.CastType.Cast, originalToType);
        } else if (castType instanceof WordType) {
            // A checkcast in the bytecodes, but it is actually a WordType coming from some native magic...just bitcast it.
            WordType toType = (WordType) castType;
            WordType fromType = (WordType) value.getType();
            if (toType.getMinBits() < fromType.getMinBits()) {
                return super.truncate(value, toType);
            } else {
                return super.bitCast(value, (WordType) castType);
            }
        } else if (castType instanceof CompoundType) {
            // A checkcast in the bytecodes but it's really casting to a structure or compound type;
            // we can't just bitcast it really, in fact it's an error unless the actual value is zero
            if (value instanceof Literal && ((Literal) value).isZero()) {
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(castType);
            } else if (castType.equals(value.getType())) {
                return value;
            } else if (value instanceof MemberSelector){
                return value;
            } else {
                ctxt.error(getLocation(), "Disallowed cast of value from %s to %s", value.getType(), castType);
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(castType);
            }
        } else if (value.getType() instanceof PointerType && castType instanceof ArrayType) {
            // narrowing a pointer to an array is actually an array view of a pointer
            return value;
        }
        throw Assert.unreachableCode();
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        ClassContext cc = getClassContext(); 
        // fetch the classfile's view of the type (or as close as we can synthesize) to save in the InstanceOf node
        ObjectType ot = null;
        int dimensions = 0;
        if (desc instanceof ArrayTypeDescriptor) {
            ot = cc.resolveArrayObjectTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
            if (ot instanceof ReferenceArrayObjectType) {
                dimensions = ((ReferenceArrayObjectType) ot).getDimensionCount();
                ot = ((ReferenceArrayObjectType) ot).getLeafElementType();
            }
        } else if (desc instanceof ClassTypeDescriptor) {
            ClassTypeDescriptor classDesc = (ClassTypeDescriptor) desc;
            String className = (classDesc.getPackageName().isEmpty() ? "" : classDesc.getPackageName() + "/") + classDesc.getClassName();
            DefinedTypeDefinition definedType = cc.findDefinedType(className);
            ot = definedType.load().getType();
        } else {
            // this comes from the classfile - it better be something the verifier allows in instanceof/checkcast expressions
            throw Assert.unreachableCode();
        }
        return instanceOf(input, ot, dimensions);
    }

    public Value new_(final ClassTypeDescriptor desc) {
        ClassContext cc = getClassContext();
        ValueType type = cc.resolveTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
        if (type instanceof ClassObjectType cot) {
            Layout layout = Layout.get(ctxt);
            CompoundType compoundType = layout.getInstanceLayoutInfo(cot.getDefinition()).getCompoundType();
            LiteralFactory lf = ctxt.getLiteralFactory();
             return super.new_(cot, lf.literalOfType(cot), lf.literalOf(compoundType.getSize()), lf.literalOf(compoundType.getAlign()));
        }
        return super.new_(desc);
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        ClassContext cc = getClassContext();
        ValueType type = cc.resolveTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
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
        ValueType type = cc.resolveTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
        if (type instanceof ArrayObjectType) {
            return super.multiNewArray((ArrayObjectType) type, dimensions);
        }
        return super.multiNewArray(desc, dimensions);
    }

    private FieldElement resolveField(final TypeDescriptor owner, final String name, final TypeDescriptor desc) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            FieldElement element = definedType.load().resolveField(desc, name);
            if (element == null) {
                throw new BlockEarlyTermination(nsfe());
            } else {
                // todo: compare descriptor
                return element;
            }
        } else {
            ctxt.error(getLocation(), "Resolve field on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsfe());
        }
    }

    private DefinedTypeDefinition resolveDescriptor(final TypeDescriptor owner) {
        if (owner instanceof ClassTypeDescriptor) {
            final String typeName;
            if (((ClassTypeDescriptor) owner).getPackageName().isEmpty()) {
                typeName = ((ClassTypeDescriptor) owner).getClassName();
            } else {
                typeName = ((ClassTypeDescriptor) owner).getPackageName() + "/" + ((ClassTypeDescriptor) owner).getClassName();
            }
            return getClassContext().findDefinedType(typeName);
        } else if (owner instanceof ArrayTypeDescriptor) {
            CoreClasses coreClasses = CoreClasses.get(ctxt);
            ArrayTypeDescriptor atd = (ArrayTypeDescriptor) owner;
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

    private BasicBlock nsfe() {
        Info info = Info.get(ctxt);
        // todo: add class name to exception string
        Value nsfe = new_(info.nsfeClass);
        call(constructorOf(nsfe, info.nsfeClass, MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
        return throw_(nsfe);
    }

    private BasicBlock nsme() {
        Info info = Info.get(ctxt);
        // todo: add class name to exception string
        Value nsme = new_(info.nsmeClass);
        call(constructorOf(nsme, info.nsmeClass, MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
        return throw_(nsme);
    }

    private ClassContext getClassContext() {
        return getCurrentElement().getEnclosingType().getContext();
    }

    static final class Info {
        final ClassTypeDescriptor nsmeClass;
        final ClassTypeDescriptor nsfeClass;

        private Info(final CompilationContext ctxt) {
            DefinedTypeDefinition type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/NoSuchMethodError");
            nsmeClass = type.getDescriptor();
            type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/NoSuchFieldError");
            nsfeClass = type.getDescriptor();
        }

        static Info get(CompilationContext ctxt) {
            return ctxt.computeAttachmentIfAbsent(KEY, () -> new Info(ctxt));
        }
    }
}
