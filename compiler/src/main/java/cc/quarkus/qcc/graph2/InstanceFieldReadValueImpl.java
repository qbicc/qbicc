package cc.quarkus.qcc.graph2;

/**
 *
 */
class InstanceFieldReadValueImpl extends InstanceFieldReadOperationImpl implements InstanceFieldReadValue {

    public String getLabelForGraph() {
        return "get-instance-field";
    }
}
