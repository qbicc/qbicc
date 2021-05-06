package org.qbicc.plugin.instanceofcheckcast;

import java.util.function.Consumer;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;

public class SupersDisplayEmitter implements Consumer<CompilationContext>  {

    @Override
    public void accept(CompilationContext ctxt) {
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);

        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        LoadedTypeDefinition jlo = jloDef.load();

        // emit the typeid[] into Object's file
        tables.emitTypeIdTable(jlo);

        // TODO: enable this when the final mising <clinit>s are tracked down
        // emit the initialization data
        // tables.emitClinitStateTable(jlo);
    }
}