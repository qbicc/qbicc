package cc.quarkus.qcc.graph;

/**
 *
 */
public interface EitherType extends Type {
    Type getFirstType();

    Type getSecondType();

    default boolean contains(Type otherType) {
        Type firstType = getFirstType();
        Type secondType = getSecondType();
        return firstType.equals(otherType) || secondType.equals(otherType)
            || firstType instanceof EitherType && ((EitherType) firstType).contains(otherType)
            || secondType instanceof EitherType && ((EitherType) secondType).contains(otherType)
            || otherType instanceof EitherType && contains(((EitherType) otherType).getFirstType()) && contains(((EitherType) otherType).getSecondType());
    }

    default boolean isAssignableFrom(Type otherType) {
        return equals(otherType) || getFirstType().isAssignableFrom(otherType) || getSecondType().isAssignableFrom(otherType);
    }

    default boolean bothAreAssignableTo(Type otherType) {
        return otherType.isAssignableFrom(getFirstType()) && otherType.isAssignableFrom(getSecondType());
    }

    static Type of(Type firstType, Type secondType) {
        if (firstType.isAssignableFrom(secondType)) {
            return firstType;
        }
        if (secondType.isAssignableFrom(firstType)) {
            return secondType;
        }
        if (firstType instanceof EitherType) {
            if (secondType instanceof EitherType) {
                return of(((EitherType) firstType).getFirstType(), of(((EitherType) firstType).getSecondType(), of(((EitherType) secondType).getFirstType(), ((EitherType) secondType).getSecondType())));
            } else {
                return of(((EitherType) firstType).getFirstType(), of(((EitherType) firstType).getSecondType(), secondType));
            }
        } else {
            return new EitherTypeImpl(firstType, secondType);
        }
    }
}
