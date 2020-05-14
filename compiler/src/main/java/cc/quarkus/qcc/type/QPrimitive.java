package cc.quarkus.qcc.type;

public class QPrimitive<JAVA_TYPE> implements QType {

    protected QPrimitive(JAVA_TYPE value) {
        this.value = value;
    }

    public JAVA_TYPE value() {
        return this.value;
    }

    @Override
    public final boolean isNull() {
        return false;
    }

    private final JAVA_TYPE value;
}
