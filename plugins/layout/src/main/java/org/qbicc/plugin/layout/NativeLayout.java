package org.qbicc.plugin.layout;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NativeLayout {
    private static final AttachmentKey<NativeLayout> KEY = new AttachmentKey<>();
    private final CompilationContext ctxt;

    private final Map<LoadedTypeDefinition, LayoutInfo> layouts = new ConcurrentHashMap<>();

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

    public LayoutInfo getLayoutInfo(DefinedTypeDefinition type) {
        LoadedTypeDefinition validated = type.load();
        LayoutInfo layoutInfo = layouts.get(validated);
        if (layoutInfo != null) {
            return layoutInfo;
        }
        int minAlignment = ctxt.getTypeSystem().getPointerAlignment(); // All fields have at least pointer alignment.
        BitSet allocated = new BitSet();
        int cnt = validated.getFieldCount();
        Map<FieldElement, CompoundType.Member> fieldToMember = new HashMap<>(cnt);
        int previousFieldOffset = 0;
        for (int i = 0; i < cnt; i ++) {
            FieldElement field = validated.getField(i);
            Assert.assertFalse(field.isStatic());
            CompoundType.Member member = computeMember(allocated, field, previousFieldOffset);
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
        layoutInfo = new LayoutInfo(allocated, compoundType, fieldToMember);
        LayoutInfo appearing = layouts.putIfAbsent(validated, layoutInfo);
        return appearing != null ? appearing : layoutInfo;
    }

    private CompoundType.Member computeMember(final BitSet allocated, final FieldElement field, int prevFieldOffset) {
        TypeSystem ts = ctxt.getTypeSystem();
        ValueType fieldType = field.getType();
        int size = (int) fieldType.getSize();
        int align = fieldType.getAlign();
        int idx;
        if (size != 0) {
            idx = find(allocated, align, size, prevFieldOffset);
            allocated.set(idx, idx + size);
        } else {
            idx = find(allocated, align, ts.getMaxAlignment(), prevFieldOffset);
        }
        return ts.getCompoundTypeMember(field.getName(), fieldType, idx, align);
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
