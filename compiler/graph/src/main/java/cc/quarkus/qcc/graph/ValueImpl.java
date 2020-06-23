package cc.quarkus.qcc.graph;

/**
 *
 */
abstract class ValueImpl extends NodeImpl implements Value {
    String getShape() {
        return "oval";
    }
}
