package cc.quarkus.qcc.driver;

import java.nio.file.Path;
import java.util.List;

/**
 * The driver configuration.
 */
public interface DriverConfig {
    String nativeImageGenerator(); // todo: replace with Platform detection

    List<Path> bootstrapModules();
}
