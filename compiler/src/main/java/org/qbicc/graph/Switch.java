package org.qbicc.graph;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
public final class Switch extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final BlockLabel defaultTargetLabel;
    private final int[] values;
    private final BlockLabel[] targetLabels;
    private final Value switchValue;
    private final BasicBlock terminatedBlock;

    Switch(final ProgramLocatable pl, final BlockEntry blockEntry, final Node dependency, final BlockLabel defaultTargetLabel, final int[] values, final BlockLabel[] targetLabels, final Value switchValue, Map<Slot, Value> targetArguments) {
        super(pl, targetArguments);
        terminatedBlock = new BasicBlock(blockEntry, this);
        this.dependency = dependency;
        this.defaultTargetLabel = defaultTargetLabel;
        // check values to make sure they're in order
        int max = Integer.MIN_VALUE;
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            final int value = values[i];
            if (value > max) {
                max = value;
            } else if (value <= max && i != 0) {
                throw new IllegalArgumentException("Switch values must strictly increase");
            }
        }
        this.values = values;
        this.targetLabels = targetLabels;
        this.switchValue = switchValue;
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    public Value getSwitchValue() {
        return switchValue;
    }

    public BlockLabel getDefaultTargetLabel() {
        return defaultTargetLabel;
    }

    public BasicBlock getDefaultTarget() {
        return BlockLabel.getTargetOf(defaultTargetLabel);
    }

    public int getNumberOfValues() {
        return values.length;
    }

    public int getValueForIndex(int index) throws IndexOutOfBoundsException {
        return values[index];
    }

    public int getIndexForValue(int value) {
        int idx = Arrays.binarySearch(values, value);
        return idx < 0 ? -1 : idx;
    }

    public int[] getValues() {
        return values;
    }

    public BlockLabel getTargetLabelForIndex(int index) {
        return targetLabels[index];
    }

    public BlockLabel getTargetLabelForValue(int value) {
        int idx = Arrays.binarySearch(values, value);
        return idx < 0 ? null : targetLabels[idx];
    }

    public BasicBlock getTargetForIndex(int index) {
        return BlockLabel.getTargetOf(targetLabels[index]);
    }

    public BasicBlock getTargetForValue(int value) {
        return BlockLabel.getTargetOf(getTargetLabelForValue(value));
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? switchValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public int getSuccessorCount() {
        return 1 + targetLabels.length;
    }

    public BasicBlock getSuccessor(final int index) {
        int len = targetLabels.length;
        return index < len ? getTargetForIndex(index) : index == len ? getDefaultTarget() : Util.throwIndexOutOfBounds(index);
    }

    public float getDensity() {
        int numVals = values.length;
        if (numVals == 0) {
            return 0.0f;
        }
        float valueRange = values[numVals - 1] - values[0] + 1;
        return (float) numVals / valueRange;
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return (Objects.hash(dependency, defaultTargetLabel, switchValue) * 19 + Arrays.hashCode(values)) * 19 + Arrays.hashCode(targetLabels);
    }

    @Override
    String getNodeName() {
        return "Switch";
    }

    public boolean equals(final Object other) {
        return other instanceof Switch && equals((Switch) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        switchValue.toReferenceString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final Switch other) {
        return this == other || other != null
            // this is expensive to fail-fast using hash code first
            && hashCode() == other.hashCode()
            && dependency.equals(other.dependency)
            && defaultTargetLabel.equals(other.defaultTargetLabel)
            && switchValue.equals(other.switchValue)
            && Arrays.equals(values, other.values)
            && Arrays.equals(targetLabels, other.targetLabels);
    }
}
