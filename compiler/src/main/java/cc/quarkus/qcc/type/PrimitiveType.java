package cc.quarkus.qcc.type;

public class PrimitiveType<JAVA_TYPE> implements QType {

    protected PrimitiveType(JAVA_TYPE value) {
        this.value = value;
    }

    public JAVA_TYPE value() {
        return this.value;
    }

    private final JAVA_TYPE value;
}
