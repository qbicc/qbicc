package cc.quarkus.qcc.machine.llvm;

/**
 *
 */
public interface Function extends Metable {
    // basic stuff

    Function returns(Value returnType);

    Parameter param(Value type);

    Function linkage(Linkage linkage);

    Function visibility(Visibility visibility);

    Function dllStorageClass(DllStorageClass dllStorageClass);

    Function callingConvention(CallingConvention callingConvention);

    Function addressNaming(AddressNaming addressNaming);

    Function addressSpace(int addressSpace);

    Function alignment(int alignment);

    Function meta(String name, Value metadata);

    Function comment(String comment);

    interface Parameter {
        /**
         * Start the next parameter.
         *
         * @param type the parameter type
         * @return the next parameter
         */
        Parameter param(Value type);

        Parameter name(String name);
    }
}
