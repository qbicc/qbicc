package org.qbicc.machine.arch;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Known vendors (required by LLVM).
 */
public enum Vendor implements PlatformComponent {
    unknown,
    apple,
    pc,
    ibm,
    redhat,
    ;

    @Override
    public Set<String> aliases() {
        return Set.of();
    }

    private static final Map<String, Vendor> index = Indexer.index(Vendor.class);

    public static Vendor forName(String name) {
        return index.getOrDefault(name.toLowerCase(Locale.ROOT), unknown);
    }

    public static Set<String> names() {
        return index.keySet();
    }

    public String llvmName() {
        return name();
    }
}
