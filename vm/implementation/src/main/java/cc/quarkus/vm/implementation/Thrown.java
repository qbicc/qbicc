package cc.quarkus.vm.implementation;

import cc.quarkus.vm.api.JavaObject;

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
