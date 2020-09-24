package cc.quarkus.qcc.graph;

/**
 * A graph factory which performs simple optimizations opportunistically.
 */
public class SimpleOptGraphFactory extends DelegatingGraphFactory {
    public SimpleOptGraphFactory(final GraphFactory delegate) {
        super(delegate);
    }

    public Value lengthOfArray(final Context ctxt, final Value array) {
        if (array instanceof NewArrayValue) {
            return ((NewArrayValue) array).getSize();
        } else {
            return getDelegate().lengthOfArray(ctxt, array);
        }
    }

    public Value if_(final Context ctxt, final Value condition, final Value trueValue, final Value falseValue) {
        if (condition instanceof ConstantValue) {
            return ((ConstantValue) condition).isTrue() ? trueValue : falseValue;
        } else if (trueValue.equals(falseValue)) {
            return trueValue;
        } else {
            return getDelegate().if_(ctxt, condition, trueValue, falseValue);
        }
    }

    public Value instanceOf(final Context ctxt, final Value v, final ClassType type) {
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
        return getDelegate().instanceOf(ctxt, v, type);
    }

    public BasicBlock if_(final Context ctxt, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
        if (condition instanceof ConstantValue) {
            NodeHandle h = new NodeHandle();
            if (((ConstantValue) condition).isTrue()) {
                BasicBlock node = goto_(ctxt, trueTarget);
                ctxt.setCurrentBlock(h);
                return node;
            } else {
                BasicBlock node = goto_(ctxt, h);
                ctxt.setCurrentBlock(h);
                return node;
            }
        } else {
            return getDelegate().if_(ctxt, condition, trueTarget, falseTarget);
        }
    }
}
