package cc.quarkus.qcc.graph;

/**
 *
 */
abstract class ValueProgramNodeImpl extends ProgramNodeImpl implements Value {
    String getShape() {
        return "oval";
    }
}
