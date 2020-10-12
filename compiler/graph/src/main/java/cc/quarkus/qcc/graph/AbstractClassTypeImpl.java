package cc.quarkus.qcc.graph;

abstract class AbstractClassTypeImpl extends AbstractType implements ClassType {
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
}
