package org.qbicc.runtime.host;

/**
 * File information on the host filesystem for a given file.
 */
public record Stat(
    long serialNumber,
    int type,
    int mode,
    int uid,
    int gid,
    long size,
    long modTime,
    long crtTime
) {

    public static final int T_UNKNOWN = 0;
    public static final int T_REG = 1;
    public static final int T_DIR = 2;
    public static final int T_LINK = 3;
}
