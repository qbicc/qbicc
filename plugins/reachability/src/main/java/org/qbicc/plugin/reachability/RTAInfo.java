package org.qbicc.plugin.reachability;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public class RTAInfo {
    private static final AttachmentKey<RTAInfo> KEY = new AttachmentKey<>();

    // Tracks reachable classes and their (direct) reachable subclasses
    private final Map<LoadedTypeDefinition, Set<LoadedTypeDefinition>> classHierarchy = new ConcurrentHashMap<>();
    // Tracks reachable interfaces and their (direct) reachable implementors
    private final Map<LoadedTypeDefinition, Set<LoadedTypeDefinition>> interfaceHierarchy = new ConcurrentHashMap<>();

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

    public boolean isLiveClass(LoadedTypeDefinition type) {
        return classHierarchy.containsKey(type);
    }

    public void addLiveClass(LoadedTypeDefinition type) {
        if (isLiveClass(type)) return;
        classHierarchy.computeIfAbsent(type, t -> ConcurrentHashMap.newKeySet());
        LoadedTypeDefinition superClass = type.getSuperClass();
        if (superClass != null) {
            addLiveClass(superClass);
            classHierarchy.get(superClass).add(type);
        }
    }

    public boolean isLiveInterface(LoadedTypeDefinition type) { return interfaceHierarchy.containsKey(type); }

    public void makeInterfaceLive(LoadedTypeDefinition type) {
        interfaceHierarchy.computeIfAbsent(type, t -> ConcurrentHashMap.newKeySet());
    }

    public void addInterfaceEdge(LoadedTypeDefinition child, LoadedTypeDefinition parent) {
        interfaceHierarchy.computeIfAbsent(parent, t -> ConcurrentHashMap.newKeySet()).add(child);
    }

    public void visitLiveInterfaces(Consumer<LoadedTypeDefinition> function) {
        for (LoadedTypeDefinition i: interfaceHierarchy.keySet()) {
            function.accept(i);
        }
    }

    public void visitLiveImplementors(LoadedTypeDefinition type, Consumer<LoadedTypeDefinition> function) {
        Set<LoadedTypeDefinition> implementors = interfaceHierarchy.get(type);
        if (implementors == null) return;
        Set<LoadedTypeDefinition> toProcess = new HashSet<>();
        collectImplementors(type, toProcess);
        for (LoadedTypeDefinition cls : toProcess) {
            function.accept(cls);
        }
    }

    private void collectImplementors(LoadedTypeDefinition type, Set<LoadedTypeDefinition> toProcess) {
        Set<LoadedTypeDefinition> implementors = interfaceHierarchy.get(type);
        if (implementors == null) return;
        for (LoadedTypeDefinition child: implementors) {
            toProcess.add(child);
            if (child.isInterface()) {
                collectImplementors(child, toProcess);
            } else {
                visitLiveSubclassesPreOrder(child, cls -> toProcess.add(cls));
            }
        }
    }

    public void visitLiveSubclassesPreOrder(LoadedTypeDefinition type, Consumer<LoadedTypeDefinition> function) {
        Set<LoadedTypeDefinition> subclasses = classHierarchy.get(type);
        if (subclasses == null) return;
        for (LoadedTypeDefinition sc: subclasses) {
            function.accept(sc);
            visitLiveSubclassesPreOrder(sc, function);
        }
    }

    public void visitLiveSubclassesPostOrder(LoadedTypeDefinition type, Consumer<LoadedTypeDefinition> function) {
        Set<LoadedTypeDefinition> subclasses = classHierarchy.get(type);
        if (subclasses == null) return;
        for (LoadedTypeDefinition sc: subclasses) {
            visitLiveSubclassesPostOrder(sc, function);
            function.accept(sc);
        }
    }

}