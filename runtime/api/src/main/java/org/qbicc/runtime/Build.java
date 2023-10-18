package org.qbicc.runtime;

import static org.qbicc.runtime.CNative.*;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 *
 */
public final class Build {
    private Build() {
    }

    /**
     * Determine if the calling method is operating in the context of the build host.
     *
     * @return {@code true} if the caller is calling from the build host, {@code false} otherwise
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    public static boolean isHost() {
        return false;
    }

    /**
     * Determine if the calling method is operating in the context of the build target.
     *
     * @return {@code true} if the caller is calling from the build target, {@code false} otherwise
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    public static boolean isTarget() {
        return false;
    }

    /**
     * Determine if the calling method is operating in the context of a regular JVM program.
     *
     * @return {@code true} if the caller is calling from a regular JVM, {@code false} otherwise
     */
    @SafePoint(SafePointBehavior.ALLOWED)
    public static boolean isJvm() {
        return true;
    }

    public static final class IsHost implements BooleanSupplier {
        public boolean getAsBoolean() {
            return isHost();
        }
    }

    public static final class IsTarget implements BooleanSupplier {
        public boolean getAsBoolean() {
            return isTarget();
        }
    }

    /**
     * Query methods to determine information about the <em>host</em> (not target) system. These methods can be used
     * to access information about the host at build or run time.
     */
    public static final class Host {

        @Fold
        public static boolean isLinux() {
            return System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT).contains("linux");
        }

        @Fold
        public static boolean isWindows() {
            return System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT).contains("windows");
        }

        @Fold
        public static boolean isMacOs() {
            return System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT).contains("mac os");
        }

        @Fold
        public static boolean isPosix() {
            return isLinux() || isMacOs();
        }

        @Fold
        public static boolean isAmd64() {
            final String archName = System.getProperty("os.arch", "unknown").toLowerCase(Locale.ROOT);
            // linux gives amd64, mac os gives x86_64 :|
            return archName.contains("amd64") || archName.contains("x86_64");
        }

        @Fold
        public static boolean isI386() {
            final String archName = System.getProperty("os.arch", "unknown").toLowerCase(Locale.ROOT);
            return archName.contains("i386") || archName.contains("i486") || archName.contains("i586")
                    || archName.contains("i686");
        }

        @Fold
        public static boolean isArm() {
            final String archName = System.getProperty("os.arch", "unknown").toLowerCase(Locale.ROOT);
            return !archName.contains("arm64") && archName.contains("arm");
        }

        @Fold
        public static boolean isAarch64() {
            final String archName = System.getProperty("os.arch", "unknown").toLowerCase(Locale.ROOT);
            return archName.contains("arm64") || archName.contains("aarch64");
        }

        public static final class IsLinux implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isLinux();
            }
        }

        public static final class IsAmd64 implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isAmd64();
            }
        }

        public static final class IsArm implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isArm();
            }
        }
    }

    /**
     * Query methods to determine information about the <em>target</em> (not host) system. These methods can be used
     * to access information about the target at build or run time.
     */
    public static final class Target {

        /**
         * Determine if the target is a virtual (Java-in-Java) environment. Such a target does not
         * have a specific OS or CPU architecture.
         *
         * @return {@code true} if the target is a JVM environment, {@code false} otherwise
         */
        @Fold
        public static boolean isVirtual() {
            return "virtual".equals(System.getProperty("os.arch"));
        }

        // OS

        public static native boolean isUnix();
        public static native boolean isLinux();
        public static native boolean isWindows();
        public static native boolean isApple();
        public static native boolean isMacOs();
        public static native boolean isIOS();
        public static native boolean isAix();
        public static native boolean isPosix();

        // CPU

        public static native boolean isAmd64();
        public static native boolean isI386();
        public static native boolean isArm();
        public static native boolean isAarch64();
        public static native boolean isWasm();
        public static native boolean isWasi();

        // Toolchain

        @Fold
        public static boolean isGcc() {
            return defined(__GNUC__);
        }

        // C environment

        @Fold
        public static boolean isGLibCLike() {
            return defined(__GNU_LIBRARY__);
        }

        @Fold
        public static boolean isGLibC() {
            return isGLibCLike() && !isUCLibC() && !isMusl();
        }

        @Fold
        public static boolean isUCLibC() {
            return defined(__UCLIBC__);
        }

        @Fold
        public static boolean isPThreads() {
            return ! isWasm();
        }

        @Fold
        public static boolean isMusl() {
            return defined(__MUSL__);
        }

        // object environment

        public static native boolean isElf();
        public static native boolean isMachO();

        // backend type

        public static boolean isLlvm() {
            // rewrite with an intrinsic if LLVM plugin is active
            return false;
        }

        private static final object __GNUC__ = constant();
        @include("<features.h>")
        private static final c_int __GNU_LIBRARY__ = constant();
        @include("<features.h>")
        private static final object __UCLIBC__ = constant();
        @include("<features.h>")
        private static final object __MUSL__ = constant();

        public static final class IsPosix implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isPosix();
            }
        }

        public static final class IsLinux implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isLinux();
            }
        }

        public static final class IsMacOs implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isMacOs();
            }
        }

        public static final class IsApple implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isApple();
            }
        }

        //

        public static final class IsAarch64 implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isAarch64();
            }
        }

        public static final class IsAmd64 implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isAmd64();
            }
        }

        public static final class IsArm implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isArm();
            }
        }

        public static final class IsWasm implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isWasm();
            }
        }

        public static final class IsWasi implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isWasi();
            }
        }
        //

        public static final class IsGcc implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isGcc();
            }
        }

        public static final class IsGLibC implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isGLibC();
            }
        }

        public static final class IsGLibCLike implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isGLibCLike();
            }
        }

        public static final class IsPThreads implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isPThreads();
            }
        }

        public static final class IsUnix implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isUnix();
            }
        }

        public static final class IsAix implements BooleanSupplier {
            public boolean getAsBoolean() {
                return isAix();
            }
        }
    }
}
