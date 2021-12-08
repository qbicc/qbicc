package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;

@SuppressWarnings("SpellCheckingInspection")
@include("<sched.h>")
@define(value = "_GNU_SOURCE")
public class Sched {
    public static native c_int sched_setaffinity(pid_t pid, size_t cpusetsize, const_cpu_set_t_ptr mask);

    public static native c_int sched_getaffinity(pid_t pid, size_t cpusetsize, cpu_set_t_ptr mask);

    // These methods simulate the behavior of the corresponding glibc macros.

    public static void CPU_ZERO(cpu_set_t_ptr set) {
        CPU_ZERO_S(sizeof(cpu_set_t.class), set);
    }

    public static void CPU_ZERO_S(size_t setSize, cpu_set_t_ptr set) {
        memset(set.cast(), zero(), setSize);
    }

    public static void CPU_SET(int cpu, cpu_set_t_ptr set) {
        CPU_SET_S(sizeof(cpu_set_t.class), cpu, set);
    }

    public static void CPU_SET_S(size_t size, int cpu, cpu_set_t_ptr set) {
        ptr<__cpu_mask> ptr = getMaskPtr(size, cpu, set);
        if (ptr != null) {
            ptr.storeUnshared(word(ptr.loadUnshared().longValue() | 1L << getBitIdx(cpu)));
        }
    }

    public static void CPU_CLR(int cpu, cpu_set_t_ptr set) {
        CPU_CLR_S(sizeof(cpu_set_t.class), cpu, set);
    }

    public static void CPU_CLR_S(size_t size, int cpu, cpu_set_t_ptr set) {
        ptr<__cpu_mask> ptr = getMaskPtr(size, cpu, set);
        if (ptr != null) {
            ptr.storeUnshared(word(ptr.loadUnshared().longValue() & 1L << getBitIdx(cpu)));
        }
    }

    public static boolean CPU_ISSET(int cpu, cpu_set_t_ptr set) {
        return CPU_ISSET_S(sizeof(cpu_set_t.class), cpu, set);
    }

    public static boolean CPU_ISSET_S(size_t size, int cpu, cpu_set_t_ptr set) {
        ptr<__cpu_mask> ptr = getMaskPtr(size, cpu, set);
        return ptr != null && (ptr.loadUnshared().longValue() & 1L << getBitIdx(cpu)) != 0;
    }

    public static int CPU_COUNT(cpu_set_t_ptr set) {
        return CPU_COUNT_S(sizeof(cpu_set_t.class), set);
    }

    public static int CPU_COUNT_S(size_t size, cpu_set_t_ptr set) {
        int total = 0;
        for (int i = 0; i < size.longValue() / sizeof(__cpu_mask.class).longValue(); i ++) {
            total += Long.bitCount(getMaskPtr(size, i, set).loadUnshared().longValue());
        }
        return total;
    }

    public static void CPU_AND(cpu_set_t_ptr dest, cpu_set_t_ptr src1, cpu_set_t_ptr src2) {
        CPU_AND_S(sizeof(cpu_set_t.class), dest, src1, src2);
    }
    public static void CPU_AND_S(size_t size, cpu_set_t_ptr dest, cpu_set_t_ptr src1, cpu_set_t_ptr src2) {
        for (int i = 0; i < size.longValue() / sizeof(__cpu_mask.class).longValue(); i ++) {
            getMaskPtr(size, i, dest).storeUnshared(word(getMaskPtr(size, i, src1).loadUnshared().longValue() & getMaskPtr(size, i, src2).loadUnshared().longValue()));
        }
    }

    public static void CPU_OS(cpu_set_t_ptr dest, cpu_set_t_ptr src1, cpu_set_t_ptr src2) {
        CPU_OS_S(sizeof(cpu_set_t.class), dest, src1, src2);
    }
    public static void CPU_OS_S(size_t size, cpu_set_t_ptr dest, cpu_set_t_ptr src1, cpu_set_t_ptr src2) {
        for (int i = 0; i < size.longValue() / sizeof(__cpu_mask.class).longValue(); i ++) {
            getMaskPtr(size, i, dest).storeUnshared(word(getMaskPtr(size, i, src1).loadUnshared().longValue() & getMaskPtr(size, i, src2).loadUnshared().longValue()));
        }
    }

    public static void CPU_XOR(cpu_set_t_ptr dest, cpu_set_t_ptr src1, cpu_set_t_ptr src2) {
        CPU_XOR_S(sizeof(cpu_set_t.class), dest, src1, src2);
    }
    public static void CPU_XOR_S(size_t size, cpu_set_t_ptr dest, cpu_set_t_ptr src1, cpu_set_t_ptr src2) {
        for (int i = 0; i < size.longValue() / sizeof(__cpu_mask.class).longValue(); i ++) {
            getMaskPtr(size, i, dest).storeUnshared(word(getMaskPtr(size, i, src1).loadUnshared().longValue() & getMaskPtr(size, i, src2).loadUnshared().longValue()));
        }
    }

    public static boolean CPU_EQUAL(cpu_set_t_ptr set1, cpu_set_t_ptr set2) {
        return CPU_EQUAL_S(sizeof(cpu_set_t.class), set1, set2);
    }
    public static boolean CPU_EQUAL_S(size_t size, cpu_set_t_ptr set1, cpu_set_t_ptr set2) {
        return memcmp(set1.cast(), set2.cast(), size).isZero();
    }

    public static size_t CPU_ALLOC_SIZE(int numCpus) {
        return word(numCpus / sizeof(__cpu_mask.class).intValue());
    }

    public static cpu_set_t_ptr CPU_ALLOC(int numCpus) {
        return malloc(CPU_ALLOC_SIZE(numCpus));
    }
    public static void CPU_FREE(cpu_set_t_ptr set) {
        free(set);
    }

    static final class __cpu_mask extends word {}

    public static final class cpu_set_t extends object {
        __cpu_mask[] __bits;
    }
    public static final class cpu_set_t_ptr extends ptr<cpu_set_t> {}
    public static final class const_cpu_set_t_ptr extends ptr<@c_const cpu_set_t> {}

    // private support methods

    private static ptr<__cpu_mask> getMaskPtr(size_t size, int cpu, cpu_set_t_ptr set) {
        int wordIdx = getWordIdx(cpu);
        if (wordIdx < size.intValue()) {
            return addr_of(set.sel().__bits[wordIdx]);
        } else {
            throw new IllegalArgumentException("Invalid CPU index");
        }
    }

    private static int getWordIdx(final int cpu) {
        return cpu / sizeof(__cpu_mask.class).intValue();
    }

    private static int getBitIdx(final int cpu) {
        return cpu % sizeof(__cpu_mask.class).intValue();
    }
}
