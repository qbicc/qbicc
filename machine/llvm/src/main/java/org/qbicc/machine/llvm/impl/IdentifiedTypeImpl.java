package org.qbicc.machine.llvm.impl;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.IdentifiedType;

import java.io.IOException;

final class IdentifiedTypeImpl extends AbstractCommentable implements IdentifiedType {
    private final String name;
    private AbstractValue type;

    IdentifiedTypeImpl(final String name) {
        this.name = name;
    }

    public Appendable appendTo(Appendable target) throws IOException {
        target.append('%').append(name).append(" = type ");

        if (type != null) {
            type.appendTo(target);
        } else {
            target.append("opaque");
        }

        return appendTrailer(target);
    }

    public LLValue asTypeRef() {
        return new Ref();
    }

    public IdentifiedType type(LLValue type) {
        this.type = (AbstractValue) type;
        return this;
    }

    public IdentifiedType comment(String comment) {
        return (IdentifiedType) super.comment(comment);
    }

    private final class Ref extends AbstractValue {
        public Appendable appendTo(Appendable target) throws IOException {
            return target.append('%').append(name);
        }
    }
}
