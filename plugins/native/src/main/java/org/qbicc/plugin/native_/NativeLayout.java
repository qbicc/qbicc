package org.qbicc.plugin.native_;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.core.ConditionEvaluation;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.ArrayAnnotationValue;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

public final class NativeLayout {
    private static final AttachmentKey<NativeLayout> KEY = new AttachmentKey<>();
    private final CompilationContext ctxt;

    private final Map<LoadedTypeDefinition, CompoundType> layouts = new ConcurrentHashMap<>();

    private NativeLayout(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static NativeLayout get(CompilationContext ctxt) {
        NativeLayout layout = ctxt.getAttachment(KEY);
        if (layout == null) {
            layout = new NativeLayout(ctxt);
            NativeLayout appearing = ctxt.putAttachmentIfAbsent(KEY, layout);
            if (appearing != null) {
                layout = appearing;
            }
        }
        return layout;
    }

    public CompoundType getLayoutInfo(DefinedTypeDefinition type) {
        LoadedTypeDefinition validated = type.load();
        CompoundType layoutInfo = layouts.get(validated);
        if (layoutInfo != null) {
            return layoutInfo;
        }
        int minAlignment = ctxt.getTypeSystem().getPointerAlignment(); // All fields have at least pointer alignment.
        BitSet allocated = new BitSet();
        int cnt = validated.getFieldCount();
        Map<FieldElement, CompoundType.Member> fieldToMember = new HashMap<>(cnt);
        int previousFieldOffset = 0;
        ConditionEvaluation conditionEvaluation = ConditionEvaluation.get(ctxt);
        eachField: for (int i = 0; i < cnt; i ++) {
            FieldElement field = validated.getField(i);
            if (field.isStatic()) {
                // ignore static field
                continue;
            }
            TypeSystem ts = ctxt.getTypeSystem();
            ValueType fieldType = field.getType();
            int size = (int) fieldType.getSize();
            int align = fieldType.getAlign();
            String fieldName = field.getName();
            boolean nameOverridden = false;
            for (Annotation annotation : field.getInvisibleAnnotations()) {
                ClassTypeDescriptor annDesc = annotation.getDescriptor();
                if (annDesc.getPackageName().equals(Native.NATIVE_PKG)) {
                    if (annDesc.getClassName().equals(Native.ANN_NAME) && ! nameOverridden) {
                        if (conditionEvaluation.evaluateConditions(validated.getContext(), type, annotation)) {
                            fieldName = ((StringAnnotationValue) annotation.getValue("value")).getString();
                            nameOverridden = true;
                        }
                    } else if (annDesc.getClassName().equals(Native.ANN_NAME_LIST) && ! nameOverridden) {
                        if (annotation.getValue("value") instanceof ArrayAnnotationValue aav) {
                            int annCnt = aav.getElementCount();
                            for (int j = 0; j < annCnt; j ++) {
                                if (aav.getValue(j) instanceof Annotation nested) {
                                    ClassTypeDescriptor nestedDesc = nested.getDescriptor();
                                    if (nestedDesc.getPackageName().equals(Native.NATIVE_PKG)) {
                                        if (nestedDesc.getClassName().equals(Native.ANN_NAME)) {
                                            if (conditionEvaluation.evaluateConditions(validated.getContext(), type, annotation)) {
                                                fieldName = ((StringAnnotationValue) annotation.getValue("value")).getString();
                                                nameOverridden = true;
                                                // stop searching for names
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (annDesc.getClassName().equals(Native.ANN_INCOMPLETE)) {
                        if (conditionEvaluation.evaluateConditions(type.getContext(), field, annotation)) {
                            continue eachField;
                        }
                    }
                }
            }
            int idx;
            if (size != 0) {
                idx = find(allocated, align, size, previousFieldOffset);
                allocated.set(idx, idx + size);
            } else {
                idx = find(allocated, align, ts.getMaxAlignment(), previousFieldOffset);
            }
            CompoundType.Member member = ts.getCompoundTypeMember(fieldName, fieldType, idx, align);
            if (member.getAlign() > minAlignment) {
                minAlignment = member.getAlign();
            }
            fieldToMember.put(field, member);
            previousFieldOffset = member.getOffset();
        }
        int size = allocated.length();
        CompoundType.Member[] membersArray = fieldToMember.values().toArray(CompoundType.Member[]::new);
        Arrays.sort(membersArray);
        List<CompoundType.Member> membersList = List.of(membersArray);
        CompoundType compoundType = ctxt.getTypeSystem().getCompoundType(CompoundType.Tag.NONE, type.getInternalName().replace('/', '.'), size, minAlignment, () -> membersList);
        CompoundType appearing = layouts.putIfAbsent(validated, compoundType);
        return appearing != null ? appearing : compoundType;
    }

    /**
     * Find a sequence of consecutive zero bits with the given alignment and count.
     *
     * @param bitSet the bit set to search
     * @param alignment the alignment
     * @param size the size
     * @param searchIdx the index in the bit set at which search begins
     * @return the bit index
     */
    private int find(BitSet bitSet, int alignment, int size, int searchIdx) {
        assert Integer.bitCount(alignment) == 1;
        int mask = alignment - 1;
        int i = bitSet.nextClearBit(searchIdx);
        // adjust for alignment
        int amt = mask - (i - 1 & mask);
        while (amt > 0) {
            i = bitSet.nextClearBit(i + amt);
            amt = mask - (i - 1 & mask);
        }
        return i;
    }

}
