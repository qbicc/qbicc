package org.qbicc.plugin.lowering;

import java.util.ArrayList;
import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.DispatchInvocation;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.object.Function;
import org.qbicc.object.Section;
import org.qbicc.object.ThreadLocalMode;
import org.qbicc.plugin.dispatch.DispatchTables;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public class InvocationLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final ExecutableElement originalElement;

    public InvocationLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        originalElement = delegate.getCurrentElement();
    }

    public Value currentThread() {
        ReferenceType type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Thread").load().getClassType().getReference();
        if (originalElement instanceof FunctionElement) {
            SymbolLiteral sym = ctxt.getCurrentThreadLocalSymbolLiteral();
            Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
            section.declareData(null, sym.getName(), ((PointerType)sym.getType()).getPointeeType()).setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
            // todo: replace symbol literal with global variable - or static field perhaps
            Value ptrVal = load(pointerHandle(sym), MemoryAtomicityMode.NONE);
            return valueConvert(ptrVal, type);
        } else {
            return parameter(type, "thr", 0);
        }
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        Function function = ctxt.getExactFunction(target);
        ctxt.declareForeignFunction(target, function, getCurrentElement());
        List<Value> args = new ArrayList<>(arguments.size() + 1);
        args.add(currentThread());
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(functionLiteral(function), args, Function.getFunctionFlags(target));
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        Value callTarget;
        if (kind == DispatchInvocation.Kind.INTERFACE) {
            callTarget = expandInterfaceDispatch(instance, target);
        } else if (kind == DispatchInvocation.Kind.VIRTUAL) {
            callTarget = expandVirtualDispatch(instance, target);
        } else {
            Function invokeTarget = ctxt.getExactFunction(target);
            ctxt.declareForeignFunction(target, invokeTarget, getCurrentElement());
            callTarget = functionLiteral(invokeTarget);
        }
        List<Value> args = new ArrayList<>(arguments.size() + 2);
        args.add(currentThread());
        args.add(instance);
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(callTarget, args, Function.getFunctionFlags(target));
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        Function function = ctxt.getExactFunction(target);
        ctxt.declareForeignFunction(target, function, getCurrentElement());
        List<Value> args = new ArrayList<>(arguments.size() + 1);
        args.add(currentThread());
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(functionLiteral(function), args, Function.getFunctionFlags(target));
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        Value callTarget;
        if (kind == DispatchInvocation.Kind.INTERFACE) {
            callTarget = expandInterfaceDispatch(instance, target);
        } else if (kind == DispatchInvocation.Kind.VIRTUAL) {
            callTarget = expandVirtualDispatch(instance, target);
        } else {
            Function invokeTarget = ctxt.getExactFunction(target);
            ctxt.declareForeignFunction(target, invokeTarget, getCurrentElement());
            callTarget = functionLiteral(invokeTarget);
        }
        List<Value> args = new ArrayList<>(arguments.size() + 2);
        args.add(currentThread());
        args.add(instance);
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(callTarget, args, Function.getFunctionFlags(target));
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        Function function = ctxt.getExactFunction(target);
        ctxt.declareForeignFunction(target, function, getCurrentElement());
        List<Value> args = new ArrayList<>(arguments.size() + 2);
        args.add(currentThread());
        args.add(instance);
        args.addAll(arguments);
        ctxt.enqueue(target);
        super.callFunction(functionLiteral(function), args, Function.getFunctionFlags(target));
        return instance;
    }

    private SymbolLiteral functionLiteral(final Function function) {
        return ctxt.getLiteralFactory().literalOfSymbol(function.getName(), function.getType());
    }

    private Value expandVirtualDispatch(Value instance, MethodElement target) {
        DispatchTables dt = DispatchTables.get(ctxt);
        DispatchTables.VTableInfo info = dt.getVTableInfo(target.getEnclosingType().load());
        if (info == null) {
            // No realized invocation targets are possible for this method!
            throw new BlockEarlyTermination(unreachable());
        }
        GlobalVariableElement vtables = dt.getVTablesGlobal();
        if (!vtables.getEnclosingType().equals(originalElement.getEnclosingType())) {
            Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
            section.declareData(null, vtables.getName(), vtables.getType());
        }
        int index = dt.getVTableIndex(target);
        Value typeId = load(instanceFieldOf(referenceHandle(instance), Layout.get(ctxt).getObjectTypeIdField()), MemoryAtomicityMode.UNORDERED);
        Value vtable = load(elementOf(globalVariable(dt.getVTablesGlobal()), typeId), MemoryAtomicityMode.UNORDERED);
        return load(memberOf(pointerHandle(bitCast(vtable, info.getType().getPointer())), info.getType().getMember(index)), MemoryAtomicityMode.UNORDERED);
    }

    // Current implementation strategy is "directly indexed itable" in the terminology of [Alpern et al 2001].
    // Very fast dispatch; but significant wasted data space due to sparse per-interface itables[].
    // However, all the invalid slots in the itable contain a pointer to VMHelpers.raiseIncompatibleClassChangerError()
    // so we do not need an explicit test at the call site.
    private Value expandInterfaceDispatch(Value instance, MethodElement target) {
        DispatchTables dt = DispatchTables.get(ctxt);
        DispatchTables.ITableInfo info = dt.getITableInfo(target.getEnclosingType().load());
        if (info == null) {
            // No realized invocation targets are possible for this method!
            invokeStatic(ctxt.getVMHelperMethod("raiseIncompatibleClassChangeError"), List.of());
            throw new BlockEarlyTermination(unreachable());
        }
        Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
        section.declareData(null, info.getGlobal().getName(), info.getGlobal().getType());
        int index = dt.getITableIndex(target);
        Value typeId = load(instanceFieldOf(referenceHandle(instance), Layout.get(ctxt).getObjectTypeIdField()), MemoryAtomicityMode.UNORDERED);
        Value itable = load(elementOf(globalVariable(info.getGlobal()), typeId), MemoryAtomicityMode.UNORDERED);
        return load(memberOf(pointerHandle(itable), info.getType().getMember(index)), MemoryAtomicityMode.UNORDERED);
    }
}
