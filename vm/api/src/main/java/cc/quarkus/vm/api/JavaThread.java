package cc.quarkus.vm.api;

import java.nio.ByteBuffer;

/**
 * A thread in the inner VM.
 * <p>
 * Any unclosed thread can be invoked upon as frequently as desired, as long as it is
 * attached to the current calling thread.  Unattached threads cannot be invoked upon.
 */
public interface JavaThread extends AutoCloseable {
    /**
     * Perform the given action with this thread attached to the host thread.  Any previously attached thread
     * is suspended.
     *
     * @param r the action to perform
     */
    void doAttached(Runnable r);

    JavaClass defineClass(String name, JavaObject classLoader, ByteBuffer bytes);

    JavaObject allocateObject(JavaClass type);

    void initObject(JavaObject newObj, Object... args);

    JavaMethod lookupStaticMethod(JavaClass type, String signature);

    JavaMethod lookupInstanceMethod(JavaClass type, String signature);

    void callStaticVoid(JavaMethod method, Object... args);

    // todo: callStatic<Primitive>, callStaticObject, callInstance*, callInstanceExact*

    // todo: get/put fields

    JavaObject newString(String str);

    // todo: arrays

    JavaVM getVM();

    String getStringRegion(JavaObject string, int offs, int len);

    boolean isRunning();

    boolean isFinished();

    /**
     * Close a thread.  The thread will exit (potentially catastrophically if the stack is non-empty).
     */
    void close();
}
