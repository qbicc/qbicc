package cc.quarkus.qcc.machine.llvm;

import cc.quarkus.qcc.machine.llvm.op.Call;

/**
 *
 */
public interface Function extends Metable {
    // basic stuff

    Function returns(LLValue returnType);

    Parameter param(LLValue type);

    Function linkage(Linkage linkage);

    Function visibility(Visibility visibility);

    Function dllStorageClass(DllStorageClass dllStorageClass);

    Function callingConvention(CallingConvention callingConvention);

    Function addressNaming(AddressNaming addressNaming);

    Function addressSpace(int addressSpace);

    Function alignment(int alignment);

    Function variadic();

    Function meta(String name, LLValue metadata);

    Function comment(String comment);

    Function signExt();

    Function zeroExt();

    LLValue asGlobal();

    interface Parameter {
        /**
         * Start the next parameter.
         *
         * @param type the parameter type
         * @return the next parameter
         */
        Parameter param(LLValue type);

        Parameter name(String name);

        Parameter signExt();

        Parameter zeroExt();

        LLValue type();

        LLValue asValue();
    }
}
