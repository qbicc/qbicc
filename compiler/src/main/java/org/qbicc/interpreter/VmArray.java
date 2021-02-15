package org.qbicc.interpreter;

/**
 *
 */
public interface VmArray extends VmObject {
    int getLength();

    int getArrayElementOffset(int index);
}
