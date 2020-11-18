package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.MethodHandle;

abstract class AbstractExactExecutableElement extends AbstractAnnotatedElement implements ExecutableElement {
    private final MethodHandle exactMethodBody;

    AbstractExactExecutableElement(final Builder builder) {
        super(builder);
        exactMethodBody = builder.exactMethodBody;
    }

    public boolean hasMethodBody() {
        return exactMethodBody != null;
    }

    public MethodHandle getMethodBody() {
        return exactMethodBody;
    }

    static abstract class Builder extends AbstractAnnotatedElement.Builder implements ExecutableElement.Builder {
        private MethodHandle exactMethodBody;

        public void setExactMethodBody(final MethodHandle methodHandle) {
            exactMethodBody = methodHandle;
        }

        public abstract AbstractExactExecutableElement build();
    }
}
