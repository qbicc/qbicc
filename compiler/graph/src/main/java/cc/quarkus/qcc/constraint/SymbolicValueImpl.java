package cc.quarkus.qcc.constraint;

import cc.quarkus.qcc.graph.ValueVisitor;

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

    public int getSourceLine() {
        return 0;
    }

    public void setSourceLine(final int sourceLine) {

    }

    public int getBytecodeIndex() {
        return 0;
    }

    public void setBytecodeIndex(final int bytecodeIndex) {

    }
}
