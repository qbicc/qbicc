package org.qbicc.plugin.constants;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.interpreter.InterpreterHaltedException;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NullableType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;
import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

/**
 * A basic block builder which substitutes reads from trivially constant static fields,
 * CNative#constants and invocations of @Fold annotated methods with their constant values.
 *
 * We leave the wholesale constant folding of loads from final static fields to
 * the InitializedStaticFieldBasicBlockBuilder, which runs during the ANALYZE
 * phase after all build time interpretation has been completed and the values
 * of all final fields have definitely been computed.
 */
public class ConstantBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ConstantBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value load(ValueHandle handle, ReadAccessMode accessMode) {
        if (handle instanceof StaticField) {
            final FieldElement fieldElement = ((StaticField) handle).getVariableElement();
            Value constantValue = Constants.get(ctxt).getConstantValue(fieldElement);
            if (constantValue != null) {
                return constantValue;
            }
            if (fieldElement.isReallyFinal()) {
                final Literal initialValue = fieldElement.getInitialValue();
                if (initialValue != null) {
                    return initialValue;
                }
            }
        }
        return getDelegate().load(handle, accessMode);
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        if (target.isFold()) try {
            return fold(target, arguments);
        } catch (Thrown t) {
            throw new BlockEarlyTermination(throw_(ctxt.getLiteralFactory().literalOf(t.getThrowable())));
        }
        return super.call(target, arguments);
    }

    @Override
    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        return target.isFold() ? fold(target, arguments) : super.callNoSideEffects(target, arguments);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        if (target.isFold()) try {
            return return_(fold(target, arguments));
        } catch (Thrown t) {
            return throw_(ctxt.getLiteralFactory().literalOf(t.getThrowable()));
        }
        return super.tailCall(target, arguments);
    }

    @Override
    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        if (target.isFold()) try {
            return return_(fold(target, arguments));
        } catch (Thrown t) {
            storeException(t);
            return goto_(catchLabel);
        }
        return super.tailInvoke(target, arguments, catchLabel);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        if (target.isFold()) try {
            Value result = fold(target, arguments);
            goto_(resumeLabel);
            return result;
        } catch (Thrown t) {
            storeException(t);
            goto_(catchLabel);
        }
        return super.invoke(target, arguments, catchLabel, resumeLabel);
    }

    private static final Object[] NO_ARGS = new Object[0];

    private Value fold(final ValueHandle target, final List<Value> arguments) throws Thrown {
        // we fold per call site, so caching does not really make sense
        if (target instanceof StaticMethodElementHandle sh && target.getValueType() instanceof FunctionType ft) {
            int size = arguments.size();
            Object[] args = size == 0 ? NO_ARGS : new Object[size];
            for (int i = 0; i < size; i ++) {
                args[i] = mapValue(arguments.get(i));
            }
            MethodElement method = sh.getExecutable();
            VmThread thread = Vm.requireCurrentThread();
            Vm vm = thread.getVM();
            // may throw!
            Object resultObj;
            try {
                resultObj = vm.invokeExact(method, null, List.of(args));
            } catch (InterpreterHaltedException e) {
                throw new BlockEarlyTermination(unreachable());
            }
            if (resultObj instanceof Boolean result) {
                return ctxt.getLiteralFactory().literalOf(result.booleanValue());
            } else if (resultObj instanceof Byte result) {
                return ctxt.getLiteralFactory().literalOf(result.byteValue());
            } else if (resultObj instanceof Short result) {
                return ctxt.getLiteralFactory().literalOf(result.shortValue());
            } else if (resultObj instanceof Integer result) {
                return ctxt.getLiteralFactory().literalOf(result.intValue());
            } else if (resultObj instanceof Long result) {
                return ctxt.getLiteralFactory().literalOf(result.longValue());
            } else if (resultObj instanceof Character result) {
                return ctxt.getLiteralFactory().literalOf(result.charValue());
            } else if (resultObj instanceof Float result) {
                return ctxt.getLiteralFactory().literalOf(result.floatValue());
            } else if (resultObj instanceof Double result) {
                return ctxt.getLiteralFactory().literalOf(result.doubleValue());
            } else if (resultObj instanceof VmObject result) {
                return ctxt.getLiteralFactory().literalOf(result);
            } else if (resultObj instanceof ValueType result) {
                return ctxt.getLiteralFactory().literalOfType(result);
            } else if (resultObj == null && ft.getReturnType() instanceof NullableType nt) {
                return ctxt.getLiteralFactory().nullLiteralOfType(nt);
            } else {
                ctxt.error(getLocation(), "Unmappable constant-folded return value %s", resultObj);
                throw new BlockEarlyTermination(unreachable());
            }
        } else {
            ctxt.error(getLocation(), "Only static methods may be folded");
            throw new BlockEarlyTermination(unreachable());
        }
    }

    private Object mapValue(final Value value) {
        if (value instanceof IntegerLiteral lit) {
            IntegerType type = lit.getType();
            boolean signed = type instanceof SignedIntegerType;
            if (type.getMinBits() == 8) {
                return Byte.valueOf(lit.byteValue());
            } else if (type.getMinBits() == 16) {
                if (signed) {
                    return Short.valueOf(lit.shortValue());
                } else {
                    return Character.valueOf(lit.charValue());
                }
            } else if (type.getMinBits() == 32) {
                return Integer.valueOf(lit.intValue());
            } else if (type.getMinBits() == 64) {
                return Long.valueOf(lit.longValue());
            }
        } else if (value instanceof BooleanLiteral lit) {
            return Boolean.valueOf(lit.booleanValue());
        } else if (value instanceof FloatLiteral lit) {
            FloatType type = lit.getType();
            if (type.getMinBits() == 32) {
                return Float.valueOf(lit.floatValue());
            } else if (type.getMinBits() == 64) {
                return Double.valueOf(lit.doubleValue());
            }
        } else if (value instanceof ObjectLiteral lit) {
            return lit.getValue();
        } else if (value instanceof StringLiteral lit) {
            return Vm.requireCurrent().intern(lit.getValue());
        } else if (value instanceof TypeLiteral lit) {
            return lit.getValue();
        }
        ctxt.error(getLocation(), "Unmappable parameter value %s for constant folding (must be a constant/literal value)", value);
        throw new BlockEarlyTermination(unreachable());
    }

    private Node storeException(final Thrown t) {
        // todo: rework when landing pads are done
        return store(instanceFieldOf(referenceHandle(load(currentThread(), SingleUnshared)), ctxt.getExceptionField()), ctxt.getLiteralFactory().literalOf(t.getThrowable()), SinglePlain);
    }
}
