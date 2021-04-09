package org.qbicc.plugin.dispatch;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.reachability.RTAInfo;
import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.ValidatedTypeDefinition;

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