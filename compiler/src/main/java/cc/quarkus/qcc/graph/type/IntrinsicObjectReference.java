package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.Core;
import cc.quarkus.qcc.type.Sentinel;
import cc.quarkus.qcc.type.TypeDefinition;

public class IntrinsicObjectReference<T> extends ObjectReference {

    public static IntrinsicObjectReference<String> newString(String val) {
        return new IntrinsicObjectReference<>(Core.java.lang.String(), val);
    }

    public static IntrinsicObjectReference<Object> newNull() {
        return new IntrinsicObjectReference<>(Core.java.lang.Object(), Sentinel.Null.NULL);
    }

    private IntrinsicObjectReference(TypeDefinition typeDefinition, T val) {
        super(typeDefinition);
        this.val = val;
    }

    public T getValue() {
        return this.val;
    }

    @Override
    public String toString() {
        return this.val.toString();
    }

    private final T val;
}
