package cc.quarkus.qcc.plugin.native_;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.ClassOf;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.NewArray;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.TypeLiteral;
import cc.quarkus.qcc.graph.literal.UndefinedLiteral;
import cc.quarkus.qcc.object.DataDeclaration;
import cc.quarkus.qcc.object.Linkage;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.object.ThreadLocalMode;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.TypeType;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.VariadicType;
import cc.quarkus.qcc.type.definition.element.MethodElement;
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
        MethodElement binding = nativeInfo.getNativeBinding(owner, name, descriptor);
        if (binding != null) {
            return super.invokeValueStatic(binding, arguments);
        }
        NativeFunctionInfo functionInfo = nativeInfo.getFunctionInfo(owner, name, descriptor);
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (functionInfo != null) {
            SymbolLiteral sym = functionInfo.symbolLiteral;
            FunctionType functionType = (FunctionType) sym.getType();
            ctxt.getImplicitSection(getCurrentElement())
                .declareFunction(functionInfo.origMethod, sym.getName(), functionType);
            // todo: prologue, epilogue (store current thread, GC state, etc.)
            int pc = functionType.getParameterCount();
            if (pc > 0 && functionType.getParameterType(pc - 1) instanceof VariadicType) {
                // build up an argument list from the varargs array
                int size = arguments.size();
                if (size < 1) {
                    throw new IllegalStateException("Unexpected argument list size");
                }
                if (size != pc) {
                    throw new IllegalStateException("Argument list size does not match function prototype size");
                }
                Value varArgArray = arguments.get(size - 1);
                if (varArgArray instanceof NewArray) {
                    ValueHandle arrayHandle = referenceHandle(varArgArray);
                    Value sizeVal = ((NewArray) varArgArray).getSize();
                    if (sizeVal instanceof IntegerLiteral) {
                        int varCnt = ((IntegerLiteral) sizeVal).intValue();
                        // original param count, minus the variadic type, plus the number of given arguments
                        List<Value> realArgs = new ArrayList<>(pc - 1 + varCnt);
                        for (int i = 0; i < size - 1; i ++) {
                            realArgs.add(arguments.get(i));
                        }
                        // array creation is expected to be optimized away
                        for (int i = 0; i < varCnt; i ++) {
                            realArgs.add(load(elementOf(arrayHandle, lf.literalOf(i)), MemoryAtomicityMode.NONE));
                        }
                        return callFunction(sym, realArgs);
                    } else {
                        // usage error
                        ctxt.error(getLocation(), "Variadic call only allowed with array of constant size");
                        return callFunction(sym, arguments);
                    }
                } else {
                    // usage error
                    ctxt.error(getLocation(), "Variadic call only allowed with immediate array argument");
                    return callFunction(sym, arguments);
                }
            }
            // not variadic; just plain call
            return callFunction(sym, arguments);
        }
        if (owner.equals(nativeInfo.cNativeDesc)) {
            switch (name) {
                case "alloca": {
                    return stackAllocate(ctxt.getTypeSystem().getUnsignedInteger8Type(), arguments.get(0), lf.literalOf(1));
                }
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
                    return lf.literalOf(size);
                }
                case "alignof": {
                    ValueType argType = arguments.get(0).getType();
                    long align;
                    if (argType instanceof TypeType) {
                        align = ((TypeType) argType).getUpperBound().getAlign();
                    } else {
                        align = argType.getAlign();
                    }
                    return lf.literalOf(align);
                }
                case "defined": {
                    return lf.literalOf(! (arguments.get(0) instanceof UndefinedLiteral));
                }
                case "isComplete": {
                    return lf.literalOf(arguments.get(0).getType().isComplete());
                }
                case "isSigned": {
                    return lf.literalOf(arguments.get(0).getType() instanceof SignedIntegerType);
                }
                case "isUnsigned": {
                    return lf.literalOf(arguments.get(0).getType() instanceof UnsignedIntegerType);
                }
                case "typesAreEquivalent": {
                    return lf.literalOf(arguments.get(0).getType().equals(arguments.get(1).getType()));
                }
                // no args
                case "zero": {
                    return lf.literalOf(0);
                }
            }
        }
        // no special behaviors
        return super.invokeValueStatic(owner, name, descriptor, arguments);
    }

    public Node invokeStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        MethodElement binding = nativeInfo.getNativeBinding(owner, name, descriptor);
        if (binding != null) {
            return super.invokeStatic(binding, arguments);
        }
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
        MethodElement binding = nativeInfo.getNativeBinding(owner, name, descriptor);
        if (binding != null) {
            return super.invokeValueStatic(binding, Native.copyWithPrefix(arguments, input, Value[]::new));
        }
        ValueType type = input.getType();
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (type instanceof IntegerType || type instanceof FloatType || type instanceof PointerType || type instanceof TypeType) {
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
                case "isNull":
                case "isZero": {
                    return isEq(input, lf.zeroInitializerLiteralOfType(input.getType()));
                }
                case "deref": {
                    return load(pointerHandle(input), MemoryAtomicityMode.UNORDERED);
                }
                case "asArray": {
                    return input;
                }
                case "get": {
                    return load(elementOf(pointerHandle(input), arguments.get(0)), MemoryAtomicityMode.UNORDERED);
                }
                case "plus": {
                    return add(input, arguments.get(0));
                }
                case "minus": {
                    return sub(input, arguments.get(0));
                }
                case "cast": {
                    ValueType castType;
                    if (arguments.size() == 1) {
                        // explicit type
                        Value arg = arguments.get(0);
                        if (arg instanceof ClassOf) {
                            ClassOf classOf = (ClassOf) arg;
                            Value classOfInput = classOf.getInput();
                            if (classOfInput instanceof TypeLiteral) {
                                TypeType castTypeType = (TypeType) classOfInput.getType();
                                castType = castTypeType.getUpperBound();
                                return doConvert(input, type, castType);
                            } else {
                                ctxt.error(getLocation(), "Expected class literal as argument to cast");
                                return input;
                            }
                        } else {
                            ctxt.error(getLocation(), "Expected class literal as argument to cast");
                            return input;
                        }
                    } else {
                        return input;
                    }
                }
            }
        }
        return super.invokeValueInstance(kind, input, owner, name, descriptor, arguments);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value input, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        MethodElement binding = nativeInfo.getNativeBinding(owner, name, descriptor);
        if (binding != null) {
            return super.invokeStatic(binding, Native.copyWithPrefix(arguments, input, Value[]::new));
        }
        if (owner.equals(nativeInfo.ptrDesc) && name.equals("set")) {
            return store(elementOf(pointerHandle(input), arguments.get(0)), arguments.get(1), MemoryAtomicityMode.UNORDERED);
        }
        return super.invokeInstance(kind, input, owner, name, descriptor, arguments);
    }

    @Override
    public ValueHandle staticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeDataInfo fieldInfo = nativeInfo.getFieldInfo(owner, name);
        if (fieldInfo != null) {
            return pointerHandle(getAndDeclareSymbolLiteral(fieldInfo));
        }
        return super.staticField(owner, name, type);
    }

    private SymbolLiteral getAndDeclareSymbolLiteral(final NativeDataInfo fieldInfo) {
        SymbolLiteral sym = fieldInfo.symbolLiteral;
        // todo: fix this for inlining
        DefinedTypeDefinition ourType = getCurrentElement().getEnclosingType();
        if (!fieldInfo.defined || fieldInfo.fieldElement.getEnclosingType() != ourType) {
            // declare it
            Section section = ctxt.getImplicitSection(ourType);
            DataDeclaration decl = section.declareData(fieldInfo.fieldElement, sym.getName(), fieldInfo.objectType);
            decl.setLinkage(Linkage.EXTERNAL);
            if (fieldInfo.fieldElement.hasAllModifiersOf(ClassFile.I_ACC_THREAD_LOCAL)) {
                decl.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
            }
        }
        return sym;
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

    private Value doConvert(final Value input, final ValueType from, final ValueType to) {
        if (to instanceof IntegerType) {
            return doConvert(input, from, (IntegerType) to);
        } else if (to instanceof FloatType) {
            return doConvert(input, from, (FloatType) to);
        } else if (to instanceof WordType) {
            return valueConvert(input, (WordType) to);
        } else {
            ctxt.error(getLocation(), "Unknown conversion from %s to %s", from, to);
            return input;
        }
    }
}
