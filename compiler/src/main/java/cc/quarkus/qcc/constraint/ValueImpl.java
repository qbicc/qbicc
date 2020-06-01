package cc.quarkus.qcc.constraint;

public class ValueImpl implements Value {

    public ValueImpl(String label) {
        this.label = label;
    }


    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
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
}
