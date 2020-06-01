package cc.quarkus.qcc.constraint;

import static cc.quarkus.qcc.constraint.Constraint.Satisfaction.*;

class IntersectionConstraintImpl extends AbstractConstraint {

    public IntersectionConstraintImpl(Constraint c1, Constraint c2) {
        this.c1 = c1;
        this.c2 = c2;
    }


    @Override
    public Satisfaction isSatisfiedBy(SatisfactionContext context) {
        Satisfaction s1 = this.c1.isSatisfiedBy(context);
        if ( s1 != YES ) {
            return NO;
        }

        Satisfaction s2 = this.c2.isSatisfiedBy(context);
        if ( s2 != YES ) {
            return NO;
        }
        return YES;
    }

    @Override
    public Satisfaction satisfies(SatisfactionContext context, RelationConstraint other) {
        Satisfaction s1 = this.c1.satisfies(context, other);
        if ( s1 == NO ) {
            return NO;
        }

        Satisfaction s2 = this.c2.satisfies(context, other);
        if ( s2 == NO ) {
            return NO;
        }

        if ( s1 == NOT_APPLICABLE && s2 == NOT_APPLICABLE ) {
            return NOT_APPLICABLE;
        }

        return YES;
    }

    @Override
    public String toString() {
        return "IntersectionConstraintImpl{" +
                "c1=" + c1 +
                ", c2=" + c2 +
                '}';
    }

    private final Constraint c1;
    private final Constraint c2;
}
