package cc.quarkus.qcc.plugin.gc.nogc;

import java.util.function.Consumer;

import cc.quarkus.qcc.type.TypeSystem;

/**
 *
 */
public class NoGcTypeSystemConfigurator implements Consumer<TypeSystem.Builder> {
    public NoGcTypeSystemConfigurator() {}

    public void accept(final TypeSystem.Builder builder) {
        // for now, references are essentially pointers
        builder.setReferenceSize(builder.getPointerSize());
        builder.setReferenceAlignment(builder.getPointerAlignment());
    }
}
