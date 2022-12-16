package org.qbicc.plugin.nativeimage;

import com.oracle.svm.core.configure.ResourcesRegistry;
import org.graalvm.nativeimage.impl.ConfigurationCondition;
import org.qbicc.context.CompilationContext;

import java.util.Collection;
import java.util.Locale;

public class QbiccResourcesRegistry implements ResourcesRegistry {
    private final CompilationContext ctxt;

    QbiccResourcesRegistry(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    @Override
    public void addResources(ConfigurationCondition condition, String pattern) {
        ctxt.warning("ignoring: addResources %s %s", condition.toString(), pattern);
    }

    @Override
    public void injectResource(Module module, String resourcePath, byte[] resourceContent) {
        ctxt.warning("ignoring: injectResource %s %s", resourcePath);
    }

    @Override
    public void ignoreResources(ConfigurationCondition condition, String pattern) {
        ctxt.warning("ignoring: ignoreResources %s %s", condition.toString(), pattern);
    }

    @Override
    public void addResourceBundles(ConfigurationCondition condition, String name) {
        ctxt.warning("ignoring: addResourceBundles %s %s", condition.toString(), name);
    }

    @Override
    public void addResourceBundles(ConfigurationCondition condition, String basename, Collection<Locale> locales) {
        ctxt.warning("ignoring: addResourceBundles %s %s %s", condition.toString(), basename, locales.toString());

    }

    @Override
    public void addClassBasedResourceBundle(ConfigurationCondition condition, String basename, String className) {
        ctxt.warning("ignoring: addClassBasedResourceBundle %s %s", condition.toString(), basename, className);
    }
}
