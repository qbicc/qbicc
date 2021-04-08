package cc.quarkus.qcc.plugin.conversion;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.VariadicType;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.InvokableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * Automatically truncate parameters to method calls.  This is the complement to the {@code promote} method in the
 * method parser, which is applied to the return types of methods to inflate them to the 32 bits specified by the JVMS;
 * however, demotion cannot happen until we know the target type of each argument, which does not happen until after
 * type resolution.
 */
public class MethodCallFixupBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public MethodCallFixupBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Node invokeStatic(MethodElement target, List<Value> arguments) {
        return super.invokeStatic(target, fixArguments(target, arguments));
    }

    @Override
    public Node invokeInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments) {
        return super.invokeInstance(kind, instance, target, fixArguments(target, arguments));
    }

    @Override
    public Value invokeValueStatic(MethodElement target, List<Value> arguments) {
        return super.invokeValueStatic(target, fixArguments(target, arguments));
    }

    @Override
    public Value invokeValueInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments) {
        return super.invokeValueInstance(kind, instance, target, fixArguments(target, arguments));
    }

    @Override
    public Value invokeConstructor(Value instance, ConstructorElement target, List<Value> arguments) {
        return super.invokeConstructor(instance, target, fixArguments(target, arguments));
    }

    @Override
    public Value callFunction(Value callTarget, List<Value> arguments) {
        return super.callFunction(callTarget, fixArguments(callTarget.getType(), arguments));
    }

    private List<Value> fixArguments(final InvokableElement target, final List<Value> arguments) {
        if (target.hasAllModifiersOf(ClassFile.I_ACC_SIGNATURE_POLYMORPHIC)) {
            // TODO: extract the signature from the method handle instance!
            return arguments;
        } else {
            return fixArguments(target.getType(), arguments);
        }
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
                return isNe(orig, ctxt.getLiteralFactory().literalOf(inputType, 0));
            }
        } else if (type instanceof UnsignedIntegerType) {
            UnsignedIntegerType inputType = (UnsignedIntegerType) type;
            if (toType instanceof UnsignedIntegerType) {
                UnsignedIntegerType outputType = (UnsignedIntegerType) toType;
                if (outputType.getMinBits() < inputType.getMinBits()) {
                    return truncate(orig, outputType);
                }
            } else if (toType instanceof BooleanType) {
                return isNe(orig, ctxt.getLiteralFactory().literalOf(inputType, 0));
            }
        }
        ctxt.error("Invalid coercion of %s to %s", type, toType);
        return orig;
    }
}
