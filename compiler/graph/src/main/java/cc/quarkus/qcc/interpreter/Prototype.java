package cc.quarkus.qcc.interpreter;

public interface Prototype {
    byte[] getBytecode();
    String getClassName();
    Class<? extends FieldContainer> getPrototypeClass();
    FieldContainer construct();
}
