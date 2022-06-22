package org.qbicc.plugin.dispatch;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.*;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;
import org.jboss.logging.Logger;
import org.qbicc.type.descriptor.MethodDescriptor;

public class DevirtualizingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final Logger log = Logger.getLogger("org.qbicc.plugin.dispatch.devirt");

    private final CompilationContext ctxt;

    public DevirtualizingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public ValueHandle interfaceMethodOf(Value instance, MethodElement target, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        MethodElement exactTarget = staticallyBind(instance, target);
        if (exactTarget != null) {
            return exactMethodOf(instance, exactTarget, callSiteDescriptor, callSiteType);
        }
        MethodElement virtualTarget = virtualizeInvokeInterface(instance, target);
        if (virtualTarget != null) {
            return virtualMethodOf(instance, virtualTarget, callSiteDescriptor, callSiteType);
        }
        return super.interfaceMethodOf(instance, target, callSiteDescriptor, callSiteType);
    }

    @Override
    public ValueHandle virtualMethodOf(Value instance, MethodElement target, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        MethodElement exactTarget = staticallyBind(instance, target);
        return exactTarget != null ? exactMethodOf(instance, exactTarget, callSiteDescriptor, callSiteType) : super.virtualMethodOf(instance, target, callSiteDescriptor, callSiteType);
    }

    /*
     * Determine if an interface call be converted to a virtual call based on the static
     * type of the receiver.
     */
    private MethodElement virtualizeInvokeInterface(final Value instance, final MethodElement target) {
        ClassObjectType classType;
        ValueType type = instance.getType();
        if (type instanceof ReferenceType) {
            PhysicalObjectType upperBound = ((ReferenceType) type).getUpperBound();
            if (upperBound instanceof ClassObjectType) {
                classType = (ClassObjectType) upperBound;
            } else {
                return null;
            }
        } else if (type instanceof ClassObjectType) {
            classType = (ClassObjectType) type;
        } else {
            return null;
        }
        // only select the class if it implements the interface; else we risk adding a more general method than we had before
        if (! classType.isSubtypeOf(target.getEnclosingType().load().getObjectType())) {
            return null;
        }
        LoadedTypeDefinition definition = classType.getDefinition().load();
        MethodElement virtual = definition.resolveMethodElementVirtual(target.getName(), target.getDescriptor());
        if (virtual != null) {
            log.debugf("Deinterfacing call to %s::%s", target.getEnclosingType().getDescriptor(), target.getName());
            return virtual;
        }
        return null;
    }

    /*
     * Determine if a virtual call can be statically bound to a single target method.
     * If yes, return the exact target method.  If no, return null.
     */
    private MethodElement staticallyBind(final Value instance, final MethodElement target) {
        // Handle a very trivial case as a proof of concept that the phase actually does something..
        if (target.isFinal() || target.getEnclosingType().isFinal() || target.isPrivate()) {
            log.debugf("Devirtualizing call to %s::%s", target.getEnclosingType().getDescriptor(), target.getName());
            return target;
        }

        // Unable to statically bind
        return null;
    }
}
