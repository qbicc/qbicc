package cc.quarkus.qcc.plugin.dispatch;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;

import java.util.function.Consumer;

public class VTableBuilder implements Consumer<CompilationContext>  {
    @Override
    public void accept(CompilationContext ctxt) {
        RTAInfo info = RTAInfo.get(ctxt);
        // TODO: Starting from java.lang.Object walk down the live class hierarchy we get
        //  from info and compute vtable layouts that contain just the methods where ctxt.wasEnqueued is true.
    }
}
