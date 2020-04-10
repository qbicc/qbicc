package cc.quarkus.qcc.machine.arch;

import io.smallrye.common.constraint.Assert;

/**
 * A platform which consists of a CPU architecture, optional sub-architecture, vendor, system, and ABI.
 */
public final class Platform {
    private final CPU<?> cpuArchitecture;
    private final CpuSubArch cpuSubArchitecture;
    private final Vendor vendor;
    private final OS os;
    private final ABI abi;

    public Platform(final CPU<NoSubArch> cpuArchitecture, final Vendor vendor, final OS os, final ABI abi) {
        this.cpuArchitecture = Assert.checkNotNullParam("cpuArchitecture", cpuArchitecture);
        this.cpuSubArchitecture = null;
        this.vendor = Assert.checkNotNullParam("vendor", vendor);
        this.os = Assert.checkNotNullParam("system", os);
        this.abi = Assert.checkNotNullParam("abi", abi);
    }

    public <S extends CpuSubArch> Platform(final CPU<S> cpuArchitecture, final S cpuSubArchitecture, final Vendor vendor, final OS os, final ABI abi) {
        this.cpuArchitecture = Assert.checkNotNullParam("cpuArchitecture", cpuArchitecture);
        this.cpuSubArchitecture = Assert.checkNotNullParam("cpuSubArchitecture", cpuSubArchitecture);
        this.vendor = Assert.checkNotNullParam("vendor", vendor);
        this.os = Assert.checkNotNullParam("system", os);
        this.abi = Assert.checkNotNullParam("abi", abi);
    }

    public CPU<?> getCpuArchitecture() {
        return cpuArchitecture;
    }

    public CpuSubArch getCpuSubArchitecture() {
        return cpuSubArchitecture;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public OS getOs() {
        return os;
    }

    public ABI getAbi() {
        return abi;
    }

    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(cpuArchitecture);
        if (cpuSubArchitecture != null) {
            b.append(cpuSubArchitecture);
        }
        b.append('-').append(vendor);
        b.append('-').append(os);
        b.append('-').append(abi);
        return b.toString();
    }
}
