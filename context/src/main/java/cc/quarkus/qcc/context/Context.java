package cc.quarkus.qcc.context;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.function.ExceptionBiConsumer;
import io.smallrye.common.function.ExceptionBiFunction;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;
import io.smallrye.common.function.ExceptionRunnable;
import io.smallrye.common.function.ExceptionSupplier;

/**
 *
 */
public final class Context {
    private static final ThreadLocal<Context> TL = new InheritableThreadLocal<>();

    private final boolean warnError;
    private final Lock lock = new ReentrantLock();
    private final List<Diagnostic> diags = new ArrayList<>(4);
    private final ConcurrentMap<AttachmentKey<?>, Object> attachments = new ConcurrentHashMap<>();
    private int errors = 0;
    private int warnings = 0;

    public Context(final boolean warnError) {
        this.warnError = warnError;
    }

    public static Context getCurrent() {
        return TL.get();
    }

    public static Context requireCurrent() {
        Context context = getCurrent();
        if (context == null) {
            throw new IllegalStateException("No compiler context available");
        }
        return context;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachment(AttachmentKey<T> key) {
        Assert.checkNotNullParam("key", key);
        return (T) attachments.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachmentOrDefault(AttachmentKey<T> key, T defVal) {
        Assert.checkNotNullParam("key", key);
        return (T) attachments.getOrDefault(key, defVal);
    }

    @SuppressWarnings("unchecked")
    public <T> T putAttachment(AttachmentKey<T> key, T value) {
        Assert.checkNotNullParam("key", key);
        Assert.checkNotNullParam("value", value);
        return (T) attachments.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T putAttachmentIfAbsent(AttachmentKey<T> key, T value) {
        Assert.checkNotNullParam("key", key);
        Assert.checkNotNullParam("value", value);
        return (T) attachments.putIfAbsent(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeAttachment(AttachmentKey<T> key) {
        Assert.checkNotNullParam("key", key);
        return (T) attachments.remove(key);
    }

    public <T> boolean removeAttachment(AttachmentKey<T> key, T expect) {
        Assert.checkNotNullParam("key", key);
        Assert.checkNotNullParam("expect", expect);
        return attachments.remove(key, expect);
    }

    @SuppressWarnings("unchecked")
    public <T> T replaceAttachment(AttachmentKey<T> key, T update) {
        Assert.checkNotNullParam("key", key);
        Assert.checkNotNullParam("update", update);
        return (T) attachments.replace(key, update);
    }

    public <T> boolean replaceAttachment(AttachmentKey<T> key, T expect, T update) {
        Assert.checkNotNullParam("key", key);
        Assert.checkNotNullParam("expect", expect);
        Assert.checkNotNullParam("update", update);
        return attachments.replace(key, expect, update);
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T computeAttachmentIfAbsent(AttachmentKey<T> key, final Supplier<T> function) {
        Assert.checkNotNullParam("key", key);
        Assert.checkNotNullParam("function", function);
        return (T) attachments.computeIfAbsent(key, k -> function.get());
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T computeAttachmentIfPresent(AttachmentKey<T> key, final Function<T, T> function) {
        Assert.checkNotNullParam("key", key);
        Assert.checkNotNullParam("function", function);
        return (T) attachments.computeIfPresent(key, (k, v) -> function.apply((T) v));
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T computeAttachment(AttachmentKey<T> key, final Function<T, T> function) {
        Assert.checkNotNullParam("key", key);
        return (T) attachments.compute(key, (k, v) -> function.apply((T) v));
    }

    public <T, U, E extends Exception> void run(ExceptionBiConsumer<T, U, E> action, T param1, U param2) throws E {
        Context old = TL.get();
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
        Context old = TL.get();
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
        Context old = TL.get();
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
        Context old = TL.get();
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
        Context old = TL.get();
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
        Context old = TL.get();
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
        final Context dc = requireCurrent();
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

    public int errors() {
        lock.lock();
        try {
            return errors;
        } finally {
            lock.unlock();
        }
    }

    public int warnings() {
        lock.lock();
        try {
            return warnings;
        } finally {
            lock.unlock();
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

    public Iterable<Diagnostic> getDiagnostics() {
        return diags;
    }

    public static void dump(PrintStream ps) {
        for (Diagnostic diag : requireCurrent().diags) {
            ps.println(diag);
        }
    }
}
