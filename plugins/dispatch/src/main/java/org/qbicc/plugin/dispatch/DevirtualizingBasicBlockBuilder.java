package org.qbicc.plugin.dispatch;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.*;
import org.qbicc.type.definition.element.MethodElement;
import org.jboss.logging.Logger;

import java.util.List;

public class DevirtualizingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final Logger log = Logger.getLogger("cc.quarkus.qcc.plugin.dispatch.devirt");

    private final CompilationContext ctxt;

    public DevirtualizingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Node invokeInstance(DispatchInvocation.Kind kind, final Value instance, MethodElement target, final List<Value> arguments) {
        if (kind == DispatchInvocation.Kind.INTERFACE) {
            MethodElement virtualTarget = virtualizeInvokeInterface(instance, target);
            if (virtualTarget == null) {
                return super.invokeInstance(kind, instance, target, arguments);
            }
            kind = DispatchInvocation.Kind.VIRTUAL;
            target = virtualTarget;
        }

        if (kind == DispatchInvocation.Kind.VIRTUAL) {
            MethodElement exactTarget = staticallyBind(instance, target);
            if (exactTarget != null) {
                return super.invokeInstance(DispatchInvocation.Kind.EXACT, instance, exactTarget, arguments);
            }
        }

        return super.invokeInstance(kind, instance, target, arguments);
    }

    public Value invokeValueInstance(DispatchInvocation.Kind kind, final Value instance, MethodElement target, final List<Value> arguments) {
        if (kind == DispatchInvocation.Kind.INTERFACE) {
            MethodElement virtualTarget = virtualizeInvokeInterface(instance, target);
            if (virtualTarget == null) {
                return super.invokeValueInstance(kind, instance, target, arguments);
            }
            kind = DispatchInvocation.Kind.VIRTUAL;
            target = virtualTarget;
        }

        if (kind == DispatchInvocation.Kind.VIRTUAL) {
            MethodElement exactTarget = staticallyBind(instance, target);
            if (exactTarget != null) {
                return super.invokeValueInstance(DispatchInvocation.Kind.EXACT, instance, exactTarget, arguments);
            }
        }

        return super.invokeValueInstance(kind, instance, target, arguments);
    }

    /*
     * Determine if an interface call be converted to a virtual call based on the static
     * type of the receiver.
     */
    private MethodElement virtualizeInvokeInterface(final Value instance, final MethodElement target) {
        // TODO:  Eventually implement logic for interface => virtual conversion based on actual compile-time type of `instance`.
        return null;
    }

    /*
     * Determine if a virtual call can be statically bound to a single target method.
     * If yes, return the exact target method.  If no, return null.
     */
    private MethodElement staticallyBind(final Value instance, final MethodElement target) {
        // Handle a very trivial case as a proof of concept that the phase actually does something..
        if (target.isFinal() || target.getEnclosingType().isFinal() || target.isPrivate()) {
            log.debugf("Devirtualizing call to %s::%s", target.getEnclosingType().getDescriptor().getClassName(), target.getName());
            return target;
        }

        // Unable to statically bind
        return null;
    }
}
