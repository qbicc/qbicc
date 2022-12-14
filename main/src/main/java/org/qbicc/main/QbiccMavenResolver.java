package org.qbicc.main;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.artifact.JavaScopes;
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
import org.qbicc.driver.ClassPathItem;

/**
 * This class is responsible for resolving the items on the class path.
 */
final class QbiccMavenResolver {
    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
    private final RepositorySystem system;
    private final QbiccBeanContainer locator;

    QbiccMavenResolver(QbiccBeanContainer locator) {
        this.locator = locator;
        this.system = locator.get(RepositorySystem.class);
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
            mirrorSelector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, false, mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
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

    List<ClassPathItem> requestArtifacts(RepositorySystemSession session, Settings settings, List<ClassPathEntry> classPathList,
                                         DiagnosticContext ctxt, Runtime.Version version) throws IOException {
        return new DefaultArtifactRequestor().requestArtifactsFromRepositories(system, session, createRemoteRepositoryList(settings), classPathList, ctxt, version);
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
