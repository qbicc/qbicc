package org.qbicc.plugin.intrinsics;

import java.util.List;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DispatchInvocation;
import org.qbicc.graph.Value;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * An instance intrinsic method which returns a value.
 */
public interface InstanceValueIntrinsic {
    Value emitIntrinsic(BasicBlockBuilder builder, DispatchInvocation.Kind kind, Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments);
}
