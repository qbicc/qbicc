package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.ParseException;

public interface Type {
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
}
