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
import cc.quarkus.qcc.type.NullType;
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

    public Value invokeValueStatic(final MethodElement target, final ValueType type, final List<Value> arguments) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeFunctionInfo functionInfo = nativeInfo.nativeFunctions.get(target);
        if (functionInfo != null) {
            ctxt.getOrAddProgramModule(getCurrentElement().getEnclosingType())
                .getOrAddSection(IMPLICIT_SECTION_NAME)
                .declareFunction(target, functionInfo.symbolLiteral.getName(), (FunctionType) functionInfo.symbolLiteral.getType());
            // todo: prologue, epilogue (store current thread, GC state, etc.)
            return callFunction(functionInfo.symbolLiteral, arguments);
        }
        if (target.getEnclosingType().internalPackageAndNameEquals(Native.NATIVE_PKG, Native.C_NATIVE)) {
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
        return super.invokeValueStatic(target, type, arguments);
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

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value input, final MethodElement target, final ValueType returnType, final List<Value> arguments) {
        ValueType type = input.getType();
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (type instanceof NullType) {
            // treat `null` as a null (zero) pointer value
            if (target.getEnclosingType().internalPackageAndNameEquals(Native.NATIVE_PKG, Native.PTR)
                || target.getEnclosingType().internalPackageAndNameEquals(Native.NATIVE_PKG, Native.WORD)
                || target.getEnclosingType().internalPackageAndNameEquals(Native.NATIVE_PKG, Native.OBJECT)) {
                String methodName = target.getName();
                switch (methodName) {
                    case "byteValue": {
                        return lf.literalOf((byte) 0);
                    }
                    case "shortValue": {
                        return lf.literalOf((short) 0);
                    }
                    case "intValue": {
                        return lf.literalOf(0);
                    }
                    case "longValue": {
                        return lf.literalOf(0L);
                    }
                    case "charValue": {
                        return lf.literalOf('\0');
                    }
                    case "floatValue": {
                        return lf.literalOf(0.0f);
                    }
                    case "doubleValue": {
                        return lf.literalOf(0.0);
                    }
                    case "isZero": {
                        return lf.literalOf(true);
                    }
                }
            }
        } else if (type instanceof IntegerType) {
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
                    IntegerType outputType = (IntegerType) returnType;
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
                    return valueConvert(input, (WordType) returnType);
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
                    return valueConvert(input, (WordType) returnType);
                }
                case "floatValue":
                case "doubleValue": {
                    // float to float
                    FloatType outputType = (FloatType) returnType;
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
        return super.invokeValueInstance(kind, input, target, returnType, arguments);
    }
}
