package cc.quarkus.qcc.graph;

abstract class AbstractUnaryValue extends AbstractValue implements UnaryValue {
    final Value input;

    AbstractUnaryValue(final Value input) {
        this.input = input;
    }

    public Value getInput() {
        return input;
    }
}
