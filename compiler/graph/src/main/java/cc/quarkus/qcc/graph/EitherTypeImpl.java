package cc.quarkus.qcc.graph;

import static java.lang.Math.*;

import cc.quarkus.qcc.constraint.Constraint;

final class EitherTypeImpl extends NodeImpl implements EitherType {
    private final Type firstType;
    private final Type secondType;

    EitherTypeImpl(final Type firstType, final Type secondType) {
        this.firstType = firstType;
        this.secondType = secondType;
    }

    public Type getFirstType() {
        return firstType;
    }

    public Type getSecondType() {
        return secondType;
    }

    public ArrayClassType getArrayClassType() {
        return null;
    }

    public int getParameterCount() {
        return 0;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        return null;
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        return null;
    }

    public String getLabelForGraph() {
        return "either";
    }

    public boolean equals(final Object obj) {
        return obj instanceof EitherType && equals((EitherType) obj);
    }

    public boolean equals(EitherType obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        Type myFirstType = this.firstType;
        Type mySecondType = this.secondType;
        Type theirFirstType = obj.getFirstType();
        Type theirSecondType = obj.getSecondType();
        return myFirstType.equals(theirFirstType) && mySecondType.equals(theirSecondType)
            || myFirstType.equals(theirSecondType) && mySecondType.equals(theirFirstType);
    }

    public int hashCode() {
        int fh = firstType.hashCode();
        int sh = secondType.hashCode();
        return min(fh, sh) * 19 + max(fh, sh);
    }
}
