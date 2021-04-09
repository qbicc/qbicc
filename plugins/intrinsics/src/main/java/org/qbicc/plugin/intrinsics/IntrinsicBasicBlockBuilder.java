package org.qbicc.plugin.intrinsics;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.DispatchInvocation;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.jboss.logging.Logger;

/**
 * The basic block builder which substitutes invocations of intrinsic methods.
 * 
 * This only handles the "unresolved" form of method calls and assumes that all
 * methods to be replaced by intrinsics originate from descriptors (ie: classfile
 * parsing).
 */
public final class IntrinsicBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public static final Logger log = Logger.getLogger("org.qbicc.plugin.intrinsics");

    private final CompilationContext ctxt;
    private final Phase phase;

    private IntrinsicBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate, Phase phase) {
        super(delegate);
        this.phase = phase;
        this.ctxt = ctxt;
    }

    public static IntrinsicBasicBlockBuilder createForAddPhase(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        return new IntrinsicBasicBlockBuilder(ctxt, delegate, Phase.ADD);
    }

    public static IntrinsicBasicBlockBuilder createForLowerPhase(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        return new IntrinsicBasicBlockBuilder(ctxt, delegate, Phase.LOWER);
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
