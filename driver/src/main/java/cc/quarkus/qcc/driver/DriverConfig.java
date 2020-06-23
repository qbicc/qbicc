package cc.quarkus.qcc.driver;

/**
 * The driver configuration.
 */
public interface DriverConfig {
    String nativeImageGenerator();

    // for now only Java
    String frontEnd();
}
