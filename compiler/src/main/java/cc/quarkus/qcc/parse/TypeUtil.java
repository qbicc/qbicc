package cc.quarkus.qcc.parse;

import cc.quarkus.qcc.graph.node.Node;

public class TypeUtil {
    public static <V> Node<V> checkType(Node<?> node, Class<V> type) {
        //System.err.println( "checkType: " + node + " // " + type );
        //new Exception().printStackTrace();;
        if ( type == null ) {
            return (Node<V>) node;
        }
        if ( type.isAssignableFrom(node.getType())) {
            return (Node<V>) node;
        }

        throw new RuntimeException( node + " (" + node.getType() + ") is not assignable to " + type);
    }

    public static <OUT_V, IN_V> OUT_V coerce(IN_V val, Class<OUT_V> outType) {
        if ( val.getClass() == outType ) {
            return (OUT_V) val;
        }
        throw new RuntimeException("Uncoercible: " + val + " to " + outType);
    }
}
