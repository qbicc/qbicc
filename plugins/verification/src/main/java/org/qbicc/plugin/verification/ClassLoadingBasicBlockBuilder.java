package org.qbicc.plugin.verification;

import java.util.List;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.PointerValue;
import org.qbicc.context.ClassContext;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.VerifyFailedException;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
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

    public ClassLoadingBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    public PointerValue instanceFieldOf(PointerValue instance, TypeDescriptor owner, String name, TypeDescriptor type) {
        if (loadClass(owner)) {
            return super.instanceFieldOf(instance, owner, name, type);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound(owner));
    }

    public PointerValue staticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        if (loadClass(owner)) {
            return super.staticField(owner, name, type);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound(owner));
    }

    public PointerValue exactMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        if (loadClass(owner)) {
            return super.exactMethodOf(instance, owner, name, descriptor);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound(owner));
    }

    public PointerValue virtualMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        if (loadClass(owner)) {
            return super.virtualMethodOf(instance, owner, name, descriptor);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound(owner));
    }

    public PointerValue interfaceMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        if (loadClass(owner)) {
            return super.interfaceMethodOf(instance, owner, name, descriptor);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound(owner));
    }

    public PointerValue staticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        if (loadClass(owner)) {
            return super.staticMethod(owner, name, descriptor);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound(owner));
    }

    public PointerValue constructorOf(Value instance, TypeDescriptor owner, MethodDescriptor descriptor) {
        if (loadClass(owner)) {
            return super.constructorOf(instance, owner, descriptor);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound(owner));
    }

    public Value checkcast(final Value value, TypeDescriptor desc) {
        TypeDescriptor orig = desc;
        while (desc instanceof ArrayTypeDescriptor) {
            desc = ((ArrayTypeDescriptor) desc).getElementTypeDescriptor();
        }
        if (desc instanceof ClassTypeDescriptor) {
            if (! loadClass((ClassTypeDescriptor) desc)) {
                // no need to continue
                throw new BlockEarlyTermination(noClassDefFound(desc));
            }
        }
        return super.checkcast(value, orig);
    }

    @Override
    public Value interfaceMethodLookup(TypeDescriptor owner, String name, MethodDescriptor descriptor, Value instanceTypeId) {
        if (loadClass(owner)) {
            return super.interfaceMethodLookup(owner, name, descriptor, instanceTypeId);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound(owner));
    }

    @Override
    public Value virtualMethodLookup(TypeDescriptor owner, String name, MethodDescriptor descriptor, Value instanceTypeId) {
        if (loadClass(owner)) {
            return super.virtualMethodLookup(owner, name, descriptor, instanceTypeId);
        }
        // no need to continue
        throw new BlockEarlyTermination(noClassDefFound(owner));
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        TypeDescriptor baseDescriptor = desc;
        while (baseDescriptor instanceof ArrayTypeDescriptor) {
            baseDescriptor = ((ArrayTypeDescriptor) baseDescriptor).getElementTypeDescriptor();
        }
        if (baseDescriptor instanceof ClassTypeDescriptor) {
            if (! loadClass((ClassTypeDescriptor) baseDescriptor)) {
                // no need to continue
                throw new BlockEarlyTermination(noClassDefFound(desc));
            }
        }
        return super.instanceOf(input, desc);
    }

    public Value new_(final ClassTypeDescriptor desc) {
        if (loadClass(desc)) {
            return super.new_(desc);
        } else {
            // no need to continue
            throw new BlockEarlyTermination(noClassDefFound(desc));
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
            throw new BlockEarlyTermination(noClassDefFound(elemDesc));
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
            throw new BlockEarlyTermination(noClassDefFound(elemDesc));
        }
    }

    private BasicBlock noClassDefFound(TypeDescriptor desc) {
        Info info = Info.get(ctxt);
        ClassTypeDescriptor ncdfeClass = info.ncdfeClass;
        Value ncdfe = new_(ncdfeClass);
        String fullName;
        if (desc instanceof ClassTypeDescriptor ctd) {
            fullName = ctd.getPackageName().isEmpty() ? ctd.getClassName() : ctd.getPackageName() + '/' + ctd.getClassName();
        } else {
            fullName = desc.toString();
        }
        StringLiteral msg = ctxt.getLiteralFactory().literalOf(fullName, getClassContext().findDefinedType("java/lang/String").load().getObjectType().getReference());
        call(constructorOf(ncdfe, ncdfeClass, info.voidStringDesc), List.of(msg));
        return throw_(ncdfe);
    }

    private BasicBlock verifyError(TypeDescriptor desc) {
        Info info = Info.get(ctxt);
        ClassTypeDescriptor veClass = info.veClass;
        Value ve = new_(veClass);
        String fullName;
        if (desc instanceof ClassTypeDescriptor ctd) {
            fullName = ctd.getPackageName().isEmpty() ? ctd.getClassName() : ctd.getPackageName() + '/' + ctd.getClassName();
        } else {
            fullName = desc.toString();
        }
        StringLiteral msg = ctxt.getLiteralFactory().literalOf(fullName, getClassContext().findDefinedType("java/lang/String").load().getObjectType().getReference());
        call(constructorOf(ve, veClass, info.voidStringDesc), List.of(msg));
        return throw_(ve);
    }

    private boolean loadClass(TypeDescriptor desc) {
        return ! (desc instanceof ClassTypeDescriptor) || loadClass((ClassTypeDescriptor) desc);
    }

    private boolean loadClass(ClassTypeDescriptor desc) {
        if (desc == getCurrentElement().getEnclosingType().getDescriptor()) {
            return true;
        }
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
        try {
            definedType.load();
        } catch (VerifyFailedException e) {
            throw new BlockEarlyTermination(verifyError(desc));
        }
        return true;
    }

    private ClassContext getClassContext() {
        return getCurrentElement().getEnclosingType().getContext();
    }

    static final class Info {
        final ClassTypeDescriptor ncdfeClass;
        final ClassTypeDescriptor veClass;
        final MethodDescriptor voidStringDesc;

        private Info(final CompilationContext ctxt) {
            DefinedTypeDefinition type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/NoClassDefFoundError");
            ncdfeClass = (ClassTypeDescriptor) type.getDescriptor();
            type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/VerifyError");
            veClass = (ClassTypeDescriptor) type.getDescriptor();
            type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String");
            voidStringDesc = MethodDescriptor.synthesize(ctxt.getBootstrapClassContext(), BaseTypeDescriptor.V, List.of(type.getDescriptor()));
        }

        static Info get(CompilationContext ctxt) {
            return ctxt.computeAttachmentIfAbsent(KEY, () -> new Info(ctxt));
        }
    }

}
