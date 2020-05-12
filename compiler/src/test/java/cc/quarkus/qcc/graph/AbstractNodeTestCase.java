package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.AbstractTestCase;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.interpret.SimpleHeap;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.TypeDescriptor;
import cc.quarkus.qcc.type.Universe;
import org.junit.Before;

public class AbstractNodeTestCase extends AbstractTestCase {

    @Before
    public void setUpNode() {
        this.method = new MockMethodDefinition<>();
        this.graph = new Graph<>(this.method);
        this.heap = new SimpleHeap();
        this.context = new MockContext(this.heap);
    }

    protected SimpleHeap heap() {
        return this.heap;
    }

    protected MockContext context() {
        return this.context;
    }

    protected RegionNode control() {
        return new RegionNode(graph(), 0, 0);
    }

    protected Graph<?> graph() {
        return this.graph;
    }

    protected MockNode<ObjectReference> set(ObjectReference val) {
        MockNode<ObjectReference> node = new MockNode<>(graph(), control(), TypeDescriptor.OBJECT, val);
        context().set(node, val);
        return node;
    }

    protected MockNode<Integer> set(Integer val) {
        MockNode<Integer> node = new MockNode<>(graph(), control(), TypeDescriptor.INT, val);
        context().set(node, val);
        return node;
    }

    protected MockNode<Long> set(Long val) {
        MockNode<Long> node = new MockNode<>(graph(), control(), TypeDescriptor.LONG, val);
        context().set(node, val);
        return node;
    }

    protected MockNode<Byte> set(Byte val) {
        MockNode<Byte> node = new MockNode<>(graph(), control(), TypeDescriptor.BYTE, val);
        context().set(node, val);
        return node;
    }

    protected Universe universe;

    private SimpleHeap heap;

    private MockContext context;

    private Graph<?> graph;

    private MockMethodDefinition<?> method;
}
