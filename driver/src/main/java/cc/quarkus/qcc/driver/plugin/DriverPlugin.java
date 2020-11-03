package cc.quarkus.qcc.driver.plugin;

import cc.quarkus.qcc.driver.Driver;

/**
 * A plugin to customize the driver build.
 */
public interface DriverPlugin {
    void accept(Driver.Builder driverBuilder);
}
