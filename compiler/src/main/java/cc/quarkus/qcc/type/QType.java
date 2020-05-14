package cc.quarkus.qcc.type;

public interface QType {

    static QType of(Object value) {
        if ( value == null ) {
            return ObjectReference.NULL;
        }
        if ( value instanceof Character ) {
            return QChar.of((Character) value);
        }
        if ( value instanceof Byte) {
            return QInt8.of((Byte) value);
        }
        if ( value instanceof Short ) {
            return QInt16.of((Short) value);
        }
        if ( value instanceof Integer ) {
            return QInt32.of((Integer) value);
        }
        if ( value instanceof Long ) {
            return QInt64.of((Long) value);
        }
        if ( value instanceof String ) {
            return HostBackedObjectReference.newString((String) value);
        }
        throw new RuntimeException( "Unable to make qtype of " + value);
    }

    default boolean isNull() {
        return false;
    }


}
