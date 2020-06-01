package cc.quarkus.qcc.constraint;

abstract class AbstractConstraint implements Constraint {

    @Override
    public Constraint union(Constraint other) {
        return new UnionConstraintImpl(this, other);
    }

    @Override
    public Constraint intersect(Constraint other) {
        return new IntersectionConstraintImpl(this, other);
    }
}
