package org.qbicc.plugin.objectmonitor;

import java.util.List;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;

/**
 * A graph factory which generates calls to runtime helpers for object monitor
 * bytecodes: monitorenter and monitorexit
 */
public class ObjectMonitorBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final MethodElement monitorEnterMethod;
    private final MethodElement monitorExitMethod;

    public ObjectMonitorBasicBlockBuilder(FactoryContext fc, BasicBlockBuilder delegate) {
        super(delegate);
        LoadedTypeDefinition jlo = getContext().getBootstrapClassContext().findDefinedType("java/lang/Object").load();
        int idx = jlo.findSingleMethodIndex(me -> me.nameEquals("monitorEnter"));
        if (idx == -1) {
            throw new IllegalStateException();
        }
        monitorEnterMethod = jlo.getMethod(idx);
        idx = jlo.findSingleMethodIndex(me -> me.nameEquals("monitorExit"));
        if (idx == -1) {
            throw new IllegalStateException();
        }
        monitorExitMethod = jlo.getMethod(idx);
    }

    public Node monitorEnter(final Value object) {
        BasicBlockBuilder fb = getFirstBuilder();
        return fb.call(fb.exactMethodOf(object, monitorEnterMethod), List.of());
    }

    public Node monitorExit(final Value object) {
        BasicBlockBuilder fb = getFirstBuilder();
        return fb.call(fb.exactMethodOf(object, monitorExitMethod), List.of());
    }
}
