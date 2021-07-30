package org.qbicc.plugin.stringpool;

import org.qbicc.context.CompilationContext;

import java.util.function.Consumer;

public class StringPoolEmitter implements Consumer<CompilationContext> {
    @Override
    public void accept(CompilationContext context) {
        StringPool stringPool = StringPool.get(context);
        stringPool.emit(context);
    }
}
