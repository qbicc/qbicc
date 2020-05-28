package cc.quarkus.qcc.graph2;

import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 * A method invocation.  Instance invocations must pass the receiver as the first argument.
 */
public interface Invocation extends MemoryState {
    MethodDescriptor<?> getInvocationTarget();

    /**
     * Set the invocation target, which in turn also sets the argument count.
     *
     * @param descriptor the method descriptor of the invocation target
     */
    void setInvocationTarget(MethodDescriptor<?> descriptor);
    int getArgumentCount();
    void setArgumentCount(int newCount);
    Value getArgument(int index);
    void setArgument(int index, Value argument);
}
