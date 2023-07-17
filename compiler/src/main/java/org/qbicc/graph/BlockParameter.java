package org.qbicc.graph;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.ValueType;

/**
 * A parameter to a basic block.
 */
public final class BlockParameter extends AbstractValue implements PinnedNode {
    private final ValueType type;
    private final boolean nullable;
    private final BlockLabel blockLabel;
    private final Slot slot;

    BlockParameter(final ProgramLocatable pl, ValueType type, boolean nullable, BlockLabel blockLabel, Slot slot) {
        super(pl);
        this.type = type;
        this.nullable = nullable;
        this.blockLabel = blockLabel;
        this.slot = slot;
    }

    @Override
    int calcHashCode() {
        return System.identityHashCode(this);
    }

    @Override
    String getNodeName() {
        return "BlockParameter";
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @Override
    public ValueType getType() {
        return type;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @Override
    public BlockLabel getPinnedBlockLabel() {
        return blockLabel;
    }

    public Slot getSlot() {
        return slot;
    }

    /**
     * Get all of the possible non-parameter argument values for this parameter.
     *
     * @return the set of possible values (not {@code null})
     */
    public Set<Value> getPossibleValues() {
        LinkedHashSet<Value> possibleValues = new LinkedHashSet<>();
        getPossibleValues(possibleValues, new HashSet<>(), false);
        return possibleValues;
    }

    public Set<Value> getPossibleValuesIncludingParameters() {
        LinkedHashSet<Value> possibleValues = new LinkedHashSet<>();
        getPossibleValues(possibleValues, new HashSet<>(), true);
        return possibleValues;
    }

    public boolean isEntryParameter() {
        return getPinnedBlock() == element().getMethodBody().getEntryBlock();
    }

    public int getIndex() {
        return slot.getIndex();
    }

    private void getPossibleValues(Set<Value> current, Set<BlockParameter> visited, boolean includeParams) {
        if (visited.add(this)) {
            BasicBlock pinnedBlock = getPinnedBlock();
            Set<BasicBlock> incoming = pinnedBlock.getIncoming();
            if (incoming.isEmpty()) {
                // initial block; the parameter is an input to the function
                current.add(this);
            } else for (BasicBlock basicBlock : incoming) {
                if (basicBlock.isReachable()) {
                    Terminator t = basicBlock.getTerminator();
                    if (t.isImplicitOutboundArgument(slot, pinnedBlock)) {
                        // we don't know the possible range of thrown values
                        current.add(this);
                    } else {
                        Value value = t.getOutboundArgument(slot);
                        if (value instanceof BlockParameter bp) {
                            if (includeParams) {
                                current.add(value);
                            }
                            bp.getPossibleValues(current, visited, includeParams);
                        } else {
                            current.add(value);
                        }
                    }
                }
            }
        }
    }

    public boolean possibleValuesAreNullable() {
        if (! nullable) {
            // avoid creating set
            return false;
        }
        return possibleValuesAreNullable(new HashSet<>());
    }

    private boolean possibleValuesAreNullable(final HashSet<BlockParameter> visited) {
        if (visited.add(this) && nullable) {
            BasicBlock pinnedBlock = getPinnedBlock();
            Set<BasicBlock> incoming = pinnedBlock.getIncoming();
            if (incoming.isEmpty()) {
                // initial block; the test above proves this value is nullable
                return true;
            } else for (BasicBlock basicBlock : incoming) {
                if (basicBlock.isReachable()) {
                    Terminator t = basicBlock.getTerminator();
                    if (t.isImplicitOutboundArgument(slot, pinnedBlock)) {
                        // unknown method return value
                        return true;
                    }
                    Value value = t.getOutboundArgument(slot);
                    if (value instanceof BlockParameter bp) {
                        if (bp.possibleValuesAreNullable(visited)) {
                            return true;
                        }
                        // else continue
                    } else if (value.isNullable()) {
                        return true;
                    }
                    // else continue
                }
            }
        }
        return false;
    }

    public StringBuilder appendQualifiedName(StringBuilder b) {
        BasicBlock pinnedBlock = getPinnedBlock();
        if (pinnedBlock == null) {
            // not yet assigned
            b.append("??").append('.');
        } else if (pinnedBlock.getIndex() != 1) {
            // block one doesn't need a qualifier, really
            pinnedBlock.toString(b).append('.');
        }
        return b.append(slot);
    }

    @Override
    public StringBuilder toReferenceString(StringBuilder b) {
        return toLValueString(b);
    }

    @Override
    StringBuilder toLValueString(StringBuilder b) {
        return appendQualifiedName(b.append('%'));
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        if (nullable) {
            b.append("nullable ");
        }
        b.append("parameter ");
        type.toString(b);
        return b;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
