package org.qbicc.maven.plugin;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.qbicc.context.Diagnostic;
import org.qbicc.machine.arch.Platform;
import org.qbicc.main.ClassPathEntry;
import org.qbicc.main.Main;

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

    @Parameter
    private List<File> librarySearchPaths;

    @Parameter
    private String classLibraryVersion;

    @Parameter(defaultValue = "${project.artifactId}", required = true)
    private String outputName;

    @Parameter
    private String platform;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // capture logs
        Logger qbiccLogger = Logger.getLogger("org.qbicc");
        qbiccLogger.setLevel(Level.INFO);
        Handler[] oldHandlers = qbiccLogger.getHandlers();
        try {
            for (Handler oldHandler : oldHandlers) {
                qbiccLogger.removeHandler(oldHandler);
            }
            qbiccLogger.addHandler(new MojoHandler(getLog()));
            executeWithLogging();
        } finally {
            // restore logging setup
            for (Handler handler : qbiccLogger.getHandlers()) {
                qbiccLogger.removeHandler(handler);
            }
            for (Handler oldHandler : oldHandlers) {
                qbiccLogger.addHandler(oldHandler);
            }
        }
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
        List<File> librarySearchPaths = this.librarySearchPaths;
        if (librarySearchPaths != null && ! librarySearchPaths.isEmpty()) {
            List<Path> pathList = new ArrayList<>(librarySearchPaths.size());
            for (File librarySearchPath : librarySearchPaths) {
                pathList.add(librarySearchPath.toPath());
            }
            builder.addLibrarySearchPaths(pathList);
        }
        if (classLibraryVersion != null) {
            builder.setClassLibVersion(classLibraryVersion);
        }
        builder.setOutputName(outputName);
        if (platform != null) {
            builder.setPlatform(Platform.parse(platform));
        }
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

    private static <E> List<E> newList(final Object ignored) {
        return new ArrayList<>();
    }
}
