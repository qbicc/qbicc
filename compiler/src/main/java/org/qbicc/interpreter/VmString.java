package org.qbicc.interpreter;

/**
 *
 */
public interface VmString extends VmObject {
    String getContent();

    boolean contentEquals(String string);

    int hashCode();
}
