package cc.quarkus.qcc.constraint;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.definition.element.Element;

public class SymbolicValueImpl implements SymbolicValue {

    SymbolicValueImpl(String label) {
        this.label = label;
    }

    @Override
    public Constraint getConstraint() {
        return null;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return null;
    }

    @Override
    public String toString() {
        return this.label;
    }

    private final String label;

    public Element getElement() {
        return null;
    }

    public int getSourceLine() {
        return 0;
    }

    public int getBytecodeIndex() {
        return 0;
    }
}
