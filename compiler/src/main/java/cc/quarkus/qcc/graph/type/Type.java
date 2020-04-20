package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.ParseException;
import cc.quarkus.qcc.graph.node.AbstractNode;
import cc.quarkus.qcc.graph.node.Node;

public interface Type<T extends Type<T>> {

    default Value<T,?> newInstance(Object...args) {
        throw new IllegalArgumentException( label() + " is not instantiable");
    }

    default void checkNewInstanceArguments(Object[] args, Class<?>...types) {
        if ( args.length > types.length ) {
            throw new IllegalArgumentException( "newInstance requires " + types.length + " arguments, received " + args.length);
        }

        for ( int i = 0 ; i < args.length ; ++i ) {
            if ( ! types[i].isAssignableFrom(args[i].getClass()))  {
                throw new IllegalArgumentException( "argument " + i + " should be of type " + types[i] + " but was " + args[0] );
            }
        }
    }

    default String label() {
        return getClass().getSimpleName();
    }

    default Type join(Type other) {
        if ( this.getClass().isAssignableFrom( other.getClass() ) ) {
            return this;
        } else if ( other.getClass().isAssignableFrom( this.getClass() ) ) {
            return other;
        }

        throw new ParseException("can not join " + this + " and " + other);
    }

    @SuppressWarnings("unchecked")
    default <V extends Value<T,V>> Node<T,V> coerce(Node<?,?> node) {
        if ( node.getType() == this ) {
            return (Node<T,V>) node;
        }
        return null;
    }

    default Value<?,?> coerce(Value<?,?> val) {
        throw new UnsupportedOperationException( this.getClass().getName() + " cannot coerce " + val);
    }
}
