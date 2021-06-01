package org.qbicc.plugin.conversion;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.type.BooleanType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PointerType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VariadicType;

/**
 * Automatically truncate parameters to method calls.  This is the complement to the {@code promote} method in the
 * method parser, which is applied to the return types of methods to inflate them to the 32 bits specified by the JVMS;
 * however, demotion cannot happen until we know the target type of each argument, which does not happen until after
 * type resolution.
 */
public class MethodCallFixupBasicBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<List<Value>, List<Value>> {
    private final CompilationContext ctxt;

    public MethodCallFixupBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        return super.call(target, target.accept(this, arguments));
    }

    @Override
    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        return super.callNoSideEffects(target, target.accept(this, arguments));
    }

    @Override
    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        return super.callNoReturn(target, target.accept(this, arguments));
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        return super.invokeNoReturn(target, target.accept(this, arguments), catchLabel);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        return super.tailCall(target, target.accept(this, arguments));
    }

    @Override
    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        return super.tailInvoke(target, target.accept(this, arguments), catchLabel);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        return super.invoke(target, target.accept(this, arguments), catchLabel, resumeLabel);
    }

    private List<Value> fixArguments(final ValueType targetType, final List<Value> arguments) {
        if (targetType instanceof PointerType) {
            return fixArguments(((PointerType) targetType).getPointeeType(), arguments);
        } else if (targetType instanceof FunctionType) {
            return fixArguments((FunctionType) targetType, arguments);
        } else {
            ctxt.error(getLocation(), "Unable to determine type of function call target (%s)", targetType);
            return arguments;
        }
    }

    private List<Value> fixArguments(final FunctionType targetType, final List<Value> arguments) {
        int tpc = targetType.getParameterCount();
        boolean variadic = tpc > 0 && targetType.getParameterType(tpc - 1) instanceof VariadicType;
        int len = variadic ? Math.min(arguments.size(), tpc - 1) : arguments.size();
        for (int i = 0; i < len; i ++) {
            Value orig = arguments.get(i);
            Value demoted = demote(orig, targetType.getParameterType(i));
            if (orig != demoted) {
                Value[] values = new Value[len];
                for (int j = 0; j < i; j ++) {
                    values[j] = arguments.get(j);
                }
                values[i] = demoted;
                for (int j = i + 1; j < len; j ++) {
                    values[j] = demote(arguments.get(j), targetType.getParameterType(j));
                }
                return List.of(values);
            }
        }
        return arguments;
    }

    private Value demote(Value orig, ValueType toType) {
        ValueType type = orig.getType();
        if (type == toType || ! (type instanceof IntegerType)) {
            return orig;
        }
        if (type instanceof SignedIntegerType) {
            SignedIntegerType inputType = (SignedIntegerType) type;
            if (toType instanceof SignedIntegerType) {
                SignedIntegerType outputType = (SignedIntegerType) toType;
                if (outputType.getMinBits() < inputType.getMinBits()) {
                    return truncate(orig, outputType);
                }
            } else if (toType instanceof UnsignedIntegerType) {
                UnsignedIntegerType outputType = (UnsignedIntegerType) toType;
                if (outputType.getMinBits() < inputType.getMinBits()) {
                    return truncate(bitCast(orig, ((SignedIntegerType) type).asUnsigned()), outputType);
                }
            } else if (toType instanceof BooleanType) {
                return truncate(orig, (BooleanType) toType);
            }
        } else if (type instanceof UnsignedIntegerType) {
            UnsignedIntegerType inputType = (UnsignedIntegerType) type;
            if (toType instanceof UnsignedIntegerType) {
                UnsignedIntegerType outputType = (UnsignedIntegerType) toType;
                if (outputType.getMinBits() < inputType.getMinBits()) {
                    return truncate(orig, outputType);
                }
            } else if (toType instanceof BooleanType) {
                return truncate(orig, (BooleanType) toType);
            }
        }
        ctxt.error("Invalid coercion of %s to %s", type, toType);
        return orig;
    }

    @Override
    public List<Value> visitUnknown(List<Value> arguments, ValueHandle valueHandle) {
        return fixArguments(valueHandle.getValueType(), arguments);
    }

    @Override
    public List<Value> visit(List<Value> arguments, ExactMethodElementHandle valueHandle) {
        if (valueHandle.getExecutable().isSignaturePolymorphic()) {
            return arguments;
        } else {
            return fixArguments(valueHandle.getValueType(), arguments);
        }
    }

    @Override
    public List<Value> visit(List<Value> arguments, InterfaceMethodElementHandle valueHandle) {
        if (valueHandle.getExecutable().isSignaturePolymorphic()) {
            return arguments;
        } else {
            return fixArguments(valueHandle.getValueType(), arguments);
        }
    }

    @Override
    public List<Value> visit(List<Value> arguments, VirtualMethodElementHandle valueHandle) {
        if (valueHandle.getExecutable().isSignaturePolymorphic()) {
            return arguments;
        } else {
            return fixArguments(valueHandle.getValueType(), arguments);
        }
    }

    @Override
    public List<Value> visit(List<Value> arguments, StaticMethodElementHandle valueHandle) {
        if (valueHandle.getExecutable().isSignaturePolymorphic()) {
            return arguments;
        } else {
            return fixArguments(valueHandle.getValueType(), arguments);
        }
    }
}
