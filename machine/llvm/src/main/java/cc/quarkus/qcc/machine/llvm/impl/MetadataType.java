package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

public class MetadataType extends AbstractValue {
    private final AbstractValue type;

    MetadataType(final AbstractValue type) {
        this.type = type;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append("metadata");
        if (type != null) {
            target.append(' ');
            type.appendTo(target);
        }
        return target;
    }
}
