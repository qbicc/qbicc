package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.node.AbstractNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.WidenNode;

public class IntType implements NumericType<IntType> {

    public static final IntType INSTANCE = new IntType();

    IntType() {

    }

    @Override
    public IntValue newInstance(Object... args) {
        checkNewInstanceArguments(args, Integer.class);
        return new IntValue((int)args[0]);
    }

    @Override
    public <V extends Value<IntType, V>> Node<IntType, V> coerce(Node<?, ?> node) {
        if ( node.getType() instanceof ByteType ) {
            return new WidenNode<>(node.getControl(), node, IntType.INSTANCE);
        }
        return null;
    }
}
