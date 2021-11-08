package org.qbicc.type.definition.element;

import java.util.List;

import org.qbicc.type.annotation.Annotation;
import io.smallrye.common.constraint.Assert;

/**
 * A program element that is annotatable.  Annotations are usually parsed by the JDK, but plugins also
 * need an easy way to examine them.
 */
public abstract class AnnotatedElement extends BasicElement {
    private final List<Annotation> visibleAnnotations;
    private final List<Annotation> invisibleAnnotations;

    AnnotatedElement() {
        super();
        visibleAnnotations = null;
        invisibleAnnotations = null;
    }

    AnnotatedElement(BuilderImpl builder) {
        super(builder);
        visibleAnnotations = builder.visibleAnnotations;
        invisibleAnnotations = builder.invisibleAnnotations;
    }

    public List<Annotation> getVisibleAnnotations() {
        return visibleAnnotations;
    }

    public List<Annotation> getInvisibleAnnotations() {
        return invisibleAnnotations;
    }

    public interface Builder extends BasicElement.Builder {
        void setVisibleAnnotations(List<Annotation> annotations);

        void setInvisibleAnnotations(List<Annotation> annotations);

        AnnotatedElement build();
    }

    static abstract class BuilderImpl extends BasicElement.BuilderImpl implements Builder {
        List<Annotation> visibleAnnotations = List.of();
        List<Annotation> invisibleAnnotations = List.of();

        BuilderImpl() {}

        BuilderImpl(final AnnotatedElement original) {
            super(original);
            visibleAnnotations = original.visibleAnnotations;
            invisibleAnnotations = original.invisibleAnnotations;
        }

        public void setVisibleAnnotations(List<Annotation> annotations) {
            visibleAnnotations = Assert.checkNotNullParam("annotations", annotations);
        }

        public void setInvisibleAnnotations(List<Annotation> annotations) {
            invisibleAnnotations = Assert.checkNotNullParam("annotations", annotations);
        }

        public abstract AnnotatedElement build();
    }
}
