package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.QNull;
import cc.quarkus.qcc.type.universe.Core;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.definition.TypeDefinition;

public class IntrinsicObjectReference<T> extends ObjectReference {

    public static IntrinsicObjectReference<String> newString(String val) {
        return new IntrinsicObjectReference<>(Core.java.lang.String(), val);
    }

    public static IntrinsicObjectReference<Object> newNull() {
        return new IntrinsicObjectReference<>(Core.java.lang.Object(), QNull.NULL);
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
