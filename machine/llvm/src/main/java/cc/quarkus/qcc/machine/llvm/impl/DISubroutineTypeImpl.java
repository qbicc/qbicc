package cc.quarkus.qcc.machine.llvm.impl;

import cc.quarkus.qcc.machine.llvm.debuginfo.DISubroutineType;

import java.io.IOException;

public class DISubroutineTypeImpl extends AbstractMetadataNode implements DISubroutineType {
    private final AbstractValue types;

    DISubroutineTypeImpl(final int index, final AbstractValue types) {
        super(index);
        this.types = types;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append("!DISubroutineType(types: ");
        types.appendTo(target);
        target.append(')');
        return appendTrailer(target);
    }

    public DISubroutineType comment(final String comment) {
        return (DISubroutineType) super.comment(comment);
    }
}
