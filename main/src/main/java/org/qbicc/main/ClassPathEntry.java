package org.qbicc.main;

import java.nio.file.Path;

import io.smallrye.common.constraint.Assert;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * An unprocessed class path entry of some sort.
 */
public abstract class ClassPathEntry {
    ClassPathEntry() {}

    public static FilePath of(Path path) {
        Assert.checkNotNullParam("path", path);
        return new FilePath(path);
    }

    public static MavenArtifact of(Artifact artifact) {
        Assert.checkNotNullParam("artifact", artifact);
        return new MavenArtifact(artifact);
    }

    public static ClassLibraries ofClassLibraries(String version) {
        Assert.checkNotNullParam("version", version);
        return new ClassLibraries(version);
    }

    public static final class FilePath extends ClassPathEntry {
        private final Path path;

        FilePath(Path path) {
            this.path = path;
        }

        public Path getPath() {
            return path;
        }

        public static final class Converter implements CommandLine.ITypeConverter<FilePath> {
            public Converter() {
            }

            @Override
            public FilePath convert(String value) {
                return ClassPathEntry.of(Path.of(value));
            }
        }
    }

    public static final class MavenArtifact extends ClassPathEntry {
        private final Artifact artifact;

        MavenArtifact(Artifact artifact) {
            this.artifact = artifact;
        }

        public Artifact getArtifact() {
            return artifact;
        }

        public static final class Converter implements CommandLine.ITypeConverter<MavenArtifact> {
            public Converter() {
            }

            @Override
            public MavenArtifact convert(String value) {
                return ClassPathEntry.of(new DefaultArtifact(value));
            }
        }
    }

    public static final class ClassLibraries extends ClassPathEntry {
        private final String version;

        ClassLibraries(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }
    }
}
