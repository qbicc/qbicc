package org.qbicc.machine.arch;

import java.util.Objects;

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
        this.os = Assert.checkNotNullParam("system", os);
        this.abi = Assert.checkNotNullParam("abi", abi);
        this.objectType = Assert.checkNotNullParam("objectType", objectType);
        hashCode = Objects.hash(cpu, os, abi, objectType);
    }

    public Platform(final Cpu cpu, final OS os) {
        this(cpu, os, os.getDefaultAbi(cpu), os.getDefaultObjectType(cpu));
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
