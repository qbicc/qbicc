package cc.quarkus.qcc.plugin.dispatch;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

import java.util.function.Consumer;

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

        // Emit the master table of all program vtables in the object file for java.lang.Object
        tables.emitVTableTable(jlo);
    }
}