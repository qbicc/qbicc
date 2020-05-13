package cc.quarkus.qcc.graph.node;

import java.util.ListIterator;
import java.util.Set;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.build.Frame;
import cc.quarkus.qcc.type.TypeDescriptor;

public abstract class AbstractControlNode<V> extends AbstractNode<V> implements ControlNode<V> {

    protected AbstractControlNode(Graph<?> graph, ControlNode<?> control, TypeDescriptor<V> outType) {
        super(graph, control, outType);
    }

    public AbstractControlNode(Graph<?> graph, TypeDescriptor<V> outType) {
        super(graph, outType);
    }

    @Override
    public void removeUnreachable(Set<ControlNode<?>> reachable) {
        ListIterator<Node<?>> iter = this.successors.listIterator();
        while (iter.hasNext()) {
            Node<?> each = iter.next();
            if (each instanceof PhiNode) {
                if (each.getSuccessors().isEmpty()) {
                    iter.remove();
                }
            }
        }
    }

   // @Override
    //public void mergeInputs() {
     //   frame().mergeInputs();
      //  for (ControlNode<?> each : getControlSuccessors()) {
       //     if (each instanceof Projection) {
        //        each.mergeInputs();
         //   }
        //}
    //}


}
