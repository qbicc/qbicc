package org.qbicc.plugin.verification;

import java.util.List;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.DispatchInvocation;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

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

    public ValueHandle instanceFieldOf(ValueHandle instance, TypeDescriptor owner, String name, TypeDescriptor type) {
        if (loadClass(owner)) {
            return super.instanceFieldOf(instance, owner, name, type);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound());
    }

    public ValueHandle staticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        if (loadClass(owner)) {
            return super.staticField(owner, name, type);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound());
    }

    public Value checkcast(final Value value, TypeDescriptor desc) {
        TypeDescriptor orig = desc;
        while (desc instanceof ArrayTypeDescriptor) {
            desc = ((ArrayTypeDescriptor) desc).getElementTypeDescriptor();
        }
        if (desc instanceof ClassTypeDescriptor) {
            if (! loadClass((ClassTypeDescriptor) desc)) {
                // no need to continue
                throw new BlockEarlyTermination(noClassDefFound());
            }
        }
        return super.checkcast(value, orig);
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        TypeDescriptor baseDescriptor = desc;
        while (baseDescriptor instanceof ArrayTypeDescriptor) {
            baseDescriptor = ((ArrayTypeDescriptor) baseDescriptor).getElementTypeDescriptor();
        }
        if (baseDescriptor instanceof ClassTypeDescriptor) {
            if (! loadClass((ClassTypeDescriptor) baseDescriptor)) {
                // no need to continue
                throw new BlockEarlyTermination(noClassDefFound());
            }
        }
        return super.instanceOf(input, desc);
    }

    public Value new_(final ClassTypeDescriptor desc) {
        if (loadClass(desc)) {
            return super.new_(desc);
        } else {
            // no need to continue
            throw new BlockEarlyTermination(noClassDefFound());
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
            // no need to continue
            throw new BlockEarlyTermination(noClassDefFound());
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
            // no need to continue
            throw new BlockEarlyTermination(noClassDefFound());
        }
    }

    public Node invokeStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        if (loadClass(owner)) {
            return super.invokeStatic(owner, name, descriptor, arguments);
        } else {
            // no need to continue
            throw new BlockEarlyTermination(noClassDefFound());
        }
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        if (loadClass(owner)) {
            return super.invokeInstance(kind, instance, owner, name, descriptor, arguments);
        } else {
            // no need to continue
            throw new BlockEarlyTermination(noClassDefFound());
        }
    }

    public Value invokeValueStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        if (loadClass(owner)) {
            return super.invokeValueStatic(owner, name, descriptor, arguments);
        } else {
            // no need to continue
            throw new BlockEarlyTermination(noClassDefFound());
        }
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        if (loadClass(owner)) {
            return super.invokeValueInstance(kind, instance, owner, name, descriptor, arguments);
        } else {
            // no need to continue
            throw new BlockEarlyTermination(noClassDefFound());
        }
    }

    private BasicBlock noClassDefFound() {
        Info info = Info.get(ctxt);
        ClassTypeDescriptor ncdfeClass = info.ncdfeClass;
        // todo: add class name to exception string
        Value ncdfe = invokeConstructor(new_(ncdfeClass), ncdfeClass, MethodDescriptor.VOID_METHOD_DESCRIPTOR, List.of());
        return throw_(ncdfe);
    }

    private boolean loadClass(TypeDescriptor desc) {
        return ! (desc instanceof ClassTypeDescriptor) || loadClass((ClassTypeDescriptor) desc);
    }

    private boolean loadClass(ClassTypeDescriptor desc) {
        final String typeName;
        if (desc.getPackageName().isEmpty()) {
            typeName = desc.getClassName();
        } else {
            typeName = desc.getPackageName() + "/" + desc.getClassName();
        }
        DefinedTypeDefinition definedType = getClassContext().findDefinedType(typeName);
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
        final ClassTypeDescriptor ncdfeClass;

        private Info(final CompilationContext ctxt) {
            DefinedTypeDefinition type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/NoClassDefFoundError");
            ncdfeClass = type.getDescriptor();
        }

        static Info get(CompilationContext ctxt) {
            return ctxt.computeAttachmentIfAbsent(KEY, () -> new Info(ctxt));
        }
    }

}
