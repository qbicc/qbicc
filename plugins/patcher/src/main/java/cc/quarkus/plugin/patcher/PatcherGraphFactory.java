package cc.quarkus.plugin.patcher;

import cc.quarkus.qcc.graph.DelegatingGraphFactory;
import cc.quarkus.qcc.graph.GraphFactory;

final class PatcherGraphFactory extends DelegatingGraphFactory implements GraphFactory {
    PatcherGraphFactory(final GraphFactory delegate) {
        super(delegate);
    }


}
