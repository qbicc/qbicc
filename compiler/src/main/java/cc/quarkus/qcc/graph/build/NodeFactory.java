package cc.quarkus.qcc.graph.build;

import java.util.function.BiFunction;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.AddNode;
import cc.quarkus.qcc.graph.node.BinaryIfNode;
import cc.quarkus.qcc.graph.node.CompareOp;
import cc.quarkus.qcc.graph.node.ConstantNode;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.GetFieldNode;
import cc.quarkus.qcc.graph.node.GetStaticNode;
import cc.quarkus.qcc.graph.node.NewNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PutFieldNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.ReturnNode;
import cc.quarkus.qcc.graph.node.SubNode;
import cc.quarkus.qcc.graph.node.UnaryIfNode;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.type.FieldDefinition;
import cc.quarkus.qcc.type.FieldDescriptor;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.Sentinel;
import cc.quarkus.qcc.type.TypeDescriptor;

public class NodeFactory {
    public NodeFactory(Graph<?> graph) {
        this.graph = graph;
    }

    public void setControl(ControlNode<?> control) {
        this.control = control;
    }

    public RegionNode regionNode() {
        return new RegionNode(this.graph);
    }

    public <V extends Number> AddNode<V> addNode(TypeDescriptor<V> type, Node<V> val1, Node<V> val2, BiFunction<V,V,V> sum) {
        return new AddNode<>(this.graph, this.control, type, val1, val2, sum);
    }

    public <V extends Number> SubNode<V> subNode(TypeDescriptor<V> type, Node<V> val1, Node<V> val2, BiFunction<V,V,V> sub) {
        return new SubNode<>(this.graph, this.control, type, val1, val2, sub);
    }

    public NewNode newNode(TypeDescriptor<ObjectReference> type) {
        return new NewNode(this.graph, this.control, type);
    }

    public BinaryIfNode<?> binaryIfNode(CompareOp op) {
        return new BinaryIfNode<>(this.graph, this.control, op);
    }

    public UnaryIfNode unaryIfNode(CompareOp op) {
        return new UnaryIfNode(this.graph, this.control, op);
    }

    public <V> GetFieldNode<V> getFieldNode(Node<ObjectReference> objectRef, FieldDefinition<V> field) {
        return new GetFieldNode<>(this.graph, this.control, objectRef, field);
    }

    public <V> GetStaticNode<V> getStaticNode(FieldDefinition<V> field) {
        return new GetStaticNode<>(this.graph, this.control, field);
    }

    public <V> ReturnNode<V> returnNode(Node<V> val) {
        return new ReturnNode<>(this.graph, this.control, val);
    }

    public <V> PutFieldNode<V> putFieldNode(Node<ObjectReference> objectRef, Node<V> value, FieldDescriptor<V> field, Node<MemoryToken> memory) {
        return new PutFieldNode<>(this.graph, this.control, objectRef, value, field, memory);
    }

    private final Graph<?> graph;
    private ControlNode<?> control;
}
