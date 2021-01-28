package cc.quarkus.qcc.plugin.verification;

import java.util.List;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.TypeSignature;

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

    public Value narrow(final Value value, final TypeDescriptor desc) {
        ClassContext cc = getClassContext();
        // it is present else {@link cc.quarkus.qcc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
        return narrow(value, cc.resolveTypeFromDescriptor(desc, List.of(/*todo*/), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty()));
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        ClassContext cc = getClassContext();
        // it is present else {@link cc.quarkus.qcc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
        return instanceOf(input, cc.resolveTypeFromDescriptor(desc, List.of(/*todo*/), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty()));
    }

    public Value new_(final ClassTypeDescriptor desc) {
        ClassContext cc = getClassContext();
        ValueType type = cc.resolveTypeFromDescriptor(desc, List.of(/*todo*/), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
        if (type instanceof ReferenceType) {
            ObjectType upperBound = ((ReferenceType) type).getUpperBound();
            if (upperBound instanceof ClassObjectType) {
                return super.new_((ClassObjectType) upperBound);
            }
        }
        ctxt.error(getLocation(), "Invalid type resolved for `new`: %s", type);
        return ctxt.getLiteralFactory().literalOfNull();
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        ClassContext cc = getClassContext();
        ValueType type = cc.resolveTypeFromDescriptor(desc, List.of(/*todo*/), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
        if (type instanceof ReferenceType) {
            ObjectType upperBound = ((ReferenceType) type).getUpperBound();
            if (upperBound instanceof ArrayObjectType) {
                return super.newArray((ArrayObjectType) upperBound, size);
            }
        }
        ctxt.error(getLocation(), "Invalid type resolved for `newArray`: %s", type);
        return ctxt.getLiteralFactory().literalOfNull();
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        ClassContext cc = getClassContext();
        ValueType type = cc.resolveTypeFromDescriptor(desc, List.of(/*todo*/), TypeSignature.synthesize(cc, desc), TypeAnnotationList.empty(), TypeAnnotationList.empty());
        if (type instanceof ReferenceType) {
            ObjectType upperBound = ((ReferenceType) type).getUpperBound();
            if (upperBound instanceof ArrayObjectType) {
                return super.multiNewArray((ArrayObjectType) upperBound, dimensions);
            }
        }
        ctxt.error(getLocation(), "Invalid type resolved for `multiNewArray`: %s", type);
        return ctxt.getLiteralFactory().literalOfNull();
    }

    public Value readInstanceField(final Value instance, final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final JavaAccessMode mode) {
        FieldElement target = resolveField(owner, name, descriptor);
        if (target != null) {
            return super.readInstanceField(instance, target, mode);
        } else {
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Value readStaticField(final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final JavaAccessMode mode) {
        FieldElement target = resolveField(owner, name, descriptor);
        if (target != null) {
            return super.readStaticField(target, mode);
        } else {
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Node writeInstanceField(final Value instance, final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final Value value, final JavaAccessMode mode) {
        FieldElement target = resolveField(owner, name, descriptor);
        if (target != null) {
            return super.writeInstanceField(instance, target, value, mode);
        } else {
            return nop();
        }
    }

    public Node writeStaticField(final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final Value value, final JavaAccessMode mode) {
        FieldElement target = resolveField(owner, name, descriptor);
        if (target != null) {
            return super.writeStaticField(target, value, mode);
        } else {
            return nop();
        }
    }

    public Node invokeStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        MethodElement target = resolveMethod(DispatchInvocation.Kind.EXACT, owner, name, descriptor);
        if (target != null) {
            return super.invokeStatic(target, arguments);
        } else {
            return nop();
        }
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        MethodElement target = resolveMethod(kind, owner, name, descriptor);
        if (target != null) {
            return super.invokeInstance(kind, instance, target, arguments);
        } else {
            return nop();
        }
    }

    public Value invokeValueStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        MethodElement target = resolveMethod(DispatchInvocation.Kind.EXACT, owner, name, descriptor);
        if (target != null) {
            return super.invokeValueStatic(target, arguments);
        } else {
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        MethodElement target = resolveMethod(kind, owner, name, descriptor);
        if (target != null) {
            return super.invokeValueInstance(kind, instance, target, arguments);
        } else {
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Value invokeConstructor(final Value instance, final TypeDescriptor owner, final MethodDescriptor descriptor, final List<Value> arguments) {
        ConstructorElement target = resolveConstructor(owner, descriptor);
        if (target != null) {
            return super.invokeConstructor(instance, target, arguments);
        } else {
            return instance;
        }
    }

    private MethodElement resolveMethod(final DispatchInvocation.Kind kind, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor) {
        if (owner instanceof ClassTypeDescriptor) {
            DefinedTypeDefinition definedType = resolveDescriptor((ClassTypeDescriptor) owner);
            // it is present else {@link cc.quarkus.qcc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            MethodElement element;
            if (kind == DispatchInvocation.Kind.EXACT) {
                element = definedType.validate().resolveMethodElementExact(name, descriptor);
            } else if (kind == DispatchInvocation.Kind.VIRTUAL) {
                element = definedType.validate().resolveMethodElementVirtual(name, descriptor);
            } else {
                assert kind == DispatchInvocation.Kind.INTERFACE;
                element = definedType.validate().resolveMethodElementInterface(name, descriptor);
            }
            if (element == null) {
                nsme();
                return null;
            } else {
                return element;
            }
        } else {
            ctxt.error(getLocation(), "Resolve method on a non-class type `%s` (did you forget a plugin?)", owner);
            nsme();
            return null;
        }
    }

    private ConstructorElement resolveConstructor(final TypeDescriptor owner, final MethodDescriptor descriptor) {
        if (owner instanceof ClassTypeDescriptor) {
            DefinedTypeDefinition definedType = resolveDescriptor((ClassTypeDescriptor) owner);
            // it is present else {@link cc.quarkus.qcc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            ConstructorElement element = definedType.validate().resolveConstructorElement(descriptor);
            if (element == null) {
                nsme();
                return null;
            } else {
                return element;
            }
        } else {
            ctxt.error(getLocation(), "Resolve constructor on a non-class type `%s` (did you forget a plugin?)", owner);
            nsme();
            return null;
        }
    }

    private FieldElement resolveField(final TypeDescriptor owner, final String name, final TypeDescriptor desc) {
        if (owner instanceof ClassTypeDescriptor) {
            DefinedTypeDefinition definedType = resolveDescriptor((ClassTypeDescriptor) owner);
            // it is present else {@link cc.quarkus.qcc.plugin.verification.ClassLoadingBasicBlockBuilder} would have failed
            FieldElement element = definedType.validate().resolveField(desc, name);
            if (element == null) {
                nsfe();
                return null;
            } else {
                // todo: compare descriptor
                return element;
            }
        } else {
            ctxt.error(getLocation(), "Resolve field on a non-class type `%s` (did you forget a plugin?)", owner);
            nsfe();
            return null;
        }
    }

    private DefinedTypeDefinition resolveDescriptor(final ClassTypeDescriptor owner) {
        final String typeName;
        if (owner.getPackageName().isEmpty()) {
            typeName = owner.getClassName();
        } else {
            typeName = owner.getPackageName() + "/" + owner.getClassName();
        }
        return getClassContext().findDefinedType(typeName);
    }

    private void nsfe() {
        Info info = Info.get(ctxt);
        // todo: add class name to exception string
        Value nsme = invokeConstructor(new_(info.nsfeClass), info.nsfeClass, MethodDescriptor.VOID_METHOD_DESCRIPTOR, List.of());
        throw_(nsme);
        // this is an unreachable block
        begin(new BlockLabel());
    }

    private void nsme() {
        Info info = Info.get(ctxt);
        // todo: add class name to exception string
        Value nsme = invokeConstructor(new_(info.nsmeClass), info.nsmeClass, MethodDescriptor.VOID_METHOD_DESCRIPTOR, List.of());
        throw_(nsme);
        // this is an unreachable block
        begin(new BlockLabel());
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
