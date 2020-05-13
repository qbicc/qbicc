package cc.quarkus.qcc.type;

public class QInt64 extends PrimitiveType<Long> implements QIntegral {

    public static QInt64 of(long value) {
        return new QInt64(value);
    }

    public QInt64(Long value) {
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
        return QInt32.of(value().intValue());
    }

    @Override
    public QInt64 asInt64() {
        return this;
    }

    @Override
    public int compareTo(QNumeric o) {
        return Long.compare(value(), o.asInt64().value());
    }
}
