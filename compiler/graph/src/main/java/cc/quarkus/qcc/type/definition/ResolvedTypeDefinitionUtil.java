package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

final class ResolvedTypeDefinitionUtil {
    private ResolvedTypeDefinitionUtil() {}

    static final MethodElement NOT_FOUND = new MethodElementStub();
    static final MethodElement END_OF_SEARCH = new MethodElementStub();

    static class MethodElementStub implements MethodElement {
        public DefinedTypeDefinition getEnclosingType() {
            return null;
        }

        public int getModifiers() {
            return 0;
        }

        public int getParameterCount() {
            return 0;
        }

        public ParameterElement getParameter(final int index) throws IndexOutOfBoundsException {
            return null;
        }

        public MethodBody getResolvedMethodBody() throws ResolutionFailedException {
            return null;
        }

        public ValueType getReturnType() {
            return null;
        }

        public String getSourceFileName() {
            return null;
        }

        public boolean hasClass2ReturnType() {
            return false;
        }

        public MethodDescriptor getDescriptor() {
            return null;
        }

        public int getVisibleAnnotationCount() {
            return 0;
        }

        public Annotation getVisibleAnnotation(final int index) throws IndexOutOfBoundsException {
            return null;
        }

        public int getInvisibleAnnotationCount() {
            return 0;
        }

        public Annotation getInvisibleAnnotation(final int index) throws IndexOutOfBoundsException {
            return null;
        }

        public boolean hasName() {
            return false;
        }

        public String getName() {
            return null;
        }

        public boolean nameEquals(final String name) {
            return false;
        }

        public boolean hasMethodBody() {
            return false;
        }

        public MethodHandle getMethodBody() {
            return null;
        }

        public boolean hasVirtualMethodBody() {
            return false;
        }

        public MethodHandle getVirtualMethodBody() {
            return null;
        }
    }
}
