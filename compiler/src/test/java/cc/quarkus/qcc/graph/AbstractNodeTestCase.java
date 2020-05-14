package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.AbstractTestCase;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.interpret.SimpleInterpreterHeap;
import cc.quarkus.qcc.interpret.InterpreterThread;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QInt64;
import cc.quarkus.qcc.type.QInt8;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.universe.Universe;
import org.junit.Before;

public class AbstractNodeTestCase extends AbstractTestCase {

    @Before
    public void setUpNode() {
        this.method = new MockMethodDefinition<>();
        this.graph = new Graph<>(this.method);
        this.heap = new SimpleInterpreterHeap();
        this.thread = new InterpreterThread(this.heap);
        this.context = new MockContext(this.thread);
    }

    protected SimpleInterpreterHeap heap() {
        return this.heap;
    }

    protected MockContext context() {
        return this.context;
    }

    protected RegionNode control() {
        return new RegionNode(graph());
    }

    protected Graph<?> graph() {
        return this.graph;
    }

    protected MockNode<ObjectReference> set(ObjectReference val) {
        MockNode<ObjectReference> node = new MockNode<>(graph(), control(), TypeDescriptor.OBJECT, val);
        context().set(node, val);
        return node;
    }

    protected MockNode<QInt32> set(int val) {
        QInt32 qval = QInt32.of(val);
        MockNode<QInt32> node = new MockNode<>(graph(), control(), TypeDescriptor.INT32, qval);
        context().set(node, qval);
        return node;
    }

    protected MockNode<QInt64> set(long val) {
        QInt64 qval = QInt64.of(val);
        MockNode<QInt64> node = new MockNode<>(graph(), control(), TypeDescriptor.INT64, qval);
        context().set(node, qval);
        return node;
    }

    protected MockNode<QInt8> set(byte val) {
        QInt8 qval = QInt8.of(val);
        MockNode<QInt8> node = new MockNode<>(graph(), control(), TypeDescriptor.INT8, qval);
        context().set(node, qval);
        return node;
    }

    protected Universe universe;

    private InterpreterThread thread;

    private SimpleInterpreterHeap heap;

    private MockContext context;

    private Graph<?> graph;

    private MockMethodDefinition<?> method;

}
