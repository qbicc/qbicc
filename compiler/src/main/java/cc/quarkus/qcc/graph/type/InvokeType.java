package cc.quarkus.qcc.graph.type;

import java.lang.reflect.Method;

import cc.quarkus.qcc.type.MethodDescriptor;

public class InvokeType extends ControlType {
    public InvokeType() {

    }

    public void setMethodDescriptor(MethodDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public ConcreteType<?> getReturnType() {
        return this.descriptor.getReturnType();
    }

    @Override
    public String label() {
        if ( this.descriptor == null ) {
            return "<unknown>";
        }
        return this.descriptor.getOwner() + "::" + this.descriptor.getName();
    }

    private MethodDescriptor descriptor;
}
