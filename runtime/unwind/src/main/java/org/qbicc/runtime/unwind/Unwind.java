package org.qbicc.runtime.unwind;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

import org.qbicc.runtime.Build;

/**
 * Unwind ABI as described at <a href="https://itanium-cxx-abi.github.io/cxx-abi/abi-eh.html">https://itanium-cxx-abi.github.io/cxx-abi/abi-eh.html</a>.
 */
@include("<unwind.h>")
@lib("gcc_s") // todo: -static-libgcc
public final class Unwind {
    private Unwind() {
    }

    public static native _Unwind_Reason_Code _Unwind_RaiseException(struct__Unwind_Exception_ptr exception_object);
    public static native void _Unwind_Resume(struct__Unwind_Exception_ptr exception_object);
    public static native void _Unwind_DeleteException(struct__Unwind_Exception_ptr exception_object);
    public static native uint64_t _Unwind_GetGR(struct__Unwind_Context_ptr context, c_int index);
    public static native void _Unwind_SetGR(struct__Unwind_Context_ptr context, c_int index, uint64_t new_value);
    public static native uint64_t _Unwind_GetIP(struct__Unwind_Context_ptr context);
    public static native void _Unwind_SetIP(struct__Unwind_Context_ptr context, uint64_t new_value);
    public static native uint64_t _Unwind_GetRegionStart(struct__Unwind_Context_ptr context);
    public static native uint64_t _Unwind_GetLanguageSpecificData(struct__Unwind_Context_ptr context);
    public static native _Unwind_Reason_Code _Unwind_ForcedUnwind(struct__Unwind_Exception_ptr exception_object, function_ptr<_Unwind_Stop_Fn> stop, void_ptr stop_parameter);

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
            SP = word(4); // esp
        } else {
            throw new IllegalStateException("Unsupported architecture");
        }
    }

    /**
     * The value for QCC's exception class; equal to {@code "QCC\0JAVA"}.
     */
    public static final uint64_t QCC_EXCEPTION_CLASS = word(0x514343004a415641L);

    /**
     * The header for a thrown exception.  The runtime is expected to create its own thrown structure which includes
     * this structure as a member.  The runtime is responsible for allocating and freeing instances.
     */
    public static final class struct__Unwind_Exception extends object {
        /**
         * "A language- and implementation-specific identifier of the kind of exception. It allows a personality routine
         * to distinguish between native and foreign exceptions, for example. By convention, the high 4 bytes indicate
         * the vendor (for instance HP\0\0), and the low 4 bytes indicate the language. For the C++ ABI described in
         * this document, the low four bytes are C++\0."
         * <p>
         * The ABI reports this as {@code uint64}, but GNU appears to use a {@code char[8]} which seems like it would be
         * wrong on big-endian systems...
         * <p>
         * GCJ uses "GNUCJAVA". We would use "QCC\0JAVA".
         */
        public uint64_t exception_class;
        /**
         * The function which frees this object.
         */
        public function_ptr<_Unwind_Exception_Cleanup_Fn> exception_cleanup;
    }

    public static final class struct__Unwind_Exception_ptr extends ptr<struct__Unwind_Exception> {}
    public static final class const_struct__Unwind_Exception_ptr extends ptr<@c_const struct__Unwind_Exception> {}
    public static final class struct__Unwind_Exception_ptr_ptr extends ptr<struct__Unwind_Exception_ptr> {}
    public static final class const_struct__Unwind_Exception_ptr_ptr extends ptr<const_struct__Unwind_Exception_ptr> {}
    public static final class struct__Unwind_Exception_ptr_const_ptr extends ptr<@c_const struct__Unwind_Exception_ptr> {}
    public static final class const_struct__Unwind_Exception_ptr_const_ptr extends ptr<@c_const const_struct__Unwind_Exception_ptr> {}

    @FunctionalInterface
    public interface _Unwind_Exception_Cleanup_Fn {
        void cleanup(_Unwind_Reason_Code reason, struct__Unwind_Exception_ptr exc);
    }

    @incomplete
    public static final class struct__Unwind_Context extends object {}

    public static final class struct__Unwind_Context_ptr extends ptr<struct__Unwind_Context> {}
    public static final class const_struct__Unwind_Context_ptr extends ptr<@c_const struct__Unwind_Context> {}
    public static final class struct__Unwind_Context_ptr_ptr extends ptr<struct__Unwind_Context_ptr> {}
    public static final class const_struct__Unwind_Context_ptr_ptr extends ptr<const_struct__Unwind_Context_ptr> {}
    public static final class struct__Unwind_Context_ptr_const_ptr extends ptr<@c_const struct__Unwind_Context_ptr> {}
    public static final class const_struct__Unwind_Context_ptr_const_ptr extends ptr<@c_const const_struct__Unwind_Context_ptr> {}



    @FunctionalInterface
    public interface _Unwind_Stop_Fn {
        _Unwind_Reason_Code run(c_int version, _Unwind_Action actions, uint64_t exception_class, struct__Unwind_Context_ptr exception_object, struct__Unwind_Context_ptr context, void_ptr stop_parameter);
    }

    public static _Unwind_Reason_Code personality(c_int version, _Unwind_Action action, uint64_t exceptionClass,
                                    struct__Unwind_Exception_ptr exceptionObject, struct__Unwind_Context_ptr context) {
        uint64_t ip = _Unwind_GetIP(context);
        uint64_t methodStart = _Unwind_GetRegionStart(context);
        uint64_t lsda = _Unwind_GetLanguageSpecificData(context);

        long offset = ip.longValue() - methodStart.longValue();
        uint8_t_ptr lsdaPtr = lsda.cast(uint8_t_ptr.class);
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

    public static long getHandlerOffset(uint8_t_ptr lsda, long pcOffset) {
        int[] offset = new int[1]; /* TODO: Avoid using new in these methods. offset should instead be passed as pointer */
        offset[0] = 0;
        uint8_t header = lsda.get(offset[0]);   // encoding of landingpad base which is generally DW_EH_PE_omit(0xff)
        int headerValue = header.byteValue();
        offset[0] += 1;
        uint8_t typeEncodingEncoding = lsda.get(offset[0]);
        offset[0] += 1;
        long typeBaseOffset = readULEB(lsda, offset);
        uint8_t callSiteEncodingEncoding = lsda.get(offset[0]);
        offset[0] += 1;
        int callSiteEncoding = callSiteEncodingEncoding.byteValue();
        long callSiteTableLength = readULEB(lsda, offset);
        long callSiteTableEnd = offset[0] + callSiteTableLength;
        while (offset[0] < callSiteTableEnd) {
            long startOffset = read(lsda, offset, callSiteEncoding);
            long size = read(lsda, offset, callSiteEncoding);
            long lpOffset = read(lsda, offset, callSiteEncoding);
            long action = readULEB(lsda, offset);
            if ((startOffset <= pcOffset) && (pcOffset < (startOffset + size))) {
                return lpOffset;
            }
        }
        return headerValue;
    }

    public static long read(uint8_t_ptr lsda, int[] offset, int callSiteEncoding) {
        long result = 0;
        switch(callSiteEncoding) {
            case 0x1:
                result = readULEB(lsda, offset);
                break;
            case 0x3:
                uint32_t_ptr temp32 = lsda.plus(offset[0]).cast(uint32_t_ptr.class);
                result = temp32.deref().longValue();
                offset[0] += 4;
                break;
            default:
                break;
        }
        return result;
    }

    public static long readULEB(uint8_t_ptr lsda, int[] offset) {
        long result = 0;
        int shift = 0;
        byte singleByte = 0;
        do {
            singleByte = lsda.get(offset[0]).byteValue();
            offset[0] += 1;
            result |= (singleByte & 0x7f) << shift;
            shift += 7;
        } while ((singleByte & 0x80) != 0);
        return result;
    }

    // TODO: support for classic ARM EHABI needs a different prototype for _Unwind_Stop_Fn
}
