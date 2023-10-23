package org.qbicc.machine.arch;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public enum Abi implements PlatformComponent {
    unknown(Set.of()),
    gnu(Set.of()),
    gnu_x32(Set.of()), // 32-bit in 64-bit
    eabi4(Set.of()), // arm
    eabi5(Set.of()), // arm
    gnueabi(Set.of()), // arm
    gnueabihf(Set.of()), // arm
    win32(Set.of()),
    wasi(Set.of()),
    ;

    private final Set<String> aliases;

    private static final Map<String, Abi> index = Indexer.index(Abi.class);

    Abi(Set<String> aliases) {
        this.aliases = aliases;
    }

    public static Abi forName(String name) {
        return index.getOrDefault(name.toLowerCase(Locale.ROOT), unknown);
    }

    public static Set<String> names() {
        return index.keySet();
    }

    @Override
    public Set<String> aliases() {
        return aliases;
    }

    public String llvmName() {
        return name();
    }
}
