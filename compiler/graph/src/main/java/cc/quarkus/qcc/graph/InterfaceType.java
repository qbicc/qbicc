package cc.quarkus.qcc.graph;

import io.smallrye.common.constraint.Assert;

/**
 * A class type that represents an interface.
 */
public interface InterfaceType extends ClassType {
    InterfaceType[] NO_INTERFACES = new InterfaceType[0];

    default ClassType getSuperClass() {
        return null;
    }

    default boolean isSuperTypeOf(ClassType other) {
        Assert.checkNotNullParam("other", other);

        if (this == other) {
            return true;
        }

        int cnt = other.getInterfaceCount();
        for (int i = 0; i < cnt; i ++) {
            if (isAssignableFrom(other.getInterface(i))) {
                return true;
            }
        }
        return false;
    }
}
