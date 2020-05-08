package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.AbstractTestCase;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.interpret.SimpleHeap;
import cc.quarkus.qcc.type.TypeDescriptor;
import cc.quarkus.qcc.type.Universe;
import org.junit.Before;

public class AbstractNodeTestCase extends AbstractTestCase {

    @Before
    public void setUpHeap() {
        this.heap = new SimpleHeap();
    }

    @Before
    public void setUpContext() {
        this.context = new MockContext(this.heap);
    }

    protected SimpleHeap heap() {
        return this.heap;
    }

    protected MockContext context() {
        return this.context;
    }

    protected RegionNode control() {
        return new RegionNode(0, 0);
    }

    protected MockNode<ObjectReference> set(ObjectReference val) {
        MockNode<ObjectReference> node = new MockNode<>(control(), TypeDescriptor.OBJECT, val);
        context().set(node, val);
        return node;
    }

    protected Universe universe;

    private SimpleHeap heap;

    private MockContext context;
}
