package org.qbicc.machine.llvm;

/**
 *
 */
public interface Function extends Metable, Attributable {
    // basic stuff

    Returns returns(LLValue returnType);

    Parameter param(LLValue type);

    Function linkage(Linkage linkage);

    Function visibility(Visibility visibility);

    Function dllStorageClass(DllStorageClass dllStorageClass);

    Function callingConvention(CallingConvention callingConvention);

    Function addressNaming(AddressNaming addressNaming);

    Function addressSpace(int addressSpace);

    Function alignment(int alignment);

    Function variadic();

    Function attribute(LLValue attribute);

    Function meta(String name, LLValue metadata);

    Function comment(String comment);

    LLValue asGlobal();

    interface Returns {
        Returns attribute(LLValue attribute);

        LLValue type();
    }

    interface Parameter {
        /**
         * Start the next parameter.
         *
         * @param type the parameter type
         * @return the next parameter
         */
        Parameter param(LLValue type);

        Parameter name(String name);

        Parameter attribute(LLValue attribute);

        Parameter immarg();

        LLValue type();

        LLValue asValue();
    }
}
