package cc.quarkus.qcc.plugin.intrinsics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.driver.Phase;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 * The intrinsics registry.  Registering all intrinsic methods here is more efficient than iterating a list of plugins.
 */
public final class Intrinsics {
    private static final AttachmentKey<Intrinsics> KEY = new AttachmentKey<>();

    private final Map<Phase, Map<TypeDescriptor, Map<String, Map<MethodDescriptor, StaticIntrinsic>>>> staticIntrinsics = new ConcurrentHashMap<>();
    private final Map<Phase, Map<TypeDescriptor, Map<String, Map<MethodDescriptor, StaticValueIntrinsic>>>> staticValueIntrinsics = new ConcurrentHashMap<>();

    private final Map<Phase, Map<TypeDescriptor, Map<String, Map<MethodDescriptor, InstanceIntrinsic>>>> instanceIntrinsics = new ConcurrentHashMap<>();
    private final Map<Phase, Map<TypeDescriptor, Map<String, Map<MethodDescriptor, InstanceValueIntrinsic>>>> instanceValueIntrinsics = new ConcurrentHashMap<>();

    private Intrinsics() {}

    public static Intrinsics get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, Intrinsics::new);
    }

    private static <K, V> Map<K, V> map(Object ignored) {
        return new ConcurrentHashMap<>();
    }

    // Static intrinsics

    public boolean registerIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc, StaticIntrinsic intrinsic) {
        return staticIntrinsics.computeIfAbsent(phase, Intrinsics::map).computeIfAbsent(owner, Intrinsics::map).computeIfAbsent(name, Intrinsics::map).putIfAbsent(desc, intrinsic) != null;
    }

    public boolean registerIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc, StaticValueIntrinsic intrinsic) {
        return staticValueIntrinsics.computeIfAbsent(phase, Intrinsics::map).computeIfAbsent(owner, Intrinsics::map).computeIfAbsent(name, Intrinsics::map).putIfAbsent(desc, intrinsic) != null;
    }

    public StaticIntrinsic getStaticIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc) {
        return staticIntrinsics.getOrDefault(phase, Map.of()).getOrDefault(owner, Map.of()).getOrDefault(name, Map.of()).get(desc);
    }

    public StaticValueIntrinsic getStaticValueIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc) {
        return staticValueIntrinsics.getOrDefault(phase, Map.of()).getOrDefault(owner, Map.of()).getOrDefault(name, Map.of()).get(desc);
    }

    // Instance intrisics

    public boolean registerIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc, InstanceIntrinsic intrinsic) {
        return instanceIntrinsics.computeIfAbsent(phase, Intrinsics::map).computeIfAbsent(owner, Intrinsics::map).computeIfAbsent(name, Intrinsics::map).putIfAbsent(desc, intrinsic) != null;
    }

    public boolean registerIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc, InstanceValueIntrinsic intrinsic) {
        return instanceValueIntrinsics.computeIfAbsent(phase, Intrinsics::map).computeIfAbsent(owner, Intrinsics::map).computeIfAbsent(name, Intrinsics::map).putIfAbsent(desc, intrinsic) != null;
    }

    public InstanceIntrinsic getInstanceIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc) {
        return instanceIntrinsics.getOrDefault(phase, Map.of()).getOrDefault(owner, Map.of()).getOrDefault(name, Map.of()).get(desc);
    }

    public InstanceValueIntrinsic getInstanceValueIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc) {
        return instanceValueIntrinsics.getOrDefault(phase, Map.of()).getOrDefault(owner, Map.of()).getOrDefault(name, Map.of()).get(desc);
    }

}
