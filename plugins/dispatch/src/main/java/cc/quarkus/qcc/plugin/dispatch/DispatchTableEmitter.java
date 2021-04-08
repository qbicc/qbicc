package cc.quarkus.qcc.plugin.dispatch;

import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

public class DispatchTableEmitter implements Consumer<CompilationContext>  {

    @Override
    public void accept(CompilationContext ctxt) {
        RTAInfo info = RTAInfo.get(ctxt);
        DispatchTables tables = DispatchTables.get(ctxt);

        // Walk down the live class hierarchy and emit vtables for each class
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        ValidatedTypeDefinition jlo = jloDef.validate();
        tables.emitVTable(jlo);
        info.visitLiveSubclassesPreOrder(jlo, cls -> tables.emitVTable(cls));

        // Emit the table of all program vtables in the object file for java.lang.Object
        tables.emitVTableTable(jlo);

        // Emit all interface dispatching tables
        tables.emitInterfaceTables(info);
    }
}