package cc.quarkus.qcc.type.descriptor;

/**
 * The kind of method handle.
 */
public enum MethodHandleKind {
    GET_FIELD(1, Shape.FIELD, true),
    GET_STATIC(2, Shape.FIELD, false),
    PUT_FIELD(3, Shape.FIELD, true),
    PUT_STATIC(4, Shape.FIELD, false),

    INVOKE_VIRTUAL(5, Shape.METHOD, true),
    INVOKE_STATIC(6, Shape.METHOD, false),
    INVOKE_SPECIAL(7, Shape.METHOD, true),
    NEW_INVOKE_SPECIAL(8, Shape.CONSTRUCTOR, false),
    INVOKE_INTERFACE(9, Shape.METHOD, true),
    ;

    private final int id;
    private final Shape target;
    private final boolean instance;

    MethodHandleKind(int id, Shape target, final boolean instance) {
        this.id = id;
        this.target = target;
        this.instance = instance;
    }

    public int getId() {
        return id;
    }

    public Shape getTarget() {
        return target;
    }

    public boolean isMethodTarget() {
        return getTarget() == Shape.METHOD;
    }

    public boolean isFieldTarget() {
        return getTarget() == Shape.FIELD;
    }

    public boolean isConstructorTarget() {
        return getTarget() == Shape.CONSTRUCTOR;
    }

    public boolean isInstance() {
        return instance;
    }

    public boolean isStatic() {
        return ! isInstance();
    }

    private static final MethodHandleKind[] values = values();

    public static MethodHandleKind forId(int id) {
        return values[id - 1];
    }

    public String toString() {
        // 21 is the longest possible string
        return toString(new StringBuilder(21)).toString();
    }

    public StringBuilder toString(StringBuilder b) {
        return b.append(name()).append('(').append(id).append(')');
    }

    public enum Shape {
        FIELD,
        METHOD,
        CONSTRUCTOR,
        ;
    }
}
