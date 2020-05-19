package cc.quarkus.qcc.type.descriptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.type.QBoolean;
import cc.quarkus.qcc.type.QChar;
import cc.quarkus.qcc.type.QDouble;
import cc.quarkus.qcc.type.QFloat;
import cc.quarkus.qcc.type.QInt16;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QInt64;
import cc.quarkus.qcc.type.QInt8;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.QVoid;
import cc.quarkus.qcc.type.universe.Core;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.definition.TypeDefinition;

public interface TypeDescriptor<T extends QType> {

    static Map<TypeDefinition,TypeDescriptor<ObjectReference>> DESCRIPTORS = new ConcurrentHashMap<>();

    static TypeDescriptor<ObjectReference> of(TypeDefinition typeDefinition) {
        return DESCRIPTORS.computeIfAbsent(typeDefinition, ObjectTypeDescriptor::new);
    }

    TypeDescriptor<QVoid> VOID = new PrimitiveTypeDescriptor<>(QVoid.class, "void");

    TypeDescriptor<QBoolean> BOOLEAN = new PrimitiveTypeDescriptor<>(QBoolean.class, "boolean");
    TypeDescriptor<QChar> CHAR = new PrimitiveTypeDescriptor<>(QChar.class, "char");

    TypeDescriptor<QInt8> INT8 = new PrimitiveTypeDescriptor<>(QInt8.class, "int8");
    //TypeDescriptor<Short> UINT8 = new PrimitiveTypeDescriptor<>(Short.class, "uint8");

    TypeDescriptor<QInt16> INT16 = new PrimitiveTypeDescriptor<>(QInt16.class, "int16");
    //TypeDescriptor<Integer> UINT16 = new PrimitiveTypeDescriptor<>(Integer.class, "uint16");

    TypeDescriptor<QInt32> INT32 = new PrimitiveTypeDescriptor<>(QInt32.class, "int32");
    //TypeDescriptor<Long> UINT32 = new PrimitiveTypeDescriptor<>(Long.class, "uint32");

    TypeDescriptor<QInt64> INT64 = new PrimitiveTypeDescriptor<>(QInt64.class, "int64");
    //TypeDescriptor<BigInteger> UINT64 = new PrimitiveTypeDescriptor<>(BigInteger.class, "uint64");

    TypeDescriptor<QFloat> FLOAT = new PrimitiveTypeDescriptor<>(QFloat.class, "float");
    TypeDescriptor<QDouble> DOUBLE = new PrimitiveTypeDescriptor<>(QDouble.class, "double");

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
