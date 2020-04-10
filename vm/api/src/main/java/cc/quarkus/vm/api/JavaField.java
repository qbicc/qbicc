package cc.quarkus.vm.api;

/**
 * A field in the nested VM.
 */
public interface JavaField {
    boolean isStatic();

    String getName();

    JavaClass getType();
}
