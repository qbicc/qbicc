package cc.quarkus.qcc.driver;

/**
 * The driver configuration.
 */
public interface DriverConfig {
    String backEnd();

    // for now only Java
    String frontEnd();
}
