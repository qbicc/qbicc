package cc.quarkus.qcc.plugin.lowering;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
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
        if (kind == DispatchInvocation.Kind.INTERFACE || kind == DispatchInvocation.Kind.VIRTUAL) {
            ctxt.warning(getLocation(), "Virtual invocation not supported yet");
            // but continue anyway just to see what would happen
        }
        Function function = ctxt.getExactFunction(target);
        List<Value> args = new ArrayList<>(arguments.size() + 2);
        args.add(ctxt.getCurrentThreadValue());
        args.add(instance);
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(functionLiteral(function), args);
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
        if (kind == DispatchInvocation.Kind.INTERFACE || kind == DispatchInvocation.Kind.VIRTUAL) {
            ctxt.warning(getLocation(), "Virtual invocation not supported yet");
            // but continue anyway just to see what would happen
        }
        Function function = ctxt.getExactFunction(target);
        List<Value> args = new ArrayList<>(arguments.size() + 2);
        args.add(ctxt.getCurrentThreadValue());
        args.add(instance);
        args.addAll(arguments);
        ctxt.enqueue(target);
        return super.callFunction(functionLiteral(function), args);
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
}
