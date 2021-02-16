package cc.quarkus.qcc.plugin.dispatch;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

import java.util.function.Consumer;

public class VTableBuilder implements Consumer<CompilationContext>  {
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
    }
}
