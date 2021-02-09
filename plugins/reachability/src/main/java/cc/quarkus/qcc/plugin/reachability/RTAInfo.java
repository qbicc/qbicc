package cc.quarkus.qcc.plugin.reachability;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;


public class RTAInfo {
    private static final AttachmentKey<RTAInfo> KEY = new AttachmentKey<>();

    // Tracks reachable classes and their (direct) reachable subclasses
    private final Map<ValidatedTypeDefinition, Set<ValidatedTypeDefinition>> classHierarchy = new ConcurrentHashMap<>();

    private final CompilationContext ctxt;

    private RTAInfo(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static RTAInfo get(CompilationContext ctxt) {
        RTAInfo info = ctxt.getAttachment(KEY);
        if (info == null) {
            info = new RTAInfo(ctxt);
            RTAInfo appearing = ctxt.putAttachmentIfAbsent(KEY, info);
            if (appearing != null) {
                info = appearing;
            }
        }
        return info;
    }

    public static void clear(CompilationContext ctxt) {
        RTAInfo info = get(ctxt);
        info.classHierarchy.clear();
    }

    public boolean isLiveClass(ValidatedTypeDefinition type) {
        return classHierarchy.containsKey(type);
    }

    public void addLiveClass(ValidatedTypeDefinition type) {
        if (isLiveClass(type)) return;
        classHierarchy.computeIfAbsent(type, t -> ConcurrentHashMap.newKeySet());
        ValidatedTypeDefinition superClass = type.getSuperClass();
        if (superClass != null) {
            addLiveClass(superClass);
            classHierarchy.get(superClass).add(type);
        }
        // TODO: Record implements hierarchy info
    }

    public void visitLiveSubclassesPreOrder(ValidatedTypeDefinition type, Consumer<ValidatedTypeDefinition> function) {
        Set<ValidatedTypeDefinition> subclasses = classHierarchy.get(type);
        if (subclasses == null) return;
        for (ValidatedTypeDefinition sc: subclasses) {
            function.accept(sc);
            visitLiveSubclassesPreOrder(sc, function);
        }
    }

    public void visitLiveSubclassesPostOrder(ValidatedTypeDefinition type, Consumer<ValidatedTypeDefinition> function) {
        Set<ValidatedTypeDefinition> subclasses = classHierarchy.get(type);
        if (subclasses == null) return;
        for (ValidatedTypeDefinition sc: subclasses) {
            visitLiveSubclassesPostOrder(sc, function);
            function.accept(sc);
        }
    }

}