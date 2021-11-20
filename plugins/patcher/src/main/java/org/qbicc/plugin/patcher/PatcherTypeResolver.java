package org.qbicc.plugin.patcher;

import org.qbicc.context.ClassContext;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DescriptorTypeResolver;

/**
 * A type resolver which transforms patch class names into the patched class name.
 */
public final class PatcherTypeResolver implements DescriptorTypeResolver.Delegating {
    private final ClassContextPatchInfo info;
    private final DescriptorTypeResolver delegate;

    private PatcherTypeResolver(final ClassContextPatchInfo info, final DescriptorTypeResolver delegate) {
        this.info = info;
        this.delegate = delegate;
    }

    public static DescriptorTypeResolver create(final ClassContext classContext, final DescriptorTypeResolver delegate) {
        Patcher patcher = Patcher.get(classContext.getCompilationContext());
        return new PatcherTypeResolver(patcher.getOrAdd(classContext), delegate);
    }

    @Override
    public DescriptorTypeResolver getDelegate() {
        return delegate;
    }

    public ValueType resolveTypeFromClassName(String packageName, String className) {
        String internalName = packageName.isEmpty() ? className : packageName + '/' + className;
        if (info.isPatchClass(internalName)) {
            String name = info.getTargetForPatchClass(internalName);
            int idx = name.lastIndexOf('/');
            if (idx == -1) {
                return getDelegate().resolveTypeFromClassName("", name);
            } else {
                return getDelegate().resolveTypeFromClassName(name.substring(0, idx), name.substring(idx + 1));
            }
        }
        return getDelegate().resolveTypeFromClassName(packageName, className);
    }
}
