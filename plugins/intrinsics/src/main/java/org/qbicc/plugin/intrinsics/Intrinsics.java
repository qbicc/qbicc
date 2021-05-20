package org.qbicc.plugin.intrinsics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * The intrinsics registry.  Registering all intrinsic methods here is more efficient than iterating a list of plugins.
 */
public final class Intrinsics {
    private static final AttachmentKey<Intrinsics> KEY = new AttachmentKey<>();

    private final Map<Phase, Map<TypeDescriptor, Map<String, Map<MethodDescriptor, StaticIntrinsic>>>> staticIntrinsics = new ConcurrentHashMap<>();

    private final Map<Phase, Map<TypeDescriptor, Map<String, Map<MethodDescriptor, InstanceIntrinsic>>>> instanceIntrinsics = new ConcurrentHashMap<>();

    private Intrinsics() {}

    public static Intrinsics get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, Intrinsics::new);
    }

    private static <K, V> Map<K, V> map(Object ignored) {
        return new ConcurrentHashMap<>();
    }

    // Static intrinsics
    public boolean registerIntrinsic(TypeDescriptor owner, String name, MethodDescriptor desc, StaticIntrinsic intrinsic) {
        return registerIntrinsic(Phase.ADD, owner, name, desc, intrinsic);
    }
    public boolean registerIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc, StaticIntrinsic intrinsic) {
        return staticIntrinsics.computeIfAbsent(phase, Intrinsics::map).computeIfAbsent(owner, Intrinsics::map).computeIfAbsent(name, Intrinsics::map).putIfAbsent(desc, intrinsic) != null;
    }

    public StaticIntrinsic getStaticIntrinsic(Phase phase, MethodElement element) {
        return element.isStatic() ? staticIntrinsics
            .getOrDefault(phase, Map.of())
            .getOrDefault(element.getEnclosingType().getDescriptor(), Map.of())
            .getOrDefault(element.getName(), Map.of())
            .get(element.getDescriptor())
            : null;
    }

    public StaticIntrinsic getStaticIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc) {
        return staticIntrinsics.getOrDefault(phase, Map.of()).getOrDefault(owner, Map.of()).getOrDefault(name, Map.of()).get(desc);
    }

    // Instance intrinsics
    public boolean registerIntrinsic(TypeDescriptor owner, String name, MethodDescriptor desc, InstanceIntrinsic intrinsic) {
        return instanceIntrinsics.computeIfAbsent(Phase.ADD, Intrinsics::map).computeIfAbsent(owner, Intrinsics::map).computeIfAbsent(name, Intrinsics::map).putIfAbsent(desc, intrinsic) != null;
    }

    public InstanceIntrinsic getInstanceIntrinsic(Phase phase, MethodElement element) {
        return !element.isStatic() ? instanceIntrinsics
            .getOrDefault(phase, Map.of())
            .getOrDefault(element.getEnclosingType().getDescriptor(), Map.of())
            .getOrDefault(element.getName(), Map.of())
            .get(element.getDescriptor())
            : null;
    }

    public InstanceIntrinsic getInstanceIntrinsic(Phase phase, TypeDescriptor owner, String name, MethodDescriptor desc) {
        return instanceIntrinsics.getOrDefault(phase, Map.of()).getOrDefault(owner, Map.of()).getOrDefault(name, Map.of()).get(desc);
    }
}
