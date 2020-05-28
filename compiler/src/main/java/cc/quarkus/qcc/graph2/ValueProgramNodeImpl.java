package cc.quarkus.qcc.graph2;

/**
 *
 */
abstract class ValueProgramNodeImpl extends ProgramNodeImpl implements Value {
    String getShape() {
        return "oval";
    }
}
