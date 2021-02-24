package cc.quarkus.qcc.plugin.objectmonitor;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * A graph factory which generates calls to runtime helpers for object monitor
 * bytecodes: monitorenter and monitorexit
 */
public class ObjectMonitorBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    private final String monitorEnterFunctionName = "monitor_enter";
    private final String monitorExitFunctionName = "monitor_exit";

    public ObjectMonitorBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Node monitorEnter(final Value object) {
        return super.monitorEnter(object);

        // TODO disabled
//        return generateObjectMonitorFunctionCall(object, monitorEnterFunctionName);
    }

    public Node monitorExit(final Value object) {
        return super.monitorExit(object);

        // TODO disabled
//        return generateObjectMonitorFunctionCall(object, monitorExitFunctionName);
    }
    
    private Value generateObjectMonitorFunctionCall(final Value object, String functionName) {
        MethodElement methodElement = ctxt.getVMHelperMethod(functionName);
        ctxt.registerEntryPoint(methodElement);
        Function function = ctxt.getExactFunction(methodElement);

        Value callTarget = ctxt.getLiteralFactory().literalOfSymbol(function.getName(), function.getType());
        List<Value> args = List.of(object);
        return super.callFunction(callTarget, args);
    }
}
