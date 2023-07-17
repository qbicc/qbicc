package org.qbicc.plugin.native_;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Auto;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DecodeReference;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Dereference;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.object.Function;
import org.qbicc.type.ArrayType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VariadicType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.methodhandle.MethodHandleKind;
import org.qbicc.type.methodhandle.MethodMethodHandleConstant;

/**
 *
 */
public class NativeBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final ClassTypeDescriptor lambdaMetafactoryDesc;
    private final MethodDescriptor metafactoryDesc;

    public NativeBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
        ClassContext bcc = getContext().getBootstrapClassContext();
        lambdaMetafactoryDesc = ClassTypeDescriptor.synthesize(bcc, "java/lang/invoke/LambdaMetafactory");
        ClassTypeDescriptor methodHandleDesc = ClassTypeDescriptor.synthesize(bcc, "java/lang/invoke/MethodHandle");
        ClassTypeDescriptor methodTypeDesc = ClassTypeDescriptor.synthesize(bcc, "java/lang/invoke/MethodType");
        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(bcc, "java/lang/String");
        ClassTypeDescriptor lookupDesc = ClassTypeDescriptor.synthesize(bcc, "java/lang/invoke/MethodHandles$Lookup");
        ClassTypeDescriptor callSiteDesc = ClassTypeDescriptor.synthesize(bcc, "java/lang/invoke/CallSite");
        metafactoryDesc = MethodDescriptor.synthesize(bcc, callSiteDesc, List.of(lookupDesc, stringDesc, methodTypeDesc, methodTypeDesc, methodHandleDesc, methodTypeDesc));
    }

    @Override
    public Value loadTypeId(Value objectPointer) {
        if (objectPointer instanceof DecodeReference) {
            // not a native type
            return super.loadTypeId(objectPointer);
        } else {
            // it is a native type
            return getLiteralFactory().literalOfType(objectPointer.getType());
        }
    }

    @Override
    public Value decodeReference(Value refVal, PointerType pointerType) {
        if (refVal.getType() instanceof ReferenceType) {
            // regular reference
            return super.decodeReference(refVal, pointerType);
        } else {
            // it is a native type
            if (refVal.getType() instanceof PointerType rpt) {
                if (pointerType.getPointeeType() instanceof VoidType || pointerType.equals(rpt)) {
                    return refVal;
                }
                return getFirstBuilder().bitCast(refVal, pointerType);
            } else {
                return refVal;
            }
        }
    }

    @Override
    public Value new_(ClassTypeDescriptor desc) {
        ClassContext classContext = getCurrentClassContext();
        DefinedTypeDefinition def = classContext.findDefinedType(desc.getPackageName() + "/" + desc.getClassName());
        if (def != null) {
            ValueType nativeType = NativeInfo.get(ctxt).getNativeType(def);
            if (nativeType instanceof VariadicType) {
                // default behavior
                return super.new_(desc);
            } else if (nativeType != null) {
                return getFirstBuilder().auto(getLiteralFactory().zeroInitializerLiteralOfType(nativeType));
            }
        }
        return super.new_(desc);
    }

    @Override
    public Value newArray(ArrayTypeDescriptor atd, Value size) {
        ClassContext classContext = getCurrentClassContext();
        TypeDescriptor etd = atd.getElementTypeDescriptor();
        if (etd instanceof ClassTypeDescriptor desc) {
            DefinedTypeDefinition def = classContext.findDefinedType(desc.getPackageName() + "/" + desc.getClassName());
            if (def != null) {
                ValueType nativeType = NativeInfo.get(ctxt).getNativeType(def);
                if (nativeType != null) {
                    if (size instanceof IntegerLiteral lit) {
                        return getFirstBuilder().auto(getLiteralFactory().zeroInitializerLiteralOfType(getTypeSystem().getArrayType(nativeType, lit.longValue())));
                    } else {
                        ctxt.error(getLocation(), "Non-constant array length for native types not supported");
                        throw new BlockEarlyTermination(unreachable());
                    }
                }
            }
        }
        return super.newArray(atd, size);
    }

    @Override
    public Value loadLength(Value arrayPointer) {
        if (arrayPointer.getType() instanceof ArrayType at) {
            return getLiteralFactory().literalOf(getTypeSystem().getSignedInteger32Type(), at.getElementCount());
        } else {
            return super.loadLength(arrayPointer);
        }
    }

    @Override
    public Value checkcast(Value value, TypeDescriptor desc) {
        if (value instanceof Auto auto) {
            return auto;
        }
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
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.call(targetPtr, receiver, mapArguments(targetPtr, arguments));
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoSideEffects(targetPtr, receiver, mapArguments(targetPtr, arguments));
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoReturn(targetPtr, receiver, mapArguments(targetPtr, arguments));
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(targetPtr, receiver, mapArguments(targetPtr, arguments), catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.tailCall(targetPtr, receiver, mapArguments(targetPtr, arguments));
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(targetPtr, receiver, mapArguments(targetPtr, arguments), catchLabel, resumeLabel, targetArguments);
    }

    /**
     * Map arguments for calls to variadic functions.  If the call target is a variadic function, then the Java-style
     * varargs are mapped to native variadic form.  If the Java varargs array is not of a constant length, an error is raised.
     *
     * @param targetPtr the pointer to the call target (must not be {@code null})
     * @param arguments the input arguments (must not be {@code null})
     * @return the mapped arguments (not {@code null})
     */
    private List<Value> mapArguments(Value targetPtr, List<Value> arguments) {
        ValueType valueType = targetPtr.getPointeeType();
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
    public Value resolveStaticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeFunctionInfo functionInfo = nativeInfo.getFunctionInfo(owner, name, descriptor);
        if (functionInfo != null) {
            if (functionInfo instanceof ExportedFunctionInfo efi) {
                // we define it
                DefinedTypeDefinition declaringClass = efi.getDeclaringClass();
                if (getRootElement().getEnclosingType() == declaringClass) {
                    // we do not have to declare it; we call it directly
                    return getLiteralFactory().literalOf(efi.getFunctionElement());
                }
            }
            // declare it
            return ctxt.getLiteralFactory().literalOf(ctxt.getOrAddProgramModule(getRootElement())
                .declareFunction(null, functionInfo.getName(), functionInfo.getType(), Function.FN_NO_SAFEPOINTS));
        }
        return super.resolveStaticMethod(owner, name, deNative(descriptor));
    }

    @Override
    public Value resolveInstanceMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.resolveInstanceMethod(deNative(owner), name, deNative(descriptor));
    }

    @Override
    public Value lookupVirtualMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        if (reference instanceof Dereference d && d.getType() instanceof FunctionType) {
            return d.getPointer();
        } else {
            return super.lookupVirtualMethod(reference, deNative(owner), name, deNative(descriptor));
        }
    }

    @Override
    public Value lookupVirtualMethod(Value reference, InstanceMethodElement method) {
        if (reference instanceof Dereference d && d.getType() instanceof FunctionType) {
            return d.getPointer();
        } else if (NativeInfo.get(ctxt).isNativeType(method.getEnclosingType())) {
            return getLiteralFactory().literalOf(method);
        } else {
            return super.lookupVirtualMethod(reference, method);
        }
    }

    @Override
    public Value lookupInterfaceMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        if (reference instanceof Dereference d && d.getType() instanceof FunctionType) {
            return d.getPointer();
        } else {
            return super.lookupInterfaceMethod(reference, deNative(owner), name, deNative(descriptor));
        }
    }

    @Override
    public Value lookupInterfaceMethod(Value reference, InstanceMethodElement method) {
        if (reference instanceof Dereference d && d.getType() instanceof FunctionType) {
            return d.getPointer();
        } else if (NativeInfo.get(ctxt).isNativeType(method.getEnclosingType())) {
            return getLiteralFactory().literalOf(method);
        } else {
            return super.lookupInterfaceMethod(reference, method);
        }
    }

    @Override
    public Value resolveConstructor(TypeDescriptor owner, MethodDescriptor descriptor) {
        return super.resolveConstructor(deNative(owner), deNative(descriptor));
    }

    @Override
    public Value resolveStaticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeDataInfo fieldInfo = nativeInfo.getFieldInfo(owner, name);
        if (fieldInfo != null) {
            return getAndDeclareSymbolLiteral(fieldInfo);
        }
        return super.resolveStaticField(deNative(owner), name, deNative(type));
    }

    @Override
    public Value instanceFieldOf(Value instancePointer, TypeDescriptor owner, String name, TypeDescriptor type) {
        return super.instanceFieldOf(instancePointer, deNative(owner), name, deNative(type));
    }

    @Override
    public Value invokeDynamic(MethodMethodHandleConstant bootstrapHandle, List<Literal> bootstrapArgs, String name, MethodDescriptor descriptor, List<Value> arguments) {
        // convert method handles of functions into function pointers
        if (bootstrapHandle.getOwnerDescriptor().equals(lambdaMetafactoryDesc) && bootstrapHandle.getMethodName().equals("metafactory") && bootstrapHandle.getDescriptor().equals(metafactoryDesc)) {
            // it's potentially a functional method handle; let's check it out
            if (bootstrapArgs.get(bootstrapArgs.size() - 2) instanceof MethodHandleLiteral mhl) {
                if (mhl.getMethodHandleConstant() instanceof MethodMethodHandleConstant mhc) {
                    if (mhc.getKind() == MethodHandleKind.INVOKE_STATIC) {
                        ClassTypeDescriptor owner = mhc.getOwnerDescriptor();
                        // force type to be loaded
                        String packageName = owner.getPackageName();
                        String intName;
                        if (packageName.isEmpty()) {
                            intName = owner.getClassName();
                        } else {
                            intName = packageName + '/' + owner.getClassName();
                        }
                        getCurrentClassContext().findDefinedType(intName).load();
                        String targetMethodName = mhc.getMethodName();
                        NativeInfo nativeInfo = NativeInfo.get(ctxt);
                        NativeFunctionInfo functionInfo = nativeInfo.getFunctionInfo(owner, targetMethodName, mhc.getDescriptor());
                        if (functionInfo != null) {
                            ExecutableElement currentElement = element();
                            return deref(getLiteralFactory().literalOf(ctxt.getOrAddProgramModule(currentElement).declareFunction(currentElement, functionInfo.getName(), functionInfo.getType())));
                        }
                    }
                }
            }
        }
        return super.invokeDynamic(bootstrapHandle, bootstrapArgs, name, descriptor, arguments);
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

    private Literal getAndDeclareSymbolLiteral(final NativeDataInfo fieldInfo) {
        ProgramObjectLiteral sym = fieldInfo.symbolLiteral;
        DefinedTypeDefinition ourType = getRootElement().getEnclosingType();
        // declare it
        LiteralFactory lf = ctxt.getLiteralFactory();
        return lf.literalOf(ctxt.getOrAddProgramModule(ourType).declareData(sym.getProgramObject()).getPointer());
    }

}
