package org.qbicc.plugin.metrics;

import java.time.Duration;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A timer which accumulates approximate CPU-wall-clock time spent on a task or category of tasks.
 */
public final class Timer extends Metric<Timer> {

    private final ThreadLocal<State> currentState;

    Timer(String name, Timer parent) {
        super(name, parent);
        currentState = parent == null ? ThreadLocal.withInitial(State::new) : parent.currentState;
    }

    @Override
    Timer constructChild(String name) {
        return new Timer(name, this);
    }

    /**
     * Get the raw nanosecond total of this timer.  The returned value is an unsigned {@code long}, which can therefore
     * represent times up to 584.94 cpu-years before wrapping around.  This is approximately equivalent to 26.7 cpu-days
     * across 8000 cores.
     *
     * @return the raw nanosecond total of this timer
     */
    public long getRawValue() {
        return super.getRawValue();
    }

    /**
     * Get the total duration of this timer.  The returned value is a duration in cpu-time, not linear time.
     *
     * @return the total duration of this timer (not {@code null})
     */
    public Duration getTotal() {
        long unsignedValue = getRawValue();
        // get seconds from unsigned raw value
        long seconds = Long.divideUnsigned(unsignedValue, 1_000_000_000L);
        int nanos = (int) Long.remainderUnsigned(unsignedValue, 1_000_000_000L);
        return Duration.ofSeconds(seconds, nanos);
    }

    /**
     * Append the total duration as a string to the given string builder.
     *
     * @param target the string builder (must not be {@code null})
     * @return the string builder
     */
    public StringBuilder getFormattedValue(StringBuilder target) {
        String totalStr = getTotal().toString();
        if (totalStr.startsWith("PT")) {
            totalStr = totalStr.substring(2);
        }
        totalStr = totalStr.toLowerCase(Locale.ROOT);
        return target.append(totalStr);
    }

    void addDuration(long start, long end) {
        addRawValue(Math.max(0, end - start));
    }

    /**
     * Do a slow-path task controlled by a {@code try} block.  This method may allocate an object so it should only
     * be used in the slow path, and should only be used in a {@code try}-with-resources block to avoid difficult
     * accounting errors.
     *
     * @return the closeable stop watch (not {@code null})
     */
    public StopWatch startTimedTryBlock() {
        Timer oldTimer = start();
        if (this != oldTimer) {
            return new ActiveStopWatch(oldTimer);
        } else {
            return NullStopWatch.INSTANCE;
        }
    }

    /**
     * Do some task, timing the work with this timer.  Any work done under another timer is timed only under that timer.
     *
     * @param task the task to perform (must not be {@code null})
     */
    public final void runTimed(Runnable task) {
        State state = currentState.get();
        Timer oldTimer = state.current;
        long start = System.nanoTime();
        if (oldTimer != null) {
            if (oldTimer == this) {
                // just run the task
                task.run();
                return;
            }
            // terminate old timer (from its start to our start)
            oldTimer.addDuration(state.start, start);
        }
        state.current = this;
        state.start = start;
        try {
            task.run();
        } finally {
            end(oldTimer);
        }
    }

    /**
     * Do some task, timing the work with this timer.  Any work done under another timer is timed only under that timer.
     *
     * @param task the task to perform (must not be {@code null})
     * @param <R> the task return type
     * @return the return value of the task
     */
    public final <R> R getTimed(Supplier<R> task) {
        State state = currentState.get();
        Timer oldTimer = state.current;
        long start = System.nanoTime();
        if (oldTimer != null) {
            if (oldTimer == this) {
                // just run the task
                return task.get();
            }
            // terminate old timer (from its start to our start)
            oldTimer.addDuration(state.start, start);
        }
        state.current = this;
        state.start = start;
        try {
            return task.get();
        } finally {
            end(oldTimer);
        }
    }

    /**
     * Do some task, timing the work with this timer.  Any work done under another timer is timed only under that timer.
     *
     * @param task the task to perform (must not be {@code null})
     * @param arg the argument to pass to the task
     * @param <T> the task parameter type
     * @param <R> the task return type
     * @return the return value of the task
     */
    public final <T, R> R applyTimed(Function<T, R> task, T arg) {
        State state = currentState.get();
        Timer oldTimer = state.current;
        long start = System.nanoTime();
        if (oldTimer != null) {
            if (oldTimer == this) {
                // just run the task
                return task.apply(arg);
            }
            // terminate old timer (from its start to our start)
            oldTimer.addDuration(state.start, start);
        }
        state.current = this;
        state.start = start;
        try {
            return task.apply(arg);
        } finally {
            end(oldTimer);
        }
    }

    /**
     * Do some task, timing the work with this timer.  Any work done under another timer is timed only under that timer.
     *
     * @param task the task to perform (must not be {@code null})
     * @param arg1 the argument to pass to the task
     * @param <T> the first task parameter type
     * @param <U> the second task parameter type
     * @param <R> the task return type
     * @return the return value of the task
     */
    public final <T, U, R> R applyTimed(BiFunction<T, U, R> task, T arg1, U arg2) {
        State state = currentState.get();
        Timer oldTimer = state.current;
        long start = System.nanoTime();
        if (oldTimer != null) {
            if (oldTimer == this) {
                // just run the task
                return task.apply(arg1, arg2);
            }
            // terminate old timer (from its start to our start)
            oldTimer.addDuration(state.start, start);
        }
        state.current = this;
        state.start = start;
        try {
            return task.apply(arg1, arg2);
        } finally {
            end(oldTimer);
        }
    }

    /**
     * Do some task, timing the work with this timer.  Any work done under another timer is timed only under that timer.
     *
     * @param task the task to perform (must not be {@code null})
     * @param arg the argument to pass to the task
     * @param <T> the task parameter type
     */
    public final <T> void acceptTimed(Consumer<T> task, T arg) {
        Timer oldTimer = start();
        if (oldTimer != this) {
            try {
                task.accept(arg);
                return;
            } finally {
                end(oldTimer);
            }
        } else {
            task.accept(arg);
        }
    }

    /**
     * Do some task, timing the work with this timer.  Any work done under another timer is timed only under that timer.
     *
     * @param task the task to perform (must not be {@code null})
     * @param arg1 the argument to pass to the task
     * @param <T> the first task parameter type
     * @param <U> the second task parameter type
     */
    public final <T, U> void acceptTimed(BiConsumer<T, U> task, T arg1, U arg2) {
        Timer oldTimer = start();
        if (oldTimer != this) {
            try {
                task.accept(arg1, arg2);
                return;
            } finally {
                end(oldTimer);
            }
        } else {
            task.accept(arg1, arg2);
        }
    }

    private Timer start() {
        State state = currentState.get();
        Timer oldTimer = state.current;
        long start = System.nanoTime();
        if (oldTimer != null) {
            if (oldTimer == this) {
                // just run the task
                return oldTimer;
            }
            // terminate old timer (from its start to our start)
            oldTimer.addDuration(state.start, start);
        }
        state.current = this;
        state.start = start;
        return oldTimer;
    }

    private void end(final Timer oldTimer) {
        State state = currentState.get();
        long end = System.nanoTime();
        addDuration(state.start, end);
        // resume old timer from our end; if oldTimer is null then state.start doesn't matter
        state.current = oldTimer;
        state.start = end;
    }

    @Override
    String getDescription() {
        return "Timer";
    }

    static final class State {
        Timer current;
        long start;
    }

    public abstract static class StopWatch implements AutoCloseable {
        public abstract void close();
    }

    final class ActiveStopWatch extends StopWatch {
        private final Thread thread;
        private boolean active = true;
        private final Timer oldTimer;

        ActiveStopWatch(Timer oldTimer) {
            this.oldTimer = oldTimer;
            thread = Thread.currentThread();
        }

        @Override
        public void close() {
            if (Thread.currentThread() != thread || ! active) {
                throw new IllegalStateException("Mismatched stop watch");
            }
            State state = currentState.get();
            if (state.current != Timer.this) {
                throw new IllegalStateException("Mismatched stop watch");
            }
            active = false;
            end(oldTimer);
        }
    }

    static class NullStopWatch extends StopWatch {
        static final NullStopWatch INSTANCE = new NullStopWatch();

        private NullStopWatch() {}

        public void close() {
        }
    }
}
