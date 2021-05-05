package org.qbicc.main;

import org.qbicc.machine.arch.Platform;
import picocli.CommandLine;

/**
 * A picocli converter for Platform values.
 */
public final class PlatformConverter implements CommandLine.ITypeConverter<Platform> {
    public Platform convert(String value) throws IllegalArgumentException {
        return Platform.parse(value);
    }
}
