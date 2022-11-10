package org.qbicc.plugin.native_;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.type.ArrayType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VariadicType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class NativeBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NativeBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value checkcast(Value value, TypeDescriptor desc) {
        // delete casts to native base types preemptively
        if (desc instanceof ClassTypeDescriptor ctd) {
            if (ctd.packageAndClassNameEquals(Native.NATIVE_PKG, Native.WORD)) {
                if (!(value.getType() instanceof WordType)) {
                    ctxt.error(getLocation(), "Invalid cast of non-word type %s to word type", value.getType());
                }
                return value;
            }
            if (ctd.packageAndClassNameEquals(Native.NATIVE_PKG, Native.OBJECT)) {
                // any value can be cast to `object`
                return value;
            }
            return super.checkcast(value, deNative(ctd));
        }
        return super.checkcast(value, desc);
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        return super.call(target, mapArguments(target, arguments));
    }

    @Override
    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        return super.callNoSideEffects(target, mapArguments(target, arguments));
    }

    @Override
    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        return super.callNoReturn(target, mapArguments(target, arguments));
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(target, mapArguments(target, arguments), catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        return super.tailCall(target, mapArguments(target, arguments));
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(target, mapArguments(target, arguments), catchLabel, resumeLabel, targetArguments);
    }

    /**
     * Map arguments for calls to variadic functions.  If the call target is a variadic function, then the Java-style
     * varargs are mapped to native variadic form.  If the Java varargs array is not of a constant length, an error is raised.
     *
     * @param handle the handle to the call target (must not be {@code null})
     * @param arguments the input arguments (must not be {@code null})
     * @return the mapped arguments (not {@code null})
     */
    private List<Value> mapArguments(ValueHandle handle, List<Value> arguments) {
        ValueType valueType = handle.getPointeeType();
        if (valueType instanceof FunctionType fnType) {
            if (fnType.isVariadic()) {
                // build up an argument list from the varargs array
                int size = arguments.size();
                if (size < 1) {
                    throw new IllegalStateException("Unexpected argument list size");
                }
                int pc = fnType.getParameterCount();
                if (size != pc) {
                    throw new IllegalStateException("Argument list size does not match function prototype size");
                }
                Value varArgArray = arguments.get(size - 1);
                if (varArgArray.getType() instanceof ArrayType at && at.getElementType() instanceof VariadicType) {
                    final long varCnt = at.getElementCount();
                    // original param count, minus the variadic type, plus the number of given arguments
                    List<Value> realArgs = new ArrayList<>((int) (pc - 1 + varCnt));
                    for (int i = 0; i < size - 1; i++) {
                        realArgs.add(arguments.get(i));
                    }
                    // array creation is expected to be optimized away
                    LiteralFactory lf = ctxt.getLiteralFactory();
                    for (int i = 0; i < varCnt; i++) {
                        realArgs.add(varArgArray.extractElement(lf, lf.literalOf(i)));
                    }
                    return realArgs;
                } else {
                    ctxt.error(getLocation(), "Variadic function argument type must be `CNative.object...`");
                }
            }
        }
        return arguments;
    }

    @Override
    public ValueHandle staticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeFunctionInfo functionInfo = nativeInfo.getFunctionInfo(owner, name, descriptor);
        if (functionInfo != null) {
            if (functionInfo instanceof ExportedFunctionInfo) {
                ExportedFunctionInfo efi = (ExportedFunctionInfo) functionInfo;
                // we define it
                DefinedTypeDefinition declaringClass = efi.getDeclaringClass();
                if (getRootElement().getEnclosingType() == declaringClass) {
                    // we do not have to declare it; we call it directly
                    return functionOf(efi.getFunctionElement());
                }
            }
            // declare it
            return pointerHandle(ctxt.getLiteralFactory().literalOf(ctxt.getOrAddProgramModule(getRootElement())
                .declareFunction(null, functionInfo.getName(), functionInfo.getType())));
        }
        return super.staticMethod(owner, name, deNative(descriptor));
    }

    @Override
    public ValueHandle exactMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.exactMethodOf(instance, deNative(owner), name, deNative(descriptor));
    }

    @Override
    public ValueHandle virtualMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.virtualMethodOf(instance, deNative(owner), name, deNative(descriptor));
    }

    @Override
    public ValueHandle interfaceMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.interfaceMethodOf(instance, deNative(owner), name, deNative(descriptor));
    }

    @Override
    public ValueHandle constructorOf(Value instance, TypeDescriptor owner, MethodDescriptor descriptor) {
        return super.constructorOf(instance, deNative(owner), deNative(descriptor));
    }

    @Override
    public ValueHandle staticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeDataInfo fieldInfo = nativeInfo.getFieldInfo(owner, name);
        if (fieldInfo != null) {
            return pointerHandle(getAndDeclareSymbolLiteral(fieldInfo));
        }
        return super.staticField(deNative(owner), name, deNative(type));
    }

    @Override
    public ValueHandle instanceFieldOf(ValueHandle instance, TypeDescriptor owner, String name, TypeDescriptor type) {
        return super.instanceFieldOf(instance, deNative(owner), name, deNative(type));
    }

    MethodDescriptor deNative(MethodDescriptor md) {
        TypeDescriptor returnType = md.getReturnType();
        TypeDescriptor fixedReturnType = deNative(returnType);
        out: if (returnType == fixedReturnType) {
            for (TypeDescriptor parameterType : md.getParameterTypes()) {
                if (parameterType != deNative(parameterType)) {
                    break out;
                }
            }
            // it's OK
            return md;
        }
        int cnt = md.getParameterTypes().size();
        List<TypeDescriptor> newParamTypes = cnt == 0 ? List.of() : Arrays.asList(new TypeDescriptor[cnt]);
        for (TypeDescriptor parameterType : md.getParameterTypes()) {
            newParamTypes.add(deNative(parameterType));
        }
        ClassContext cc = getRootElement().getEnclosingType().getContext();
        return MethodDescriptor.synthesize(cc, fixedReturnType, newParamTypes);
    }

    TypeDescriptor deNative(TypeDescriptor td) {
        return td instanceof ClassTypeDescriptor ctd ? deNative(ctd) : td;
    }

    ClassTypeDescriptor deNative(ClassTypeDescriptor ctd) {
        ClassContext cc = getRootElement().getEnclosingType().getContext();
        String className = ctd.getClassName();
        if (className.endsWith("$_native")) {
            String packageName = ctd.getPackageName();
            String newClassName = className.substring(0, className.length() - 8);
            return ClassTypeDescriptor.synthesize(cc, packageName.isEmpty() ? newClassName : packageName + '/' + newClassName);
        } else {
            return ctd;
        }
    }

    private PointerLiteral getAndDeclareSymbolLiteral(final NativeDataInfo fieldInfo) {
        PointerLiteral sym = fieldInfo.symbolLiteral;
        DefinedTypeDefinition ourType = getRootElement().getEnclosingType();
        // declare it
        LiteralFactory lf = ctxt.getLiteralFactory();
        return lf.literalOf(ctxt.getOrAddProgramModule(ourType).declareData(sym.getPointer(ProgramObjectPointer.class).getProgramObject()).getPointer());
    }

}
