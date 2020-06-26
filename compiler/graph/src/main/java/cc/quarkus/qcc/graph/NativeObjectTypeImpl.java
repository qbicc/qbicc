package cc.quarkus.qcc.graph;

abstract class NativeObjectTypeImpl extends NodeImpl implements NativeObjectType {
    volatile PointerType pointerType;

    public PointerType getPointerType() {
        PointerType pointerType = this.pointerType;
        if (pointerType == null) {
            synchronized (this) {
                pointerType = this.pointerType;
                if (pointerType == null) {
                    this.pointerType = pointerType = new PointerTypeImpl(this);
                }
            }
        }
        return pointerType;
    }
}
