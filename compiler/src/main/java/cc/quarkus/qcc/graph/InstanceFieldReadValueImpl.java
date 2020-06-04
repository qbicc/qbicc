package cc.quarkus.qcc.graph;

/**
 *
 */
class InstanceFieldReadValueImpl extends InstanceFieldReadOperationImpl implements InstanceFieldReadValue {

    public String getLabelForGraph() {
        return "get-instance-field";
    }
}
