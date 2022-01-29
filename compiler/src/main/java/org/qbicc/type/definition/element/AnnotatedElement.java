package org.qbicc.type.definition.element;

import java.util.ArrayList;
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
        void addVisibleAnnotations(List<Annotation> annotations);

        void addInvisibleAnnotations(List<Annotation> annotations);

        AnnotatedElement build();

        interface Delegating extends BasicElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void addVisibleAnnotations(List<Annotation> annotations) {
                getDelegate().addVisibleAnnotations(annotations);
            }

            @Override
            default void addInvisibleAnnotations(List<Annotation> annotations) {
                getDelegate().addInvisibleAnnotations(annotations);
            }

            @Override
            default AnnotatedElement build() {
                return getDelegate().build();
            }
        }
    }

    static abstract class BuilderImpl extends BasicElement.BuilderImpl implements Builder {
        List<Annotation> visibleAnnotations = List.of();
        List<Annotation> invisibleAnnotations = List.of();

        BuilderImpl(int index) {
            super(index);
        }

        BuilderImpl(final AnnotatedElement original) {
            super(original);
            visibleAnnotations = original.visibleAnnotations;
            invisibleAnnotations = original.invisibleAnnotations;
        }

        public void addVisibleAnnotations(List<Annotation> annotations) {
            Assert.checkNotNullParam("annotations", annotations);
            if (visibleAnnotations == null) {
                visibleAnnotations = annotations;
            } else if (!annotations.isEmpty()) {
                ArrayList<Annotation> tmp = new ArrayList<>();
                tmp.addAll(visibleAnnotations);
                tmp.addAll(annotations);
                visibleAnnotations = List.copyOf(tmp);
            }
        }

        public void addInvisibleAnnotations(List<Annotation> annotations) {
            Assert.checkNotNullParam("annotations", annotations);
            if (invisibleAnnotations == null) {
                invisibleAnnotations = annotations;
            } else if (!annotations.isEmpty()) {
                ArrayList<Annotation> tmp = new ArrayList<>();
                tmp.addAll(invisibleAnnotations);
                tmp.addAll(annotations);
                invisibleAnnotations = List.copyOf(tmp);
            }
        }

        public abstract AnnotatedElement build();
    }
}
