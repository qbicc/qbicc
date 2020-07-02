package cc.quarkus.qcc.driver;

import cc.quarkus.qcc.graph.GraphFactory;

/**
 * A plugin for graph factory manipulation.
 */
public interface GraphFactoryPlugin {
    /**
     * Get the priority of this plugin.  Lower numbers come earlier in the chain, and higher numbers come later.
     *
     * @return the priority of this plugin
     */
    int getPriority();

    /**
     * Construct this graph factory plugin.
     *
     * @param delegate the graph factory to delegate to (not {@code null})
     * @return the intercepting graph factory (must not be {@code null})
     */
    GraphFactory construct(GraphFactory delegate);
}
