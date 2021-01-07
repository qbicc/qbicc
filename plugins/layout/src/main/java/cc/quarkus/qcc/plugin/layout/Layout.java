package cc.quarkus.qcc.plugin.layout;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 *
 */
public final class Layout {
    private static final AttachmentKey<Layout> KEY = new AttachmentKey<>();

    private final Map<ValidatedTypeDefinition, LayoutInfo> instanceLayouts = new ConcurrentHashMap<>();
    private final CompilationContext ctxt;

    private Layout(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static Layout get(CompilationContext ctxt) {
        Layout layout = ctxt.getAttachment(KEY);
        if (layout == null) {
            layout = new Layout(ctxt);
            Layout appearing = ctxt.putAttachmentIfAbsent(KEY, layout);
            if (appearing != null) {
                layout = appearing;
            }
        }
        return layout;
    }

    public LayoutInfo getInstanceLayoutInfo(DefinedTypeDefinition type) {
        if (type.isInterface()) {
            throw new IllegalArgumentException("Interfaces have no instance layout");
        }
        ValidatedTypeDefinition validated = type.validate();
        LayoutInfo layoutInfo = instanceLayouts.get(validated);
        if (layoutInfo != null) {
            return layoutInfo;
        }
        ValidatedTypeDefinition superClass = validated.getSuperClass();
        LayoutInfo superLayout;
        if (superClass != null) {
            superLayout = getInstanceLayoutInfo(superClass);
        } else {
            superLayout = null;
        }
        BitSet allocated = new BitSet();
        if (superLayout != null) {
            allocated.or(superLayout.allocated);
        }
        int cnt = validated.getFieldCount();
        int ic = 0;
        CompoundType.Member[] allMembers = new CompoundType.Member[cnt];
        CompoundType.Member[] instanceMembers = new CompoundType.Member[cnt];
        ClassContext classContext = type.getContext();

        for (int i = 0; i < cnt; i ++) {
            // todo: skip unused fields?
            FieldElement field = validated.getField(i);
            if (field.isStatic()) {
                continue;
            }
            allMembers[i] = instanceMembers[ic++] = computeMember(allocated, field);
        }
        int size = allocated.length();
        List<CompoundType.Member> fieldIndexToMember = List.of(allMembers);
        CompoundType compoundType = ctxt.getTypeSystem().getCompoundType(CompoundType.Tag.NONE, type.getInternalName().replace('/', '.'), size, 1, Arrays.copyOf(instanceMembers, ic));
        layoutInfo = new LayoutInfo(allocated, compoundType, fieldIndexToMember);
        LayoutInfo appearing = instanceLayouts.putIfAbsent(validated, layoutInfo);
        return appearing != null ? appearing : layoutInfo;
    }

    private CompoundType.Member computeMember(final BitSet allocated, final FieldElement field) {
        TypeSystem ts = ctxt.getTypeSystem();
        ValueType fieldType = field.getType(List.of(/* todo */));
        int size = (int) fieldType.getSize();
        int align = fieldType.getAlign();
        int idx = find(allocated, align, size);
        allocated.set(idx, idx + size);
        return ts.getCompoundTypeMember(field.getName(), fieldType, idx, align);
    }

    /**
     * Find a sequence of consecutive zero bits with the given alignment and count.  Current implementation finds the
     * first fit.
     *
     * @param bitSet the bit set to search
     * @param alignment the alignment
     * @param size the size
     * @return the bit index
     */
    private int find(BitSet bitSet, int alignment, int size) {
        assert Integer.bitCount(alignment) == 1;
        int mask = alignment - 1;
        int i = bitSet.nextClearBit(0);
        int n;
        for (;;) {
            // adjust for alignment
            int amt = mask - (i - 1 & mask);
            while (amt > 0) {
                i = bitSet.nextClearBit(i + amt);
                amt = mask - (i - 1 & mask);
            }
            // check the size
            n = bitSet.nextSetBit(i);
            if (n == -1 || n - i >= size) {
                // found a fit
                return i;
            }
            // try the next spot
            i = bitSet.nextClearBit(i);
        }
    }

    public static final class LayoutInfo {
        private final BitSet allocated;
        private final CompoundType compoundType;
        private final List<CompoundType.Member> fieldIndexToMember;

        LayoutInfo(final BitSet allocated, final CompoundType compoundType, final List<CompoundType.Member> fieldIndexToMember) {
            this.allocated = allocated;
            this.compoundType = compoundType;
            this.fieldIndexToMember = fieldIndexToMember;
        }

        public CompoundType getCompoundType() {
            return compoundType;
        }

        public List<CompoundType.Member> getFieldIndexToMember() {
            return fieldIndexToMember;
        }

        public CompoundType.Member getMember(FieldElement element) {
            return fieldIndexToMember.get(element.getIndex());
        }
    }
}
