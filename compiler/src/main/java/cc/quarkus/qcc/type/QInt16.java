package cc.quarkus.qcc.type;

public class QInt16 extends QPrimitive<Short> implements QIntegral {
    public static QInt16 of(short value) {
        return new QInt16(value);
    }

    protected QInt16(Short value) {
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
        return this;
    }

    @Override
    public QInt32 asInt32() {
        return QInt32.of(value());
    }

    @Override
    public QInt64 asInt64() {
        return QInt64.of(value());
    }

    @Override
    public int compareTo(QNumeric o) {
        return Short.compare(value(), o.asInt16().value());
    }
}
