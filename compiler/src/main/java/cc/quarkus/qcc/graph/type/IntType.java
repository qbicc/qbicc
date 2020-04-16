package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.WidenNode;

public class IntType implements ConcreteType<IntValue> {

    public static final IntType INSTANCE = new IntType();

    IntType() {

    }


    @Override
    public IntValue newInstance(Object... args) {
        checkNewInstanceArguments(args, Integer.class);
        return new IntValue((int)args[0]);
    }

    @Override
    public <T extends Type<?>> Node<T> coerce(Node<?> node) {
        if ( node.getType() instanceof ByteType ) {
            return new WidenNode<T>(node.getControlPredecessors().iterator().next(), node, (T) ByteType.INSTANCE);
        }
        return null;
    }
}
