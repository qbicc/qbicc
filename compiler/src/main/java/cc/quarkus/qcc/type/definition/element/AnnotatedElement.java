package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.annotation.Annotation;

/**
 * A program element that is annotatable.  Annotations are usually parsed by the JDK, but plugins also
 * need an easy way to examine them.
 */
public interface AnnotatedElement extends BasicElement {
    int getVisibleAnnotationCount();

    Annotation getVisibleAnnotation(int index) throws IndexOutOfBoundsException;

    int getInvisibleAnnotationCount();

    Annotation getInvisibleAnnotation(int index) throws IndexOutOfBoundsException;

    interface Builder extends BasicElement.Builder {
        void expectVisibleAnnotationCount(int count);

        void addVisibleAnnotation(Annotation annotation);

        void expectInvisibleAnnotationCount(int count);

        void addInvisibleAnnotation(Annotation annotation);

        AnnotatedElement build();
    }
}
