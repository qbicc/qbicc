package cc.quarkus.qcc.constraint;

import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.ValueType;

public class ValueImpl implements Value {

    public ValueImpl(String label) {
        this.label = label;
    }


    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return null;
    }

    @Override
    public Constraint getConstraint() {
        return this.constraint;
    }

    @Override
    public String toString() {
        return "ValueImpl{" +
                "label=" + label +
                //", constraint=" + constraint +
                '}';
    }

    private final String label;

    private Constraint constraint;

    public ValueType getType() {
        return null;
    }

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
