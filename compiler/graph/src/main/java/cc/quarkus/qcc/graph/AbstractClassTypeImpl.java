package cc.quarkus.qcc.graph;

abstract class AbstractClassTypeImpl extends NodeImpl implements ClassType {
    private volatile ArrayClassType arrayType;
    private volatile UninitializedType uninitializedType;

    public UninitializedType uninitialized() {
        UninitializedType uninitializedType = this.uninitializedType;
        if (uninitializedType == null) {
            synchronized (this) {
                uninitializedType = this.uninitializedType;
                if (uninitializedType == null) {
                    this.uninitializedType = uninitializedType = new UninitializedTypeImpl(this);
                }
            }
        }
        return uninitializedType;
    }

    public ArrayClassType getArrayType() {
        ArrayClassType arrayType = this.arrayType;
        if (arrayType == null) {
            synchronized (this) {
                arrayType = this.arrayType;
                if (arrayType == null) {
                    this.arrayType = arrayType = new ArrayClassTypeImpl(this);
                }
            }
        }
        return arrayType;
    }

}
