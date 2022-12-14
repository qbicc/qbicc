package org.qbicc.main;

import javax.inject.Named;

import org.apache.maven.wagon.Wagon;
import org.eclipse.aether.transport.wagon.WagonConfigurator;

/**
 * An empty configurator for Wagon.
 */
@Named("qbicc")
public final class QbiccWagonConfigurator implements WagonConfigurator {
    @Override
    public void configure(Wagon wagon, Object configuration) throws Exception {
        // no operation
    }
}
