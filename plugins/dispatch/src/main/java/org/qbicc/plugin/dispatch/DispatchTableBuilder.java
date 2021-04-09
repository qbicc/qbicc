package org.qbicc.plugin.dispatch;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.reachability.RTAInfo;
import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.ValidatedTypeDefinition;

import java.util.function.Consumer;

public class DispatchTableBuilder implements Consumer<CompilationContext>  {
    @Override
    public void accept(CompilationContext ctxt) {
        RTAInfo info = RTAInfo.get(ctxt);
        DispatchTables tables = DispatchTables.get(ctxt);

        // Starting from java.lang.Object walk down the live class hierarchy and
        //  compute vtable layouts that contain just the methods where ctxt.wasEnqueued is true.
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        ValidatedTypeDefinition jlo = jloDef.validate();
        tables.buildFilteredVTable(jlo);
        info.visitLiveSubclassesPreOrder(jlo, cls -> tables.buildFilteredVTable(cls));

        // Synthesize GlobalVariable for vtables[]
        tables.buildVTablesGlobal(jlo);

        // Now build the interface dispatching structures for the reachable methods
        info.visitLiveInterfaces(i -> tables.buildFilteredITableForInterface(i));
    }
}
