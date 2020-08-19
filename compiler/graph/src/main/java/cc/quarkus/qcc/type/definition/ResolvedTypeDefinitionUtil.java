package cc.quarkus.qcc.type.definition;

final class ResolvedTypeDefinitionUtil {
    private ResolvedTypeDefinitionUtil() {}

    static final MethodHandle NOT_FOUND = new MethodHandleStub();
    static final MethodHandle END_OF_SEARCH = new MethodHandleStub();

    static class MethodHandleStub implements MethodHandle {
        public int getModifiers() {
            return 0;
        }

        public int getParameterCount() {
            return 0;
        }

        public MethodBody getResolvedMethodBody() throws ResolutionFailedException {
            return null;
        }
    }
}
