package org.qbicc.interpreter;

/**
 * A thread in the inner VM.
 * <p>
 * Any unclosed thread can be invoked upon as frequently as desired, as long as it is
 * attached to the current calling thread.  Unattached threads cannot be invoked upon.
 */
public interface VmThread extends VmObject {

    Vm getVM();

    boolean isRunning();

    boolean isFinished();

    // todo: maybe "join" instead; what about interruption? what about same-thread?
    void await();
}
