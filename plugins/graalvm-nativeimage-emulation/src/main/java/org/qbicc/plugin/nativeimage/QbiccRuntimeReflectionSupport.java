package org.qbicc.plugin.nativeimage;

import org.graalvm.nativeimage.impl.ConfigurationCondition;
import org.graalvm.nativeimage.impl.RuntimeReflectionSupport;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.reachability.RuntimeReflectionRoots;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class QbiccRuntimeReflectionSupport implements RuntimeReflectionSupport {
    private final CompilationContext ctxt;

    QbiccRuntimeReflectionSupport(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public Map<Class<?>, Set<Class<?>>> getReflectionInnerClasses() {
        throw new UnsupportedOperationException();
    }

    public Set<Field> getReflectionFields() {
        throw new UnsupportedOperationException();
    }

    public Set<Executable> getReflectionExecutables() {
        throw new UnsupportedOperationException();
    }

    public Object getAccessor(Executable method) {
        throw new UnsupportedOperationException();
    }

    public Set<?> getHidingReflectionFields() {
        throw new UnsupportedOperationException();
    }

    public Set<?> getHidingReflectionMethods() {
        throw new UnsupportedOperationException();
    }

    public Set<Executable> getHiddenMethods() {
        throw new UnsupportedOperationException();
    }

    public Set<Executable> getQueriedOnlyMethods() {
        throw new UnsupportedOperationException();
    }

    public Object[] getRecordComponents(Class<?> type) {
        throw new UnsupportedOperationException();
    }

    public void registerHeapDynamicHub(Object hub) {
        throw new UnsupportedOperationException();
    }

    public Set<?> getHeapDynamicHubs() {
        throw new UnsupportedOperationException();
    }

    public void registerHeapReflectionObject(AccessibleObject object) {
        throw new UnsupportedOperationException();
    }

    public Set<AccessibleObject> getHeapReflectionObjects() {
        throw new UnsupportedOperationException();
    }

    public int getReflectionClassesCount() {
        throw new UnsupportedOperationException();
    }

    public int getReflectionMethodsCount() {
        throw new UnsupportedOperationException();
    }

    public int getReflectionFieldsCount() {
        throw new UnsupportedOperationException();
    }

    public boolean requiresProcessing() {
        throw new UnsupportedOperationException();
    }

    public void register(ConfigurationCondition condition, Class<?>... classes) {
        Arrays.stream(classes).forEach(clazz -> register(condition, false, clazz));
    }

    public void register(ConfigurationCondition condition, boolean unsafeAllocated, Class<?> clazz) {
        RuntimeReflectionRoots.get(ctxt).registerClass(clazz);
    }

    public void register(ConfigurationCondition condition, boolean queriedOnly, Executable... methods) {
        if (methods.length > 0) {
            RuntimeReflectionRoots.get(ctxt).registerMethods(methods);
        }
    }

    public void register(ConfigurationCondition condition, boolean finalIsWritable, Field... fields) {
        if (fields.length > 0) {
            RuntimeReflectionRoots.get(ctxt).registerFields(fields);
        }
    }
}
