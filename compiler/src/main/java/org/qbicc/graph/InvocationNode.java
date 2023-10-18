package org.qbicc.graph;

import java.util.Iterator;
import java.util.List;

import org.qbicc.type.InvokableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.VoidType;

/**
 * A node which performs some kind of invocation.
 */
public interface InvocationNode extends Node {

    /**
     * Get the pointer to the invocation target.
     *
     * @return the invocation target pointer (not {@code null})
     */
    Value getTarget();

    /**
     * Get the invocation receiver reference, if any.
     * May be an empty value of type {@code void} for non-instance invocations.
     *
     * @return the invocation receiver (not {@code null})
     */
    Value getReceiver();

    /**
     * Determine whether this node is invoking on a target which returns {@code void}.
     *
     * @return {@code true} if the target returns {@code void}, {@code false} otherwise
     */
    default boolean isVoidCall() {
        return getTarget().getReturnType() instanceof VoidType;
    }

    /**
     * Get the invocation arguments.
     *
     * @return the invocation arguments (not {@code null})
     */
    List<Value> getArguments();

    @Override
    default int getValueDependencyCount() {
        return getArguments().size() + 2;
    }

    @Override
    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> getTarget();
            case 1 -> getReceiver();
            default -> getArguments().get(index - 2);
        };
    }

    /**
     * Get the type of the callee.
     *
     * @return the callee type (not {@code null})
     */
    default InvokableType getCalleeType() {
        return getTarget().getType(PointerType.class).getPointeeType(InvokableType.class);
    }

    static StringBuilder toRValueString(InvocationNode node, String label, StringBuilder b) {
        Value receiver = node.getReceiver();
        if (! (receiver.getType() instanceof VoidType)) {
            receiver.toReferenceString(b).append("::");
        }
        Value target = node.getTarget();
        target.toReferenceString(b.append(label).append(' '));
        List<Value> arguments = node.getArguments();
        b.append('(');
        Iterator<Value> iter = arguments.iterator();
        if (iter.hasNext()) {
            iter.next().toReferenceString(b);
            while (iter.hasNext()) {
                iter.next().toReferenceString(b.append(',').append(' '));
            }
        }
        b.append(')');
        if (target.isNoThrow()) {
            b.append(" no-throw");
        }
        return b;
    }
}
