package cc.quarkus.qcc.graph2;

/**
 *
 */
final class StaticFieldReadValueImpl extends FieldOperationImpl implements FieldReadValue {

    public String getLabelForGraph() {
        return "get-static-field";
    }
}
