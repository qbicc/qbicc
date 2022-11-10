package org.qbicc.plugin.serialization;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.interpreter.Vm;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;

import java.util.List;
import java.util.Map;

/**
 * This BBB ensures that all the Strings that will be needed by MethodDataEmitter
 * are interned and serialized to the BuildTimeHeap before the heap is emitted.
 */
public final class MethodDataStringsSerializer extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public MethodDataStringsSerializer(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    private void createMethodDataStrings() {
        ExecutableElement element = getCurrentElement();
        Vm vm = ctxt.getVm();
        BuildtimeHeap heap = BuildtimeHeap.get(ctxt);

        String methodName = "";
        if (element instanceof ConstructorElement) {
            methodName = "<init>";
        } else if (element instanceof InitializerElement) {
            methodName = "<clinit>";
        } else if (element instanceof MethodElement) {
            methodName = ((MethodElement)element).getName();
        } else if (element instanceof FunctionElement) {
            methodName = ((FunctionElement)element).getName();
        }

        String fileName = element.getSourceFileName();
        String className = element.getEnclosingType().getInternalName().replace('/', '.');
        String methodDesc = element.getDescriptor().toString();

        // the ProgramObjects being created here will be looked up by MethodDateEmitter later.
        if (fileName != null) {
            heap.serializeVmObject(vm.intern(fileName), true);
        }
        heap.serializeVmObject(vm.intern(className), true);
        heap.serializeVmObject(vm.intern(methodName), true);
        heap.serializeVmObject(vm.intern(methodDesc), true);
    }

    public Value call(ValueHandle target, List<Value> arguments) {
        createMethodDataStrings();
        return super.call(target, arguments);
    }

    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        createMethodDataStrings();
        return super.callNoSideEffects(target, arguments);
    }

    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        createMethodDataStrings();
        return super.callNoReturn(target, arguments);
    }

    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        createMethodDataStrings();
        return super.invoke(target, arguments, catchLabel, resumeLabel, targetArguments);
    }

    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        createMethodDataStrings();
        return super.invokeNoReturn(target, arguments, catchLabel, targetArguments);
    }

    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        createMethodDataStrings();
        return super.tailCall(target, arguments);
    }
}
