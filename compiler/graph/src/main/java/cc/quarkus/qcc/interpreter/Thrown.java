package cc.quarkus.qcc.interpreter;

@SuppressWarnings("serial")
public final class Thrown extends RuntimeException {
    private final JavaObject throwable;

    public Thrown(final JavaObject throwable) {
        this.throwable = throwable;
    }

    public JavaObject getThrowable() {
        return throwable;
    }
}
