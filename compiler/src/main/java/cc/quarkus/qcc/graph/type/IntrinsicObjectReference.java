package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.Core;
import cc.quarkus.qcc.type.TypeDefinition;

public class IntrinsicObjectReference<T> extends ObjectReference {

    public static IntrinsicObjectReference<String> newString(String val) {
        return new IntrinsicObjectReference<>(Core.java.lang.String(), val);
    }

    private IntrinsicObjectReference(TypeDefinition typeDefinition, T val) {
        super(typeDefinition);
        this.val = val;
    }

    public T getValue() {
        return this.val;
    }

    private final T val;
}
