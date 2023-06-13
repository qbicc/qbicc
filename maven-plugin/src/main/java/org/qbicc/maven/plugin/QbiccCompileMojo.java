package org.qbicc.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.DiagnosticContext;
import org.qbicc.driver.ClassPathItem;
import org.qbicc.machine.arch.Platform;
import org.qbicc.main.ClassPathEntry;
import org.qbicc.main.DefaultArtifactRequestor;
import org.qbicc.main.Main;
import org.qbicc.plugin.llvm.LLVMConfiguration;
import org.qbicc.plugin.llvm.ReferenceStrategy;

/**
 *
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class QbiccCompileMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The name of the main class.
     */
    @Parameter(required = true)
    private String mainClass;

    @Parameter(defaultValue = "${project.build.directory}/native", required = true)
    private File outputPath;

    @Parameter(defaultValue = "true")
    private boolean smallTypeIds;

    @Parameter(defaultValue = "true")
    private boolean optEscapeAnalysis;

    @Parameter(defaultValue = "true")
    private boolean optGotos;

    @Parameter(defaultValue = "false")
    private boolean optInlining;

    @Parameter(defaultValue = "true")
    private boolean optPhis;

    @Parameter(defaultValue = "1")
    private int optLevel;

    @Parameter(defaultValue = "false", property = "qbicc.emit-asm")
    private boolean emitAsm;

    @Parameter(defaultValue = "false", property = "qbicc.emit-llvm-ir")
    private boolean emitLlvmIr;

    @Parameter(defaultValue = "false", property = "skipNative")
    private boolean skip;

    @Parameter(defaultValue = "false", property = "qbicc.llvm.opaque-pointers")
    private boolean llvmOpaquePointers;

    @Parameter
    private List<File> librarySearchPaths;

    @Parameter
    private String classLibraryVersion;

    @Parameter(defaultValue = "${project.artifactId}", required = true)
    private String outputName;

    @Parameter
    private String platform;

    @Parameter
    private List<String> llcOptions;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySystemSession;

    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepositories;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("(Native compilation skipped)");
            return;
        }
        // capture logs
        MojoLogger.pluginLog = getLog();
        executeWithLogging();
    }

    private void executeWithLogging() throws MojoExecutionException, MojoFailureException {
        List<String> runtimeClasspathElements;
        try {
            runtimeClasspathElements = project.getRuntimeClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Resolution failed", e);
        }

        Main.Builder builder = Main.builder();
        for (String runtimeClasspathElement : runtimeClasspathElements) {
            builder.addAppPath(ClassPathEntry.of(Path.of(runtimeClasspathElement)));
        }
        builder.setMainClass(mainClass);
        builder.setOutputPath(outputPath.toPath());
        builder.setSmallTypeIds(smallTypeIds);
        builder.setIsPie(true);
        builder.setOptEscapeAnalysis(optEscapeAnalysis);
        builder.setOptGotos(optGotos);
        builder.setOptInlining(optInlining);
        builder.setOptPhis(optPhis);
        builder.setOptLevel(optLevel);
        builder.setClassPathResolver(this::resolveClassPath);
        List<File> librarySearchPaths = this.librarySearchPaths;
        if (librarySearchPaths != null && ! librarySearchPaths.isEmpty()) {
            List<Path> pathList = new ArrayList<>(librarySearchPaths.size());
            for (File librarySearchPath : librarySearchPaths) {
                pathList.add(librarySearchPath.toPath());
            }
            builder.addLibrarySearchPaths(pathList);
        }
        builder.addLibrarySearchPaths(splitPathString(System.getenv("LIBRARY_PATH")));
        if (classLibraryVersion != null) {
            builder.setClassLibVersion(classLibraryVersion);
        }
        builder.setOutputName(outputName);
        if (platform != null) {
            builder.setPlatform(Platform.parse(platform));
        }
        builder.setLlvmConfigurationBuilder(LLVMConfiguration.builder()
            .setCompileOutput(true)
            .setPlatform(Platform.HOST_PLATFORM)
            .setStatepointEnabled(true)
            .setReferenceStrategy(ReferenceStrategy.POINTER_AS1)
            .setPie(true)
            .setEmitIr(emitLlvmIr)
            .setEmitAssembly(emitAsm)
            .setOpaquePointers(llvmOpaquePointers)
            .addLlcOptions(llcOptions == null ? List.of() : llcOptions)
        );
        final Map<Diagnostic.Level, List<Diagnostic>> map = new EnumMap<>(Diagnostic.Level.class);
        builder.setDiagnosticsHandler(new Consumer<Iterable<Diagnostic>>() {
            @Override
            public void accept(Iterable<Diagnostic> diagnostics) {
                for (Diagnostic diagnostic : diagnostics) {
                    map.computeIfAbsent(diagnostic.getLevel(), QbiccCompileMojo::newList).add(diagnostic);
                }
            }
        });
        Main main = builder.build();
        try {
            main.call();
        } catch (Throwable t) {
            throw new MojoExecutionException("qbicc compilation failed unexpectedly", t);
        }
        List<Diagnostic> diags;
        diags = map.get(Diagnostic.Level.WARNING);
        if (diags != null && ! diags.isEmpty()) {
            for (Diagnostic diag : diags) {
                getLog().warn(diag.toString());
            }
        }
        diags = map.get(Diagnostic.Level.ERROR);
        if (diags != null && ! diags.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (Diagnostic diag : diags) {
                getLog().error(diag.toString());
                diag.appendTo(b);
            }
            String shortMessage;
            if (diags.size() == 1) {
                shortMessage = diags.get(0).toString();
            } else {
                shortMessage = "Compilation failed";
            }
            throw new MojoFailureException(null, shortMessage, b.toString());
        }
    }

    private void resolveClassPath(final DiagnosticContext ctxt, final Consumer<ClassPathItem> consumer, final List<ClassPathEntry> classPathEntries, Runtime.Version version) throws IOException {
        final DefaultArtifactRequestor requestor = new DefaultArtifactRequestor();
        final List<ClassPathItem> result = requestor.requestArtifactsFromRepositories(repoSystem, repositorySystemSession, remoteRepositories, classPathEntries, ctxt, version);
        result.forEach(consumer);
    }

    private static <E> List<E> newList(final Object ignored) {
        return new ArrayList<>();
    }

    private static List<Path> splitPathString(String str) {
        if (str == null || str.isEmpty()) {
            return List.of();
        }
        char psc = File.pathSeparatorChar;
        int start = 0;
        int idx = str.indexOf(psc);
        ArrayList<Path> list = new ArrayList<>();
        for (;;) {
            String subStr;
            if (idx == -1) {
                subStr = str.substring(start);
            } else {
                subStr = str.substring(start, idx);
            }
            if (! subStr.isEmpty()) {
                list.add(Path.of(subStr));
            }
            if (idx == -1) {
                return list;
            } else {
                start = idx + 1;
            }
            idx = str.indexOf(psc, start);
        }
    }
}
