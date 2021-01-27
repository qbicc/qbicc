package cc.quarkus.qcc.plugin.objectmonitor;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.TypeSystem;

/**
 * A graph factory which generates calls to runtime helpers for object monitor
 * bytecodes: monitorenter and monitorexit
 */
public class ObjectMonitorBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ObjectMonitorBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Node monitorEnter(final Value object) {
        return super.monitorEnter(object);

//        final String monitorEnterFunctionName = "monitor_enter";
//        return generateObjectMonitorFunctionCall(object, monitorEnterFunctionName);
    }

    public Node monitorExit(final Value object) {
        return super.monitorExit(object);

//        final String monitorExitFunctionName = "monitor_exit";
//        return generateObjectMonitorFunctionCall(object, monitorExitFunctionName);
    }
    
    private Value generateObjectMonitorFunctionCall(final Value object, String functionName) {
        TypeSystem ts = ctxt.getTypeSystem();
        FunctionType functionType = ts.getFunctionType(ts.getVoidType(), object.getType());
        Value callTarget = ctxt.getLiteralFactory().literalOfSymbol(functionName, functionType);
        List<Value> args = List.of(object);
        return super.callFunction(callTarget, args);
    }
}
