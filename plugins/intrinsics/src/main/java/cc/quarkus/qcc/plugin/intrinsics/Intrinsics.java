package cc.quarkus.qcc.plugin.intrinsics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 * The intrinsics registry.  Registering all intrinsic methods here is more efficient than iterating a list of plugins.
 */
public final class Intrinsics {
    private static final AttachmentKey<Intrinsics> KEY = new AttachmentKey<>();

    private final Map<TypeDescriptor, Map<String, Map<MethodDescriptor, StaticIntrinsic>>> staticIntrinsics = new ConcurrentHashMap<>();
    private final Map<TypeDescriptor, Map<String, Map<MethodDescriptor, StaticValueIntrinsic>>> staticValueIntrinsics = new ConcurrentHashMap<>();

    private Intrinsics() {}

    public static Intrinsics get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, Intrinsics::new);
    }

    private static <K, V> Map<K, V> map(Object ignored) {
        return new ConcurrentHashMap<>();
    }

    public boolean registerIntrinsic(TypeDescriptor owner, String name, MethodDescriptor desc, StaticIntrinsic intrinsic) {
        return staticIntrinsics.computeIfAbsent(owner, Intrinsics::map).computeIfAbsent(name, Intrinsics::map).putIfAbsent(desc, intrinsic) != null;
    }

    public boolean registerIntrinsic(TypeDescriptor owner, String name, MethodDescriptor desc, StaticValueIntrinsic intrinsic) {
        return staticValueIntrinsics.computeIfAbsent(owner, Intrinsics::map).computeIfAbsent(name, Intrinsics::map).putIfAbsent(desc, intrinsic) != null;
    }

    public StaticIntrinsic getStaticIntrinsic(TypeDescriptor owner, String name, MethodDescriptor desc) {
        return staticIntrinsics.getOrDefault(owner, Map.of()).getOrDefault(name, Map.of()).get(desc);
    }

    public StaticValueIntrinsic getStaticValueIntrinsic(TypeDescriptor owner, String name, MethodDescriptor desc) {
        return staticValueIntrinsics.getOrDefault(owner, Map.of()).getOrDefault(name, Map.of()).get(desc);
    }

}
