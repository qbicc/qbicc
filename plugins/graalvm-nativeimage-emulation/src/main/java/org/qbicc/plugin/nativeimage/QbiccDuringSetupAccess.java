package org.qbicc.plugin.nativeimage;

import org.graalvm.nativeimage.hosted.Feature;
import org.qbicc.context.CompilationContext;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public class QbiccDuringSetupAccess implements Feature.DuringSetupAccess {
    final CompilationContext ctxt;

    QbiccDuringSetupAccess(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    @Override
    public void registerObjectReplacer(Function<Object, Object> replacer) {
        ctxt.warning("Ignoring registerObjectReplacer");
    }

    @Override
    public Class<?> findClassByName(String className) {
        throw new UnsupportedOperationException("findClassByName");
    }

    @Override
    public List<Path> getApplicationClassPath() {
        throw new UnsupportedOperationException("getApplicationClassPath");
    }

    @Override
    public List<Path> getApplicationModulePath() {
        throw new UnsupportedOperationException("getApplicationModulePath");
    }

    @Override
    public ClassLoader getApplicationClassLoader() {
        throw new UnsupportedOperationException("getApplicationClassLoader");
    }
}
