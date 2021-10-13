package org.qbicc.plugin.layout;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public final class Layout {
    private static final AttachmentKey<Layout> KEY = new AttachmentKey<>();
    private static final AttachmentKey<Layout> I_KEY = new AttachmentKey<>();

    private final Map<LoadedTypeDefinition, LayoutInfo> instanceLayouts = new ConcurrentHashMap<>();
    private final Map<LoadedTypeDefinition, LayoutInfo> staticLayouts = new ConcurrentHashMap<>();
    private final CompilationContext ctxt;

    private Layout(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static void unlock(CompilationContext ctxt) {
        ctxt.putAttachmentIfAbsent(KEY, new Layout(ctxt));
    }

    public static Layout get(CompilationContext ctxt) {
        Layout layout = ctxt.getAttachment(KEY);
        if (layout == null) {
            throw new IllegalStateException("Layout is not yet available");
        }
        return layout;
    }

    public static Layout getForInterpreter(CompilationContext ctxt) {
        Layout layout = ctxt.getAttachment(I_KEY);
        if (layout == null) {
            layout = new Layout(ctxt);
            Layout appearing = ctxt.putAttachmentIfAbsent(I_KEY, layout);
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
        LoadedTypeDefinition validated = type.load();
        LayoutInfo layoutInfo = instanceLayouts.get(validated);
        if (layoutInfo != null) {
            return layoutInfo;
        }
        // ignore super class layout for native types
        LoadedTypeDefinition superClass = validated.getSuperClass();
        LayoutInfo superLayout;
        int minAlignment;
        if (superClass != null) {
            superLayout = getInstanceLayoutInfo(superClass);
            minAlignment = superLayout.getCompoundType().getAlign();
        } else {
            superLayout = null;
            minAlignment = ctxt.getTypeSystem().getPointerAlignment(); // All objects have at least pointer alignment.
        }
        BitSet allocated = new BitSet();
        if (superLayout != null) {
            allocated.or(superLayout.getAllocatedBits());
        }
        int cnt = validated.getFieldCount();
        Map<FieldElement, CompoundType.Member> fieldToMember = superLayout == null ? new HashMap<>(cnt) : new HashMap<>(superLayout.getFieldsMap());
        FieldElement trailingArray = null;
        for (int i = 0; i < cnt; i ++) {
            // todo: skip unused fields?
            FieldElement field = validated.getField(i);
            if (field.isStatic()) {
                continue;
            }
            if (field.getType().getSize() == 0) {
                Assert.assertTrue(trailingArray == null); // At most one trailing array per type!
                trailingArray = field; // defer until all other fields are allocated
            } else {
                CompoundType.Member member = computeMember(allocated, field);
                if (member.getAlign() > minAlignment) {
                    minAlignment = member.getAlign();
                }
                fieldToMember.put(field, member);
            }
        }
        if (trailingArray != null) {
            CompoundType.Member member = computeMember(allocated, trailingArray);
            if (member.getAlign() > minAlignment) {
                minAlignment = member.getAlign();
            }
            fieldToMember.put(trailingArray, member);
        }
        int size = allocated.length();
        CompoundType.Member[] membersArray = fieldToMember.values().toArray(CompoundType.Member[]::new);
        Arrays.sort(membersArray);
        List<CompoundType.Member> membersList = List.of(membersArray);
        CompoundType compoundType = ctxt.getTypeSystem().getCompoundType(CompoundType.Tag.NONE, type.getInternalName().replace('/', '.'), size, minAlignment, () -> membersList);
        layoutInfo = new LayoutInfo(allocated, compoundType, fieldToMember);
        LayoutInfo appearing = instanceLayouts.putIfAbsent(validated, layoutInfo);
        return appearing != null ? appearing : layoutInfo;
    }

    /**
     * Compute the static layout information of the given type for the interpreter.  The run time does not store
     * static fields in a structure.
     *
     * @param type the type (must not be {@code null})
     * @return the layout info, or {@code null} if there are no static fields
     */
    public LayoutInfo getInterpreterStaticLayoutInfo(DefinedTypeDefinition type) {
        LoadedTypeDefinition loaded = type.load();
        LayoutInfo layoutInfo = staticLayouts.get(loaded);
        if (layoutInfo != null) {
            return layoutInfo;
        }
        int cnt = loaded.getFieldCount();
        if (cnt == 0) {
            return null;
        }
        BitSet allocated = new BitSet();
        Map<FieldElement, CompoundType.Member> fieldToMember = new HashMap<>(cnt);
        for (int i = 0; i < cnt; i ++) {
            FieldElement field = loaded.getField(i);
            if (! field.isStatic() || field.isThreadLocal()) {
                continue;
            }
            fieldToMember.put(field, computeMember(allocated, field));
        }
        int size = allocated.length();
        CompoundType.Member[] membersArray = fieldToMember.values().toArray(CompoundType.Member[]::new);
        Arrays.sort(membersArray);
        List<CompoundType.Member> membersList = List.of(membersArray);
        CompoundType compoundType = ctxt.getTypeSystem().getCompoundType(CompoundType.Tag.NONE, type.getInternalName().replace('/', '.'), size, 1, () -> membersList);
        layoutInfo = new LayoutInfo(allocated, compoundType, fieldToMember);
        LayoutInfo appearing = staticLayouts.putIfAbsent(loaded, layoutInfo);
        return appearing != null ? appearing : layoutInfo;
    }

    private ValueType widenBoolean(ValueType type) {
        if (type instanceof BooleanType) {
            return ctxt.getTypeSystem().getUnsignedInteger8Type();
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            ValueType elementType = arrayType.getElementType();
            ValueType widened = widenBoolean(elementType);
            return elementType == widened ? type : ctxt.getTypeSystem().getArrayType(widened, arrayType.getElementCount());
        } else {
            return type;
        }
    }

    private CompoundType.Member computeMember(final BitSet allocated, final FieldElement field) {
        TypeSystem ts = ctxt.getTypeSystem();
        ValueType fieldType = widenBoolean(field.getType());
        int size = (int) fieldType.getSize();
        int align = fieldType.getAlign();
        int idx;
        if (size != 0) {
            idx = find(allocated, align, size);
            allocated.set(idx, idx + size);
        } else {
            idx = find(allocated, align, ts.getMaxAlignment());
        }
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
            i = bitSet.nextClearBit(n);
        }
    }
}
