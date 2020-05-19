package cc.quarkus.qcc.type.descriptor;

import java.util.Objects;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.definition.TypeDefinition;

public class ObjectTypeDescriptor implements TypeDescriptor<ObjectReference> {
    ObjectTypeDescriptor(TypeDefinition typeDefinition) {
        this.typeDefinition = typeDefinition;
    }

    @Override
    public TypeDescriptor<?> baseType() {
        return TypeDescriptor.OBJECT;
    }

    @Override
    public Class<ObjectReference> type() {
        return ObjectReference.class;
    }

    @Override
    public String label() {
        return this.typeDefinition.getName();
    }

    public TypeDefinition getTypeDefinition() {
        return this.typeDefinition;
    }

    @Override
    public String toString() {
        return this.getTypeDefinition().getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        cc.quarkus.qcc.type.descriptor.ObjectTypeDescriptor that = (cc.quarkus.qcc.type.descriptor.ObjectTypeDescriptor) o;
        return Objects.equals(typeDefinition, that.typeDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeDefinition);
    }

    private final TypeDefinition typeDefinition;
}
