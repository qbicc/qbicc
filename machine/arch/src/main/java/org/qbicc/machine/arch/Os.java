package org.qbicc.machine.arch;

import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.constraint.Assert;

/**
 * The known operating systems.
 */
public enum Os implements PlatformComponent {
    unknown(Abi.unknown, Vendor.unknown, ObjectType.unknown),
    none(Abi.unknown, Vendor.unknown, ObjectType.unknown),
    linux(Abi.gnu, Vendor.unknown, ObjectType.elf) {
        @Override
        public Abi defaultAbi(Cpu cpu) {
            return cpu == Cpu.arm ? Abi.gnueabi : super.defaultAbi(cpu);
        }
    },
    win32("\\", ";", "\r\n", Abi.win32, Vendor.pc, ObjectType.coff, "windows", "windows32"),
    macos(Abi.unknown, Vendor.apple, ObjectType.macho, "macosx", "darwin", /* system prop */ "Mac OS X") {
        @Override
        public OsVersion mapVersion(OsVersion osVersion) {
            Assert.checkNotNullParam("osVersion", osVersion);
            if (osVersion instanceof MacOsVersion mov) {
                return mov;
            } else if (osVersion instanceof NoVersion) {
                return MacOsVersion.DEFAULT;
            } else {
                throw new IllegalArgumentException("macOS does not support versions of type " + osVersion.getClass());
            }
        }

        @Override
        public OsVersion makeVersion(String osString, int major, int minor, int micro) {
            if (osString.equalsIgnoreCase("darwin")) {
                return MacOsVersion.fromDarwinVersion(major, minor);
            } else {
                return MacOsVersion.of(major, minor);
            }
        }

        @Override
        public OsVersion defaultVersion() {
            return MacOsVersion.DEFAULT;
        }

        @Override
        public ByteOrder unicodeByteOrder(Cpu cpu) {
            return ByteOrder.BIG_ENDIAN;
        }
    },
    ios(Abi.unknown, Vendor.apple, ObjectType.macho),
    wasi(Abi.unknown, Vendor.unknown, ObjectType.wasm, "wasm"),
    aix(Abi.unknown, Vendor.ibm, ObjectType.elf) {
        @Override
        public Map<String, String> globalDefinitions() {
            return Map.of("_THREAD_SAFE_ERRNO", "1");
        }
    },
    ;

    private final Abi defaultAbi;
    private final Vendor defaultVendor;
    private final ObjectType defaultObjectType;
    private final String fileSeparator;
    private final String pathSeparator;
    private final String lineSeparator;

    private final Set<String> aliases;

    Os(final Abi defaultAbi, Vendor defaultVendor, final ObjectType defaultObjectType, final String... aliases) {
        this("/", ":", "\n", defaultAbi, defaultVendor, defaultObjectType, aliases);
    }

    Os(String fileSeparator, String pathSeparator, String lineSeparator, final Abi defaultAbi, Vendor defaultVendor, final ObjectType defaultObjectType, final String... aliases) {
        this.defaultVendor = defaultVendor;
        this.aliases = Set.of(aliases);
        this.defaultAbi = defaultAbi;
        this.defaultObjectType = defaultObjectType;
        this.fileSeparator = fileSeparator;
        this.pathSeparator = pathSeparator;
        this.lineSeparator = lineSeparator;
    }

    public Abi defaultAbi(final Cpu cpu) {
        return defaultAbi;
    }

    public ObjectType defaultObjectType(final Cpu cpu) {
        return defaultObjectType;
    }

    public Vendor defaultVendor() {
        return defaultVendor;
    }

    public ByteOrder unicodeByteOrder(final Cpu cpu) {
        return cpu.byteOrder();
    }

    public String llvmName() {
        return name();
    }

    private static final Map<String, Os> index = Indexer.index(Os.class);

    public static Os forName(String name) {
        return index.getOrDefault(name.toLowerCase(Locale.ROOT), unknown);
    }

    public static Set<String> names() {
        return index.keySet();
    }

    public static Collection<Os> valueCollection() {
        return index.values();
    }

    public Map<String, String> globalDefinitions() {
        return Map.of();
    }

    public String fileSeparator() {
        return fileSeparator;
    }

    public String pathSeparator() {
        return pathSeparator;
    }

    public String lineSeparator() {
        return lineSeparator;
    }

    @Override
    public Set<String> aliases() {
        return aliases;
    }

    public OsVersion makeVersion(final String osString, int major, int minor, int micro) {
        return NoVersion.none;
    }

    public OsVersion defaultVersion() {
        return NoVersion.none;
    }

    public OsVersion mapVersion(final OsVersion osVersion) {
        Assert.checkNotNullParam("osVersion", osVersion);
        if (osVersion != NoVersion.none) {
            throw new IllegalArgumentException("OS version not supported for OS " + this);
        }
        return osVersion;
    }
}
