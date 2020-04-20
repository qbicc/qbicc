package cc.quarkus.qcc.graph.type;

public interface NumericValue<T extends NumericType<T>, V extends Value<T,V>> extends Value<T,V> {

    default short shortValue() {
        return (short) intValue();
    }

    default int intValue() {
        return (int) longValue();
    }

    default long longValue() {
        throw new RuntimeException("not implemented");
    }

    default float floatValue() {
        return (float) doubleValue();
    }

    default double doubleValue() {
        throw new RuntimeException("not implemented");
    }

    NumericValue<T,V> add(NumericValue<T,V> other);
    NumericValue<T,V> sub(NumericValue<T,V> other);
    NumericValue<T,V> div(NumericValue<T,V> other);
    NumericValue<T,V> mul(NumericValue<T,V> other);

}
