package cc.quarkus.qcc.diagnostic;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import io.smallrye.common.function.ExceptionBiConsumer;
import io.smallrye.common.function.ExceptionBiFunction;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;
import io.smallrye.common.function.ExceptionRunnable;
import io.smallrye.common.function.ExceptionSupplier;

/**
 *
 */
public final class DiagnosticContext implements Iterable<Diagnostic> {
    private static final ThreadLocal<DiagnosticContext> TL = new InheritableThreadLocal<>();

    private final boolean warnError;
    private final Lock lock = new ReentrantLock();
    private final List<Diagnostic> diags = new ArrayList<>(4);
    private int errors = 0;
    private int warnings = 0;

    public DiagnosticContext(final boolean warnError) {
        this.warnError = warnError;
    }

    public static DiagnosticContext getCurrent() {
        return TL.get();
    }

    public static DiagnosticContext requireCurrent() {
        DiagnosticContext context = getCurrent();
        if (context == null) {
            throw new IllegalStateException("No diagnostic context available");
        }
        return context;
    }

    public <T, U, E extends Exception> void run(ExceptionBiConsumer<T, U, E> action, T param1, U param2) throws E {
        DiagnosticContext old = TL.get();
        if (old == this) {
            action.accept(param1, param2);
            return;
        }
        TL.set(this);
        try {
            action.accept(param1, param2);
        } finally {
            if (old == null) {
                TL.remove();
            } else {
                TL.set(old);
            }
        }
    }

    public <T, E extends Exception> void run(ExceptionConsumer<T, E> action, T param) throws E {
        DiagnosticContext old = TL.get();
        if (old == this) {
            action.accept(param);
            return;
        }
        TL.set(this);
        try {
            action.accept(param);
        } finally {
            if (old == null) {
                TL.remove();
            } else {
                TL.set(old);
            }
        }
    }

    public <E extends Exception> void run(ExceptionRunnable<E> action) throws E {
        DiagnosticContext old = TL.get();
        if (old == this) {
            action.run();
            return;
        }
        TL.set(this);
        try {
            action.run();
        } finally {
            if (old == null) {
                TL.remove();
            } else {
                TL.set(old);
            }
        }
    }

    public <T, U, R, E extends Exception> R run(ExceptionBiFunction<T, U, R, E> action, T param1, U param2) throws E {
        DiagnosticContext old = TL.get();
        if (old == this) {
            return action.apply(param1, param2);
        }
        TL.set(this);
        try {
            return action.apply(param1, param2);
        } finally {
            if (old == null) {
                TL.remove();
            } else {
                TL.set(old);
            }
        }
    }

    public <T, R, E extends Exception> R run(ExceptionFunction<T, R, E> action, T param) throws E {
        DiagnosticContext old = TL.get();
        if (old == this) {
            return action.apply(param);
        }
        TL.set(this);
        try {
            return action.apply(param);
        } finally {
            if (old == null) {
                TL.remove();
            } else {
                TL.set(old);
            }
        }
    }

    public <R, E extends Exception> R run(ExceptionSupplier<R, E> action) throws E {
        DiagnosticContext old = TL.get();
        if (old == this) {
            return action.get();
        }
        TL.set(this);
        try {
            return action.get();
        } finally {
            if (old == null) {
                TL.remove();
            } else {
                TL.set(old);
            }
        }
    }

    public static void msg(Location loc, Diagnostic.Level level, String fmt, Object... args) {
        final DiagnosticContext dc = requireCurrent();
        if (dc.warnError && level == Diagnostic.Level.WARNING) {
            level = Diagnostic.Level.ERROR;
        }
        dc.lock.lock();
        try {
            dc.diags.add(new Diagnostic(loc, level, fmt, args));
            if (level == Diagnostic.Level.ERROR) {
                dc.errors++;
            } else if (level == Diagnostic.Level.WARNING) {
                dc.warnings++;
            }
        } finally {
            dc.lock.unlock();
        }
    }

    public static int errors() {
        DiagnosticContext dc = requireCurrent();
        dc.lock.lock();
        try {
            return dc.errors;
        } finally {
            dc.lock.unlock();
        }
    }

    public static int warnings() {
        DiagnosticContext dc = requireCurrent();
        dc.lock.lock();
        try {
            return dc.warnings;
        } finally {
            dc.lock.unlock();
        }
    }

    public static void error(Location loc, String fmt, Object... args) {
        msg(loc, Diagnostic.Level.ERROR, fmt, args);
    }

    public static void warning(Location loc, String fmt, Object... args) {
        msg(loc, Diagnostic.Level.WARNING, fmt, args);
    }

    public static void note(Location loc, String fmt, Object... args) {
        msg(loc, Diagnostic.Level.NOTE, fmt, args);
    }

    public static void debug(Location loc, String fmt, Object... args) {
        msg(loc, Diagnostic.Level.DEBUG, fmt, args);
    }

    public Iterator<Diagnostic> iterator() {
        return diags.iterator();
    }

    public void forEach(final Consumer<? super Diagnostic> action) {
        diags.forEach(action);
    }

    public Spliterator<Diagnostic> spliterator() {
        return diags.spliterator();
    }

    public static void dump(PrintStream ps) {
        for (Diagnostic diag : requireCurrent().diags) {
            ps.println(diag);
        }
    }
}
