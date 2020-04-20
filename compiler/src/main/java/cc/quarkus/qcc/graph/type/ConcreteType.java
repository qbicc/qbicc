package cc.quarkus.qcc.graph.type;

public interface ConcreteType<T extends ConcreteType<T>> extends Type<T> {

    @Override
    default String label() {
        String n = getClass().getSimpleName();
        return n.substring(0, n.length() - "type".length()).toLowerCase();
    }
}
