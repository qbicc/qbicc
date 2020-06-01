package cc.quarkus.qcc.constraint;

import static cc.quarkus.qcc.constraint.Constraint.Satisfaction.*;
import static cc.quarkus.qcc.constraint.RelationConstraint.Op.*;

class RelationConstraintImpl extends AbstractConstraint implements RelationConstraint {

    RelationConstraintImpl(Op op, Value value) {
        this.op = op;
        this.value = value;
    }

    @Override
    public Op getOp() {
        return this.op;
    }

    @Override
    public Value getValue() {
        return this.value;
    }

    @Override
    public Satisfaction isSatisfiedBy(SatisfactionContext context) {
        Value target = context.getTarget();
        Constraint other = target.getConstraint();

        if (other == null) {
            return Satisfaction.NO;
        }

        if (other instanceof RelationConstraint) {
            Op otherOp = ((RelationConstraint) other).getOp();
            Value otherValue = ((RelationConstraint) other).getValue();

            boolean sameValue = this.value.equals(otherValue);

            if (sameValue) {
                switch (this.op) {
                    case GT:
                        if (otherOp == GT) {
                            return YES;
                        }
                        break;
                    case GE:
                        if (otherOp == GT || otherOp == GE || otherOp == EQ) {
                            return YES;
                        }
                        break;
                    case LT:
                        if (otherOp == LT) {
                            return YES;
                        }
                        break;
                    case LE:
                        if (otherOp == LT || otherOp == LE || otherOp == EQ) {
                            return YES;
                        }
                        break;
                    case EQ:
                        if (otherOp == EQ) {
                            return YES;
                        }
                        break;
                    case NE:
                        if (otherOp == NE) {
                            return YES;
                        }
                        break;
                }
                return NO;
            } else {
                // different value, but does that value suffice?
                return isSatisfiedBy(otherValue);
            }
        }
        return ((AbstractConstraint)other).satisfies(context, this);
    }

    @Override
    public Satisfaction satisfies(SatisfactionContext context, RelationConstraintImpl other) {
        if ( context.isSeen(this, other)) {
            return NO;
        }
        Op otherOp = other.getOp();
        Value otherValue = other.getValue();

        SymbolicValue symbol = null;

        if ( otherValue instanceof SymbolicValue ) {
            symbol = (SymbolicValue) otherValue;
            Value boundValue = context.getBinding((SymbolicValue) otherValue);
            if ( boundValue != null ) {
                otherValue = boundValue;
            } else {
                otherValue = this.value;
            }
        }

        boolean sameValue = this.value.equals(otherValue);

        if ( ! sameValue ) {
            Constraint next = this.value.getConstraint();
            if ( next == null || next == this) {
                return NOT_APPLICABLE;
            }
            context.seen(this, other);
            return ((AbstractConstraint)next).satisfies(context, other);
        }

        switch (this.op) {
            case GT:
                if (otherOp == GT) {
                    maybeBind(context, symbol);
                    context.seen(this, other);
                    return YES;
                }
                break;
            case GE:
                if (otherOp == GE || otherOp == EQ) {
                    maybeBind(context, symbol);
                    context.seen(this, other);
                    return YES;
                }
                break;
            case LT:
                if (otherOp == LT) {
                    maybeBind(context, symbol);
                    context.seen(this, other);
                    return YES;
                }
                break;
            case LE:
                if (otherOp == LE || otherOp == EQ) {
                    maybeBind(context, symbol);
                    context.seen(this, other);
                    return YES;
                }
                break;
            case EQ:
                if (otherOp == EQ) {
                    maybeBind(context, symbol);
                    context.seen(this, other);
                    return YES;
                }
                break;
            case NE:
                if (otherOp == NE) {
                    maybeBind(context, symbol);
                    context.seen(this, other);
                    return YES;
                }
                break;
        }
        return NOT_APPLICABLE;
    }

    protected void maybeBind(SatisfactionContext ctx, SymbolicValue symbol) {
        if ( symbol != null ) {
            ctx.bind(symbol, this.value);
        }
    }

    @Override
    public String toString() {
        return "RelationConstraintImpl{" +
                "op=" + op +
                ", value=" + value +
                '}';
    }

    private final Op op;

    private final Value value;
}
