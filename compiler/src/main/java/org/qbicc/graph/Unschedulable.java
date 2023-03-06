package org.qbicc.graph;

import java.util.Set;

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

    @Override
    default BasicBlock getScheduledBlock() {
        return null;
    }

    @Override
    default int getBlockIndex() {
        return -1;
    }

    @Override
    default void setScheduledBlock(BasicBlock block) {
        throw new UnsupportedOperationException("Cannot schedule unschedulable node");
    }

    @Override
    default Set<Value> getLiveIns() {
        return Set.of();
    }

    @Override
    default void setLiveIns(Set<Value> live) {
        throw new UnsupportedOperationException("Cannot schedule unschedulable node");
    }

    @Override
    default Set<Value> getLiveOuts() {
        return Set.of();
    }

    @Override
    default void setLiveOuts(Set<Value> live) {
        throw new UnsupportedOperationException("Cannot schedule unschedulable node");
    }
}
