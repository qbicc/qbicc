package org.qbicc.plugin.objectmonitor;
import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.element.MethodElement;

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
        return generateObjectMonitorFunctionCall(object, monitorEnterFunctionName);
    }

    public Node monitorExit(final Value object) {
        return generateObjectMonitorFunctionCall(object, monitorExitFunctionName);
    }
    
    private Value generateObjectMonitorFunctionCall(final Value object, String functionName) {
        MethodElement methodElement = ctxt.getVMHelperMethod(functionName);
        List<Value> args = List.of(object);
        return getFirstBuilder().call(getFirstBuilder().staticMethod(methodElement), args);
    }
}
