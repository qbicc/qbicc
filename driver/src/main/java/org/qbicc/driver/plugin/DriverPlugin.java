package org.qbicc.driver.plugin;

import org.qbicc.driver.Driver;

/**
 * A plugin to customize the driver build.
 */
public interface DriverPlugin {
    void accept(Driver.Builder driverBuilder);
}
