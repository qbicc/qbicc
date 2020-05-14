package cc.quarkus.qcc.type;

public class QChar extends QPrimitive<Character> implements QIntegral {

    public static QChar of(char value) {
        return new QChar(value);
    }

    protected QChar(Character value) {
        super(value);
    }

    @Override
    public QChar asChar() {
        return this;
    }

    @Override
    public QInt8 asInt8() {
        return QInt8.of((byte) value().charValue());
    }

    @Override
    public QInt16 asInt16() {
        return QInt16.of((short) value().charValue());
    }

    @Override
    public QInt32 asInt32() {
        return QInt32.of((int) value());
    }

    @Override
    public QInt64 asInt64() {
        return QInt64.of((long) value());
    }

    @Override
    public int compareTo(QNumeric o) {
        return Character.compare(value(), o.asChar().value());
    }
}
