package org.qbicc.plugin.methodinfo.valueinfo;

import java.util.Arrays;

import io.smallrye.common.constraint.Assert;

/**
 * Specifies the register that the value is contained within.
 */
public final class RegisterValueInfo extends ValueInfo {
    private static volatile RegisterValueInfo[] cache;

    private final int registerNumber;

    RegisterValueInfo(int registerNumber) {
        super(Integer.hashCode(registerNumber));
        this.registerNumber = registerNumber;
    }

    /**
     * Get the DWARF register number.
     *
     * @return the DWARF register number
     */
    public int getRegisterNumber() {
        return registerNumber;
    }

    /**
     * Get the register value info for the given register number.
     *
     * @param dwarfRegNum the register number
     * @return the register value info
     */
    public static RegisterValueInfo forRegisterNumber(int dwarfRegNum) {
        Assert.checkMinimumParameter("dwarfRegNum", 0, dwarfRegNum);
        Assert.checkMaximumParameter("dwarfRegNum", 63, dwarfRegNum);
        RegisterValueInfo[] cache = RegisterValueInfo.cache;
        if (cache == null || cache.length <= dwarfRegNum) {
            synchronized (RegisterValueInfo.class) {
                cache = RegisterValueInfo.cache;
                if (cache == null || cache.length <= dwarfRegNum) {
                    final RegisterValueInfo[] newArray;
                    int start;
                    if (cache != null) {
                        newArray = Arrays.copyOf(cache, dwarfRegNum + 1);
                        start = cache.length;
                    } else {
                        newArray = new RegisterValueInfo[dwarfRegNum + 1];
                        start = 0;
                    }
                    for (int i = start; i < newArray.length; i ++) {
                        newArray[i] = new RegisterValueInfo(i);
                    }
                    cache = newArray;
                    RegisterValueInfo.cache = cache;
                }
            }
        }
        return cache[dwarfRegNum];
    }

    @Override
    public String toString() {
        return "reg[" + registerNumber + "]";
    }

    @Override
    public boolean equals(ValueInfo other) {
        return other instanceof RegisterValueInfo rvi && equals(rvi);
    }

    public boolean equals(RegisterValueInfo other) {
        return super.equals(other) && registerNumber == other.registerNumber;
    }
}
