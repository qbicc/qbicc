package org.qbicc.machine.llvm.stackmap;

/**
 * A visitor for stack maps.
 */
public interface StackMapVisitor {
    /**
     * Handle the start of the stack map.
     *
     * @param version the stack map version
     * @param fnCount the number of functions in the stack map
     * @param recCount the total number of records in the stack map
     */
    default void start(int version, long fnCount, long recCount) {}

    /**
     * Handle the start of a function.
     *
     * @param fnIndex the index of the function, counting up from 0
     * @param address the function address
     * @param stackSize the function stack size
     * @param recordCount the number of records in this function
     */
    default void startFunction(long fnIndex, long address, long stackSize, long recordCount) {}

    /**
     * Handle the end of the function.
     *
     * @param fnIndex the index of the function
     */
    default void endFunction(long fnIndex) {}

    /**
     * Handle the start of a record within a function.
     *
     * @param recIndex the index of the record within the function, counting up from 0
     * @param patchPointId the patch point ID
     * @param offset the instruction offset
     * @param locCnt the number of locations in this record
     * @param liveOutCnt the number of live-outs in this record
     */
    default void startRecord(long recIndex, long patchPointId, long offset, int locCnt, int liveOutCnt) {}

    /**
     * Handle the end of a record within a function.
     *
     * @param recIndex the index of the record within the function
     */
    default void endRecord(long recIndex) {}

    /**
     * Handle a location within a record.
     *
     * @param locIndex the index of the location within the record, counting up from 0
     * @param type the location type
     * @param size the location size
     * @param regNum the register number (only valid if {@code type} is {@link LocationType#Register Register},
     *               {@link LocationType#Direct Direct}, or {@link LocationType#Indirect Indirect}
     * @param data the location's offset or value
     */
    default void location(int locIndex, LocationType type, int size, int regNum, long data) {}

    /**
     * Handle a live-out within a record.
     *
     * @param liveOutIndex the index of the live-out within the record, counting up from 0
     * @param regNum the register number
     * @param size the size of the value
     */
    default void liveOut(int liveOutIndex, int regNum, int size) {}

    /**
     * Handle the end of the stack map.
     */
    default void end() {}
}
