package org.qbicc.runtime.unwind;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;

import org.qbicc.runtime.Build;

/**
 * Unwind ABI as described at <a href="https://itanium-cxx-abi.github.io/cxx-abi/abi-eh.html">https://itanium-cxx-abi.github.io/cxx-abi/abi-eh.html</a>.
 */
@include("<unwind.h>")
@lib(value = "gcc_s", unless = { Build.Target.IsMacOs.class, Build.Target.IsWasm.class } ) // todo: -static-libgcc
@lib(value = "gcc_s.1", when = Build.Target.IsMacOs.class) // todo: -static-libgcc
public final class Unwind {
    private Unwind() {
    }

    public static native _Unwind_Reason_Code _Unwind_RaiseException(ptr<struct__Unwind_Exception> exception_object);
    public static native void _Unwind_Resume(ptr<struct__Unwind_Exception> exception_object);
    public static native void _Unwind_DeleteException(ptr<struct__Unwind_Exception> exception_object);
    public static native unsigned_long _Unwind_GetGR(ptr<struct__Unwind_Context> context, c_int index);
    public static native void _Unwind_SetGR(ptr<struct__Unwind_Context> context, c_int index, unsigned_long new_value);
    public static native unsigned_long _Unwind_GetIP(ptr<struct__Unwind_Context> context);
    public static native void _Unwind_SetIP(ptr<struct__Unwind_Context> context, unsigned_long new_value);
    public static native unsigned_long _Unwind_GetRegionStart(ptr<struct__Unwind_Context> context);
    public static native unsigned_long _Unwind_GetLanguageSpecificData(ptr<struct__Unwind_Context> context);
    public static native _Unwind_Reason_Code _Unwind_ForcedUnwind(ptr<struct__Unwind_Exception> exception_object, ptr<function<_Unwind_Stop_Fn>> stop, ptr<?> stop_parameter);

    public static final class _Unwind_Reason_Code extends word {}

    public static final _Unwind_Reason_Code _URC_NO_REASON = constant();
    public static final _Unwind_Reason_Code _URC_OK = constant();
    public static final _Unwind_Reason_Code _URC_FOREIGN_EXCEPTION_CAUGHT = constant();
    public static final _Unwind_Reason_Code _URC_FATAL_PHASE2_ERROR = constant();
    public static final _Unwind_Reason_Code _URC_FATAL_PHASE1_ERROR = constant();
    public static final _Unwind_Reason_Code _URC_NORMAL_STOP = constant();
    public static final _Unwind_Reason_Code _URC_END_OF_STACK = constant();
    public static final _Unwind_Reason_Code _URC_HANDLER_FOUND = constant();
    public static final _Unwind_Reason_Code _URC_INSTALL_CONTEXT = constant();
    public static final _Unwind_Reason_Code _URC_CONTINUE_UNWIND = constant();
    @incomplete(unless = Build.Target.IsArm.class) // EH ABI only?
    public static final _Unwind_Reason_Code _URC_FAILURE = constant();

    public static final class _Unwind_Action extends word {}

    public static final _Unwind_Action _UA_SEARCH_PHASE = constant();
    public static final _Unwind_Action _UA_CLEANUP_PHASE = constant();
    public static final _Unwind_Action _UA_HANDLER_FRAME = constant();
    public static final _Unwind_Action _UA_FORCE_UNWIND = constant();
    public static final _Unwind_Action _UA_END_OF_STACK = constant(); // gcc extension...

    public static final c_int SP;

    static {
        if (Build.Target.isAarch64()) {
            SP = word(31); // SP
        } else if (Build.Target.isArm()) {
            SP = word(13); // R13
        } else if (Build.Target.isAmd64()) {
            SP = word(7); // rsp
        } else if (Build.Target.isI386()) {
            SP = word(4); // esp {
        } else if (Build.Target.isWasm()){
            SP = word(4);
        } else {
            throw new IllegalStateException("Unsupported architecture");
        }
    }

    /**
     * The value for QBICC's exception class; equal to {@code "qbicJAVA"}.
     */
    public static final uint64_t QBICC_EXCEPTION_CLASS = word(0x716269634a415641L);

    /**
     * The header for a thrown exception.  The runtime is expected to create its own thrown structure which includes
     * this structure as a member.  The runtime is responsible for allocating and freeing instances.
     */
    @align_as(max_align_t.class) // Force alignment; See https://github.com/qbicc/qbicc/pull/623 for discussion of problem with MacOS unwind.h
    public static final class struct__Unwind_Exception extends struct {
        /**
         * "A language- and implementation-specific identifier of the kind of exception. It allows a personality routine
         * to distinguish between native and foreign exceptions, for example. By convention, the high 4 bytes indicate
         * the vendor (for instance HP\0\0), and the low 4 bytes indicate the language. For the C++ ABI described in
         * this document, the low four bytes are C++\0."
         * <p>
         * The ABI reports this as {@code uint64}, but GNU appears to use a {@code char[8]} which seems like it would be
         * wrong on big-endian systems...
         * <p>
         * GCJ uses "GNUCJAVA". We would use "qbicJAVA".
         */
        public uint64_t exception_class;
        /**
         * The function which frees this object.
         */
        public ptr<function<_Unwind_Exception_Cleanup_Fn>> exception_cleanup;
    }

    @FunctionalInterface
    public interface _Unwind_Exception_Cleanup_Fn {
        void cleanup(_Unwind_Reason_Code reason, ptr<struct__Unwind_Exception> exc);
    }

    @incomplete
    public static final class struct__Unwind_Context extends struct {}

    @FunctionalInterface
    public interface _Unwind_Stop_Fn {
        _Unwind_Reason_Code run(c_int version, _Unwind_Action actions, uint64_t exception_class, ptr<struct__Unwind_Context> exception_object, ptr<struct__Unwind_Context> context, ptr<?> stop_parameter);
    }

    @export
    public static _Unwind_Reason_Code personality(c_int version, _Unwind_Action action, uint64_t exceptionClass,
                                    ptr<struct__Unwind_Exception> exceptionObject, ptr<struct__Unwind_Context> context) {
        unsigned_long ip = _Unwind_GetIP(context);
        // ip points to instruction after the call, therefore subtract 1 to bring it in the call instruction range.
        ip = word(ip.longValue() - 1);
        unsigned_long methodStart = _Unwind_GetRegionStart(context);
        unsigned_long lsda = _Unwind_GetLanguageSpecificData(context);

        long offset = ip.longValue() - methodStart.longValue();
        ptr<uint8_t> lsdaPtr = lsda.cast();
        long lpOffset = getHandlerOffset(lsdaPtr, offset);
        if ((action.intValue() & _UA_SEARCH_PHASE.intValue()) != 0) {
            if (lpOffset == 0) {
                return _URC_CONTINUE_UNWIND;
            }
            return _URC_HANDLER_FOUND;
        } else if ((action.intValue() & _UA_HANDLER_FRAME.intValue()) != 0) {
            _Unwind_SetIP(context, word(methodStart.longValue() + lpOffset));
            return _URC_INSTALL_CONTEXT;
        } else {
            return _URC_CONTINUE_UNWIND;
        }
    }

    public static long getHandlerOffset(ptr<uint8_t> lsda, long pcOffset) {
        int offset = auto(0);
        ptr<int32_t> offsetPtr = addr_of(offset);
        uint8_t header = lsda.plus(offset++).loadPlain();   // encoding of landingpad base which is generally DW_EH_PE_omit(0xff)
        uint8_t typeEncodingEncoding = lsda.plus(offset++).loadPlain();
        if (typeEncodingEncoding.byteValue() != (byte)0xff) {   // check for DW_EH_PE_omit(0xff)
            long typeBaseOffset = readULEB(lsda, offsetPtr);
        }
        uint8_t callSiteEncodingEncoding = lsda.plus(offset++).loadPlain();
        int callSiteEncoding = callSiteEncodingEncoding.byteValue();
        long callSiteTableLength = readULEB(lsda, offsetPtr);
        long callSiteTableEnd = offset + callSiteTableLength;
        while (offsetPtr.loadUnshared().intValue() < callSiteTableEnd) {
            long startOffset = read(lsda, offsetPtr, callSiteEncoding);
            long size = read(lsda, offsetPtr, callSiteEncoding);
            long lpOffset = read(lsda, offsetPtr, callSiteEncoding);
            long action = readULEB(lsda, offsetPtr);
            if ((startOffset <= pcOffset) && (pcOffset < (startOffset + size))) {
                return lpOffset;
            }
        }
        return 0;
    }

    public static long read(ptr<uint8_t> lsda, ptr<int32_t> offset, int callSiteEncoding) {
        long result = 0;
        switch (callSiteEncoding) {
            case 0x1 -> result = readULEB(lsda, offset);
            case 0x3 -> {
                int offsetVal = offset.loadPlain().intValue();
                ptr<uint32_t> adjustedLsda = lsda.plus(offsetVal).cast();
                result = adjustedLsda.loadPlain().longValue();
                offset.storePlain(word(offsetVal + 4));
            }
            default -> {
            }
        }
        return result;
    }

    public static long readULEB(ptr<uint8_t> lsda, ptr<int32_t> offset) {
        long result = 0;
        int shift = 0;
        byte singleByte = 0;
        do {
            int offsetVal = offset.loadPlain().intValue();
            singleByte = lsda.plus(offsetVal).loadPlain().byteValue();
            offset.storePlain(word(offsetVal + 1));
            result |= (singleByte & 0x7fL) << shift;
            shift += 7;
        } while ((singleByte & 0x80) != 0);
        return result;
    }

    // TODO: support for classic ARM EHABI needs a different prototype for _Unwind_Stop_Fn
}
