package cc.quarkus.qcc.runtime.unwind;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.stdc.Stdint.*;

import cc.quarkus.qcc.runtime.Build;

/**
 * Unwind ABI as described at <a href="https://itanium-cxx-abi.github.io/cxx-abi/abi-eh.html">https://itanium-cxx-abi.github.io/cxx-abi/abi-eh.html</a>.
 */
@include("<unwind.h>")
@lib("gcc_s") // todo: -static-libgcc
public final class Unwind {
    private Unwind() {
    }

    public static native _Unwind_Reason_Code _Unwind_RaiseException(ptr<_Unwind_Exception> exception_object);
    public static native void _Unwind_Resume(ptr<_Unwind_Exception> exception_object);
    public static native void _Unwind_DeleteException(ptr<_Unwind_Exception> exception_object);
    public static native uint64_t _Unwind_GetGR(ptr<_Unwind_Context> context, c_int index);
    public static native void _Unwind_SetGR(ptr<_Unwind_Context> context, c_int index, uint64_t new_value);
    public static native uint64_t _Unwind_GetIP(ptr<_Unwind_Context> context);
    public static native void _Unwind_SetIP(ptr<_Unwind_Context> context, uint64_t new_value);
    public static native uint64_t _Unwind_GetRegionStart(ptr<_Unwind_Context> context);
    public static native uint64_t _Unwind_GetLanguageSpecificData(ptr<_Unwind_Context> context);
    public static native _Unwind_Reason_Code _Unwind_ForcedUnwind(ptr<_Unwind_Exception> exception_object, ptr<function<_Unwind_Stop_Fn>> stop, ptr<?> stop_parameter);

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

    /**
     * The header for a thrown exception.  The runtime is expected to create its own thrown structure which includes
     * this structure as a member.  The runtime is responsible for allocating and freeing instances.
     */
    public static final class _Unwind_Exception extends object {
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
        public ptr<function<_Unwind_Exception_Cleanup_Fn>> exception_cleanup;
    }

    @FunctionalInterface
    public interface _Unwind_Exception_Cleanup_Fn {
        void cleanup(_Unwind_Reason_Code reason, ptr<_Unwind_Exception> exc);
    }

    public static final class _Unwind_Context extends object {}

    @FunctionalInterface
    public interface _Unwind_Stop_Fn {
        _Unwind_Reason_Code run(int version, _Unwind_Action actions, uint64_t exception_class, ptr<_Unwind_Context> exception_object, ptr<_Unwind_Context> context, ptr<?> stop_parameter);
    }

    // TODO: support for classic ARM EHABI needs a different prototype for _Unwind_Stop_Fn
}
