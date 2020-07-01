package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

/**
 * A graph factory which performs simple optimizations opportunistically.
 */
public class SimpleOptGraphFactory implements GraphFactory {
    private final GraphFactory delegate;

    public SimpleOptGraphFactory(final GraphFactory delegate) {
        this.delegate = delegate;
    }

    public GraphFactory getDelegate() {
        return delegate;
    }

    public Value if_(final Value condition, final Value trueValue, final Value falseValue) {
        if (condition instanceof ConstantValue) {
            return ((ConstantValue) condition).isTrue() ? trueValue : falseValue;
        } else {
            return getDelegate().if_(condition, trueValue, falseValue);
        }
    }

    public PhiValue phi(final Type type, final BasicBlock basicBlock) {
        return getDelegate().phi(type, basicBlock);
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

    public Value unaryOperation(final UnaryValue.Kind kind, final Value v) {
        return getDelegate().unaryOperation(kind, v);
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

    public MemoryStateValue new_(final MemoryState input, final ClassType type) {
        return getDelegate().new_(input, type);
    }

    public MemoryStateValue newArray(final MemoryState input, final ArrayType type, final Value size) {
        return getDelegate().newArray(input, type, size);
    }

    public MemoryStateValue pointerLoad(final MemoryState input, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return getDelegate().pointerLoad(input, pointer, accessMode, atomicityMode);
    }

    public MemoryStateValue readInstanceField(final MemoryState input, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
        return getDelegate().readInstanceField(input, instance, owner, name, mode);
    }

    public MemoryStateValue readStaticField(final MemoryState input, final ClassType owner, final String name, final JavaAccessMode mode) {
        return getDelegate().readStaticField(input, owner, name, mode);
    }

    public MemoryStateValue readArrayValue(final MemoryState input, final Value array, final Value index, final JavaAccessMode mode) {
        return getDelegate().readArrayValue(input, array, index, mode);
    }

    public MemoryState pointerStore(final MemoryState input, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return getDelegate().pointerStore(input, pointer, value, accessMode, atomicityMode);
    }

    public MemoryState writeInstanceField(final MemoryState input, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeInstanceField(input, instance, owner, name, value, mode);
    }

    public MemoryState writeStaticField(final MemoryState input, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeStaticField(input, owner, name, value, mode);
    }

    public MemoryState writeArrayValue(final MemoryState input, final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeArrayValue(input, array, index, value, mode);
    }

    public MemoryState fence(final MemoryState input, final MemoryAtomicityMode fenceType) {
        return getDelegate().fence(input, fenceType);
    }

    public MemoryState invokeMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return getDelegate().invokeMethod(input, owner, method, arguments);
    }

    public MemoryState invokeInstanceMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return getDelegate().invokeInstanceMethod(input, instance, kind, owner, method, arguments);
    }

    public MemoryStateValue invokeValueMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return getDelegate().invokeValueMethod(input, owner, method, arguments);
    }

    public MemoryStateValue invokeInstanceValueMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return getDelegate().invokeInstanceValueMethod(input, instance, kind, owner, method, arguments);
    }

    public Terminator goto_(final MemoryState input, final NodeHandle targetHandle) {
        return getDelegate().goto_(input, targetHandle);
    }

    public Terminator if_(final MemoryState input, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
        if (condition instanceof ConstantValue) {
            return ((ConstantValue) condition).isTrue() ? goto_(input, trueTarget) : goto_(input, falseTarget);
        }
        return getDelegate().if_(input, condition, trueTarget, falseTarget);
    }

    public Terminator return_(final MemoryState input) {
        return getDelegate().return_(input);
    }

    public Terminator return_(final MemoryState input, final Value value) {
        return getDelegate().return_(input, value);
    }

    public Terminator throw_(final MemoryState input, final Value value) {
        return getDelegate().throw_(input, value);
    }

    public Terminator switch_(final MemoryState input, final Value value, final int[] checkValues, final NodeHandle[] targets, final NodeHandle defaultTarget) {
        return getDelegate().switch_(input, value, checkValues, targets, defaultTarget);
    }

    public Terminator tryInvokeMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return getDelegate().tryInvokeMethod(input, owner, method, arguments, returnTarget, catchTarget);
    }

    public Terminator tryInvokeInstanceMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return getDelegate().tryInvokeInstanceMethod(input, instance, kind, owner, method, arguments, returnTarget, catchTarget);
    }

    public TerminatorValue tryInvokeValueMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return getDelegate().tryInvokeValueMethod(input, owner, method, arguments, returnTarget, catchTarget);
    }

    public TerminatorValue tryInvokeInstanceValueMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return getDelegate().tryInvokeInstanceValueMethod(input, instance, kind, owner, method, arguments, returnTarget, catchTarget);
    }

    public Terminator tryThrow(final MemoryState input, final Value value, final NodeHandle catchTarget) {
        return getDelegate().tryThrow(input, value, catchTarget);
    }

    public BasicBlock block(final Terminator term) {
        return getDelegate().block(term);
    }
}
