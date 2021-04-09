package org.qbicc.machine.arch;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class ABI extends PlatformComponent {
    public static final ABI UNKNOWN = new ABI("unknown");
    public static final ABI GNU = new ABI("gnu");
    public static final ABI GNUX32 = new ABI("gnux32"); // 32-bit in 64-bit
    public static final ABI EABI4 = new ABI("eabi4"); // arm
    public static final ABI EABI5 = new ABI("eabi5"); // arm
    public static final ABI GNUEABI = new ABI("gnueabi"); // arm
    public static final ABI GNUEABIHF = new ABI("gnueabihf"); // arm
    public static final ABI ELF = new ABI("elf");
    public static final ABI WIN32 = new ABI("win32");

    ABI(final String name) {
        super(name);
    }

    private static final Map<String, ABI> index = Indexer.index(ABI.class);

    public static ABI forName(String name) {
        return index.getOrDefault(name.toLowerCase(Locale.ROOT), UNKNOWN);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
