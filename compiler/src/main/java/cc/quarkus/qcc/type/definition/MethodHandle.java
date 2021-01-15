package cc.quarkus.qcc.type.definition;

/**
 * A handle to a method body.  All method references are resolved to method handles.
 */
public interface MethodHandle {

    void replaceMethodBody(MethodBody newBody);

    MethodBody getOrCreateMethodBody() throws ResolutionFailedException;

    MethodBody getMethodBody();

    MethodBody getPreviousMethodBody();
}
