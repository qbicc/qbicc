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

import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.DiagnosticContext;
import org.qbicc.context.Location;
import org.qbicc.driver.ClassPathElement;
import org.qbicc.driver.ClassPathItem;

/**
 * This class is responsible for resolving the items on the class path.
 */
final class QbiccMavenResolver {
    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
    private final RepositorySystem system;

    QbiccMavenResolver(ServiceLocator locator) {
        this.system = locator.getService(RepositorySystem.class);
    }

    Settings createSettings(DiagnosticContext ctxt, File globalSettings, File userSettings) throws SettingsBuildingException {
        SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        if (globalSettings != null) {
            request.setGlobalSettingsFile(globalSettings);
        }
        if (userSettings != null) {
            request.setUserSettingsFile(userSettings);
        }
        request.setSystemProperties(System.getProperties());
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();

        SettingsBuildingResult result = builder.build(request);
        for (SettingsProblem problem : result.getProblems()) {
            Location loc = Location.builder().setSourceFilePath(problem.getSource()).setLineNumber(problem.getLineNumber()).build();
            SettingsProblem.Severity severity = problem.getSeverity();
            Diagnostic.Level level = switch (severity) {
                case FATAL, ERROR -> Diagnostic.Level.ERROR;
                case WARNING -> Diagnostic.Level.WARNING;
            };
            ctxt.msg(null, loc, level, "Maven settings problem: %s", problem.getMessage());
        }
        Settings settings = result.getEffectiveSettings();
        // now apply some defaults...
        if (settings.getLocalRepository() == null) {
            settings.setLocalRepository(System.getProperty("user.home", "") + File.separator + ".m2" + File.separator + "repository");
        }
        return settings;
    }

    RepositorySystemSession createSession(final Settings settings) {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        // offline = "simple"
        // normal = "enhanced"
        String repositoryType = "default";
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, new LocalRepository(new File(settings.getLocalRepository()), repositoryType)));
//        session.setWorkspaceReader(new QbiccWorkspaceReader(main));
//        session.setTransferListener(new QbiccLoggingTransferListener());
//        session.setRepositoryListener(new QbiccLoggingRepositoryListener());
        session.setOffline(settings.isOffline());

        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for (Mirror mirror : settings.getMirrors()) {
            mirrorSelector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
        }
        session.setMirrorSelector(mirrorSelector);

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for (org.apache.maven.settings.Proxy proxy : settings.getProxies()) {
            proxySelector.add(convertProxy(proxy), proxy.getNonProxyHosts());
        }
        session.setProxySelector(proxySelector);

        session.setDependencyManager(new ClassicDependencyManager());
        session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(true, true));
        session.setDependencyTraverser(new FatArtifactTraverser());

        DependencyGraphTransformer dependencyGraphTransformer = new ChainedDependencyGraphTransformer(
            new ConflictResolver(
                new NearestVersionSelector(),
                new JavaScopeSelector(),
                new SimpleOptionalitySelector(),
                new JavaScopeDeriver()
            ),
            new JavaDependencyContextRefiner()
        );
        session.setDependencyGraphTransformer(dependencyGraphTransformer);

        DefaultArtifactTypeRegistry artifactTypeRegistry = new DefaultArtifactTypeRegistry();
        artifactTypeRegistry.add(new DefaultArtifactType("pom"));
        artifactTypeRegistry.add(new DefaultArtifactType("maven-plugin", "jar", "", "java"));
        artifactTypeRegistry.add(new DefaultArtifactType("jar", "jar", "", "java"));
        artifactTypeRegistry.add(new DefaultArtifactType("ejb", "jar", "", "java"));
        artifactTypeRegistry.add(new DefaultArtifactType("ejb-client", "jar", "client", "java"));
        artifactTypeRegistry.add(new DefaultArtifactType("test-jar", "jar", "tests", "java"));
        artifactTypeRegistry.add(new DefaultArtifactType("javadoc", "jar", "javadoc", "java"));
        artifactTypeRegistry.add(new DefaultArtifactType("java-source", "jar", "sources", "java", false, false));
        artifactTypeRegistry.add(new DefaultArtifactType("war", "war", "", "java", false, true));
        artifactTypeRegistry.add(new DefaultArtifactType("ear", "ear", "", "java", false, true));
        artifactTypeRegistry.add(new DefaultArtifactType("rar", "rar", "", "java", false, true));
        artifactTypeRegistry.add(new DefaultArtifactType("par", "par", "", "java", false, true));
        session.setArtifactTypeRegistry(artifactTypeRegistry);

        session.setSystemProperties(System.getProperties());
        session.setConfigProperties(System.getProperties());

        session.setDependencySelector(new AndDependencySelector(
            new ScopeDependencySelector(Set.of(JavaScopes.RUNTIME, JavaScopes.COMPILE), Set.of()),
            new OptionalDependencySelector()
        ));

        return session;
    }

    List<RemoteRepository> createRemoteRepositoryList(final Settings settings) {
        List<RemoteRepository> basicList;
        if (! settings.isOffline()) {
            RemoteRepository.Builder builder = new RemoteRepository.Builder("central", "default", MAVEN_CENTRAL);
            builder.setSnapshotPolicy(new RepositoryPolicy(false, null, null));
            RemoteRepository remoteRepository = builder.build();
            basicList = List.of(remoteRepository);
        } else {
            return List.of();
        }
        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for (Mirror mirror : settings.getMirrors()) {
            mirrorSelector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
        }
        Set<RemoteRepository> mirroredRepos = new LinkedHashSet<RemoteRepository>();
        for (RemoteRepository repository : basicList) {
            RemoteRepository mirror = mirrorSelector.getMirror(repository);
            mirroredRepos.add(mirror != null ? mirror : repository);
        }
        final Set<RemoteRepository> authorizedRepos = new LinkedHashSet<RemoteRepository>();
        for (RemoteRepository remoteRepository : mirroredRepos) {
            final RemoteRepository.Builder builder = new RemoteRepository.Builder(remoteRepository);
            Server server = settings.getServer(remoteRepository.getId());
            if (server != null) {
                final AuthenticationBuilder authenticationBuilder = new AuthenticationBuilder()
                        .addUsername(server.getUsername())
                        .addPassword(server.getPassword())
                        .addPrivateKey(server.getPrivateKey(), server.getPassphrase());
                builder.setAuthentication(authenticationBuilder.build());
            }
            authorizedRepos.add(builder.build());
        }
        return List.copyOf(authorizedRepos);
    }

    List<ClassPathItem> requestArtifacts(RepositorySystemSession session, Settings settings, List<ClassPathEntry> classPathList, DiagnosticContext ctxt) throws IOException {
        List<RemoteRepository> remoteRepositoryList = createRemoteRepositoryList(settings);
        CollectRequest collectRequest = new CollectRequest((Dependency)null, null, system.newResolutionRepositories(session, remoteRepositoryList));
        Map<String, Map<String, ClassPathEntry>> gaToCpe = new HashMap<>();
        for (ClassPathEntry classPathEntry : classPathList) {
            if (classPathEntry instanceof ClassPathEntry.ClassLibraries cl) {
                DefaultArtifact artifact = new DefaultArtifact("org.qbicc.rt", "qbicc-rt", "", "pom", cl.getVersion());
                Dependency dependency = new Dependency(artifact, "runtime", Boolean.FALSE);
                collectRequest.addDependency(dependency);
                gaToCpe.computeIfAbsent(artifact.getGroupId(), QbiccMavenResolver::newMap).put(artifact.getArtifactId(), classPathEntry);
            } else if (classPathEntry instanceof ClassPathEntry.MavenArtifact ma) {
                Artifact artifact = ma.getArtifact();
                Dependency dependency = new Dependency(artifact, "runtime", Boolean.FALSE);
                collectRequest.addDependency(dependency);
                gaToCpe.computeIfAbsent(artifact.getGroupId(), QbiccMavenResolver::newMap).put(artifact.getArtifactId(), classPathEntry);
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
                resultMapping.computeIfAbsent(entry, QbiccMavenResolver::newList).add(result);
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
                        appendArtifactResult(session, resultList, artifactResult);
                    }
                } else if (classPathEntry instanceof ClassPathEntry.FilePath fp) {
                    Path path = fp.getPath();
                    if (Files.isDirectory(path)) {
                        resultList.add(new ClassPathItem(path.toString(), List.of(ClassPathElement.forDirectory(path)), List.of()));
                    } else if (Files.isRegularFile(path)) {
                        ClassPathElement element = ClassPathElement.forJarFile(path);
                        try {
                            resultList.add(new ClassPathItem(path.toString(), List.of(element), List.of()));
                        } catch (Throwable t) {
                            element.close();
                            throw t;
                        }
                    }
                }
            }
            // now append the remaining ones
            for (ArtifactResult unmappedResult : unmappedResults) {
                // todo - log?
                appendArtifactResult(session, resultList, unmappedResult);
            }
        } catch (Throwable t) {
            for (ClassPathItem item : resultList) {
                item.close();
            }
            throw t;
        }
        return List.copyOf(resultList);
    }

    private static <K, V> Map<K, V> newMap(final Object ignored) {
        return new HashMap<>();
    }

    private void appendArtifactResult(final RepositorySystemSession session, final List<ClassPathItem> resultList, final ArtifactResult artifactResult) throws IOException {
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
            ClassPathElement element = ClassPathElement.forJarFile(jarFile);
            List<ClassPathElement> jarPath = List.of(element);
            try {
                ClassPathElement sourceElement = sourceFile == null ? null : ClassPathElement.forJarFile(sourceFile);
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

    private void populateChildren(final ClassPathEntry parent, final DependencyNode dependencyNode, final Map<String, Map<String, ClassPathEntry>> gaToCpe) {
        for (DependencyNode child : dependencyNode.getChildren()) {
            Artifact artifact = child.getArtifact();
            gaToCpe.computeIfAbsent(artifact.getGroupId(), QbiccMavenResolver::newMap).putIfAbsent(artifact.getArtifactId(), parent);
            populateChildren(parent, child, gaToCpe);
        }
    }

    private ClassPathEntry findDependency(final DependencyNode dependencyNode, final Map<String, Map<String, ClassPathEntry>> gaToCpe) {
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

    private static <E> List<E> newList(final Object ignored) {
        return new ArrayList<>();
    }

    static Proxy convertProxy(org.apache.maven.settings.Proxy proxy) {
        final Authentication authentication;
        if (proxy.getUsername() != null || proxy.getPassword() != null) {
            authentication = new AuthenticationBuilder().addUsername(proxy.getUsername())
                    .addPassword(proxy.getPassword()).build();
        } else {
            authentication = null;
        }
        return new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authentication);
    }

    public File getGlobalSettings() {
        String mavenHome = System.getProperty("maven.home");
        return mavenHome == null ? null : new File(mavenHome + File.separator + "conf" + File.separator + "settings.xml");
    }

    public File getUserSettings() {
        String userHome = System.getProperty("user.home");
        return userHome == null ? null : new File(userHome + File.separator + ".m2" + File.separator + "settings.xml");
    }
}
