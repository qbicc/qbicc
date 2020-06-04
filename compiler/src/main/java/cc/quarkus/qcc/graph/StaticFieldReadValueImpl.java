package cc.quarkus.qcc.graph;

/**
 *
 */
final class StaticFieldReadValueImpl extends FieldOperationImpl implements FieldReadValue {

    public String getLabelForGraph() {
        return "get-static-field";
    }
}
