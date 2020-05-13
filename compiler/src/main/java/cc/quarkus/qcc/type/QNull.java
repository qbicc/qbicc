package cc.quarkus.qcc.type;

public class QNull implements QType {
    public static final QNull NULL = new QNull();

    private QNull() {

    }

    @Override
    public String toString() {
        return "<<null>>";
    }
}
