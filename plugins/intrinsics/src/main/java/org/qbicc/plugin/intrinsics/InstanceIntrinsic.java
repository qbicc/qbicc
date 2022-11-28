package org.qbicc.plugin.intrinsics;

import java.util.List;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.InstanceMethodLiteral;

/**
 * An instance intrinsic method.
 */
public interface InstanceIntrinsic {
    /**
     * Write out the intrinsic.
     *
     * @param builder the block builder to use (not {@code null})
     * @param instance the instance value (not {@code null})
     * @param targetPtr the invocation target (not {@code null})
     * @param arguments the list of arguments passed to the call (not {@code null})
     * @return the return value of the intrinsic (may be {@code null} if the method is {@code void})
     */
    Value emitIntrinsic(BasicBlockBuilder builder, Value instance, InstanceMethodLiteral targetPtr, List<Value> arguments);
}
