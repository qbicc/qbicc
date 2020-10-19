package cc.quarkus.qcc.type.definition;

/**
 * A handle to a method body.  All method references are resolved to method handles.
 */
public interface MethodHandle {

    int getModifiers();

    int getParameterCount();

    MethodBody createMethodBody() throws ResolutionFailedException;
}
