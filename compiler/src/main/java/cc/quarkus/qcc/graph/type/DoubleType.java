package cc.quarkus.qcc.graph.type;

public class DoubleType implements ConcreteType<Double> {
    public static final DoubleType INSTANCE = new DoubleType();
    private DoubleType() {

    }
}
