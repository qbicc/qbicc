package cc.quarkus.c_native.api;

import java.util.Locale;
import java.util.function.BooleanSupplier;

import cc.quarkus.api.Fold;

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
    public static native boolean isHost();

    /**
     * Determine if the calling method is operating in the context of the build target.
     *
     * @return {@code true} if the caller is calling from the build target, {@code false} otherwise
     */
    public static native boolean isTarget();

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

        public static boolean isUnix() {
            return CNative.defined(__unix__);
        }

        public static boolean isLinux() {
            return CNative.defined(linux);
        }

        public static boolean isWindows() {
            return CNative.defined(_WIN32) || CNative.defined(WIN32);
        }

        public static boolean isApple() {
            return CNative.defined(__APPLE__) && __APPLE__.booleanValue();
        }

        public static boolean isMacOs() {
            return isApple() && TARGET_OS_MAC.booleanValue();
        }

        public static boolean isIOS() {
            return isApple() && TARGET_OS_IPHONE.booleanValue();
        }

        public static boolean isAix() {
            return CNative.defined(_AIX);
        }

        public static boolean isPosix() {
            return CNative.defined(__unix__) && CNative.defined(_POSIX_VERSION);
        }

        // CPU

        public static boolean isAmd64() {
            return CNative.defined(GCC) && CNative.defined(x86_64) || CNative.defined(MSVC) && CNative.defined(_M_AMD64);
        }

        public static boolean isI386() {
            return CNative.defined(GCC) && CNative.defined(__i386__) || CNative.defined(MSVC) && CNative.defined(_M_X86);
        }

        public static boolean isArm() {
            return CNative.defined(GCC) && CNative.defined(__arm__) || CNative.defined(MSVC) && CNative.defined(_M_ARM);
        }

        public static boolean isAarch64() {
            return CNative.defined(GCC) && CNative.defined(__aarch64__);
        }

        // Toolchain

        public static boolean isGcc() {
            return CNative.defined(GCC);
        }

        // C environment

        public static boolean isGLibCLike() {
            return CNative.defined(__GNU_LIBRARY__);
        }

        public static boolean isGLibC() {
            return isGLibCLike() && !isUCLibC() && !isMusl();
        }

        public static boolean isUCLibC() {
            return CNative.defined(__UCLIBC__);
        }

        public static boolean isMusl() {
            return CNative.defined(__MUSL__);
        }

        // object environment

        public static boolean isElf() {
            return CNative.defined(__ELF__);
        }

        public static boolean isMachO() {
            return CNative.defined(__MACH__);
        }

        private static final CNative.object __MACH__ = CNative.constant();
        private static final CNative.object __ELF__ = CNative.constant();
        private static final CNative.object __unix__ = CNative.constant();
        @CNative.include(value = "<unistd.h>", when = IsUnix.class)
        private static final CNative.c_long _POSIX_VERSION = CNative.constant();
        @CNative.include(value = "<TargetConditionals.h>", when = IsApple.class)
        private static final CNative.c_int TARGET_OS_IPHONE = CNative.constant();
        @CNative.include(value = "<TargetConditionals.h>", when = IsApple.class)
        private static final CNative.c_int TARGET_OS_MAC = CNative.constant();
        private static final CNative.c_int __APPLE__ = CNative.constant();
        private static final CNative.object _WIN32 = CNative.constant();
        private static final CNative.object WIN32 = CNative.constant();
        private static final CNative.object GCC = CNative.constant();
        private static final CNative.object __i386__ = CNative.constant();
        private static final CNative.object MSVC = CNative.constant();
        private static final CNative.object x86_64 = CNative.constant();
        private static final CNative.object _M_AMD64 = CNative.constant();
        private static final CNative.object _M_X86 = CNative.constant();
        private static final CNative.object linux = CNative.constant();
        private static final CNative.object __arm__ = CNative.constant();
        private static final CNative.object __aarch64__ = CNative.constant();
        private static final CNative.object _M_ARM = CNative.constant();
        @CNative.include("<feature.h>")
        private static final CNative.c_int __GNU_LIBRARY__ = CNative.constant();
        @CNative.include("<feature.h>")
        private static final CNative.object __UCLIBC__ = CNative.constant();
        @CNative.include("<feature.h>")
        private static final CNative.object __MUSL__ = CNative.constant();
        private static final CNative.object _AIX = CNative.constant();

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
