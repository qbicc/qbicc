package cc.quarkus.qcc.compiler.frontend.java;

import java.nio.file.Path;
import java.util.List;

/**
 *
 */
public interface JavaFrontEndConfig {
    // todo: List<Path> jdkClassPath();

    List<Path> applicationClassPath();

    // todo: Path jarPath();
}
