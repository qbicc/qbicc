package cc.quarkus.qcc.plugin.lowering;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockEarlyTermination;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.object.ThreadLocalMode;
import cc.quarkus.qcc.plugin.dispatch.DispatchTables;
import cc.quarkus.qcc.plugin.layout.Layout;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FunctionElement;
import cc.quarkus.qcc.type.definition.element.GlobalVariableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

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
        ReferenceType type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Thread").validate().getClassType().getReference();
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
        return super.callFunction(functionLiteral(function), args);
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
        return super.callFunction(callTarget, args);
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        Function function = ctxt.getExactFunction(target);
        ctxt.declareForeignFunction(target, function, getCurrentElement());
        List<Value> args = new ArrayList<>(arguments.size() + 1);
        args.add(currentThread());
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(functionLiteral(function), args);
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
        return super.callFunction(callTarget, args);
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        Function function = ctxt.getExactFunction(target);
        ctxt.declareForeignFunction(target, function, getCurrentElement());
        List<Value> args = new ArrayList<>(arguments.size() + 2);
        args.add(currentThread());
        args.add(instance);
        args.addAll(arguments);
        ctxt.enqueue(target);
        super.callFunction(functionLiteral(function), args);
        return instance;
    }

    private SymbolLiteral functionLiteral(final Function function) {
        return ctxt.getLiteralFactory().literalOfSymbol(function.getName(), function.getType());
    }

    private Value expandVirtualDispatch(Value instance, MethodElement target) {
        DispatchTables dt = DispatchTables.get(ctxt);
        DispatchTables.VTableInfo info = dt.getVTableInfo(target.getEnclosingType().validate());
        if (info == null) {
            // No realized invocation targets are possible for this method!
            throw new BlockEarlyTermination(unreachable());
        }
        GlobalVariableElement vtables = dt.getVTablesGlobal();
        if (!vtables.getEnclosingType().equals(originalElement.getEnclosingType())) {
            Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
            section.declareData(null, vtables.getName(), vtables.getType(List.of()));
        }
        int index = dt.getVTableIndex(target);
        Value typeId = load(instanceFieldOf(referenceHandle(instance), Layout.get(ctxt).getObjectTypeIdField()), MemoryAtomicityMode.UNORDERED);
        Value vtable = load(elementOf(globalVariable(dt.getVTablesGlobal()), typeId), MemoryAtomicityMode.UNORDERED);
        return load(memberOf(pointerHandle(bitCast(vtable, info.getType().getPointer())), info.getType().getMember(index)), MemoryAtomicityMode.UNORDERED);
    }

    // Current implementation strategy is "directly indexed itable" in the terminology of [Alpern et al 2001].
    // Very fast dispatch; but significant wasted data space due to sparse per-interface itables[]
    private Value expandInterfaceDispatch(Value instance, MethodElement target) {
        DispatchTables dt = DispatchTables.get(ctxt);
        DispatchTables.ITableInfo info = dt.getITableInfo(target.getEnclosingType().validate());
        if (info == null) {
            // No realized invocation targets are possible for this method!
            // Throwing BlockEarlyTermination(unreachable()) here breaks compilation, so do something hackier...
            // Construct a doomed-to-fail indirect call target by invoking a zero initializer of the right function type.
            ctxt.warning(originalElement, "Unreachable interface invoke of %s compiled to *(null)()", target);
            FunctionType funType = ctxt.getFunctionTypeForElement(target);
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(funType.getPointer());
        }
        Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
        section.declareData(null, info.getGlobal().getName(), info.getGlobal().getType(List.of()));
        int index = dt.getITableIndex(target);
        Value typeId = load(instanceFieldOf(referenceHandle(instance), Layout.get(ctxt).getObjectTypeIdField()), MemoryAtomicityMode.UNORDERED);
        Value itable = load(elementOf(globalVariable(info.getGlobal()), typeId), MemoryAtomicityMode.UNORDERED);
        return load(memberOf(pointerHandle(itable), info.getType().getMember(index)), MemoryAtomicityMode.UNORDERED);
    }
}
