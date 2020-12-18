package cc.quarkus.qcc.type;

/**
 * A physical object that is some kind of array.
 */
public abstract class ArrayObjectType extends PhysicalObjectType {

    private final ClassObjectType objectClass;

    ArrayObjectType(final TypeSystem typeSystem, final int hashCode, final boolean const_, final ClassObjectType objectClass) {
        super(typeSystem, hashCode, const_);
        this.objectClass = objectClass;
    }

    public boolean hasSuperClass() {
        return true;
    }

    public ArrayObjectType asConst() {
        return (ArrayObjectType) super.asConst();
    }

    abstract ArrayObjectType constructConst();

    public ClassObjectType getSuperClassType() {
        return objectClass;
    }

    public abstract ValueType getElementType();
}
