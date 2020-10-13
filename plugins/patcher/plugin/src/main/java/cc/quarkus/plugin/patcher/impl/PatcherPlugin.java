package cc.quarkus.plugin.patcher.impl;

import java.util.List;

import cc.quarkus.qcc.driver.Plugin;
import cc.quarkus.qcc.graph.BasicBlockBuilder;

/**
 * The QCC patcher plugin.
 */
public class PatcherPlugin implements Plugin {
    public String getName() {
        return "qcc.patcher";
    }

    public List<BasicBlockBuilder.Factory> getBasicBlockBuilderFactoryPlugins() {
        return List.of(GRAPH_PLUGIN);
    }

    // todo: register a jandex hook of some sort...

    private static final BasicBlockBuilder.Factory GRAPH_PLUGIN = PatcherGraphFactory::new;
}
