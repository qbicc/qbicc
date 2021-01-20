package cc.quarkus.qcc.machine.llvm.impl;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.debuginfo.MetadataTuple;

import java.io.IOException;

final class MetadataTupleImpl extends AbstractMetadataNode implements MetadataTuple {
    private Element lastElement;

    MetadataTupleImpl(final int index) {
        super(index);
    }

    MetadataTupleImpl(final String name) {
        super(name);
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);
        target.append('!').append('{');
        if (lastElement != null)
            lastElement.appendTo(target);
        target.append('}');

        return appendTrailer(target);
    }

    public MetadataTuple comment(final String comment) {
        return (MetadataTuple)super.comment(comment);
    }

    public MetadataTuple elem(final LLValue type, final LLValue value) {
        lastElement = new Element(this, lastElement, (AbstractValue) type, (AbstractValue) value);
        return this;
    }

    static final class Element extends AbstractEmittable {
        final MetadataTupleImpl tuple;
        final Element prev;
        final AbstractValue type;
        final AbstractValue value;

        public Element(final MetadataTupleImpl tuple, final Element prev, final AbstractValue type, final AbstractValue value) {
            this.tuple = tuple;
            this.prev = prev;
            this.type = type;
            this.value = value;
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            if (prev != null) {
                prev.appendTo(target);
                target.append(',').append(' ');
            }

            if (type != null) {
                type.appendTo(target);
                target.append(' ');
            }

            value.appendTo(target);
            return target;
        }
    }
}
