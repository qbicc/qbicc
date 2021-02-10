package cc.quarkus.qcc.machine.llvm.impl;

import cc.quarkus.qcc.machine.llvm.debuginfo.DISubrange;

import java.io.IOException;

final class DISubrangeImpl extends AbstractMetadataNode implements DISubrange {
    private final long count;

    DISubrangeImpl(final int index, final long count) {
        super(index);
        this.count = count;
    }

    public Appendable appendTo(Appendable target) throws IOException {
        super.appendTo(target);

        target.append("!DISubrange(count: ");
        appendDecimal(target, count);

        target.append(')');
        return appendTrailer(target);
    }

    public DISubrange comment(String comment) {
        return (DISubrange) super.comment(comment);
    }
}
