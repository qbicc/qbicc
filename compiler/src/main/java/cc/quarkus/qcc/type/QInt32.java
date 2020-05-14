package cc.quarkus.qcc.type;

public class QInt32 extends QPrimitive<Integer> implements QIntegral {
    public static QInt32 of(int val) {
        return new QInt32(val);
    }

    protected QInt32(Integer value) {
        super(value);
    }

    @Override
    public QChar asChar() {
        return QChar.of((char) value().shortValue());
    }

    @Override
    public QInt8 asInt8() {
        return QInt8.of(value().byteValue());
    }

    @Override
    public QInt16 asInt16() {
        return QInt16.of(value().shortValue());
    }

    @Override
    public QInt32 asInt32() {
        return this;
    }

    @Override
    public QInt64 asInt64() {
        return QInt64.of(value());
    }

    @Override
    public int compareTo(QNumeric o) {
        return Integer.compare(value(), o.asInt32().value());
    }
}
