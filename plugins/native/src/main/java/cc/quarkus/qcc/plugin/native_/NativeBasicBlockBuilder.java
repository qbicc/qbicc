package cc.quarkus.qcc.plugin.native_;

import static cc.quarkus.qcc.context.CompilationContext.*;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.MemoryAccessMode;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.UndefinedLiteral;
import cc.quarkus.qcc.object.DataDeclaration;
import cc.quarkus.qcc.object.Linkage;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.object.ThreadLocalMode;
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
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
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
        if (functionInfo != null) {
            ctxt.getImplicitSection(getCurrentElement())
                .declareFunction(functionInfo.origMethod, functionInfo.symbolLiteral.getName(), (FunctionType) functionInfo.symbolLiteral.getType());
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
                    case "deref": {
                        return pointerLoad(input, MemoryAccessMode.PLAIN, MemoryAtomicityMode.UNORDERED);
                    }
                    case "asArray": {
                        return input;
                    }
                    case "get": {
                        return readArrayValue(input, arguments.get(0), JavaAccessMode.DETECT);
                    }
                    case "plus": {
                        return add(input, arguments.get(0));
                    }
                    case "minus": {
                        return sub(input, arguments.get(0));
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
                case "deref": {
                    return pointerLoad(input, MemoryAccessMode.PLAIN, MemoryAtomicityMode.UNORDERED);
                }
                case "asArray": {
                    return input;
                }
                case "get": {
                    return readArrayValue(input, arguments.get(0), JavaAccessMode.DETECT);
                }
                case "plus": {
                    return add(input, arguments.get(0));
                }
                case "minus": {
                    return sub(input, arguments.get(0));
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
            return writeArrayValue(input, arguments.get(0), arguments.get(1), JavaAccessMode.DETECT);
        }
        return super.invokeInstance(kind, input, owner, name, descriptor, arguments);
    }

    @Override
    public Value readStaticField(TypeDescriptor owner, String name, TypeDescriptor descriptor, JavaAccessMode mode) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeDataInfo fieldInfo = nativeInfo.getFieldInfo(owner, name);
        if (fieldInfo != null) {
            // todo: fix memory access modes
            return super.pointerLoad(getAndDeclareSymbolLiteral(fieldInfo), MemoryAccessMode.PLAIN, MemoryAtomicityMode.UNORDERED);
        }
        return super.readStaticField(owner, name, descriptor, mode);
    }

    @Override
    public Node writeStaticField(TypeDescriptor owner, String name, TypeDescriptor descriptor, Value value, JavaAccessMode mode) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeDataInfo fieldInfo = nativeInfo.getFieldInfo(owner, name);
        if (fieldInfo != null) {
            // todo: fix memory access modes
            return super.pointerStore(getAndDeclareSymbolLiteral(fieldInfo), value, MemoryAccessMode.PLAIN, MemoryAtomicityMode.UNORDERED);
        }
        return super.writeStaticField(owner, name, descriptor, value, mode);
    }

    private SymbolLiteral getAndDeclareSymbolLiteral(final NativeDataInfo fieldInfo) {
        SymbolLiteral sym = fieldInfo.symbolLiteral;
        // todo: fix this for inlining
        DefinedTypeDefinition ourType = getCurrentElement().getEnclosingType();
        if (!fieldInfo.defined || fieldInfo.fieldElement.getEnclosingType() != ourType) {
            // declare it
            Section section = ctxt.getOrAddProgramModule(ourType).getOrAddSection(IMPLICIT_SECTION_NAME);
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

    public Value readArrayValue(final Value array, final Value index, final JavaAccessMode mode) {
        ValueType type = array.getType();
        if (type instanceof PointerType) {
            // todo: these access modes are only approximately correct
            return pointerLoad(add(array, index), MemoryAccessMode.PLAIN, mode == JavaAccessMode.PLAIN ? MemoryAtomicityMode.UNORDERED : MemoryAtomicityMode.ACQUIRE);
        } else {
            return super.readArrayValue(array, index, mode);
        }
    }

    public Node writeArrayValue(final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        ValueType type = array.getType();
        if (type instanceof PointerType) {
            // todo: these access modes are only approximately correct
            return pointerStore(add(array, index), value, MemoryAccessMode.PLAIN, mode == JavaAccessMode.PLAIN ? MemoryAtomicityMode.UNORDERED : MemoryAtomicityMode.RELEASE);
        } else {
            return super.writeArrayValue(array, index, value, mode);
        }
    }
}
