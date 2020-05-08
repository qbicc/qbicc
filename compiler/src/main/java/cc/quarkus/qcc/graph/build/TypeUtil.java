package cc.quarkus.qcc.graph.build;

import cc.quarkus.qcc.graph.node.ConstantNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.WidenNode;
import cc.quarkus.qcc.type.Sentinel;
import cc.quarkus.qcc.type.TypeDescriptor;

public class TypeUtil {

    @SuppressWarnings("unchecked")
    public static <V> Node<V> checkType(Node<?> node, Class<V> type) {
        if ( node instanceof ConstantNode && node.getType() == Sentinel.Void.class ) {
            return (Node<V>) node;
        }
        if ( type == null ) {
            return (Node<V>) node;
        }
        if ( type.isAssignableFrom(node.getType())) {
            return (Node<V>) node;
        }

        return maybeWiden(node, type);
    }

    @SuppressWarnings("unchecked")
    public static <INPUT_V, OUTPUT_V> Node<OUTPUT_V> maybeWiden(Node<INPUT_V> node, Class<OUTPUT_V> type) {
        if ( type == Byte.class) {
            return new WidenNode<>(node.getControl(), node, (TypeDescriptor<OUTPUT_V>) TypeDescriptor.BYTE);
        } else if ( type == Short.class ) {
            return new WidenNode<>(node.getControl(), node, (TypeDescriptor<OUTPUT_V>) TypeDescriptor.SHORT);
        } else if ( type == Integer.class ) {
            return new WidenNode<>(node.getControl(), node, (TypeDescriptor<OUTPUT_V>) TypeDescriptor.INT);
        } else if ( type == Long.class ) {
            return new WidenNode<>(node.getControl(), node, (TypeDescriptor<OUTPUT_V>) TypeDescriptor.LONG);
        }
        throw new RuntimeException( node + " (" + node.getType() + ") is not assignable to " + type);
    }
}
