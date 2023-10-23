package org.qbicc.machine.arch;

import io.smallrye.common.constraint.Assert;

/**
 * A version for the {@code macos} operating system.
 */
public final class MacOsVersion implements Comparable<MacOsVersion>, OsVersion {
    private final int major;
    private final int minor;

    /**
     * Construct a new instance.
     *
     * @param major the major version (must be at least {@code 10})
     * @param minor the minor version (must be non-negative)
     */
    public MacOsVersion(int major, int minor) {
        Assert.checkMinimumParameter("major", 10, major);
        Assert.checkMinimumParameter("minor", 0, minor);
        this.major = major;
        this.minor = minor;
    }

    public static MacOsVersion fromDarwinVersion(int major, int minor) {
        // according to LLVM, Darwin sort-of tracks the macos version...
        if (minor < 0) {
            minor = 0;
        }
        if (major < 4) {
            // don't know, really. just guess
            return new MacOsVersion(10, 0);
        } else if (major <= 19) {
            // darwin4 = macos10.0, darwin5 = macos10.1, etc.
            return new MacOsVersion(10, major - 4);
        } else {
            // darwin20 = macos11.0, darwin21 = macos12.0, etc.
            return new MacOsVersion(major - 9, minor);
        }
    }

    public static MacOsVersion of(final int major, int minor) {
        if (major < 10) {
            return DEFAULT;
        }
        if (minor < 0) {
            minor = 0;
        }
        return new MacOsVersion(major, minor);
    }

    @Override
    public int compareTo(MacOsVersion o) {
        int res = Integer.compare(major, o.major);
        return res != 0 ? res : Integer.compare(minor, o.minor);
    }

    @Override
    public int hashCode() {
        return major * 19 + minor;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MacOsVersion other && equals(other);
    }

    public boolean equals(MacOsVersion other) {
        return this == other || other != null && major == other.major && minor == other.minor;
    }

    @Override
    public String toString() {
        return major + "." + minor + ".0";
    }

    // 10.4 is the default used by LLVM.
    public static final MacOsVersion DEFAULT = new MacOsVersion(10, 4);
}
