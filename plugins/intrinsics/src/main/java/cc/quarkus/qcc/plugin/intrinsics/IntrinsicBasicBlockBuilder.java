package cc.quarkus.qcc.plugin.intrinsics;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.driver.Phase;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.definition.element.MethodElement;
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
public abstract class IntrinsicBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public static final Logger log = Logger.getLogger("cc.quarkus.qcc.plugin.intrinsics");

    private final CompilationContext ctxt;
    private final Phase phase;

    public static final class AddIntrinsicBasicBlockBuilder extends IntrinsicBasicBlockBuilder {
        public AddIntrinsicBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
            super(ctxt, delegate, Phase.ADD);
        }
    }

    public static final class LowerIntrinsicBasicBlockBuilder extends IntrinsicBasicBlockBuilder {
        public LowerIntrinsicBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
            super(ctxt, delegate, Phase.LOWER);
        }
    }

    public IntrinsicBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate, Phase phase) {
        super(delegate);
        this.phase = phase;
        this.ctxt = ctxt;
    }

    public Node invokeStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found StaticIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, owner, name, descriptor, arguments);
        }
        return super.invokeStatic(owner, name, descriptor, arguments);
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        TypeDescriptor owner = target.getEnclosingType().getDescriptor();
        String name = target.getName();
        MethodDescriptor descriptor = target.getDescriptor();
        StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found StaticIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, owner, name, descriptor, arguments);
        }
        return super.invokeStatic(target, arguments);
    }

    public Value invokeValueStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        StaticValueIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticValueIntrinsic(phase, owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found StaticValueIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, owner, name, descriptor, arguments);
        }
        return super.invokeValueStatic(owner, name, descriptor, arguments);
    }

    public Value invokeValueStatic(MethodElement target, final List<Value> arguments) {
        TypeDescriptor owner = target.getEnclosingType().getDescriptor();
        String name = target.getName();
        MethodDescriptor descriptor = target.getDescriptor();
        StaticValueIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticValueIntrinsic(phase, owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found StaticValueIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, owner, name, descriptor, arguments);
        }
        return super.invokeValueStatic(target, arguments);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found InstanceIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, kind, instance, owner, name, descriptor, arguments);
        }
        return super.invokeInstance(kind, instance, owner, name, descriptor, arguments);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, MethodElement target, final List<Value> arguments) {
        TypeDescriptor owner = target.getEnclosingType().getDescriptor();
        String name = target.getName();
        MethodDescriptor descriptor = target.getDescriptor();
        InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found InstanceIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, kind, instance, owner, name, descriptor, arguments);
        }
        return super.invokeInstance(kind, instance, target, arguments);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        InstanceValueIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceValueIntrinsic(phase, owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found InstanceValueIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, kind, instance, owner, name, descriptor, arguments);
        }
        return super.invokeValueInstance(kind, instance, owner, name, descriptor, arguments);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, MethodElement target, final List<Value> arguments) {
        TypeDescriptor owner = target.getEnclosingType().getDescriptor();
        String name = target.getName();
        MethodDescriptor descriptor = target.getDescriptor();
        InstanceValueIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceValueIntrinsic(phase, owner, name, descriptor);
        if (intrinsic != null) {
            log.debugf("found InstanceValueIntrinsic for owner(%s) name(%s) descriptor(%s)", owner, name, descriptor);
            return intrinsic.emitIntrinsic(this, kind, instance, owner, name, descriptor, arguments);
        }
        return super.invokeValueInstance(kind, instance, target, arguments);
    }
}
