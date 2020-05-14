package cc.quarkus.qcc.type;

public class QInt8 extends PrimitiveType<Byte> implements QIntegral {
    public static QInt8 of(byte value) {
        return new QInt8(value);
    }

    protected QInt8(Byte value) {
        super(value);
    }

    @Override
    public QChar asChar() {
        return QChar.of((char) value().shortValue());
    }

    @Override
    public QInt8 asInt8() {
        return this;
    }

    @Override
    public QInt16 asInt16() {
        return QInt16.of(value());
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
        return Byte.compare(value(), o.asInt8().value());
    }
}
