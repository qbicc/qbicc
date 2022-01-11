package org.qbicc.runtime.linux;

import org.qbicc.runtime.Build;

/**
 *
 */
public class Linux {
    public static int getKernelMajor() {
        if (! Build.Target.isLinux()) {
            throw new UnsupportedOperationException("Linux only");
        }
        if (Build.isHost()) {
            throw new IllegalStateException("Can only be called at run time");
        }
        return Linux$_runtime.KERN_MAJOR;
    }

    public static int getKernelMinor() {
        if (! Build.Target.isLinux()) {
            throw new UnsupportedOperationException("Linux only");
        }
        if (Build.isHost()) {
            throw new IllegalStateException("Can only be called at run time");
        }
        return Linux$_runtime.KERN_MINOR;
    }

    public static boolean kernelAtLeast(int major, int minor) {
        if (! Build.Target.isLinux()) {
            throw new UnsupportedOperationException("Linux only");
        }
        if (Build.isHost()) {
            throw new IllegalStateException("Can only be called at run time");
        }
        int actualMajor = Linux$_runtime.KERN_MAJOR;
        return actualMajor > major || actualMajor == major && Linux$_runtime.KERN_MINOR >= minor;
    }
}
