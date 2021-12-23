package org.qbicc.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.DependencyCollector;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.impl.Installer;
import org.eclipse.aether.impl.LocalRepositoryProvider;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.OfflineController;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryConnectorProvider;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.UpdateCheckManager;
import org.eclipse.aether.impl.UpdatePolicyAnalyzer;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider;
import org.eclipse.aether.internal.impl.DefaultDeployer;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider;
import org.eclipse.aether.internal.impl.DefaultMetadataResolver;
import org.eclipse.aether.internal.impl.DefaultOfflineController;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultRepositoryConnectorProvider;
import org.eclipse.aether.internal.impl.DefaultRepositoryEventDispatcher;
import org.eclipse.aether.internal.impl.DefaultRepositoryLayoutProvider;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.internal.impl.DefaultTrackingFileManager;
import org.eclipse.aether.internal.impl.DefaultTransporterProvider;
import org.eclipse.aether.internal.impl.DefaultUpdateCheckManager;
import org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.Maven2RepositoryLayoutFactory;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.TrackingFileManager;
import org.eclipse.aether.internal.impl.collect.DefaultDependencyCollector;
import org.eclipse.aether.internal.impl.synccontext.DefaultSyncContextFactory;
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactorySelector;
import org.eclipse.aether.internal.impl.synccontext.named.SimpleNamedLockFactorySelector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumPolicyProvider;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.spi.log.Logger;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;

/**
 * Our implementation of {@link ServiceLocator} for Maven resolution.
 */
final class QbiccServiceLocator implements ServiceLocator {

    private static final Map<Class<?>, List<Supplier<Object>>> serviceClasses = Map.ofEntries(
        Map.entry(ArtifactDescriptorReader.class, List.of(DefaultArtifactDescriptorReader::new)),
        Map.entry(ArtifactResolver.class, List.of(DefaultArtifactResolver::new)),
        Map.entry(ChecksumPolicyProvider.class, List.of(DefaultChecksumPolicyProvider::new)),
        Map.entry(DependencyCollector.class, List.of(DefaultDependencyCollector::new)),
        Map.entry(Deployer.class, List.of(DefaultDeployer::new)),
        Map.entry(FileProcessor.class, List.of(DefaultFileProcessor::new)),
        Map.entry(Installer.class, List.of(DefaultInstaller::new)),
        Map.entry(LocalRepositoryManagerFactory.class, List.of(SimpleLocalRepositoryManagerFactory::new, EnhancedLocalRepositoryManagerFactory::new)),
        Map.entry(LocalRepositoryProvider.class, List.of(DefaultLocalRepositoryProvider::new)),
        Map.entry(MetadataGeneratorFactory.class, List.of(SnapshotMetadataGeneratorFactory::new, VersionsMetadataGeneratorFactory::new)),
        Map.entry(MetadataResolver.class, List.of(DefaultMetadataResolver::new)),
        Map.entry(ModelBuilder.class, List.of(() -> new DefaultModelBuilderFactory().newInstance())),
        Map.entry(NamedLockFactorySelector.class, List.of(SimpleNamedLockFactorySelector::new)),
        Map.entry(OfflineController.class, List.of(DefaultOfflineController::new)),
        Map.entry(RemoteRepositoryManager .class, List.of(DefaultRemoteRepositoryManager::new)),
        Map.entry(RepositoryConnectorFactory.class, List.of(BasicRepositoryConnectorFactory::new)),
        Map.entry(RepositoryConnectorProvider.class, List.of(DefaultRepositoryConnectorProvider::new)),
        Map.entry(RepositoryEventDispatcher.class, List.of(DefaultRepositoryEventDispatcher::new)),
        Map.entry(RepositoryLayoutFactory.class, List.of(Maven2RepositoryLayoutFactory::new)),
        Map.entry(RepositoryLayoutProvider.class, List.of(DefaultRepositoryLayoutProvider::new)),
        Map.entry(RepositorySystem.class, List.of(DefaultRepositorySystem::new)),
        Map.entry(SyncContextFactory.class, List.of(DefaultSyncContextFactory::new)),
        Map.entry(org.eclipse.aether.impl.SyncContextFactory.class, List.of(org.eclipse.aether.internal.impl.synccontext.legacy.DefaultSyncContextFactory::new)),
        Map.entry(TrackingFileManager.class, List.of(DefaultTrackingFileManager::new)),
        Map.entry(TransporterFactory.class, List.of(WagonTransporterFactory::new)),
        Map.entry(TransporterProvider.class, List.of(DefaultTransporterProvider::new)),
        Map.entry(UpdateCheckManager.class, List.of(DefaultUpdateCheckManager::new)),
        Map.entry(UpdatePolicyAnalyzer.class, List.of(DefaultUpdatePolicyAnalyzer::new)),
        Map.entry(VersionRangeResolver.class, List.of(DefaultVersionRangeResolver::new)),
        Map.entry(VersionResolver.class, List.of(DefaultVersionResolver::new)),

        // custom implementations

        Map.entry(WagonProvider.class, List.of(QbiccWagonProvider::new)),
        Map.entry(LoggerFactory.class, List.of(Log::new))
    );
    private final Map<Class<?>, List<?>> instantiated = new ConcurrentHashMap<>();
    

    @Override
    public <T> T getService(Class<T> serviceType) {
        List<T> services = getServices(serviceType);
        if (services.size() == 1) {
            return services.iterator().next();
        } else if (services.size() > 1) {
            throw new IllegalStateException("Too many implementations of " + serviceType);
        }
        return null;

    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getServices(Class<T> serviceType) {
        List<?> cachedList = instantiated.get(serviceType);
        if (cachedList == null) {
            List<Supplier<Object>> suppliers = serviceClasses.get(serviceType);
            if (suppliers != null) {
                ArrayList<T> list = new ArrayList<>(suppliers.size());
                for (Supplier<Object> supplier : suppliers) {
                    Object service = supplier.get();
                    if (service instanceof Service srv) {
                        srv.initService(this);
                    }
                    list.add(serviceType.cast(service));
                }
                cachedList = List.copyOf(list);
            } else {
                cachedList = List.of();
            }
        }
        List<?> appearing = instantiated.putIfAbsent(serviceType, cachedList);
        if (appearing != null) {
            cachedList = appearing;
        }
        return (List<T>) cachedList;
    }

    static final class Log implements LoggerFactory {
        @Override
        public Logger getLogger(String name) {
            org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(name);
            return new Logger() {
                @Override
                public boolean isDebugEnabled() {
                    return log.isDebugEnabled();
                }

                @Override
                public void debug(String msg) {
                    log.debug(msg, (Throwable)null);
                }

                @Override
                public void debug(String msg, Throwable error) {
                    log.debug(msg, error);
                }

                @Override
                public boolean isWarnEnabled() {
                    return log.isEnabled(org.jboss.logging.Logger.Level.WARN);
                }

                @Override
                public void warn(String msg) {
                    log.warn(msg, (Throwable)null);
                }

                @Override
                public void warn(String msg, Throwable error) {
                    log.warn(msg, error);
                }
            };
        }
    }
}
