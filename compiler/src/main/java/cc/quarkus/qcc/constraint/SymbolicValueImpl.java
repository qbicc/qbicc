package cc.quarkus.qcc.constraint;

public class SymbolicValueImpl implements SymbolicValue {

    SymbolicValueImpl(String label) {
        this.label = label;
    }

    @Override
    public Constraint getConstraint() {
        return null;
    }

    @Override
    public String toString() {
        return this.label;
    }

    private final String label;
}
