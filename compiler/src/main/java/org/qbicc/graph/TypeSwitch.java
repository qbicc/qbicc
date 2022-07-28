package org.qbicc.graph;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A switch over a concrete type ID, matching exact type IDs only (this is not a subclass test).
 * Type switches differ from integer switches in that the actual type ID values are not known until late in compilation.
 * The switch targets will be sorted by type during lowering.
 */
public final class TypeSwitch extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final BlockLabel defaultTargetLabel;
    private final Map<ObjectType, BlockLabel> valueToTargetMap;
    private final List<BlockLabel> successors;
    private final Value switchValue;
    private final BasicBlock terminatedBlock;

    TypeSwitch(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, final Node dependency, final BlockLabel defaultTargetLabel, final Map<ObjectType, BlockLabel> valueToTargetMap, final Value switchValue) {
        super(callSite, element, line, bci);
        terminatedBlock = new BasicBlock(blockEntry, this);
        this.dependency = dependency;
        this.defaultTargetLabel = defaultTargetLabel;
        this.valueToTargetMap = valueToTargetMap;
        successors = List.copyOf(valueToTargetMap.values());
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

    public Map<ObjectType, BlockLabel> getValueToTargetMap() {
        return valueToTargetMap;
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
        return 1 + successors.size();
    }

    public BasicBlock getSuccessor(final int index) {
        return BlockLabel.getTargetOf(getSuccessorLabel(index));
    }

    public BlockLabel getSuccessorLabel(final int index) {
        int len = successors.size();
        return index < len ? successors.get(index) : index == len ? getDefaultTargetLabel() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(dependency, defaultTargetLabel, switchValue, valueToTargetMap);
    }

    @Override
    String getNodeName() {
        return "TypeSwitch";
    }

    public boolean equals(final Object other) {
        return other instanceof TypeSwitch && equals((TypeSwitch) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        switchValue.toString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final TypeSwitch other) {
        return this == other || other != null
            // this is expensive to fail-fast using hash code first
            && hashCode() == other.hashCode()
            && dependency.equals(other.dependency)
            && defaultTargetLabel.equals(other.defaultTargetLabel)
            && switchValue.equals(other.switchValue)
            && valueToTargetMap.equals(other.valueToTargetMap);
    }
}
