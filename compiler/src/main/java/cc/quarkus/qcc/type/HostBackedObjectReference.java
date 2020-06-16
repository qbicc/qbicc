package cc.quarkus.qcc.type;

import cc.quarkus.qcc.type.definition.TypeDefinition;

public class HostBackedObjectReference<T> extends ObjectReference {

    public static HostBackedObjectReference<String> newString(String val) {
        throw new UnsupportedOperationException("return new HostBackedObjectReference<>(Core.java.lang.String(), val);");
    }

    HostBackedObjectReference(TypeDefinition typeDefinition, T val) {
        super(typeDefinition);
        this.val = val;
    }

    public T getValue() {
        return this.val;
    }

    @Override
    public String toString() {
        if ( this.val == null ) {
            return "objref:null";
        }
        return this.val.toString();
    }

    private final T val;
}
