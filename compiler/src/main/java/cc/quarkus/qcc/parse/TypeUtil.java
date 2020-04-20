package cc.quarkus.qcc.parse;

import cc.quarkus.qcc.graph.ParseException;
import cc.quarkus.qcc.graph.node.AbstractNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.type.AnyType;
import cc.quarkus.qcc.graph.type.ObjectType;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;

public class TypeUtil {
    @SuppressWarnings("unchecked")
    public static Node<?,?> checkType(Node<?,?> val, Type<?> type) {
        if ( type == AnyType.INSTANCE ) {
            return  val;
        }
        if ( type instanceof ObjectType ) {
            if ( ((ObjectType) type).isAssignableFrom(val.getType())) {
                return val;
            }
        }

        if ((type instanceof ObjectType && !((ObjectType) type).isAssignableFrom(val.getType())) || (type != val.getType())) {
            Node<T,?> coerced = val.tryCoerce(type);
            if (coerced != null ) {
                return coerced;
            }
            throw new ParseException(val + " is not assignable to " + type);
        }
        return val;
    }
}
