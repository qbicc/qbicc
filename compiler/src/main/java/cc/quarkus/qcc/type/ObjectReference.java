package cc.quarkus.qcc.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectReference {

    public ObjectReference(TypeDefinition typeDefinition) {
        this.typeDefinition = typeDefinition;
    }

    public TypeDefinition getTypeDefinition() {
        return this.typeDefinition;
    }

    public TypeDescriptor<ObjectReference> getTypeDescriptor() {
        return TypeDescriptor.of(getTypeDefinition());
    }

    public <V> V getField(FieldDescriptor<V> field) {
        return field.get(this);
    }

    public <V> void putField(FieldDescriptor<V> field, V val) {
        field.put(this, val);
    }

    @SuppressWarnings("unchecked")
    <V> V getFieldValue(FieldDefinition<V> field) {
        Object v = this.fields.get(field);
        if ( v == Sentinel.Null.NULL ) {
            return null;
        }
        return (V) v;
    }

    <V> void setFieldValue(FieldDefinition<V> field, V val) {
        //System.err.println( "setFieldValue: " +field + " = " + val);
        if ( val == null ) {
            this.fields.put(field, Sentinel.Null.NULL);
        } else {
            this.fields.put(field, val);
        }
    }

    @Override
    public String toString() {
        return "ObjectReference{" +
                "typeDefinition=" + typeDefinition +
                //", fields=" + fields +
                '}';
    }

    private final TypeDefinition typeDefinition;
    private final Map<FieldDescriptor<?>, Object> fields = new ConcurrentHashMap<>();
}
