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
    private final Os os;
    private final OsVersion osVersion;
    private final Vendor vendor;
    private final Abi abi;
    private final ObjectType objectType;
    private final int hashCode;

    public Platform(final Cpu cpu, final Os os, OsVersion osVersion, final Vendor vendor, final Abi abi, final ObjectType objectType) {
        this.cpu = Assert.checkNotNullParam("cpu", cpu);
        this.os = Assert.checkNotNullParam("os", os);
        this.osVersion = os.mapVersion(Assert.checkNotNullParam("osVersion", osVersion));
        this.vendor = Assert.checkNotNullParam("vendor", vendor);
        this.abi = Assert.checkNotNullParam("abi", abi);
        this.objectType = Assert.checkNotNullParam("objectType", objectType);
        hashCode = Objects.hash(cpu, os, osVersion, vendor, abi, objectType);
    }

    public Platform(final Cpu cpu, final Os os, final OsVersion osVersion, final Abi abi, final ObjectType objectType) {
        this(cpu, os, osVersion, os.defaultVendor(), abi, objectType);
    }

    public Platform(final Cpu cpu, final Os os, final OsVersion osVersion, final Vendor vendor, final Abi abi) {
        this(cpu, os, osVersion, vendor, abi, os.defaultObjectType(cpu));
    }

    public Platform(final Cpu cpu, final Os os, final Abi abi, final ObjectType objectType) {
        this(cpu, os, NoVersion.none, os.defaultVendor(), abi, objectType);
    }

    public Platform(final Cpu cpu, final Os os, final OsVersion osVersion, Abi abi) {
        this(cpu, os, osVersion, abi, os.defaultObjectType(cpu));
    }

    public Platform(final Cpu cpu, final Os os, Abi abi) {
        this(cpu, os, abi, os.defaultObjectType(cpu));
    }

    public Platform(final Cpu cpu, final Os os, final OsVersion osVersion) {
        this(cpu, os, osVersion, os.defaultAbi(cpu));
    }

    public Platform(final Cpu cpu, final Os os) {
        this(cpu, os, os.defaultAbi(cpu));
    }

    public Cpu cpu() {
        return cpu;
    }

    public Os os() {
        return os;
    }

    public OsVersion osVersion() {
        return osVersion;
    }

    public Vendor vendor() {
        return vendor;
    }

    public Abi abi() {
        return abi;
    }

    public boolean isWasm() {
        return cpu == Cpu.wasm32;
    }

    public ObjectType objectType() {
        return objectType;
    }

    public String toString() {
        return cpu.simpleName() + '-' + vendor + '-' + os + osVersion + '-' + abi + '-' + objectType;
    }

    public String llvmString() {
        String llvmString = cpu().llvmName() + "-" + vendor().llvmName() + "-" + os().llvmName() + osVersion();
        if (abi() != Abi.unknown) {
            return llvmString + "-" + abi().llvmName();
        } else {
            return llvmString;
        }
    }

    /**
     * Format a section name according to the rules of this platform.
     *
     * @param segmentName the simple segment name (must not be {@code null})
     * @param simpleNameParts the simple section name parts (must not be {@code null})
     * @return the platform-specific section name (not {@code null})
     */
    public String formatSectionName(String segmentName, String... simpleNameParts) {
        return objectType.formatSectionName(segmentName, simpleNameParts);
    }

    public String formatStartOfSectionSymbolName(String segmentName, String simpleName) {
        return objectType.formatStartOfSectionSymbolName(segmentName, simpleName);
    }

    public String formatEndOfSectionSymbolName(String segmentName, String simpleName) {
        return objectType.formatEndOfSectionSymbolName(segmentName, simpleName);
    }

    public boolean isSupersetOf(Platform other) {
        return cpu.equals(other.cpu) && os.equals(other.os) && abi.equals(other.abi);
    }

    public boolean equals(final Object obj) {
        return obj instanceof Platform && equals((Platform) obj);
    }

    public boolean equals(final Platform obj) {
        return obj == this || obj != null && cpu.equals(obj.cpu) && os.equals(obj.os) && vendor.equals(obj.vendor) && abi.equals(obj.abi) && objectType.equals(obj.objectType);
    }

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?");
    private static final Pattern PLATFORM_PATTERN;

    static {
        StringBuilder b = new StringBuilder(256);
        // CPU
        b.append('(');
        appendNameAlt(b, Cpu.names().iterator());
        b.append(')');
        // OS
        b.append('-');
        b.append('(');
        appendNameAlt(b, Os.names().iterator());
        b.append(')');
        // OS version (optional)
        b.append('(');
        b.append("\\d+(?:\\.\\d+(?:\\.\\d+)?)?");
        b.append(')').append('?');
        // vendor (optional)
        b.append("(?:-(");
        appendNameAlt(b, Vendor.names().iterator());
        b.append("))?");
        // ABI (optional)
        b.append("(?:-(");
        appendNameAlt(b, Abi.names().iterator());
        b.append("))?");
        PLATFORM_PATTERN = Pattern.compile(b.toString(), Pattern.CASE_INSENSITIVE);
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
            String cpuName = matcher.group(1);
            String osStr = matcher.group(2);
            String osVersionStr = matcher.group(3);
            String vendorStr = matcher.group(4);
            String abiStr = matcher.group(5);
            Cpu cpu = Cpu.forName(cpuName);
            Os os = Os.forName(osStr);
            OsVersion version;
            if (osVersionStr != null) {
                Matcher verMatcher = VERSION_PATTERN.matcher(osVersionStr);
                if (! verMatcher.matches()) {
                    // ???
                    throw new IllegalStateException();
                }
                int major = Integer.parseInt(verMatcher.group(0));
                int minor = Integer.parseInt(Objects.requireNonNullElse(verMatcher.group(1), "0"));
                int micro = Integer.parseInt(Objects.requireNonNullElse(verMatcher.group(2), "0"));
                version = os.makeVersion(osStr, major, minor, micro);
            } else {
                version = os.defaultVersion();
            }
            Abi abi;
            if (abiStr != null) {
                abi = Abi.forName(abiStr);
            } else {
                abi = os.defaultAbi(cpu);
            }
            Vendor vendor;
            if (vendorStr != null) {
                vendor = Vendor.forName(vendorStr);
            } else {
                vendor = os.defaultVendor();
            }
            return new Platform(cpu, os, version, vendor, abi);
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
        final String versionName = System.getProperty("os.version", "unknown");
        Matcher verMatcher = VERSION_PATTERN.matcher(versionName);
        Os os = Os.forName(osName);
        OsVersion version;
        if (verMatcher.matches()) {
            int major = Integer.parseInt(verMatcher.group(1));
            int minor = Integer.parseInt(Objects.requireNonNullElse(verMatcher.group(2), "0"));
            int micro = Integer.parseInt(Objects.requireNonNullElse(verMatcher.group(3), "0"));
            version = os.makeVersion(osName, major, minor, micro);
        } else {
            version = os.defaultVersion();
        }
        return new Platform(Cpu.forName(cpuName), os, version);
    }
}
