package cc.quarkus.qcc.type;

public class QBoolean extends PrimitiveType<Boolean> {

    public static final QBoolean TRUE = new QBoolean(true);
    public static final QBoolean FALSE = new QBoolean(false);

    public static QBoolean of(boolean value) {
        if ( value ) {
            return TRUE;
        }
        return FALSE;
    }

    private QBoolean(Boolean value) {
        super(value);
    }
}
