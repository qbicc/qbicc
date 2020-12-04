package cc.quarkus.qcc.plugin.native_;

import static cc.quarkus.qcc.context.CompilationContext.*;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public class NativeBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NativeBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeFunctionInfo functionInfo = nativeInfo.nativeFunctions.get(target);
        if (functionInfo != null) {
            // todo: avoid declaring more than once; cache or something
            ctxt.getOrAddProgramModule(getCurrentElement().getEnclosingType())
                .getOrAddSection(IMPLICIT_SECTION_NAME)
                .declareFunction(target, functionInfo.symbolLiteral.getName(), (FunctionType) functionInfo.symbolLiteral.getType());
            // todo: prologue, epilogue (store current thread, GC state, etc.)
            return callFunction(functionInfo.symbolLiteral, arguments);
        }
        if (target.getEnclosingType().internalNameEquals(Native.C_NATIVE)) {
            switch (target.getName()) {
                case "word": {
                    return arguments.get(0);
                }
                case "uword": {
                    return bitCast(arguments.get(0), ((IntegerType)arguments.get(0).getType()).asUnsigned());
                }
                case "sizeof": {
                    return ctxt.getLiteralFactory().literalOf(arguments.get(0).getType().getSize());
                }
                case "alignof": {
                    return ctxt.getLiteralFactory().literalOf(arguments.get(0).getType().getAlign());
                }
            }
        }
        // no special behaviors
        return super.invokeValueStatic(target, arguments);
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeFunctionInfo functionInfo = nativeInfo.nativeFunctions.get(target);
        if (functionInfo != null) {
            // todo: store current thread into TLS for recursive Java call-in
            return callFunction(functionInfo.symbolLiteral, arguments);
        }
        // no special behaviors
        return super.invokeStatic(target, arguments);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value input, final MethodElement target, final List<Value> arguments) {
        ValueType type = input.getType();
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (type instanceof IntegerType) {
            IntegerType inputType = (IntegerType) type;
            // special handling for conversion methods
            String methodName = target.getName();
            switch (methodName) {
                case "byteValue":
                case "shortValue":
                case "intValue":
                case "longValue":
                case "charValue": {
                    // integer to integer
                    IntegerType outputType = (IntegerType) target.getReturnType();
                    if (outputType.getMinBits() > inputType.getMinBits()) {
                        return super.extend(input, outputType);
                    } else if (outputType.getMinBits() < inputType.getMinBits()) {
                        return super.truncate(input, outputType);
                    } else {
                        return super.bitCast(input, outputType);
                    }
                }
                case "floatValue":
                case "doubleValue": {
                    // integer to float
                    return valueConvert(input, (WordType) target.getReturnType());
                }
                case "isZero": {
                    return cmpEq(input, lf.literalOf(0));
                }
            }
        } else if (type instanceof FloatType) {
            FloatType inputType = (FloatType) type;
            // special handling for conversion methods
            String methodName = target.getName();
            switch (methodName) {
                case "byteValue":
                case "shortValue":
                case "intValue":
                case "longValue":
                case "charValue": {
                    // float to integer
                    return valueConvert(input, (WordType) target.getReturnType());
                }
                case "floatValue":
                case "doubleValue": {
                    // float to float
                    FloatType outputType = (FloatType) target.getReturnType();
                    if (outputType.getMinBits() > inputType.getMinBits()) {
                        return super.extend(input, outputType);
                    } else if (outputType.getMinBits() < inputType.getMinBits()) {
                        return super.truncate(input, outputType);
                    } else {
                        return input;
                    }
                }
                case "isZero": {
                    return cmpEq(input, lf.literalOf(0.0));
                }
            }
        }
        return super.invokeValueInstance(kind, input, target, arguments);
    }
}
