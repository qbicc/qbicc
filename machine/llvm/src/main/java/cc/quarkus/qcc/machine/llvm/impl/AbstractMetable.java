package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.Metable;
import cc.quarkus.qcc.machine.llvm.LLValue;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
abstract class AbstractMetable extends AbstractCommentable implements Metable {
    MetaItem lastMetaItem;

    AbstractMetable() {}

    public Metable meta(final String name, final LLValue data) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("data", data);
        lastMetaItem = new MetaItem(lastMetaItem, name, (AbstractValue) data);
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
        final MetaItem prev;
        final String name;
        final AbstractValue metaValue;

        MetaItem(final MetaItem prev, final String name, final AbstractValue metaValue) {
            this.prev = prev;
            this.name = name;
            this.metaValue = metaValue;
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            if (prev != null) {
                prev.appendTo(target);
            }
            target.append(',').append(' ').append('!').append(name).append(' ');
            metaValue.appendTo(target);
            return target;
        }
    }
}
