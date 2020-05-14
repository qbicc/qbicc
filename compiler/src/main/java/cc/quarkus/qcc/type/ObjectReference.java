package cc.quarkus.qcc.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.type.definition.FieldDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.universe.Core;

public class ObjectReference implements QType {

    public static final ObjectReference NULL = new HostBackedObjectReference<>(Core.java.lang.Object(), null);

    public ObjectReference(TypeDefinition typeDefinition) {
        this.typeDefinition = typeDefinition;
    }

    public TypeDefinition getTypeDefinition() {
        return this.typeDefinition;
    }

    public TypeDescriptor<ObjectReference> getTypeDescriptor() {
        return TypeDescriptor.of(getTypeDefinition());
    }

    public <V extends QType> V getField(FieldDescriptor<V> field) {
        return field.get(this);
    }

    public <V extends QType> void putField(FieldDescriptor<V> field, V val) {
        field.put(this, val);
    }

    @SuppressWarnings("unchecked")
    public <V extends QType> V getFieldValue(FieldDefinition<V> field) {
        QType v = this.fields.get(field);
        if ( v == null ) {
            v = NULL;
        }
        return (V) v;
    }

    public <V extends QType> void setFieldValue(FieldDefinition<V> field, V val) {
        //System.err.println( "setFieldValue: " +field + " = " + val);
        if ( val == null ) {
            this.fields.put(field, NULL);
        } else {
            this.fields.put(field, val);
        }
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public String toString() {
        return "ObjectReference{" +
                "typeDefinition=" + typeDefinition +
                //", fields=" + fields +
                '}';
    }

    private final TypeDefinition typeDefinition;
    private final Map<FieldDescriptor<?>, QType> fields = new ConcurrentHashMap<>();
}
