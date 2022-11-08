package org.qbicc.graph;

/**
 * Represents a node that cannot be scheduled.
 */
public interface Unschedulable extends Node {
    @Override
    default int getScheduleIndex() {
        return -1;
    }

    @Override
    default void setScheduleIndex(int index) {
        throw new UnsupportedOperationException("Cannot schedule unschedulable node");
    }
}
