package cc.quarkus.qcc.type.definition;

/**
 * A handle to a method body.  All method references are resolved to method handles.
 */
public interface MethodHandle {
    MethodHandle VOID_EMPTY = new MethodHandle() {
        public int getModifiers() {
            return 0;
        }

        public int getParameterCount() {
            return 0;
        }

        public MethodBody getResolvedMethodBody() throws ResolutionFailedException {
            return MethodBody.VOID_EMPTY;
        }
    };

    int getModifiers();

    int getParameterCount();

    MethodBody getResolvedMethodBody() throws ResolutionFailedException;
}
