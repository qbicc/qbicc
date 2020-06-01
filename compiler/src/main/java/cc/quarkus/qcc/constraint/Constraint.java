package cc.quarkus.qcc.constraint;

public interface Constraint {

    static Constraint lessThan(Value v) {
        return new RelationConstraintImpl(RelationConstraintImpl.Op.LT, v);
    }

    static Constraint lessThanOrEqualTo(Value v) {
        return new RelationConstraintImpl(RelationConstraintImpl.Op.LE, v);
    }

    static Constraint greaterThan(Value v) {
        return new RelationConstraintImpl(RelationConstraintImpl.Op.GT, v);
    }

    static Constraint greaterThanOrEqualTo(Value v) {
        return new RelationConstraintImpl(RelationConstraintImpl.Op.GE, v);
    }

    static Constraint equalTo(Value v) {
        return new RelationConstraintImpl(RelationConstraintImpl.Op.EQ, v);
    }

    static Constraint notEqualTo(Value v) {
        return new RelationConstraintImpl(RelationConstraintImpl.Op.NE, v);
    }

    Constraint union(Constraint other);
    Constraint intersect(Constraint other);

    enum Satisfaction {
        YES,
        NO,
        NOT_APPLICABLE,
    }

    /** Determine if this constraint tree or subtree is satisfied by the given context.
     *
     * @param context The target and other book-keeping context.
     * @return YES if satisfied, else NO.
     */
    Satisfaction isSatisfiedBy(SatisfactionContext context);

    /** Determine if this constraint tree or subtree is satisfied by the given Value.
     *
     * @param value The target value to evaluate.
     * @return YES if satisfied, else NO.
     */
    default Satisfaction isSatisfiedBy(Value value) {
        return isSatisfiedBy( new SatisfactionContextImpl( value ) );
    }

}

