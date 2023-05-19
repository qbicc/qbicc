package org.qbicc.plugin.layout;

import org.qbicc.type.StructType;
import org.qbicc.type.definition.element.FieldElement;

import java.util.BitSet;
import java.util.Collections;
import java.util.Map;

public final class LayoutInfo {
    private final BitSet allocated;
    private final StructType structType;
    private final Map<FieldElement, StructType.Member> fieldToMember;

    LayoutInfo(final BitSet allocated, final StructType structType, final Map<FieldElement, StructType.Member> fieldToMember) {
        this.allocated = allocated;
        this.structType = structType;
        this.fieldToMember = fieldToMember;
    }

    public BitSet getAllocatedBits() {
        return (BitSet) allocated.clone();
    }

    public StructType getStructType() {
        return structType;
    }

    public Map<FieldElement, StructType.Member> getFieldsMap() { return Collections.unmodifiableMap(fieldToMember); }

    public StructType.Member getMember(FieldElement element) {
        return fieldToMember.get(element);
    }
}
