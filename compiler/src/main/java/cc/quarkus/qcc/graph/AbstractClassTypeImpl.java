package cc.quarkus.qcc.graph;

abstract class AbstractClassTypeImpl extends NodeImpl implements ClassType {
    private volatile ArrayClassType arrayType;

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
