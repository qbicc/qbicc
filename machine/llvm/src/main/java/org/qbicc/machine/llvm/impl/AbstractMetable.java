package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.Metable;
import org.qbicc.machine.llvm.LLValue;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
abstract class AbstractMetable extends AbstractCommentable implements Metable {
    MetaItem lastMetaItem;
    boolean metaHasComma;

    AbstractMetable() {}

    public Metable meta(final String name, final LLValue data) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("data", data);
        lastMetaItem = new MetaItem(this, lastMetaItem, name, (AbstractValue) data);
        return this;
    }

    public Metable comment(final String comment) {
        super.comment(comment);
        return this;
    }

    Appendable appendTrailer(final Appendable target) throws IOException {
        appendMeta(target);
        return super.appendTrailer(target);
    }

    Appendable appendMeta(final Appendable target) throws IOException {
        final MetaItem item = this.lastMetaItem;
        if (item != null) {
            item.appendTo(target);
        }
        return target;
    }

    static final class MetaItem extends AbstractEmittable {
        final AbstractMetable parent;
        final MetaItem prev;
        final String name;
        final AbstractValue metaValue;

        MetaItem(final AbstractMetable parent, final MetaItem prev, final String name, final AbstractValue metaValue) {
            this.parent = parent;
            this.prev = prev;
            this.name = name;
            this.metaValue = metaValue;
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            if (prev != null) {
                prev.appendTo(target);
            }
            if (parent.metaHasComma) {
                target.append(',');
            }
            target.append(' ').append('!').append(name).append(' ');
            metaValue.appendTo(target);
            return target;
        }
    }
}
