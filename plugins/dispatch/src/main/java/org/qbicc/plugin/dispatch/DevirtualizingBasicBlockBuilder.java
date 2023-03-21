package org.qbicc.plugin.dispatch;

import org.jboss.logging.Logger;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.MethodElement;

public class DevirtualizingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final Logger log = Logger.getLogger("org.qbicc.plugin.dispatch.devirt");

    public DevirtualizingBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
    }

    @Override
    public Value lookupInterfaceMethod(Value reference, InstanceMethodElement method) {
        MethodElement exactTarget = staticallyBind(method);
        if (exactTarget != null) {
            return getLiteralFactory().literalOf(exactTarget);
        }
        InstanceMethodElement virtualTarget = virtualizeInvokeInterface(reference, method);
        if (virtualTarget != null) {
            return getFirstBuilder().lookupVirtualMethod(reference, virtualTarget);
        }
        return super.lookupInterfaceMethod(reference, method);
    }

    @Override
    public Value lookupVirtualMethod(Value reference, InstanceMethodElement method) {
        MethodElement exactTarget = staticallyBind(method);
        return exactTarget != null ? getLiteralFactory().literalOf(exactTarget) : super.lookupVirtualMethod(reference, method);
    }

    /*
     * Determine if an interface call be converted to a virtual call based on the static
     * type of the receiver.
     */
    private InstanceMethodElement virtualizeInvokeInterface(final Value reference, final InstanceMethodElement target) {
        ClassObjectType classType;
        ValueType type = reference.getType();
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
        InstanceMethodElement virtual = (InstanceMethodElement) definition.resolveMethodElementVirtual(getCurrentClassContext(), target.getName(), target.getDescriptor());
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
    private MethodElement staticallyBind(final MethodElement target) {
        // Handle a very trivial case as a proof of concept that the phase actually does something..
        if (target.isFinal() || target.getEnclosingType().isFinal() || target.isPrivate()) {
            log.debugf("Devirtualizing call to %s::%s", target.getEnclosingType().getDescriptor(), target.getName());
            return target;
        }

        // Unable to statically bind
        return null;
    }
}
