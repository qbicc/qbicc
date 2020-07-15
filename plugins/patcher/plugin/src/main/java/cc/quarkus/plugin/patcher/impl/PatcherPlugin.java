package cc.quarkus.plugin.patcher.impl;

import java.util.List;

import cc.quarkus.qcc.driver.GraphFactoryPlugin;
import cc.quarkus.qcc.driver.Plugin;
import cc.quarkus.qcc.graph.GraphFactory;

/**
 * The QCC patcher plugin.
 */
public class PatcherPlugin implements Plugin {
    public String getName() {
        return "qcc.patcher";
    }

    public List<GraphFactoryPlugin> getGraphFactoryPlugins() {
        return List.of(GRAPH_PLUGIN);
    }

    // todo: register a jandex hook of some sort...

    private static final GraphFactoryPlugin GRAPH_PLUGIN = new GraphFactoryPlugin() {
        public int getPriority() {
            return 1000;
        }

        public GraphFactory construct(final GraphFactory delegate) {
            return new PatcherGraphFactory(delegate);
        }
    };
}
