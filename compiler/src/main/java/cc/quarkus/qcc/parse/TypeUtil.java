package cc.quarkus.qcc.parse;

import cc.quarkus.qcc.graph.ParseException;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.type.AnyType;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.ObjectType;
import cc.quarkus.qcc.graph.type.Type;

public class TypeUtil {
    @SuppressWarnings("unchecked")
    public static <T extends Type> Node<T> checkType(Node<?> val, Type type) {
        if ( type == AnyType.INSTANCE ) {
            return (Node<T>) val;
        }

        if ((type instanceof ObjectType && !((ObjectType) type).isAssignableFrom(val.getType())) || (type != val.getType())) {
            throw new ParseException(val + " is not assignable to " + type);
        }
        return (Node<T>) val;
    }
}
