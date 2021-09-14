package org.qbicc.type;

import java.util.function.Consumer;

public enum Primitive {
    // Predefine the set of primitive types in Java
    BOOLEAN("boolean"),
    BYTE("byte"),
    SHORT("short"),
    CHAR("char"),
    INT("int"),
    FLOAT("float"),
    LONG("long"),
    DOUBLE("double"),
    VOID("void");

    private final String name;
    private int typeId;
    private ValueType type;

    Primitive(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public ValueType getType() {
        return this.type;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    public static void forEach(Consumer<Primitive> function) {
        for (Primitive type : Primitive.values()) {
            function.accept(type);
        }
    }

    public static Primitive getPrimitiveFor(String typeName) {
        Primitive type;
        switch(typeName) {
            case "byte":
                type = BYTE;
                break;
            case "short":
                type = SHORT;
                break;
            case "int":
                type = INT;
                break;
            case "long":
                type = LONG;
                break;
            case "char":
                type = CHAR;
                break;
            case "float":
                type = FLOAT;
                break;
            case "double":
                type = DOUBLE;
                break;
            case "boolean":
                type = BOOLEAN;
                break;
            case "void":
                type = VOID;
                break;
            default:
                throw new IllegalStateException("Unexpected primitive type: " + typeName);
        }
        return type;
    }
}
