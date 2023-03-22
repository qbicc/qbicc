package org.qbicc.type.definition;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A type ID which identifies a type class.
 */
public abstract class TypeId {
    private static final VarHandle arrayTypeIdHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "arrayTypeId", VarHandle.class, TypeId.class, ArrayTypeId.class);

    @SuppressWarnings("unused") // arrayTypeIdHandle
    private volatile ArrayTypeId arrayTypeId;

    TypeId() {}

    public ArrayTypeId getArrayTypeId() {
        ArrayTypeId arrayTypeId = this.arrayTypeId;
        if (arrayTypeId == null) {
            arrayTypeId = new ArrayTypeId(this);
            final ArrayTypeId appearing = (ArrayTypeId) arrayTypeIdHandle.compareAndExchange(this, null, arrayTypeId);
            if (appearing != null) {
                arrayTypeId = appearing;
            }
        }
        return arrayTypeId;
    }
}
