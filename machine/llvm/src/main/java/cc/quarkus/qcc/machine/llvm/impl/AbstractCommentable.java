package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.Commentable;
import io.smallrye.common.constraint.Assert;

abstract class AbstractCommentable extends AbstractEmittable implements Commentable {
    Comment lastComment;

    public Commentable comment(final String comment) {
        Assert.checkNotNullParam("comment", comment);
        if (comment.indexOf('\n') != -1 || comment.indexOf('\r') != -1) {
            throw new IllegalArgumentException("Multi-line comments not supported");
        }
        lastComment = new Comment(lastComment, comment);
        return this;
    }

    Appendable appendTrailer(final Appendable target) throws IOException {
        return appendComment(target);
    }

    Appendable appendComment(final Appendable target) throws IOException {
        final Comment item = lastComment;
        if (item != null) {
            item.appendTo(target);
        }
        return target;
    }

    private static final class Comment extends AbstractEmittable{
        final Comment prev;
        final String text;

        Comment(final Comment prev, final String text) {
            this.prev = prev;
            this.text = text;
        }

        public Appendable appendTo(final Appendable target) throws IOException {
            if (prev != null) {
                prev.appendTo(target);
                target.append(System.lineSeparator());
                target.append(' ').append(' ').append(' ');
            }
            target.append(' ').append(';').append(' ').append(text);
            return target;
        }
    }
}
