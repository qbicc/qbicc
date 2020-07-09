package cc.quarkus.qcc.interpreter;

import static java.lang.Math.*;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;
import cc.quarkus.qcc.type.universe.Universe;

final class JavaVMImpl implements JavaVM {
    private boolean exited;
    private int exitCode = -1;
    private final Lock vmLock = new ReentrantLock();
    private final Condition stopCondition = vmLock.newCondition();
    private final Condition signalCondition = vmLock.newCondition();
    private final ArrayDeque<Signal> signalQueue = new ArrayDeque<>();
    private final Set<JavaThread> threads = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<JavaThread> attachedThread = new ThreadLocal<>();
    private final JavaClassImpl classClass;
    private final ConcurrentMap<JavaObject, Universe> classLoaderLoaders = new ConcurrentHashMap<>();
    private final ConcurrentMap<Universe, JavaObject> loaderClassLoaders = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClassType, JavaClassImpl> loadedClasses = new ConcurrentHashMap<>();

    JavaVMImpl(Function<String, ByteBuffer> classFinder) {
        Universe bootstrapLoader = new Universe(null);
        defineBootClass(bootstrapLoader, classFinder, "java/lang/Object");
        VerifiedTypeDefinition classClassDef = defineBootClass(bootstrapLoader, classFinder, "java/lang/Class").verify();
        classClass = new JavaClassImpl(this, classClassDef, true /* special ctor for Class.class */);
    }

    private static DefinedTypeDefinition defineBootClass(final Universe bootstrapLoader, final Function<String, ByteBuffer> classFinder, String name) {
        ByteBuffer bytes = classFinder.apply(name);
        if (bytes == null) {
            throw new IllegalArgumentException("Initial class finder cannot find bootstrap class \"" + name + "\"");
        }
        return bootstrapLoader.defineClass(name, bytes);
    }

    public JavaThread newThread(final String threadName, final JavaObject threadGroup, final boolean daemon) {
        return new JavaThreadImpl(threadName, threadGroup, daemon, this);
    }

    void tryAttach(JavaThread thread) throws IllegalStateException {
        if (attachedThread.get() != null) {
            throw new IllegalStateException("Thread is already attached");
        }
        attachedThread.set(thread);
    }

    void detach(JavaThread thread) throws IllegalStateException {
        JavaThread existing = attachedThread.get();
        if (existing != thread) {
            throw new IllegalStateException("Thread is not attached");
        }
        attachedThread.remove();
    }

    public JavaThread currentThread() {
        return attachedThread.get();
    }

    public void deliverSignal(final Signal signal) {
        vmLock.lock();
        try {
            signalQueue.addLast(signal);
            signalCondition.notify();
        } finally {
            vmLock.unlock();
        }
    }

    Signal awaitSignal() throws InterruptedException {
        vmLock.lockInterruptibly();
        try {
            Signal signal;
            for (;;) {
                signal = signalQueue.pollFirst();
                if (signal != null) {
                    return signal;
                }
                signalCondition.await();
            }
        } finally {
            vmLock.unlock();
        }
    }

    public int awaitTermination() throws InterruptedException {
        vmLock.lockInterruptibly();
        try {
            while (! threads.isEmpty()) {
                stopCondition.await();
            }
            return max(0, exitCode);
        } finally {
            vmLock.unlock();
        }
    }

    void exit(int status) {
        vmLock.lock();
        try {
            if (! exited) {
                exitCode = status;
                exited = true;
            }
        } finally {
            vmLock.unlock();
        }
    }

    public void close() {
        vmLock.lock();
        try {
            exit(134); // SIGKILL-ish
            for (JavaThread thread : threads) {
                thread.close();
            }
        } finally {
            vmLock.unlock();
        }
    }

    JavaClassImpl getClassClass() {
        return classClass;
    }

    Universe getDictionaryFor(final JavaObject classLoader) {
        Universe dictionary = classLoaderLoaders.get(classLoader);
        if (dictionary == null) {
            throw new IllegalStateException("Class loader object is unknown");
        }
        return dictionary;
    }

    JavaObject getClassLoaderFor(final Universe dictionary) {
        JavaObject classLoader = loaderClassLoaders.get(dictionary);
        if (classLoader == null) {
            throw new IllegalStateException("Class loader object is unknown");
        }
        return classLoader;
    }

    JavaClassImpl getJavaClassOf(final ClassType type) {
        return loadedClasses.get(type);
    }

    void registerJavaClassOf(final ClassType classType, final JavaClassImpl javaClass) {
        if (loadedClasses.putIfAbsent(classType, javaClass) != null) {
            throw new IllegalStateException("Class registered twice");
        }
    }

    void registerDictionaryFor(final JavaObject classLoader, final Universe dictionary) {
        if (classLoaderLoaders.putIfAbsent(classLoader, dictionary) != null) {
            throw new IllegalStateException("Class loader already registered");
        }
        if (loaderClassLoaders.putIfAbsent(dictionary, classLoader) != null) {
            throw new IllegalStateException("Class loader already registered (partially)");
        }
    }
}
