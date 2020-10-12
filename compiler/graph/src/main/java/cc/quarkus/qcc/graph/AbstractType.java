package cc.quarkus.qcc.graph;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

abstract class AbstractType implements Type {
    private static final VarHandle arrayClassTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "arrayClassType", VarHandle.class, AbstractType.class, ArrayClassType.class);

    private volatile ArrayClassType arrayClassType;

    public ArrayClassType getArrayClassType() {
        ArrayClassType value;
        value = this.arrayClassType;
        if (value != null) {
            return value;
        }
        ArrayClassType newVal = new ArrayClassTypeImpl(this);
        while (! arrayClassTypeHandle.compareAndSet(this, null, newVal)) {
            value = this.arrayClassType;
            if (value != null) {
                return value;
            }
        }
        return newVal;
    }
}
