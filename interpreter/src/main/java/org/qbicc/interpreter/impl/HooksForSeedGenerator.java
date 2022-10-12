package org.qbicc.interpreter.impl;

import java.security.SecureRandom;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForSeedGenerator {
    HooksForSeedGenerator() {}

    @Hook
    static VmArray getSystemEntropy(VmThread thread) {
        return thread.getVM().newByteArray(new SecureRandom().generateSeed(20));
    }

    @Hook
    static void generateSeed(VmThread thread, VmByteArrayImpl bytes) {
        byte[] seed = new SecureRandom().generateSeed(bytes.getLength());
        for (int i = 0; i < seed.length; i++) {
            bytes.getArray()[i] = seed[i];
        }
    }
}
