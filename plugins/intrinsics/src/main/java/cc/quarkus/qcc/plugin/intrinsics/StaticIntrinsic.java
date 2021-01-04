package cc.quarkus.qcc.plugin.intrinsics;

import java.util.List;

import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 * A static intrinsic method which returns no value.
 */
public interface StaticIntrinsic {
    Node emitIntrinsic(BasicBlockBuilder builder, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments);
}
