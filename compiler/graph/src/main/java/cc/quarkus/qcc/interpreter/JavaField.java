package cc.quarkus.qcc.interpreter;

/**
 * A field in the nested VM.
 */
public interface JavaField {
    boolean isStatic();

    String getName();

    JavaClass getType();
}
