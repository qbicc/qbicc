package cc.quarkus.qcc.graph2;

final class ReturnInstructionImpl extends InstructionImpl implements ReturnInstruction {
    public String getLabelForGraph() {
        return "return";
    }
}
