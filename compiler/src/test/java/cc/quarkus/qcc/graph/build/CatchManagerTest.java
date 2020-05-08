package cc.quarkus.qcc.graph.build;

import java.util.List;

import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.MockMethodDefinition;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.type.Universe;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class CatchManagerTest {

    @Test
    public void test() {
        Universe universe = new Universe( new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));

        CatchManager manager = new CatchManager(new Graph<>(new MockMethodDefinition<>()));
        RegionNode fnfe1 = manager.addCatch(10, 20, universe.findClass("java/io/FileNotFoundException"), 40);
        RegionNode ioe1 = manager.addCatch(10, 20, universe.findClass("java/io/IOException"), 70);
        RegionNode fin1 = manager.addCatch(10, 20, null, 90);

        assertThat(fnfe1).isNotSameAs(ioe1);
        assertThat(fnfe1).isNotSameAs(fin1);
        assertThat(ioe1).isNotSameAs(fin1);

        RegionNode t2 = manager.addCatch(0, 30, universe.findClass("java/lang/Throwable"), 170);
        RegionNode fin2 = manager.addCatch(0, 30, null, 190);

        assertThat(fnfe1).isNotSameAs(t2);
        assertThat(ioe1).isNotSameAs(t2);
        assertThat(fin1).isNotSameAs(fin2);

        List<TryRange> ranges = manager.getCatchesFor(15);

        assertThat(ranges).hasSize(2);
        assertThat(ranges.get(0).getCatches()).hasSize(3);

        CatchMatcher m1 = ranges.get(0).getCatches().get(0).getMatcher();
        assertThat(m1.getIncludeTypes()).contains(universe.findClass("java/io/FileNotFoundException"));
        assertThat(m1.getExcludeTypes()).isEmpty();

        CatchMatcher m2 = ranges.get(0).getCatches().get(1).getMatcher();
        assertThat(m2.getIncludeTypes()).contains(universe.findClass("java/io/IOException"));
        assertThat(m2.getExcludeTypes()).contains(universe.findClass( "java/io/FileNotFoundException"));

        assertThat( ranges.get(0).getCatches().get(2).getMatcher().isMatchAll()).isTrue();

    }
}
