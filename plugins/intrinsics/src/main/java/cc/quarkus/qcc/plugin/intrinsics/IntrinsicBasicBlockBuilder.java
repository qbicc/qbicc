package cc.quarkus.qcc.plugin.intrinsics;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import org.jboss.logging.Logger;

/**
 * The basic block builder which substitutes invocations of intrinsic methods.
 * 
 * This only handles the "unresolved" form of method calls and assumes that all
 * methods to be replaced by intrinsics originate from descriptors (ie: classfile
 * parsing).
 */
public final class IntrinsicBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public static final Logger log = Logger.getLogger("cc.quarkus.qcc.plugin.intrinsics");

    private final CompilationContext ctxt;

    public IntrinsicBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Node invokeStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found StaticIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, owner, name, descriptor, arguments);
        }
        return super.invokeStatic(owner, name, descriptor, arguments);
    }

    public Value invokeValueStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        StaticValueIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticValueIntrinsic(owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found StaticValueIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, owner, name, descriptor, arguments);
        }
        return super.invokeValueStatic(owner, name, descriptor, arguments);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found InstanceIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, kind, instance, owner, name, descriptor, arguments);
        }
        return super.invokeInstance(kind, instance, owner, name, descriptor, arguments);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        InstanceValueIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceValueIntrinsic(owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found InstanceValueIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, kind, instance, owner, name, descriptor, arguments);
        }
        return super.invokeValueInstance(kind, instance, owner, name, descriptor, arguments);
    }
}
