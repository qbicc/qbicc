package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

final class SwitchImpl extends TerminatorImpl implements Switch {
    private static final int[] NO_VALUES = new int[0];
    private static final NodeHandle[] NO_TARGETS = new NodeHandle[0];

    NodeHandle defaultTarget;
    int numValues = 0;
    int[] values = new int[5];
    NodeHandle[] targets = new NodeHandle[5];

    public BasicBlock getDefaultTarget() {
        return NodeHandle.getTargetOf(defaultTarget);
    }

    public void setDefaultTarget(final BasicBlock target) {
        setDefaultTarget(NodeHandle.of(target));
    }

    void setDefaultTarget(final NodeHandle target) {
        defaultTarget = target;
    }

    public BasicBlock getTargetForValue(final int value) {
        int idx = Arrays.binarySearch(values, 0, numValues, value);
        return idx < 0 ? null : NodeHandle.getTargetOf(targets[idx]);
    }

    public void setTargetForValue(final int value, final BasicBlock target) {
        setTargetForValue(value, NodeHandle.of(target));
    }

    void setTargetForValue(final int value, final NodeHandle target) {
        int idx = Arrays.binarySearch(values, 0, numValues, value);
        if (idx < 0) {
            // insert at (-(insertion point) - 1)
            idx = - (idx + 1);
            // ensure the array is big enough
            if (values.length == numValues) {
                values = Arrays.copyOf(values, values.length << 1);
                targets = Arrays.copyOf(targets, targets.length << 1);
            }
            if (idx < numValues) {
                // make a hole
                int cnt = numValues - idx;
                System.arraycopy(values, idx, values, idx + 1, cnt);
                System.arraycopy(targets, idx, targets, idx + 1, cnt);
            }
            values[idx] = value;
        }
        targets[idx] = target;
    }

    public int getNumberOfValues() {
        return numValues;
    }

    public int getValue(final int index) throws IndexOutOfBoundsException {
        if (index < 0 || index > numValues) {
            throw new IndexOutOfBoundsException(index);
        }
        return values[index];
    }

    public float getDensity() {
        if (numValues == 0) {
            return 0.0f;
        }
        float numVals = numValues;
        float valueRange = values[numValues - 1] - values[0] + 1;
        return numVals / valueRange;
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {
        super.writeToGraph(visited, graph, knownBlocks);
        for (int i = 0; i < numValues; i ++) {
            addEdgeTo(visited, graph, NodeHandle.getTargetOf(targets[i]), "case " + values[i], "green", "solid", knownBlocks);
        }
        addEdgeTo(visited, graph, NodeHandle.getTargetOf(defaultTarget), "default", "green", "solid", knownBlocks);
    }

    public String getLabelForGraph() {
        return "switch";
    }
}
