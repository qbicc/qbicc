package cc.quarkus.qcc.interpreter;

@SuppressWarnings("serial")
final class Thrown extends RuntimeException {
    private final JavaObject throwable;

    Thrown(final JavaObject throwable) {
        this.throwable = throwable;
    }

    JavaObject getThrowable() {
        return throwable;
    }
}
