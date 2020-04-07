package cc.quarkus.qcc.graph.type;

public class VoidType implements ConcreteType<Void> {

    public static final VoidType INSTANCE = new VoidType();

    private VoidType() {

    }

}
