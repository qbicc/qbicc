package org.qbicc.machine.arch;

import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.smallrye.common.constraint.Assert;

/**
 * A platform which consists of a CPU architecture, operating system, ABI, and object file type.
 */
public final class Platform {
    private final Cpu cpu;
    private final OS os;
    private final ABI abi;
    private final ObjectType objectType;
    private final int hashCode;

    public Platform(final Cpu cpu, final OS os, final ABI abi, final ObjectType objectType) {
        this.cpu = Assert.checkNotNullParam("cpu", cpu);
        this.os = Assert.checkNotNullParam("os", os);
        this.abi = Assert.checkNotNullParam("abi", abi);
        this.objectType = Assert.checkNotNullParam("objectType", objectType);
        hashCode = Objects.hash(cpu, os, abi, objectType);
    }

    public Platform(final Cpu cpu, final OS os, ABI abi) {
        this(cpu, os, abi, os.getDefaultObjectType(cpu));
    }

    public Platform(final Cpu cpu, final OS os) {
        this(cpu, os, os.getDefaultAbi(cpu));
    }

    public Cpu getCpu() {
        return cpu;
    }

    public OS getOs() {
        return os;
    }

    public ABI getAbi() {
        return abi;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public String toString() {
        return cpu.getSimpleName() + '-' + os + '-' + abi + '-' + objectType;
    }

    public boolean isSupersetOf(Platform other) {
        return cpu.incorporates(other.cpu) && os.equals(other.os) && abi.equals(other.abi);
    }

    public boolean equals(final Object obj) {
        return obj instanceof Platform && equals((Platform) obj);
    }

    public boolean equals(final Platform obj) {
        return obj == this || obj != null && cpu.equals(obj.cpu) && os.equals(obj.os) && abi.equals(obj.abi);
    }

    private static final Pattern PLATFORM_PATTERN;

    static {
        StringBuilder b = new StringBuilder(256);
        b.append('('); // first capture group: CPU
        Iterator<String> iterator = Cpu.getNames().iterator();
        appendNameAlt(b, iterator);
        b.append(')').append('-');
        b.append('('); // second capture group: OS
        iterator = OS.getNames().iterator();
        appendNameAlt(b, iterator);
        b.append(')');
        b.append("(?:");
        b.append('-');
        b.append('('); // final capture group: ABI (optional)
        iterator = ABI.getNames().iterator();
        appendNameAlt(b, iterator);
        b.append(')').append(')').append('?');
        PLATFORM_PATTERN = Pattern.compile(b.toString());
    }

    private static void appendNameAlt(final StringBuilder b, final Iterator<String> iterator) {
        if (iterator.hasNext()) {
            String name = iterator.next();
            b.append(Pattern.quote(name));
            while (iterator.hasNext()) {
                b.append('|');
                name = iterator.next();
                b.append(Pattern.quote(name));
            }
        }
    }

    public static Platform parse(String platformString) throws IllegalArgumentException {
        Matcher matcher = PLATFORM_PATTERN.matcher(platformString);
        if (matcher.matches()) {
            String cpu = matcher.group(1);
            String os = matcher.group(2);
            String abi = matcher.group(3);
            if (abi == null) {
                return new Platform(Cpu.forName(cpu), OS.forName(os));
            } else {
                return new Platform(Cpu.forName(cpu), OS.forName(os), ABI.forName(abi));
            }
        } else {
            throw new IllegalArgumentException("Invalid platform string (expected \"cpuname-osname[-abiname]\"");
        }
    }

    public int hashCode() {
        return hashCode;
    }

    public static final Platform HOST_PLATFORM;

    static {
        HOST_PLATFORM = detectHostPlatform();
    }

    private static Platform detectHostPlatform() {
        final String osName = System.getProperty("os.name", "unknown");
        final String cpuName = System.getProperty("os.arch", "unknown");
        return new Platform(Cpu.forName(cpuName), OS.forName(osName));
    }
}
