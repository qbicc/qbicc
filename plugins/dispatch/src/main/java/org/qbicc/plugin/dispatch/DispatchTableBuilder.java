package org.qbicc.plugin.dispatch;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.util.function.Consumer;

public class DispatchTableBuilder implements Consumer<CompilationContext>  {
    @Override
    public void accept(CompilationContext ctxt) {
        ReachabilityInfo info = ReachabilityInfo.get(ctxt);
        DispatchTables tables = DispatchTables.get(ctxt);

        // Starting from java.lang.Object walk down the live class hierarchy and
        //  compute vtable layouts that contain just the methods where ctxt.wasEnqueued is true.
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        LoadedTypeDefinition jlo = jloDef.load();
        tables.buildFilteredVTable(jlo);
        info.visitReachableSubclassesPreOrder(jlo, tables::buildFilteredVTable);

        // Synthesize GlobalVariable for vtables[] and itable_dict[]
        tables.buildVTablesGlobal(jlo);
        tables.buildITablesGlobal(jlo);

        // Synthesize GlobalVariable for rtinit[]
        tables.buildRTInitGlobal(jlo);

        // Now build the interface dispatching structures for the reachable methods
        info.visitReachableInterfaces(tables::buildFilteredITableForInterface);
    }
}
