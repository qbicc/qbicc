package cc.quarkus.qcc.type;

public class QDouble extends QPrimitive<Double> {
    public static QDouble of(double value) {
        return new QDouble(value);
    }

    protected QDouble(Double value) {
        super(value);
    }

}

