package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.MethodHandle;

abstract class AbstractExactExecutableElement extends AbstractAnnotatedElement implements ExactExecutableElement {
    private final MethodHandle exactMethodBody;

    AbstractExactExecutableElement(final Builder builder) {
        super(builder);
        exactMethodBody = builder.exactMethodBody;
    }

    public boolean hasExactMethodBody() {
        return exactMethodBody != null;
    }

    public MethodHandle getExactMethodBody() {
        return exactMethodBody;
    }

    static abstract class Builder extends AbstractAnnotatedElement.Builder implements ExactExecutableElement.Builder {
        private MethodHandle exactMethodBody;

        public void setExactMethodBody(final MethodHandle methodHandle) {
            exactMethodBody = methodHandle;
        }

        public abstract AbstractExactExecutableElement build();
    }
}
