package cc.quarkus.qcc.interpreter;

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

    /**
     * Ensure that the given class is initialized.
     *
     * @param clazz the class to initialize
     */
    void initClass(JavaClass clazz);

    JavaObject allocateObject(JavaClass type);

    void initObject(JavaObject newObj, JavaConstructor ctor, Object... args);

    JavaConstructor lookupConstructor(JavaClass type, String name, String signature);

    JavaMethod lookupStaticMethod(JavaClass type, String name, String signature);

    JavaMethod lookupInstanceMethod(JavaClass type, String name, String signature);

    void callStaticVoid(JavaMethod method, Object... args);

    // todo: callStatic<Primitive>, callStaticObject, callInstance*, callInstanceExact*

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
