package cc.quarkus.qcc.compiler.backend.llvm.generic;

import cc.quarkus.qcc.compiler.backend.api.BackEnd;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.type.universe.Universe;

public final class LLVMBackEnd implements BackEnd {
    public LLVMBackEnd() {}

    public String getName() {
        return "llvm-generic";
    }

    public void compile(final Universe universe) {
        final Context context = Context.requireCurrent();
        Context.error(null, "This is a test error! Compilation shall fail!");
        // done?
        return;
    }
}
