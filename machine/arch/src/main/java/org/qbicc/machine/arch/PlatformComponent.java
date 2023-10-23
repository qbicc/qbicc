package org.qbicc.machine.arch;

import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 */
public sealed interface PlatformComponent permits Abi, Cpu, Cpu.SubArch, Os, ObjectType, Vendor {
    String name();

    Set<String> aliases();

    default String matchRegex() {
        return Pattern.quote(name());
    }
}
