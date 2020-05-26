package cc.quarkus.qcc.type.descriptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.QVoid;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.universe.Core;

public interface TypeDescriptor<T> {

    static Map<TypeDefinition,TypeDescriptor<ObjectReference>> DESCRIPTORS = new ConcurrentHashMap<>();

    static TypeDescriptor<ObjectReference> of(TypeDefinition typeDefinition) {
        return DESCRIPTORS.computeIfAbsent(typeDefinition, ObjectTypeDescriptor::new);
    }

    TypeDescriptor<QVoid> VOID = new PrimitiveTypeDescriptor<>(QVoid.class, "void");

    TypeDescriptor<Boolean> BOOLEAN = new PrimitiveTypeDescriptor<>(Boolean.class, "boolean");
    TypeDescriptor<Character> CHAR = new PrimitiveTypeDescriptor<>(Character.class, "char");

    TypeDescriptor<Byte> INT8 = new PrimitiveTypeDescriptor<>(Byte.class, "int8");
    //TypeDescriptor<Short> UINT8 = new PrimitiveTypeDescriptor<>(Short.class, "uint8");

    TypeDescriptor<Short> INT16 = new PrimitiveTypeDescriptor<>(Short.class, "int16");
    //TypeDescriptor<Integer> UINT16 = new PrimitiveTypeDescriptor<>(Integer.class, "uint16");

    TypeDescriptor<Integer> INT32 = new PrimitiveTypeDescriptor<>(Integer.class, "int32");
    //TypeDescriptor<Long> UINT32 = new PrimitiveTypeDescriptor<>(Long.class, "uint32");

    TypeDescriptor<Long> INT64 = new PrimitiveTypeDescriptor<>(Long.class, "int64");
    //TypeDescriptor<BigInteger> UINT64 = new PrimitiveTypeDescriptor<>(BigInteger.class, "uint64");

    TypeDescriptor<Float> FLOAT = new PrimitiveTypeDescriptor<>(Float.class, "float");
    TypeDescriptor<Double> DOUBLE = new PrimitiveTypeDescriptor<>(Double.class, "double");

    TypeDescriptor<ObjectReference> OBJECT = of(Core.java.lang.Object());
    TypeDescriptor<ObjectReference> THROWABLE = of(Core.java.lang.Throwable());

    default TypeDescriptor<?> array(int dim) {
        if (dim == 0) {
            return this;
        }
        return new ArrayTypeDescriptor(this).array(dim - 1);
    }

    Class<T> type();
    String label();

    default TypeDescriptor<?> baseType() {
        return this;
    }

}
