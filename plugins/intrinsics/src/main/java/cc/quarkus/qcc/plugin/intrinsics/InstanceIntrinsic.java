package cc.quarkus.qcc.plugin.intrinsics;

import java.util.List;

import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 * An instance intrinsic method which returns no value.
 */
public interface InstanceIntrinsic {
    Node emitIntrinsic(BasicBlockBuilder builder, DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments);
    Node emitIntrinsic(BasicBlockBuilder builder, DispatchInvocation.Kind kind, Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments);
}
