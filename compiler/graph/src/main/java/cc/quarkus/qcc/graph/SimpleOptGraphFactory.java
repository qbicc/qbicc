package cc.quarkus.qcc.graph;

/**
 * A graph factory which performs simple optimizations opportunistically.
 */
public class SimpleOptGraphFactory extends DelegatingGraphFactory {
    public SimpleOptGraphFactory(final GraphFactory delegate) {
        super(delegate);
    }

    public Value if_(final Value condition, final Value trueValue, final Value falseValue) {
        if (condition instanceof ConstantValue) {
            return ((ConstantValue) condition).isTrue() ? trueValue : falseValue;
        } else {
            return getDelegate().if_(condition, trueValue, falseValue);
        }
    }

    public Value binaryOperation(final CommutativeBinaryValue.Kind kind, final Value v1, final Value v2) {
        switch (kind) {
            case ADD: {
                if (v1 instanceof ConstantValue) {
                    ConstantValue c1 = (ConstantValue) v1;
                    if (c1.isZero()) {
                        return v2;
                    }
                } else if (v2 instanceof ConstantValue) {
                    ConstantValue c2 = (ConstantValue) v2;
                    if (c2.isZero()) {
                        return v1;
                    }
                }
                break;
            }
            case MULTIPLY: {
                if (v1 instanceof ConstantValue) {
                    ConstantValue c1 = (ConstantValue) v1;
                    if (c1.isZero()) {
                        return c1;
                    } else if (c1.isOne()) {
                        return v2;
                    }
                } else if (v2 instanceof ConstantValue) {
                    ConstantValue c2 = (ConstantValue) v2;
                    if (c2.isZero()) {
                        return c2;
                    } else if (c2.isOne()) {
                        return v1;
                    }
                }
                break;
            }
            case AND: {
                if (v1 instanceof ConstantValue) {
                    ConstantValue c1 = (ConstantValue) v1;
                    if (c1.isZero()) {
                        return c1;
                    }
                } else if (v2 instanceof ConstantValue) {
                    ConstantValue c2 = (ConstantValue) v2;
                    if (c2.isZero()) {
                        return c2;
                    }
                }
                break;
            }
            case OR: {
                if (v1 instanceof ConstantValue) {
                    ConstantValue c1 = (ConstantValue) v1;
                    if (c1.isZero()) {
                        return v2;
                    }
                } else if (v2 instanceof ConstantValue) {
                    ConstantValue c2 = (ConstantValue) v2;
                    if (c2.isZero()) {
                        return v1;
                    }
                }
                break;
            }
            case XOR:
                break;
            // todo: constraints...
            case CMP_EQ:
                break;
            case CMP_NE:
                break;
        }
        return getDelegate().binaryOperation(kind, v1, v2);
    }

    public Value binaryOperation(final NonCommutativeBinaryValue.Kind kind, final Value v1, final Value v2) {
        switch (kind) {
            case UNSIGNED_SHR:
            case SHR:
            case SHL: {
                if (v2 instanceof ConstantValue) {
                    ConstantValue c2 = (ConstantValue) v2;
                    if (c2.isZero()) {
                        return v1;
                    }
                }
                break;
            }
            case SUB: {
                if (v1 == v2) {
                    return v1.getType().zero();
                } else if (v2 instanceof ConstantValue) {
                    ConstantValue c2 = (ConstantValue) v2;
                    if (c2.isZero()) {
                        return v1;
                    }
                }
                break;
            }
            case DIV: {
                if (v1 instanceof ConstantValue) {
                    ConstantValue c1 = (ConstantValue) v1;
                    if (c1.isZero()) {
                        return c1;
                    }
                } else if (v2 instanceof ConstantValue) {
                    ConstantValue c2 = (ConstantValue) v2;
                    if (c2.isOne()) {
                        return v1;
                    }
                }
                break;
            }
            case MOD: {
                if (v1 instanceof ConstantValue) {
                    ConstantValue c1 = (ConstantValue) v1;
                    if (c1.isZero()) {
                        return c1;
                    }
                } else if (v2 instanceof ConstantValue) {
                    ConstantValue c2 = (ConstantValue) v2;
                    if (c2.isOne()) {
                        return c2.getType().zero();
                    }
                }
                break;
            }
            // todo: constraints...
            case CMP_LT:
                break;
            case CMP_GT:
                break;
            case CMP_LE:
                break;
            case CMP_GE:
                break;
        }
        return getDelegate().binaryOperation(kind, v1, v2);
    }

    public Value instanceOf(final Value v, final ClassType type) {
        Type inType = v.getType();
        if (inType instanceof ReferenceType) {
            ReferenceType referenceType = (ReferenceType) inType;
            if (type.isSuperTypeOf(referenceType.getUpperBound())) {
                // always true
                return Value.TRUE;
            } else {
                ClassType lowerBound = referenceType.getLowerBound();
                if (lowerBound != null && lowerBound.isSuperTypeOf(type) && lowerBound != type) {
                    // always false
                    return Value.FALSE;
                }
            }
        }
        return getDelegate().instanceOf(v, type);
    }

    public Value reinterpretCast(final Value v, final Type type) {
        if (v.getType() == type) {
            return v;
        } else {
            return getDelegate().reinterpretCast(v, type);
        }
    }

    public Value castOperation(final WordCastValue.Kind kind, final Value value, final WordType toType) {
        if (value.getType() == toType) {
            return value;
        } else {
            return getDelegate().castOperation(kind, value, toType);
        }
    }

    public Terminator if_(final Node dependency, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
        if (condition instanceof ConstantValue) {
            return ((ConstantValue) condition).isTrue() ? goto_(dependency, trueTarget) : goto_(dependency, falseTarget);
        }
        return getDelegate().if_(dependency, condition, trueTarget, falseTarget);
    }
}
