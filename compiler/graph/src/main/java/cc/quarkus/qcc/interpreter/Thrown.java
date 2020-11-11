package cc.quarkus.qcc.interpreter;

@SuppressWarnings("serial")
public final class Thrown extends RuntimeException {
    private final VmObject throwable;

    public Thrown(final VmObject throwable) {
        this.throwable = throwable;
    }

    public VmObject getThrowable() {
        return throwable;
    }
}
