package org.qbicc.plugin.conversion;

import java.util.List;
import java.util.function.Supplier;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.DispatchInvocation;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class CloneConversionBasicBlockBuilder extends DelegatingBasicBlockBuilder implements Supplier<CloneConversionBasicBlockBuilder.Info> {
    private static final AttachmentKey<Info> KEY = new AttachmentKey<>();
    private final CompilationContext ctxt;

    public CloneConversionBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        if (name.equals("clone") && descriptor.getParameterTypes().isEmpty()) {
            if (owner instanceof ArrayTypeDescriptor) {
                return clone(instance);
            } else if (isJLO(owner)) {
                BlockLabel goAhead = new BlockLabel();
                BlockLabel throwIt = new BlockLabel();
                Info info = getInfo(ctxt);
                if_(instanceOf(instance, info.cloneable), goAhead, throwIt);
                begin(throwIt);
                throw_(invokeConstructor(new_(info.notSupp), info.notSupp, MethodDescriptor.VOID_METHOD_DESCRIPTOR, List.of()));
                begin(goAhead);
                return clone(instance);
            }
        }
        return super.invokeValueInstance(kind, instance, owner, name, descriptor, arguments);
    }

    private boolean isJLO(final TypeDescriptor owner) {
        return owner instanceof ClassTypeDescriptor && isJLO((ClassTypeDescriptor) owner);
    }

    private boolean isJLO(final ClassTypeDescriptor owner) {
        return owner.getPackageName().equals("java/lang") && owner.getClassName().equals("Object");
    }

    Info getInfo(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, this);
    }

    // implement Supplier to avoid lambda
    public Info get() {
        return new Info(ctxt);
    }

    static final class Info {
        final ClassTypeDescriptor cloneable;
        final ClassTypeDescriptor notSupp;

        Info(CompilationContext ctxt) {
            ClassContext cc = ctxt.getBootstrapClassContext();
            cloneable = ClassTypeDescriptor.synthesize(cc, "java/lang/Cloneable");
            notSupp = ClassTypeDescriptor.synthesize(cc, "java/lang/CloneNotSupportedException");
        }

    }
}
