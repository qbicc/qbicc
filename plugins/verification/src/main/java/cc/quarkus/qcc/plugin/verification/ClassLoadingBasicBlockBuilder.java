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
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 * A basic block builder which ensures that all call and field target classes are loaded.  If the class is not
 * loaded, the access will be replaced with a {@code java.lang.NoClassDefFoundError} throw.
 */
public class ClassLoadingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final AttachmentKey<Info> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;

    public ClassLoadingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value narrow(final Value value, TypeDescriptor desc) {
        while (desc instanceof ArrayTypeDescriptor) {
            desc = ((ArrayTypeDescriptor) desc).getElementTypeDescriptor();
        }
        if (desc instanceof ClassTypeDescriptor) {
            if (! loadClass((ClassTypeDescriptor) desc)) {
                noClassDefFound();
                return ctxt.getLiteralFactory().literalOfNull();
            }
        }
        return super.narrow(value, desc);
    }

    public Value instanceOf(final Value input, TypeDescriptor desc) {
        while (desc instanceof ArrayTypeDescriptor) {
            desc = ((ArrayTypeDescriptor) desc).getElementTypeDescriptor();
        }
        if (desc instanceof ClassTypeDescriptor) {
            if (! loadClass((ClassTypeDescriptor) desc)) {
                noClassDefFound();
                return ctxt.getLiteralFactory().literalOf(false);
            }
        }
        return super.instanceOf(input, desc);
    }

    public Value new_(final ClassTypeDescriptor desc) {
        if (loadClass(desc)) {
            return super.new_(desc);
        } else {
            noClassDefFound();
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        TypeDescriptor elemDesc = desc.getElementTypeDescriptor();
        while (elemDesc instanceof ArrayTypeDescriptor) {
            elemDesc = ((ArrayTypeDescriptor) elemDesc).getElementTypeDescriptor();
        }
        if (! (elemDesc instanceof ClassTypeDescriptor) || loadClass((ClassTypeDescriptor) elemDesc)) {
            return super.newArray(desc, size);
        } else {
            noClassDefFound();
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        TypeDescriptor elemDesc = desc.getElementTypeDescriptor();
        while (elemDesc instanceof ArrayTypeDescriptor) {
            elemDesc = ((ArrayTypeDescriptor) elemDesc).getElementTypeDescriptor();
        }
        if (! (elemDesc instanceof ClassTypeDescriptor) || loadClass((ClassTypeDescriptor) elemDesc)) {
            return super.multiNewArray(desc, dimensions);
        } else {
            noClassDefFound();
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Value readInstanceField(final Value instance, final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final JavaAccessMode mode) {
        if (loadClass(owner)) {
            return super.readInstanceField(instance, owner, name, descriptor, mode);
        } else {
            noClassDefFound();
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Value readStaticField(final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final JavaAccessMode mode) {
        if (loadClass(owner)) {
            return super.readStaticField(owner, name, descriptor, mode);
        } else {
            noClassDefFound();
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Node writeInstanceField(final Value instance, final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final Value value, final JavaAccessMode mode) {
        if (loadClass(owner)) {
            return super.writeInstanceField(instance, owner, name, descriptor, value, mode);
        } else {
            noClassDefFound();
            return nop();
        }
    }

    public Node writeStaticField(final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final Value value, final JavaAccessMode mode) {
        if (loadClass(owner)) {
            return super.writeStaticField(owner, name, descriptor, value, mode);
        } else {
            noClassDefFound();
        }
        return nop();
    }

    public Node invokeStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        if (loadClass(owner)) {
            return super.invokeStatic(owner, name, descriptor, arguments);
        } else {
            noClassDefFound();
            return nop();
        }
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        if (loadClass(owner)) {
            return super.invokeInstance(kind, instance, owner, name, descriptor, arguments);
        } else {
            noClassDefFound();
            return nop();
        }
    }

    public Value invokeValueStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        if (loadClass(owner)) {
            return super.invokeValueStatic(owner, name, descriptor, arguments);
        } else {
            noClassDefFound();
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        if (loadClass(owner)) {
            return super.invokeValueInstance(kind, instance, owner, name, descriptor, arguments);
        } else {
            noClassDefFound();
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    private void noClassDefFound() {
        Info info = Info.get(ctxt);
        ClassTypeDescriptor ncdfeClass = info.ncdfeClass;
        // todo: add class name to exception string
        Value ncdfe = invokeConstructor(new_(info.ncdfeClassId), ncdfeClass, MethodDescriptor.VOID_METHOD_DESCRIPTOR, List.of());
        throw_(ncdfe);
        // this is an unreachable block
        begin(new BlockLabel());
    }

    private boolean loadClass(TypeDescriptor desc) {
        return ! (desc instanceof ClassTypeDescriptor) || loadClass((ClassTypeDescriptor) desc);
    }

    private boolean loadClass(ClassTypeDescriptor desc) {
        DefinedTypeDefinition definedType = getClassContext().findDefinedType(desc.getPackageName() + "/" + desc.getClassName());
        if (definedType == null) {
            return false;
        }
        definedType.validate();
        return true;
    }

    private ClassContext getClassContext() {
        return getCurrentElement().getEnclosingType().getContext();
    }

    static final class Info {
        final ClassTypeIdLiteral ncdfeClassId;
        final ClassTypeDescriptor ncdfeClass;

        private Info(final CompilationContext ctxt) {
            DefinedTypeDefinition type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/NoClassDefFoundError");
            ncdfeClassId = (ClassTypeIdLiteral) type.validate().getTypeId();
            ncdfeClass = type.getDescriptor();
        }

        static Info get(CompilationContext ctxt) {
            return ctxt.computeAttachmentIfAbsent(KEY, () -> new Info(ctxt));
        }
    }

}
