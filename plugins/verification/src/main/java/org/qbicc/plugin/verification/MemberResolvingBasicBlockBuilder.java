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
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.ConstantLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
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
            MethodElement element = definedType.load().resolveMethodElementExact(name, descriptor);
            if (element == null) {
                throw new BlockEarlyTermination(nsme());
            } else {
                return exactMethodOf(instance, element);
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
                return virtualMethodOf(instance, element);
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
                return interfaceMethodOf(instance, element);
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            throw new BlockEarlyTermination(nsme());
        }
    }

    public ValueHandle staticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        DefinedTypeDefinition definedType = resolveDescriptor(owner);
        if (definedType != null) {
            // it is present else {@link org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            MethodElement element = definedType.load().resolveMethodElementExact(name, descriptor);
            if (element == null) {
                throw new BlockEarlyTermination(nsme());
            } else {
                return staticMethod(element);
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
                return constructorOf(instance, element);
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
        } else if (castType instanceof ReferenceType) {
            ReferenceType referenceType = (ReferenceType) castType;
            ObjectType toType = referenceType.getUpperBound();
            int toDimensions = 0;
            if (toType instanceof ReferenceArrayObjectType) {
                toDimensions = ((ReferenceArrayObjectType) toType).getDimensionCount();
                toType = ((ReferenceArrayObjectType) toType).getLeafElementType();
            }
            return checkcast(value, cc.getLiteralFactory().literalOfType(toType), cc.getLiteralFactory().literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), toDimensions), CheckCast.CastType.Cast, referenceType);
        } else if (castType instanceof WordType) {
            // A checkcast in the bytecodes, but it is actually a WordType coming from some native magic...just bitcast it.
            WordType toType = (WordType) castType;
            WordType fromType = (WordType) value.getType();
            if (toType.getMinBits() < fromType.getMinBits()) {
                return super.truncate(value, toType);
            } else {
                return super.bitCast(value, (WordType) castType);
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
            String className = (classDesc.getPackageName() != "" ? classDesc.getPackageName() + "/" : "") + classDesc.getClassName();
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
        if (type instanceof ReferenceType) {
            ObjectType upperBound = ((ReferenceType) type).getUpperBound();
            if (upperBound instanceof ClassObjectType) {
                return super.new_((ClassObjectType) upperBound);
            }
        }
        return super.new_(desc);
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        ClassContext cc = getClassContext();
        ValueType type = cc.resolveTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
        if (type instanceof ReferenceType) {
            ObjectType upperBound = ((ReferenceType) type).getUpperBound();
            if (upperBound instanceof ArrayObjectType) {
                return super.newArray((ArrayObjectType) upperBound, size);
            }
        } else if (type instanceof ArrayType) {
            // it's a native array
            return stackAllocate(((ArrayType) type).getElementType(), size, ctxt.getLiteralFactory().literalOf(1));
        }
        return super.newArray(desc, size);
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        ClassContext cc = getClassContext();
        ValueType type = cc.resolveTypeFromDescriptor(desc, TypeParameterContext.of(getCurrentElement()), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
        if (type instanceof ReferenceType) {
            ObjectType upperBound = ((ReferenceType) type).getUpperBound();
            if (upperBound instanceof ArrayObjectType) {
                return super.multiNewArray((ArrayObjectType) upperBound, dimensions);
            }
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
            Layout layout = Layout.get(ctxt);
            ArrayTypeDescriptor atd = (ArrayTypeDescriptor) owner;
            TypeDescriptor elementDesc = atd.getElementTypeDescriptor();
            if (elementDesc instanceof BaseTypeDescriptor) {
                // find out which one it belongs to
                if (elementDesc == BaseTypeDescriptor.B) {
                    return layout.getByteArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.C) {
                    return layout.getCharArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.D) {
                    return layout.getDoubleArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.F) {
                    return layout.getFloatArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.I) {
                    return layout.getIntArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.J) {
                    return layout.getLongArrayContentField().getEnclosingType();
                } else if (elementDesc == BaseTypeDescriptor.S) {
                    return layout.getShortArrayContentField().getEnclosingType();
                } else {
                    assert elementDesc == BaseTypeDescriptor.Z;
                    return layout.getShortArrayContentField().getEnclosingType();
                }
            } else if (elementDesc instanceof ClassTypeDescriptor || elementDesc instanceof ArrayTypeDescriptor) {
                return layout.getRefArrayContentField().getEnclosingType();
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
