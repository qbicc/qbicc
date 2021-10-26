package org.qbicc.plugin.intrinsics;

import java.util.List;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;

/**
 * A static intrinsic method which returns no value.
 */
public interface StaticIntrinsic {
    /**
     * Write out the intrinsic.
     *
     * @param builder the block builder to use (not {@code null})
     * @param target the invocation target (not {@code null})
     * @param arguments the list of arguments passed to the call (not {@code null})
     * @return the return value of the intrinsic (may be {@code null} if the method is {@code void})
     */
    Value emitIntrinsic(BasicBlockBuilder builder, StaticMethodElementHandle target, List<Value> arguments);
}
