package cc.quarkus.qcc.type;

public class QFloat extends QPrimitive<Float> {

    public static QFloat of(float value) {
        return new QFloat(value);
    }

    protected QFloat(Float value) {
        super(value);
    }
}
