package cc.quarkus.qcc.type.descriptor;

import cc.quarkus.qcc.type.QType;

public class PrimitiveTypeDescriptor<QTYPE extends QType> implements TypeDescriptor<QTYPE> {

    PrimitiveTypeDescriptor(Class<QTYPE> javaType, String label) {
        this.type = javaType;
        this.label = label;
    }

    @Override
    public Class<QTYPE> type() {
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

    private final Class<QTYPE> type;
    private final String label;
}
