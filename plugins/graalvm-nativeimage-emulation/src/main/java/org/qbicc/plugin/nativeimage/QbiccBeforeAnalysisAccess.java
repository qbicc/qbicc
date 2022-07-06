package org.qbicc.plugin.nativeimage;

import org.graalvm.nativeimage.hosted.Feature;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.reachability.ReachabilityRoots;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class QbiccBeforeAnalysisAccess implements Feature.BeforeAnalysisAccess {
    final CompilationContext ctxt;

    QbiccBeforeAnalysisAccess(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    @Override
    public void registerAsUsed(Class<?> type) {
        LoadedTypeDefinition ltd = ClassMapper.mapClass(ctxt, type);
        if (ltd != null) {
            ReachabilityRoots.get(ctxt).registerReflectiveClass(ltd);
        }
    }

    @Override
    public void registerAsInHeap(Class<?> type) {
        // qbicc can ignore this.
        // We fully interpret the <clinits> to construct the buildtime heap,
        // so we can know exactly all concrete types that are in it simply by tracing it.
    }

    @Override
    public void registerAsAccessed(Field field) {
        FieldElement fe = ClassMapper.mapField(ctxt, field);
        if (fe != null) {
            ReachabilityRoots.get(ctxt).registerReflectiveField(fe);
        }
    }

    @Override
    public void registerAsUnsafeAccessed(Field field) {
        FieldElement fe = ClassMapper.mapField(ctxt, field);
        if (fe != null) {
            ReachabilityRoots.get(ctxt).registerReflectiveField(fe);
        }
    }

    @Override
    public void registerClassInitializerReachabilityHandler(Consumer<Feature.DuringAnalysisAccess> callback, Class<?> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerMethodOverrideReachabilityHandler(BiConsumer<Feature.DuringAnalysisAccess,Executable> callback, Executable baseMethod) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerReachabilityHandler(Consumer<Feature.DuringAnalysisAccess> callback, Object... elements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerSubtypeReachabilityHandler(BiConsumer<Feature.DuringAnalysisAccess,Class<?>> callback, Class<?> baseClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> findClassByName(String className) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassLoader getApplicationClassLoader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Path> getApplicationClassPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Path> getApplicationModulePath() {
        throw new UnsupportedOperationException();
    }
}
