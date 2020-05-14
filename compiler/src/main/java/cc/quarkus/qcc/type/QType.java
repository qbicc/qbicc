package cc.quarkus.qcc.type;

import cc.quarkus.qcc.graph.type.IntrinsicObjectReference;

public interface QType {
    static QType of(Object value) {
        if ( value == null ) {
            return QNull.NULL;
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
            return IntrinsicObjectReference.newString((String) value);
        }
        throw new RuntimeException( "Unable to make qtype of " + value);
    }
}
