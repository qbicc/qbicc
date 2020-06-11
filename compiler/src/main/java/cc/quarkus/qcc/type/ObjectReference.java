package cc.quarkus.qcc.type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.type.definition.FieldDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;
import cc.quarkus.qcc.type.universe.Core;

public class ObjectReference {

    public static final ObjectReference NULL = new HostBackedObjectReference<>(Core.java.lang.Object(), null);

    public ObjectReference(TypeDefinition typeDefinition) {
        this.typeDefinition = typeDefinition;
    }

    public Object getFieldValue(FieldDefinition field) {
        Object v = this.fields.get(field);
        if ( v == null ) {
            v = NULL;
        }
        return v;
    }

    public void setFieldValue(FieldDefinition field, Object val) {
        //System.err.println( "setFieldValue: " +field + " = " + val);
        if ( val == null ) {
            this.fields.put(field, NULL);
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
    private final Map<FieldDescriptor, Object> fields = new ConcurrentHashMap<>();
}
