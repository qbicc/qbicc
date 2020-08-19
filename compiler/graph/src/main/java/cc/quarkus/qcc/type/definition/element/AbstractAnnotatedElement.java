package cc.quarkus.qcc.type.definition.element;

import java.util.Arrays;

import cc.quarkus.qcc.type.annotation.Annotation;

abstract class AbstractAnnotatedElement extends AbstractBasicElement implements AnnotatedElement {
    private final Annotation[] visibleAnnotations;
    private final Annotation[] invisibleAnnotations;

    AbstractAnnotatedElement(Builder builder) {
        super(builder);
        int cnt = builder.visibleCnt;
        visibleAnnotations = cnt == 0 ? Annotation.NO_ANNOTATIONS : Arrays.copyOf(builder.visibleAnnotations, cnt);
        cnt = builder.invisibleCnt;
        invisibleAnnotations = cnt == 0 ? Annotation.NO_ANNOTATIONS : Arrays.copyOf(builder.invisibleAnnotations, cnt);
    }

    public int getVisibleAnnotationCount() {
        return visibleAnnotations.length;
    }

    public Annotation getVisibleAnnotation(final int index) throws IndexOutOfBoundsException {
        return visibleAnnotations[index];
    }

    public int getInvisibleAnnotationCount() {
        return invisibleAnnotations.length;
    }

    public Annotation getInvisibleAnnotation(final int index) throws IndexOutOfBoundsException {
        return invisibleAnnotations[index];
    }

    abstract static class Builder extends AbstractBasicElement.Builder implements AnnotatedElement.Builder {
        Annotation[] visibleAnnotations;
        int visibleCnt;
        Annotation[] invisibleAnnotations;
        int invisibleCnt;

        public void expectVisibleAnnotationCount(final int count) {
            Annotation[] annotations = this.visibleAnnotations;
            if (annotations == null) {
                this.visibleAnnotations = new Annotation[count];
            } else if (annotations.length <= count) {
                this.visibleAnnotations = Arrays.copyOf(annotations, count);
            }
        }

        public void addVisibleAnnotation(final Annotation annotation) {
            if (annotation == null) {
                return;
            }
            Annotation[] annotations = this.visibleAnnotations;
            int cnt = this.visibleCnt;
            if (annotations == null) {
                this.visibleAnnotations = annotations = new Annotation[4];
            } else if (annotations.length == cnt) {
                this.visibleAnnotations = annotations = Arrays.copyOf(annotations, cnt + (cnt + 1 >>> 1));
            }
            annotations[cnt] = annotation;
            this.visibleCnt = cnt + 1;
        }

        public void expectInvisibleAnnotationCount(final int count) {
            Annotation[] annotations = this.invisibleAnnotations;
            if (annotations == null) {
                this.invisibleAnnotations = new Annotation[count];
            } else if (annotations.length <= count) {
                this.invisibleAnnotations = Arrays.copyOf(annotations, count);
            }
        }

        public void addInvisibleAnnotation(final Annotation annotation) {
            if (annotation == null) {
                return;
            }
            Annotation[] annotations = this.invisibleAnnotations;
            int cnt = this.invisibleCnt;
            if (annotations == null) {
                this.invisibleAnnotations = annotations = new Annotation[4];
            } else if (annotations.length == cnt) {
                this.invisibleAnnotations = annotations = Arrays.copyOf(annotations, cnt + (cnt + 1 >>> 1));
            }
            annotations[cnt] = annotation;
            this.invisibleCnt = cnt + 1;
        }

        public abstract AbstractAnnotatedElement build();
    }
}
