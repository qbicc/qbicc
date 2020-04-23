package cc.quarkus.qcc.type;

import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.IfToken;
import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.graph.type.ObjectReference;
import cc.quarkus.qcc.graph.type.StartToken;

public interface TypeDescriptor<T> {

    static TypeDescriptor<ObjectReference> of(TypeDefinition typeDefinition) {
        return new ObjectTypeDescriptor(typeDefinition);
    }

    TypeDescriptor<Void> VOID = new PrimitiveTypeDescriptor<>(Void.class, "void");
    TypeDescriptor<Boolean> BOOLEAN = new PrimitiveTypeDescriptor<>(Boolean.class, "boolean");
    TypeDescriptor<Byte> BYTE = new PrimitiveTypeDescriptor<>(Byte.class, "byte");
    TypeDescriptor<Character> CHAR = new PrimitiveTypeDescriptor<>(Character.class, "char");
    TypeDescriptor<Short> SHORT = new PrimitiveTypeDescriptor<>(Short.class, "short");
    TypeDescriptor<Integer> INT = new PrimitiveTypeDescriptor<>(Integer.class, "int");
    TypeDescriptor<Long> LONG = new PrimitiveTypeDescriptor<>(Long.class, "long");
    TypeDescriptor<Float> FLOAT = new PrimitiveTypeDescriptor<>(Float.class, "float");
    TypeDescriptor<Double> DOUBLE = new PrimitiveTypeDescriptor<>(Double.class, "double");
    TypeDescriptor<ObjectReference> OBJECT = of(Core.java.lang.Object());

    default TypeDescriptor<?> array(int dim) {
        if (dim == 1) {
            return this;
        }
        return new ArrayTypeDescriptor(this).array(dim - 1);
    }

    Class<T> valueType();
    String label();

    class PrimitiveTypeDescriptor<T> implements TypeDescriptor<T> {

        private PrimitiveTypeDescriptor(Class<T> type, String label) {
            this.type = type;
            this.label = label;
        }

        @Override
        public Class<T> valueType() {
            return this.type;
        }

        @Override
        public String label() {
            return this.label;
        }

        private final Class<T> type;

        private final String label;
    }

    class ObjectTypeDescriptor implements TypeDescriptor<ObjectReference> {
        private ObjectTypeDescriptor(TypeDefinition typeDefinition) {
            this.typeDefinition = typeDefinition;
        }

        @Override
        public Class<ObjectReference> valueType() {
            return ObjectReference.class;
        }

        @Override
        public String label() {
            return this.typeDefinition.getName();
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

        @Override
        public String label() {
            return this.elementType.label() + "[]";
        }

        private final TypeDescriptor<?> elementType;
    }

    class EphemeralTypeDescriptor<T> implements TypeDescriptor<T> {

        public static final TypeDescriptor<StartToken> START_TOKEN = new EphemeralTypeDescriptor<>(StartToken.class);
        public static final TypeDescriptor<EndToken> END_TOKEN = new EphemeralTypeDescriptor<>(EndToken.class);
        public static final TypeDescriptor<InvokeToken> INVOKE_TOKEN = new EphemeralTypeDescriptor<>(InvokeToken.class);
        public static final TypeDescriptor<ControlToken> CONTROL_TOKEN = new EphemeralTypeDescriptor<>(ControlToken.class);
        public static final TypeDescriptor<IOToken> IO_TOKEN = new EphemeralTypeDescriptor<>(IOToken.class);
        public static final TypeDescriptor<MemoryToken> MEMORY_TOKEN = new EphemeralTypeDescriptor<>(MemoryToken.class);

        public static final TypeDescriptor<IfToken> IF_TOKEN = new EphemeralTypeDescriptor<>(IfToken.class);

        private EphemeralTypeDescriptor(Class<T> valueType) {
            this.valueType = valueType;
        }

        @Override
        public Class<T> valueType() {
            return this.valueType;
        }

        @Override
        public String label() {
            return this.valueType.getSimpleName();
        }

        private final Class<T> valueType;
    }
}
