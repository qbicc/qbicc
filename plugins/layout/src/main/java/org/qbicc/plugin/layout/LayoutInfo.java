package org.qbicc.plugin.layout;

import org.qbicc.type.CompoundType;
import org.qbicc.type.definition.element.FieldElement;

import java.util.BitSet;
import java.util.Collections;
import java.util.Map;

public final class LayoutInfo {
    private final BitSet allocated;
    private final CompoundType compoundType;
    private final Map<FieldElement, CompoundType.Member> fieldToMember;

    LayoutInfo(final BitSet allocated, final CompoundType compoundType, final Map<FieldElement, CompoundType.Member> fieldToMember) {
        this.allocated = allocated;
        this.compoundType = compoundType;
        this.fieldToMember = fieldToMember;
    }

    public BitSet getAllocatedBits() {
        return (BitSet) allocated.clone();
    }

    public CompoundType getCompoundType() {
        return compoundType;
    }

    public Map<FieldElement, CompoundType.Member> getFieldsMap() { return Collections.unmodifiableMap(fieldToMember); }

    public CompoundType.Member getMember(FieldElement element) {
        return fieldToMember.get(element);
    }
}
