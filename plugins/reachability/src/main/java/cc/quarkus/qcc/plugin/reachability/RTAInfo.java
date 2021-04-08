package cc.quarkus.qcc.plugin.reachability;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.InterfaceObjectType;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

import java.util.HashSet;
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
        ReachabilityBlockBuilder.rtaLog.debugf("Clearing RTAInfo %s classes; %s interfaces", info.classHierarchy.size(), info.interfaceHierarchy.size());
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

    public void makeInterfaceLive(ValidatedTypeDefinition type) {
        interfaceHierarchy.computeIfAbsent(type, t -> ConcurrentHashMap.newKeySet());
    }

    public void addInterfaceEdge(ValidatedTypeDefinition child, ValidatedTypeDefinition parent) {
        interfaceHierarchy.computeIfAbsent(parent, t -> ConcurrentHashMap.newKeySet()).add(child);
    }

    public void visitLiveInterfaces(Consumer<ValidatedTypeDefinition> function) {
        for (ValidatedTypeDefinition i: interfaceHierarchy.keySet()) {
            function.accept(i);
        }
    }

    public void visitLiveImplementors(ValidatedTypeDefinition type, Consumer<ValidatedTypeDefinition> function) {
        Set<ValidatedTypeDefinition> implementors = interfaceHierarchy.get(type);
        if (implementors == null) return;
        Set<ValidatedTypeDefinition> toProcess = new HashSet<>();
        collectImplementors(type, toProcess);
        for (ValidatedTypeDefinition cls : toProcess) {
            function.accept(cls);
        }
    }

    private void collectImplementors(ValidatedTypeDefinition type, Set<ValidatedTypeDefinition> toProcess) {
        Set<ValidatedTypeDefinition> implementors = interfaceHierarchy.get(type);
        if (implementors == null) return;
        for (ValidatedTypeDefinition child: implementors) {
            toProcess.add(child);
            if (child.isInterface()) {
                collectImplementors(child, toProcess);
            } else {
                visitLiveSubclassesPreOrder(child, cls -> toProcess.add(cls));
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