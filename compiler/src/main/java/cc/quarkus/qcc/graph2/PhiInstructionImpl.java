package cc.quarkus.qcc.graph2;

final class PhiInstructionImpl extends InstructionImpl implements PhiInstruction {
    private final PhiValue phiValue;

    PhiInstructionImpl(final PhiValue phiValue) {
        this.phiValue = phiValue;
    }

    public PhiValue getValue() {
        return phiValue;
    }

    public String getLabelForGraph() {
        return "inst:phi";
    }
}
