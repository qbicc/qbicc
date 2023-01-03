package org.graalvm.nativeimage;

// TODO: works around direct reference to org.graalvm.nativeimage.ImageInfo in quarkus 2.14
//       Once we upgrade quarkus-qbicc to a Quarkus version that includes
//       https://github.com/quarkusio/quarkus/pull/29993 we can get rid of this Hook.
public class ImageInfo {
    public static native boolean inImageBuildtimeCode();
}
