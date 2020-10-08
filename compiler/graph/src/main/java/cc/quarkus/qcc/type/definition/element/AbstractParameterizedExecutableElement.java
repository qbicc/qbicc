package cc.quarkus.qcc.type.definition.element;

import java.util.Arrays;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.interpreter.JavaVM;
import cc.quarkus.qcc.type.descriptor.ParameterizedExecutableDescriptor;

abstract class AbstractParameterizedExecutableElement extends AbstractExactExecutableElement implements ParameterizedExecutableElement {
    private final ParameterElement[] parameters;

    AbstractParameterizedExecutableElement(final Builder builder) {
        super(builder);
        int cnt = builder.parameterCount;
        parameters = cnt == 0 ? ParameterElement.NO_PARAMETERS : Arrays.copyOf(builder.parameters, cnt);
    }

    public int getParameterCount() {
        return parameters.length;
    }

    public ParameterElement getParameter(final int index) throws IndexOutOfBoundsException {
        return parameters[index];
    }

    static abstract class Builder extends AbstractExactExecutableElement.Builder implements ParameterizedExecutableElement.Builder {
        ParameterElement[] parameters;
        int parameterCount;

        public void expectParameterCount(final int count) {
            ParameterElement[] parameters = this.parameters;
            if (parameters == null) {
                this.parameters = new ParameterElement[count];
            } else if (parameters.length <= count) {
                this.parameters = Arrays.copyOf(parameters, count);
            }
        }

        public void addParameter(final ParameterElement parameter) {
            if (parameter == null) {
                return;
            }
            ParameterElement[] parameters = this.parameters;
            int cnt = this.parameterCount;
            if (parameters == null) {
                this.parameters = parameters = new ParameterElement[4];
            } else if (parameters.length == cnt) {
                this.parameters = parameters = Arrays.copyOf(parameters, cnt + (cnt + 1 >>> 1));
            }
            parameters[cnt] = parameter;
            this.parameterCount = cnt + 1;
        }

        ParameterizedExecutableDescriptor getParameterizedExecutableDescriptor() {
            int cnt = this.parameterCount;
            Type[] types = new Type[cnt];
            for (int i = 0; i < cnt; i ++) {
                types[i] = parameters[i].getType();
            }
            return JavaVM.requireCurrent().getParameterizedExecutableDescriptor(types);
        }

        public abstract AbstractParameterizedExecutableElement build();
    }
}
