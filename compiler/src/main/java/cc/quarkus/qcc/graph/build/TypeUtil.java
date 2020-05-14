package cc.quarkus.qcc.graph.build;

import cc.quarkus.qcc.graph.node.ConstantNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.WidenNode;
import cc.quarkus.qcc.type.QInt16;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QInt64;
import cc.quarkus.qcc.type.QInt8;
import cc.quarkus.qcc.type.QIntegral;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.QVoid;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class TypeUtil {

    @SuppressWarnings("unchecked")
    public static <V extends QType> Node<V> checkType(Node<?> node, Class<V> type) {
        if ( node instanceof ConstantNode && node.getType() == QVoid.class ) {
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
    public static <INPUT_V extends QType, OUTPUT_V extends QType> Node<OUTPUT_V> maybeWiden(Node<INPUT_V> node, Class<OUTPUT_V> type) {
        if ( type == QInt8.class) {
            return new WidenNode(node.getGraph(), node.getControl(), node, (TypeDescriptor<OUTPUT_V>) TypeDescriptor.INT8);
        } else if ( type == QInt16.class ) {
            return new WidenNode(node.getGraph(), node.getControl(), node, (TypeDescriptor<OUTPUT_V>) TypeDescriptor.INT16);
        } else if ( type == QInt32.class ) {
            return new WidenNode(node.getGraph(), node.getControl(), node, (TypeDescriptor<OUTPUT_V>) TypeDescriptor.INT32);
        } else if ( type == QInt64.class ) {
            return new WidenNode(node.getGraph(), node.getControl(), node, (TypeDescriptor<OUTPUT_V>) TypeDescriptor.INT64);
        }
        throw new RuntimeException( node + " (" + node.getType() + ") is not assignable to " + type);
    }
}
