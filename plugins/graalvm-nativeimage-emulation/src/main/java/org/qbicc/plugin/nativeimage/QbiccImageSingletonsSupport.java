package org.qbicc.plugin.nativeimage;

import org.graalvm.nativeimage.impl.ImageSingletonsSupport;

import java.util.concurrent.ConcurrentHashMap;

public class QbiccImageSingletonsSupport extends ImageSingletonsSupport {
    private final ConcurrentHashMap<Class<?>, Object> map = new ConcurrentHashMap<>();

    QbiccImageSingletonsSupport() {
        ImageSingletonsSupport.installSupport(this);
    }

    @Override
    public <T> void add(Class<T> key, T value) {
        map.put(key, value);
    }

    @Override
    public <T> T lookup(Class<T> key) {
        return (T)map.get(key);
    }

    @Override
    public boolean contains(Class<?> key) {
        return map.containsKey(key);
    }
}
