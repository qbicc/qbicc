package org.qbicc.main;

import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory;
import org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.impl.guice.AetherModule;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonConfigurator;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;

class QbiccResolverModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new AetherModule());

        // make module "complete" by binding things not bound by AetherModule
        bind(ArtifactDescriptorReader.class).to(DefaultArtifactDescriptorReader.class).in(Singleton.class);
        bind(VersionResolver.class).to(DefaultVersionResolver.class).in(Singleton.class);
        bind(VersionRangeResolver.class).to(DefaultVersionRangeResolver.class).in(Singleton.class);
        bind(MetadataGeneratorFactory.class).annotatedWith(Names.named("snapshot"))
            .to(SnapshotMetadataGeneratorFactory.class).in(Singleton.class);

        bind(MetadataGeneratorFactory.class).annotatedWith(Names.named("versions"))
            .to(VersionsMetadataGeneratorFactory.class).in(Singleton.class);

        bind(RepositoryConnectorFactory.class).annotatedWith(Names.named("basic"))
            .to(BasicRepositoryConnectorFactory.class);
        bind(TransporterFactory.class).annotatedWith(Names.named("file")).to(WagonTransporterFactory.class);
        bind(TransporterFactory.class).annotatedWith(Names.named("http")).to(WagonTransporterFactory.class);
        bind(WagonConfigurator.class).toInstance((wagon, configuration) -> { /* no-op */ });
        bind(WagonProvider.class).to(QbiccWagonProvider.class);

    }

    /**
     * Repository system connectors (needed for remote transport).
     */
    @Provides
    @Singleton
    Set<RepositoryConnectorFactory> provideRepositoryConnectorFactories(
        @Named("basic") RepositoryConnectorFactory basic) {
        return Set.of(basic);
    }

    /**
     * Repository system transporters (needed for remote transport).
     */
    @Provides
    @Singleton
    Set<TransporterFactory> provideTransporterFactories(@Named("file") TransporterFactory file,
                                                        @Named("http") TransporterFactory http) {
        return Set.of(file, http);
    }

    /**
     * Repository metadata generators (needed for remote transport).
     */
    @Provides
    @Singleton
    Set<MetadataGeneratorFactory> provideMetadataGeneratorFactories(
        @Named("snapshot") MetadataGeneratorFactory snapshot,
        @Named("versions") MetadataGeneratorFactory versions) {
        return Set.of(snapshot, versions);
    }

    /**
     * Simple instance provider for model builder factory. Note: Maven 3.8.1 {@link ModelBuilder} is annotated
     * and would require much more.
     */
    @Provides
    ModelBuilder provideModelBuilder() {
        return new DefaultModelBuilderFactory().newInstance();
    }
}
