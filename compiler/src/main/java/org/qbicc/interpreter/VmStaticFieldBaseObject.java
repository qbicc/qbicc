package org.qbicc.interpreter;

/**
 * A "fake" object used as the static field base for some class.
 */
public interface VmStaticFieldBaseObject extends VmObject {

    /**
     * Get the class for which this object represents the static field base.
     *
     * @return the class (not {@code null})
     */
    VmClass getEnclosedVmClass();

}
