package org.qbicc.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.DiagnosticContext;
import org.qbicc.driver.ClassPathElement;
import org.qbicc.driver.ClassPathItem;

/**
 * The class implementing the actual artifact request logic.
 */
public final class DefaultArtifactRequestor {
    public DefaultArtifactRequestor() {}

    public List<ClassPathItem> requestArtifactsFromRepositories(RepositorySystem system, final RepositorySystemSession session, final List<RemoteRepository> remoteRepositoryList,
                                                                final List<ClassPathEntry> classPathList, final DiagnosticContext ctxt, Runtime.Version version) throws IOException {
        CollectRequest collectRequest = new CollectRequest((Dependency)null, null, system.newResolutionRepositories(session, remoteRepositoryList));
        Map<String, Map<String, ClassPathEntry>> gaToCpe = new HashMap<>();
        for (ClassPathEntry classPathEntry : classPathList) {
            if (classPathEntry instanceof ClassPathEntry.ClassLibraries cl) {
                DefaultArtifact artifact = new DefaultArtifact("org.qbicc.rt", "qbicc-rt", "", "pom", cl.getVersion());
                Dependency dependency = new Dependency(artifact, "runtime", Boolean.FALSE);
                collectRequest.addDependency(dependency);
                gaToCpe.computeIfAbsent(artifact.getGroupId(), DefaultArtifactRequestor::newMap).put(artifact.getArtifactId(), classPathEntry);
            } else if (classPathEntry instanceof ClassPathEntry.MavenArtifact ma) {
                Artifact artifact = ma.getArtifact();
                Dependency dependency = new Dependency(artifact, "runtime", Boolean.FALSE);
                collectRequest.addDependency(dependency);
                gaToCpe.computeIfAbsent(artifact.getGroupId(), DefaultArtifactRequestor::newMap).put(artifact.getArtifactId(), classPathEntry);
            } else if (classPathEntry instanceof ClassPathEntry.FilePath) {
                // TODO: open JAR file or directory path to see if it is a Maven artifact, and treat it as an override
                // otherwise, do not add it to the requests list
            }
        }
        DependencyRequest depReq = new DependencyRequest(collectRequest, null);
        DependencyResult dependencyResult;
        try {
            dependencyResult = system.resolveDependencies(session, depReq);
        } catch (DependencyResolutionException e) {
            Diagnostic parent = ctxt.error("Failed to resolve dependencies: %s", e);
            DependencyResult result = e.getResult();
            List<ArtifactResult> artifactResults = result.getArtifactResults();
            for (ArtifactResult artifactResult : artifactResults) {
                List<Exception> exceptions = artifactResult.getExceptions();
                for (Exception exception : exceptions) {
                    ctxt.error(parent, "Resolve of %s failed: %s", artifactResult.getRequest().getArtifact(), exception);
                }
            }
            for (Exception collectException : result.getCollectExceptions()) {
                ctxt.error(parent, "Collect exception: %s", collectException);
            }
            return List.of();
        }
        List<ArtifactResult> artifactResults = dependencyResult.getArtifactResults();
        Map<ClassPathEntry, List<ArtifactResult>> resultMapping = new HashMap<>();
        for (ArtifactResult result : artifactResults) {
            DependencyNode node = result.getRequest().getDependencyNode();
            ClassPathEntry entry = findDependency(node, gaToCpe);
            if (entry != null) {
                resultMapping.computeIfAbsent(entry, DefaultArtifactRequestor::newList).add(result);
                populateChildren(entry, node, gaToCpe);
            }
            Artifact resultArtifact = result.getArtifact();
            if (result.isMissing()) {
                ctxt.error("Required artifact is missing: %s", resultArtifact);
            } else if (! result.isResolved()) {
                ctxt.error("Required artifact is not missing but wasn't resolved: %s", resultArtifact);
            }
        }
        // second pass - produce the class path items
        List<ClassPathItem> resultList = new ArrayList<>();
        Set<ArtifactResult> unmappedResults = new LinkedHashSet<>(artifactResults);
        try {
            for (ClassPathEntry classPathEntry : classPathList) {
                if (classPathEntry instanceof ClassPathEntry.MavenArtifact || classPathEntry instanceof ClassPathEntry.ClassLibraries) {
                    // we requested it from Maven
                    for (ArtifactResult artifactResult : resultMapping.getOrDefault(classPathEntry, List.of())) {
                        unmappedResults.remove(artifactResult);
                        appendArtifactResult(system, session, resultList, artifactResult, version);
                    }
                } else if (classPathEntry instanceof ClassPathEntry.FilePath fp) {
                    Path path = fp.getPath();
                    if (Files.isDirectory(path)) {
                        resultList.add(new ClassPathItem(path.toString(), List.of(ClassPathElement.forDirectory(path)), List.of()));
                    } else if (Files.isRegularFile(path)) {
                        ClassPathElement element = ClassPathElement.forJarFile(path, version);
                        try {
                            resultList.add(new ClassPathItem(path.toString(), List.of(element), List.of()));
                        } catch (Throwable t) {
                            element.close();
                            throw t;
                        }
                    } else if (! Files.exists(path)) {
                        ctxt.warning("Class path entry \"%s\" does not exist", path);
                    }
                }
            }
            // now append the remaining ones
            for (ArtifactResult unmappedResult : unmappedResults) {
                // todo - log?
                appendArtifactResult(system, session, resultList, unmappedResult, version);
            }
        } catch (Throwable t) {
            for (ClassPathItem item : resultList) {
                item.close();
            }
            throw t;
        }
        return List.copyOf(resultList);
    }

    static <K, V> Map<K, V> newMap(final Object ignored) {
        return new HashMap<>();
    }

    static void appendArtifactResult(RepositorySystem system, final RepositorySystemSession session, final List<ClassPathItem> resultList,
                                     final ArtifactResult artifactResult, Runtime.Version version) throws IOException {
        if (artifactResult.isResolved() && !artifactResult.isMissing()) {
            Artifact resultArtifact = artifactResult.getArtifact();
            if (! artifactResult.getArtifact().getExtension().equals("jar")) {
                // aggregator POM perhaps; skip
                return;
            }
            File jarFile = resultArtifact.getFile();
            File sourceFile = null;
            ArtifactRequest sourceRequest = new ArtifactRequest(new DefaultArtifact(resultArtifact.getGroupId(), resultArtifact.getArtifactId(), "source", "jar", resultArtifact.getVersion()), null, null);
            try {
                ArtifactResult sourceResult = system.resolveArtifact(session, sourceRequest);
                if (sourceResult != null && sourceResult.isResolved() && !sourceResult.isMissing()) {
                    sourceFile = sourceResult.getArtifact().getFile();
                }
            } catch (ArtifactResolutionException ignored) {}
            ClassPathElement element = ClassPathElement.forJarFile(jarFile, version);
            List<ClassPathElement> jarPath = List.of(element);
            try {
                ClassPathElement sourceElement = sourceFile == null ? null : ClassPathElement.forJarFile(sourceFile, version);
                List<ClassPathElement> sourcePath = sourceFile == null ? List.of() : List.of(sourceElement);
                try {
                    resultList.add(new ClassPathItem(resultArtifact.toString(), jarPath, sourcePath));
                } catch (Throwable t) {
                    if (sourceElement != null) {
                        sourceElement.close();
                    }
                    throw t;
                }
            } catch (Throwable t) {
                element.close();
                throw t;
            }
        }
    }

    static void populateChildren(final ClassPathEntry parent, final DependencyNode dependencyNode, final Map<String, Map<String, ClassPathEntry>> gaToCpe) {
        for (DependencyNode child : dependencyNode.getChildren()) {
            Artifact artifact = child.getArtifact();
            gaToCpe.computeIfAbsent(artifact.getGroupId(), DefaultArtifactRequestor::newMap).putIfAbsent(artifact.getArtifactId(), parent);
            populateChildren(parent, child, gaToCpe);
        }
    }

    static ClassPathEntry findDependency(final DependencyNode dependencyNode, final Map<String, Map<String, ClassPathEntry>> gaToCpe) {
        if (dependencyNode == null) {
            return null;
        }
        Dependency dependency = dependencyNode.getDependency();
        Artifact artifact = dependency.getArtifact();
        ClassPathEntry classPathEntry = gaToCpe.getOrDefault(artifact.getGroupId(), Map.of()).get(artifact.getArtifactId());
        if (classPathEntry != null) {
            return classPathEntry;
        }
        for (DependencyNode child : dependencyNode.getChildren()) {
            ClassPathEntry found = findDependency(child, gaToCpe);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    static <E> List<E> newList(final Object ignored) {
        return new ArrayList<>();
    }
}
