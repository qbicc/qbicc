package cc.quarkus.qcc.type.descriptor;

public class PrimitiveTypeDescriptor<T> implements TypeDescriptor<T> {

    PrimitiveTypeDescriptor(Class<T> javaType, String label) {
        this.type = javaType;
        this.label = label;
    }

    @Override
    public Class<T> type() {
        return this.type;
    }

    @Override
    public String label() {
        return this.label;
    }

    @Override
    public String toString() {
        return label();
    }

    private final Class<T> type;
    private final String label;
}
