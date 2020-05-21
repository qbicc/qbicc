package cc.quarkus.qcc.graph2;

/**
 *
 */
abstract class ValueImpl extends NodeImpl implements Value {
    String getShape() {
        return "oval";
    }
}
