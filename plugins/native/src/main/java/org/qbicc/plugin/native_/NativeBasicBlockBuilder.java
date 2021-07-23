package org.qbicc.plugin.native_;

import java.util.ArrayList;
import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CastValue;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.NewArray;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Linkage;
import org.qbicc.object.Section;
import org.qbicc.object.ThreadLocalMode;
import org.qbicc.type.ArrayType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class NativeBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NativeBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
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
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        return super.invokeNoReturn(target, mapArguments(target, arguments), catchLabel);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        return super.tailCall(target, mapArguments(target, arguments));
    }

    @Override
    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        return super.tailInvoke(target, mapArguments(target, arguments), catchLabel);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        return super.invoke(target, mapArguments(target, arguments), catchLabel, resumeLabel);
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
        ValueType valueType = handle.getValueType();
        if (valueType instanceof FunctionType) {
            FunctionType fnType = (FunctionType) valueType;
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
                if (varArgArray instanceof NewArray) {
                    ValueHandle arrayHandle = referenceHandle(varArgArray);
                    Value sizeVal = ((NewArray) varArgArray).getSize();
                    // see through casts
                    while (sizeVal instanceof CastValue) {
                        sizeVal = ((CastValue) sizeVal).getInput();
                    }
                    if (sizeVal instanceof IntegerLiteral) {
                        int varCnt = ((IntegerLiteral) sizeVal).intValue();
                        // original param count, minus the variadic type, plus the number of given arguments
                        List<Value> realArgs = new ArrayList<>(pc - 1 + varCnt);
                        for (int i = 0; i < size - 1; i++) {
                            realArgs.add(arguments.get(i));
                        }
                        // array creation is expected to be optimized away
                        LiteralFactory lf = ctxt.getLiteralFactory();
                        for (int i = 0; i < varCnt; i++) {
                            realArgs.add(load(elementOf(arrayHandle, lf.literalOf(i)), MemoryAtomicityMode.NONE));
                        }
                        return realArgs;
                    } else {
                        // usage error
                        ctxt.error(getLocation(), "Variadic call only allowed with array of constant size");
                        return arguments;
                    }
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
            return functionOf(ctxt.getImplicitSection(getRootElement())
                .declareFunction(null, functionInfo.getName(), functionInfo.getType()));
        }
        return super.staticMethod(owner, name, descriptor);
    }

    @Override
    public ValueHandle staticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeDataInfo fieldInfo = nativeInfo.getFieldInfo(owner, name);
        if (fieldInfo != null) {
            // todo: convert to GlobalVariable
            SymbolLiteral sym = getAndDeclareSymbolLiteral(fieldInfo);
            if (sym.getType() instanceof ArrayType) {
                return nativeArrayHandle(sym);
            } else {
                return pointerHandle(getAndDeclareSymbolLiteral(fieldInfo));
            }
        }
        return super.staticField(owner, name, type);
    }

    private SymbolLiteral getAndDeclareSymbolLiteral(final NativeDataInfo fieldInfo) {
        SymbolLiteral sym = fieldInfo.symbolLiteral;
        DefinedTypeDefinition ourType = getRootElement().getEnclosingType();
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

}
