package cc.quarkus.qcc.constraint;

abstract class AbstractConstraint implements Constraint {

    @Override
    public Constraint union(Constraint other) {
        return new UnionConstraintImpl(this, (AbstractConstraint) other);
    }

    @Override
    public Constraint intersect(Constraint other) {
        return new IntersectionConstraintImpl(this, (AbstractConstraint) other);
    }

    /** Determine if this constraint satisfies another constraint.
     *
     * This constraint may be stronger than the other context, in which case,
     * the answer will be YES. If this constraint is irrelevant to the other
     * constraint, the answer will be NOT_APPLICABLE. It will only be a definite
     * NO in the case of recursive unsolvable constraint relations.
     *
     * You probably want to use {@link #isSatisfiedBy}. This method should
     * probably be moved into the Impl's and change some casting/signatures
     * within the others.
     *
     * @param context The target and other book-keeping context.
     * @param other The other constraint to test.
     * @return YES if this constraint satisfies, the other, else NOT_APPLICABLE,
     * or maybe NO.
     */
    abstract Satisfaction satisfies(SatisfactionContext context, RelationConstraintImpl other);
}
