package cc.quarkus.qcc.plugin.lowering;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.MemoryAccessMode;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.plugin.dispatch.DispatchTables;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public class InvocationLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public InvocationLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        Function function = ctxt.getExactFunction(target);
        ctxt.declareForeignFunction(target, function, getCurrentElement());
        List<Value> args = new ArrayList<>(arguments.size() + 1);
        args.add(ctxt.getCurrentThreadValue());
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(functionLiteral(function), args);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        Value callTarget;
        Function invokeTarget = ctxt.getExactFunction(target);
        if (kind == DispatchInvocation.Kind.INTERFACE) {
            ctxt.warning(getLocation(), "interface invocation not supported yet");
            // but continue with bogus call target just to see what would happen
            callTarget = functionLiteral(invokeTarget);
        } else if (kind == DispatchInvocation.Kind.VIRTUAL) {
            callTarget = expandVirtualDispatch(instance, target, invokeTarget);
        } else {
            callTarget = functionLiteral(invokeTarget);
        }
        List<Value> args = new ArrayList<>(arguments.size() + 2);
        args.add(ctxt.getCurrentThreadValue());
        args.add(instance);
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(callTarget, args);
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        Function function = ctxt.getExactFunction(target);
        ctxt.declareForeignFunction(target, function, getCurrentElement());
        List<Value> args = new ArrayList<>(arguments.size() + 1);
        args.add(ctxt.getCurrentThreadValue());
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(functionLiteral(function), args);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        Value callTarget;
        Function invokeTarget = ctxt.getExactFunction(target);
        if (kind == DispatchInvocation.Kind.INTERFACE) {
            ctxt.warning(getLocation(), "interface invocation not supported yet");
            // but continue with bogus call target just to see what would happen
            callTarget = functionLiteral(invokeTarget);
        } else if (kind == DispatchInvocation.Kind.VIRTUAL) {
            callTarget = expandVirtualDispatch(instance, target, invokeTarget);
        } else {
            callTarget = functionLiteral(invokeTarget);
        }
        List<Value> args = new ArrayList<>(arguments.size() + 2);
        args.add(ctxt.getCurrentThreadValue());
        args.add(instance);
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(callTarget, args);
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        Function function = ctxt.getExactFunction(target);
        ctxt.declareForeignFunction(target, function, getCurrentElement());
        List<Value> args = new ArrayList<>(arguments.size() + 2);
        args.add(ctxt.getCurrentThreadValue());
        args.add(instance);
        args.addAll(arguments);
        ctxt.enqueue(target);
        super.callFunction(functionLiteral(function), args);
        return instance;
    }

    // TODO: Ensuring the vtable is added to its class's static data really belongs in the lowering of new to object allocation...
    //       Move this to NoGcBasicBlockBuilder when https://github.com/quarkuscc/qcc/pull/141/ is merged
    public Value new_(final ClassObjectType type) {
        DispatchTables dt = DispatchTables.get(ctxt);
        dt.getSymbolForVTablePtr(type.getDefinition().validate()); // has the side effect of putting vtable into static data of defining class's object file
        return super.new_(type);
    }

    private SymbolLiteral functionLiteral(final Function function) {
        return ctxt.getLiteralFactory().literalOfSymbol(function.getName(), function.getType());
    }

    /*
     * Implement naive scheme where first word of an object simply contains the vtable pointer.
     * Eventually we will need to do more (mask off bottom bits, or load an index and then index into a vtable table, etc).
     */
    private Value expandVirtualDispatch(Value instance, MethodElement target, Function invokeTarget) {
        DispatchTables dt = DispatchTables.get(ctxt);
        Value vtable = pointerLoad(bitCast(instance, ctxt.getTypeSystem().getVoidType().getPointer().getPointer().getPointer()), MemoryAccessMode.PLAIN, MemoryAtomicityMode.UNORDERED);
        Value index = ctxt.getLiteralFactory().literalOf(dt.getVTableIndex(target));
        Value fptr = pointerLoad(add(vtable, index), MemoryAccessMode.PLAIN, MemoryAtomicityMode.UNORDERED);
        return bitCast(fptr, invokeTarget.getType().getPointer());
    }
}
