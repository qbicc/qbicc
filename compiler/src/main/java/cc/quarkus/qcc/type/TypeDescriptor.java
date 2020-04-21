package cc.quarkus.qcc.type;

import cc.quarkus.qcc.graph.type.ObjectReference;

public interface TypeDescriptor<T> {

    static TypeDescriptor<ObjectReference> of(TypeDefinition typeDefinition) {
        return new ObjectTypeDescriptor(typeDefinition);
    }

    static TypeDescriptor<Void> VOID = new PrimitiveTypeDescriptor<>(Void.class);
    static TypeDescriptor<Boolean> BOOLEAN = new PrimitiveTypeDescriptor<>(Boolean.class);
    static TypeDescriptor<Byte> BYTE = new PrimitiveTypeDescriptor<>(Byte.class);
    static TypeDescriptor<Character> CHAR = new PrimitiveTypeDescriptor<>(Character.class);
    static TypeDescriptor<Short> SHORT = new PrimitiveTypeDescriptor<>(Short.class);
    static TypeDescriptor<Integer> INT = new PrimitiveTypeDescriptor<>(Integer.class);
    static TypeDescriptor<Long> LONG = new PrimitiveTypeDescriptor<>(Long.class);
    static TypeDescriptor<Float> FLOAT = new PrimitiveTypeDescriptor<>(Float.class);
    static TypeDescriptor<Double> DOUBLE = new PrimitiveTypeDescriptor<>(Double.class);

    default TypeDescriptor<?> array(int dim) {
        if (dim == 1) {
            return this;
        }
        return new ArrayTypeDescriptor(this).array(dim - 1);
    }

    Class<T> valueType();

    class PrimitiveTypeDescriptor<T> implements TypeDescriptor<T> {

        private PrimitiveTypeDescriptor(Class<T> type) {
            this.type = type;
        }

        @Override
        public Class<T> valueType() {
            return this.type;
        }

        private final Class<T> type;
    }

    class ObjectTypeDescriptor implements TypeDescriptor<ObjectReference> {
        private ObjectTypeDescriptor(TypeDefinition typeDefinition) {
            this.typeDefinition = typeDefinition;
        }

        @Override
        public Class<ObjectReference> valueType() {
            return ObjectReference.class;
        }

        private final TypeDefinition typeDefinition;
    }

    class ArrayTypeDescriptor implements TypeDescriptor<ObjectReference> {
        private ArrayTypeDescriptor(TypeDescriptor<?> elementType) {
            this.elementType = elementType;
        }

        @Override
        public Class<ObjectReference> valueType() {
            return ObjectReference.class;
        }

        private final TypeDescriptor<?> elementType;
    }
}
