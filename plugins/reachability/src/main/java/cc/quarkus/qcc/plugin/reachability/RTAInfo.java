package cc.quarkus.qcc.plugin.reachability;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.InterfaceObjectType;
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
    // Tracks reachable interfaces and their (direct) reachable implementors
    private final Map<ValidatedTypeDefinition, Set<ValidatedTypeDefinition>> interfaceHierarchy = new ConcurrentHashMap<>();

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
        info.interfaceHierarchy.clear();
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
    }

    public boolean isLiveInterface(ValidatedTypeDefinition type) { return interfaceHierarchy.containsKey(type); }

    public void addInterfaceEdge(ValidatedTypeDefinition child, ValidatedTypeDefinition parent) {
        interfaceHierarchy.computeIfAbsent(parent, t -> ConcurrentHashMap.newKeySet()).add(child);
    }

    // NOTE: If there are diamonds in the interface hierarchy, we may visit an implementor multiple times.
    //       We could avoid that by building a set before we visit anyone, but the only client of this function
    //       is robust against duplicates, so don't bother to handle this fringe case until we need to care.
    public void visitLiveImplementors(ValidatedTypeDefinition type, Consumer<ValidatedTypeDefinition> function) {
        Set<ValidatedTypeDefinition> implementors = interfaceHierarchy.get(type);
        if (implementors == null) return;
        for (ValidatedTypeDefinition child: implementors) {
            function.accept(child);
            if (child.isInterface()) {
                visitLiveImplementors(child, function);
            } else {
                visitLiveSubclassesPreOrder(child, function);
            }
        }
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