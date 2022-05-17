package org.qbicc.machine.vio;

import java.io.IOException;

import io.smallrye.common.function.ExceptionBiConsumer;
import io.smallrye.common.function.ExceptionBiFunction;
import io.smallrye.common.function.ExceptionBiPredicate;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;
import io.smallrye.common.function.ExceptionObjIntConsumer;
import io.smallrye.common.function.ExceptionObjLongConsumer;
import io.smallrye.common.function.ExceptionPredicate;
import io.smallrye.common.function.ExceptionSupplier;
import io.smallrye.common.function.ExceptionToLongBiFunction;
import io.smallrye.common.function.ExceptionToLongFunction;

/**
 *
 */
final class VIOFile {
    private int refCnt;
    private IoHandler handler;

    VIOFile() {}

    void open(ExceptionSupplier<IoHandler, IOException> factory) throws IOException {
        synchronized (this) {
            if (this.handler != null) {
                throw new AlreadyOpenException();
            }
            // do this first in case an exception is thrown
            IoHandler handler = factory.get();
            assert refCnt == 0;
            refCnt = 1;
            this.handler = handler;
        }
    }

    void acquireOne() throws IOException {
        synchronized (this) {
            int refCnt = this.refCnt;
            if (refCnt == 0) {
                throw new BadDescriptorException();
            }
            this.refCnt = refCnt + 1;
        }
    }

    void releaseOne() throws IOException {
        synchronized (this) {
            if (--refCnt == 0) {
                IoHandler handler = this.handler;
                this.handler = null;
                // do this last in case an exception is thrown
                handler.close();
            }
        }
    }

    <H extends IoHandler> void run(Class<H> handlerType, ExceptionConsumer<H, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            action.accept(handlerType.cast(handler));
        }
    }

    <H extends IoHandler, T> void run(Class<H> handlerType, T argument, ExceptionBiConsumer<H, T, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            action.accept(handlerType.cast(handler), argument);
        }
    }

    <H extends IoHandler> void run(Class<H> handlerType, long argument, ExceptionObjLongConsumer<H, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            action.accept(handlerType.cast(handler), argument);
        }
    }

    <H extends IoHandler> void run(Class<H> handlerType, int argument, ExceptionObjIntConsumer<H, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            action.accept(handlerType.cast(handler), argument);
        }
    }

    <H extends IoHandler, R> R call(Class<H> handlerType, ExceptionFunction<H, R, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            return action.apply(handlerType.cast(handler));
        }
    }

    <H extends IoHandler> long call(final Class<H> handlerType, final long argument, final ExceptionObjLongToLongFunction<H, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            return action.applyAsLong(handlerType.cast(handler), argument);
        }
    }

    <H extends IoHandler, T, R> R call(Class<H> handlerType, T argument, ExceptionBiFunction<H, T, R, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            return action.apply(handlerType.cast(handler), argument);
        }
    }

    <H extends IoHandler> long call(Class<H> handlerType, ExceptionToLongFunction<H, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            return action.apply(handlerType.cast(handler));
        }
    }

    <H extends IoHandler, T> long call(Class<H> handlerType, T argument, ExceptionToLongBiFunction<H, T, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            return action.apply(handlerType.cast(handler), argument);
        }
    }

    <H extends IoHandler> boolean test(Class<H> handlerType, ExceptionPredicate<H, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            return action.test(handlerType.cast(handler));
        }
    }

    <H extends IoHandler, T> boolean test(Class<H> handlerType, T argument, ExceptionBiPredicate<H, T, IOException> action) throws IOException {
        synchronized (this) {
            IoHandler handler = this.handler;
            if (! handlerType.isInstance(handler)) {
                throw BadDescriptorException.fileNotOpen();
            }
            return action.test(handlerType.cast(handler), argument);
        }
    }
}
