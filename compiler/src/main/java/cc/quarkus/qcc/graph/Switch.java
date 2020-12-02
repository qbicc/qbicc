package cc.quarkus.qcc.graph;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
public final class Switch extends AbstractNode implements Terminator {
    private static final int[] NO_VALUES = new int[0];
    private static final BlockLabel[] NO_TARGETS = new BlockLabel[0];

    private final Node dependency;
    private final BlockLabel defaultTargetLabel;
    private final int[] values;
    private final BlockLabel[] targetLabels;
    private final Value switchValue;

    Switch(final int line, final int bci, final Node dependency, final BlockLabel defaultTargetLabel, final int[] values, final BlockLabel[] targetLabels, final Value switchValue) {
        super(line, bci);
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

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
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

    public boolean equals(final Object other) {
        return other instanceof Switch && equals((Switch) other);
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
