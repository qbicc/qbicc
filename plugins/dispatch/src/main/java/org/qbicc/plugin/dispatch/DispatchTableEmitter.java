package org.qbicc.plugin.dispatch;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.reachability.RTAInfo;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;

public class DispatchTableEmitter implements Consumer<CompilationContext>  {

    @Override
    public void accept(CompilationContext ctxt) {
        RTAInfo info = RTAInfo.get(ctxt);
        DispatchTables tables = DispatchTables.get(ctxt);

        // Walk down the live class hierarchy and emit vtables for each class
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        LoadedTypeDefinition jlo = jloDef.load();
        tables.emitVTable(jlo);
        info.visitReachableSubclassesPreOrder(jlo, tables::emitVTable);

        // Walk down the live class hierarchy and emit the itables for each class
        tables.emitITables(jlo);
        info.visitReachableSubclassesPreOrder(jlo, tables::emitITables);

        // Emit the root tables of all program vtables and itables in the object file for java.lang.Object
        tables.emitVTableTable(jlo);
        tables.emitITableTable(jlo);
    }
}