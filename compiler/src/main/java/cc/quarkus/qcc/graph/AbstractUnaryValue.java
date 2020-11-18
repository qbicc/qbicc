package cc.quarkus.qcc.graph;

abstract class AbstractUnaryValue extends AbstractValue implements UnaryValue {
    final Value input;

    AbstractUnaryValue(final int line, final int bci, final Value input) {
        super(line, bci);
        this.input = input;
    }

    public Value getInput() {
        return input;
    }
}
