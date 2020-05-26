package cc.quarkus.qcc.type;

public class QVoid {

    public static final QVoid VOID = new QVoid();

    private QVoid() {

    }

    @Override
    public String toString() {
        return "<<void>>";
    }
}
