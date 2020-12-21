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
import cc.quarkus.qcc.graph.literal.UndefinedLiteral;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.NullType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.TypeType;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class NativeBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NativeBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value invokeValueStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeFunctionInfo functionInfo = nativeInfo.getFunctionInfo(owner, name, descriptor);
        if (functionInfo != null) {
            ctxt.getOrAddProgramModule(getCurrentElement().getEnclosingType())
                .getOrAddSection(IMPLICIT_SECTION_NAME)
                .declareFunction(null, functionInfo.symbolLiteral.getName(), (FunctionType) functionInfo.symbolLiteral.getType());
            // todo: prologue, epilogue (store current thread, GC state, etc.)
            return callFunction(functionInfo.symbolLiteral, arguments);
        }
        if (owner.equals(nativeInfo.cNativeDesc)) {
            switch (name) {
                case "word": {
                    return arguments.get(0);
                }
                case "uword": {
                    return bitCast(arguments.get(0), ((IntegerType)arguments.get(0).getType()).asUnsigned());
                }
                case "sizeofArray":
                case "sizeof": {
                    ValueType argType = arguments.get(0).getType();
                    long size;
                    if (argType instanceof TypeType) {
                        size = ((TypeType) argType).getUpperBound().getSize();
                    } else {
                        size = argType.getSize();
                    }
                    return ctxt.getLiteralFactory().literalOf(size);
                }
                case "alignof": {
                    ValueType argType = arguments.get(0).getType();
                    long align;
                    if (argType instanceof TypeType) {
                        align = ((TypeType) argType).getUpperBound().getAlign();
                    } else {
                        align = argType.getAlign();
                    }
                    return ctxt.getLiteralFactory().literalOf(align);
                }
                case "defined": {
                    return ctxt.getLiteralFactory().literalOf(! (arguments.get(0) instanceof UndefinedLiteral));
                }
                case "isComplete": {
                    return ctxt.getLiteralFactory().literalOf(arguments.get(0).getType().isComplete());
                }
                case "isSigned": {
                    return ctxt.getLiteralFactory().literalOf(arguments.get(0).getType() instanceof SignedIntegerType);
                }
                case "isUnsigned": {
                    return ctxt.getLiteralFactory().literalOf(arguments.get(0).getType() instanceof UnsignedIntegerType);
                }
                case "typesAreEquivalent": {
                    return ctxt.getLiteralFactory().literalOf(arguments.get(0).getType().equals(arguments.get(1).getType()));
                }
                // no args
                case "zero": {
                    return ctxt.getLiteralFactory().literalOf(0);
                }
            }
        }
        // no special behaviors
        return super.invokeValueStatic(owner, name, descriptor, arguments);
    }

    public Node invokeStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeFunctionInfo functionInfo = nativeInfo.getFunctionInfo(owner, name, descriptor);
        if (functionInfo != null) {
            // todo: store current thread into TLS for recursive Java call-in
            return callFunction(functionInfo.symbolLiteral, arguments);
        }
        // no special behaviors
        return super.invokeStatic(owner, name, descriptor, arguments);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value input, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        ValueType type = input.getType();
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (type instanceof NullType) {
            // treat `null` as a null (zero) pointer value
            if (owner.equals(nativeInfo.ptrDesc) || owner.equals(nativeInfo.wordDesc) || owner.equals(nativeInfo.cObjectDesc)) {
                switch (name) {
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
        } else if (type instanceof IntegerType || type instanceof FloatType || type instanceof PointerType) {
            TypeSystem ts = ctxt.getTypeSystem();
            switch (name) {
                case "byteValue": {
                    return doConvert(input, type, ts.getSignedInteger8Type());
                }
                case "shortValue": {
                    return doConvert(input, type, ts.getSignedInteger16Type());
                }
                case "intValue": {
                    return doConvert(input, type, ts.getSignedInteger32Type());
                }
                case "longValue": {
                    return doConvert(input, type, ts.getSignedInteger64Type());
                }
                case "charValue": {
                    return doConvert(input, type, ts.getUnsignedInteger16Type());
                }
                case "floatValue": {
                    return doConvert(input, type, ts.getFloat32Type());
                }
                case "doubleValue": {
                    return doConvert(input, type, ts.getFloat64Type());
                }
                case "isZero": {
                    return cmpEq(input, lf.literalOf(0));
                }
            }
        }
        return super.invokeValueInstance(kind, input, owner, name, descriptor, arguments);
    }

    private Value doConvert(final Value input, final ValueType from, final IntegerType outputType) {
        if (from instanceof IntegerType) {
            IntegerType inputType = (IntegerType) from;
            if (outputType.getMinBits() > inputType.getMinBits()) {
                return super.extend(input, outputType);
            } else if (outputType.getMinBits() < inputType.getMinBits()) {
                return super.truncate(input, outputType);
            } else {
                return super.bitCast(input, outputType);
            }
        } else if (from instanceof FloatType || from instanceof PointerType) {
            return super.valueConvert(input, outputType);
        } else {
            // ??
            return input;
        }
    }

    private Value doConvert(final Value input, final ValueType from, final FloatType outputType) {
        if (from instanceof IntegerType) {
            return super.valueConvert(input, outputType);
        } else if (from instanceof PointerType) {
            ctxt.error(getLocation(), "Invalid conversion from pointer to floating-point type");
            return super.valueConvert(input, outputType);
        } else {
            if (from instanceof FloatType) {
                FloatType inputType = (FloatType) from;
                if (outputType.getMinBits() > inputType.getMinBits()) {
                    return super.extend(input, outputType);
                } else if (outputType.getMinBits() < inputType.getMinBits()) {
                    return super.truncate(input, outputType);
                } else {
                    return input;
                }
            } else {
                // ??
                return input;
            }
        }
    }
}
