package cc.quarkus.qcc.type;

public interface QNumeric extends QType, Comparable<QNumeric> {
    QChar asChar();
    QInt8 asInt8();
    QInt16 asInt16();
    QInt32 asInt32();
    QInt64 asInt64();
}
