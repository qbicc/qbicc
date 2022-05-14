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
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
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

    private final Map<LoadedTypeDefinition, LayoutInfo> instanceLayouts = new ConcurrentHashMap<>();
    private final Map<LoadedTypeDefinition, LayoutInfo> staticLayouts = new ConcurrentHashMap<>();
    private final Map<ObjectType, LayoutInfo> arrayLayouts = new ConcurrentHashMap<>();
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

    /**
     * Get the layout info for a reference array which is narrowed to the given type.
     *
     * @param arrayClass the array class definition (must not be {@code null})
     * @param elementType the element type (must not be {@code null})
     * @return the layout info (not {@code null})
     */
    public LayoutInfo getArrayLayoutInfo(DefinedTypeDefinition arrayClass, ObjectType elementType) {
        LayoutInfo layoutInfo = arrayLayouts.get(elementType);
        if (layoutInfo != null) {
            return layoutInfo;
        }
        // start with the prototype
        LayoutInfo protoInfo = getInstanceLayoutInfo(arrayClass);
        CompoundType protoCompound = protoInfo.getCompoundType();
        int memberCount = protoCompound.getMemberCount();
        if (memberCount < 1) {
            throw new IllegalArgumentException();
        }
        CompoundType.Member lastMember = protoCompound.getMember(memberCount - 1);
        if (lastMember.getType() instanceof ArrayType at) {
            if (at.getElementCount() == 0 && at.getElementType() instanceof ReferenceType) {
                // replace
                CompoundType.Member[] newMembers = protoCompound.getMembers().toArray(CompoundType.Member[]::new);
                TypeSystem ts = ctxt.getTypeSystem();
                CompoundType.Member newLastMember = ts.getCompoundTypeMember(lastMember.getName(), ts.getArrayType(elementType.getReference(), 0), lastMember.getOffset(), lastMember.getAlign());
                newMembers[memberCount - 1] = newLastMember;
                CompoundType newType = ts.getCompoundType(CompoundType.Tag.CLASS, elementType.getReferenceArrayObject().toFriendlyString(), protoCompound.getSize(), protoCompound.getAlign(), () -> List.of(newMembers));
                Map<FieldElement, CompoundType.Member> newMapping = new HashMap<>(protoInfo.getFieldsMap());
                newMapping.replaceAll((fe, m) -> m == lastMember ? newLastMember : m);
                layoutInfo = new LayoutInfo(protoInfo.getAllocatedBits(), newType, newMapping);
                LayoutInfo appearing = arrayLayouts.putIfAbsent(elementType, layoutInfo);
                return appearing != null ? appearing : layoutInfo;
            }
        }
        throw new IllegalArgumentException();
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
            if (!field.getType().isComplete()) {
                continue; // skip fields whose type is unresolved
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
                field.setOffset(member.getOffset());
            }
        }
        int size;
        if (trailingArray != null) {
            CompoundType.Member member = computeMember(allocated, trailingArray);
            if (member.getAlign() > minAlignment) {
                minAlignment = member.getAlign();
            }
            fieldToMember.put(trailingArray, member);
            trailingArray.setOffset(member.getOffset());
            size = member.getOffset();
        } else {
            size = allocated.length();
        }
        CompoundType.Member[] membersArray = fieldToMember.values().toArray(CompoundType.Member[]::new);
        Arrays.sort(membersArray);
        List<CompoundType.Member> membersList = List.of(membersArray);
        CompoundType compoundType = ctxt.getTypeSystem().getCompoundType(CompoundType.Tag.CLASS, type.getInternalName().replace('/', '.'), size, minAlignment, () -> membersList);
        layoutInfo = new LayoutInfo(allocated, compoundType, fieldToMember);
        LayoutInfo appearing = instanceLayouts.putIfAbsent(validated, layoutInfo);
        return appearing != null ? appearing : layoutInfo;
    }

    /**
     * Compute the static layout information of the given type.
     *
     * @param type the type (must not be {@code null})
     * @return the layout info, or {@code null} if there are no static fields
     */
    public LayoutInfo getStaticLayoutInfo(DefinedTypeDefinition type) {
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
        TypeSystem ts = ctxt.getTypeSystem();
        int minAlignment = ts.getPointerAlignment();
        for (int i = 0; i < cnt; i ++) {
            FieldElement field = loaded.getField(i);
            if (! field.isStatic() || field.isThreadLocal()) {
                continue;
            }
            CompoundType.Member member = computeMember(allocated, field);
            if (member.getAlign() > minAlignment) {
                minAlignment = member.getAlign();
            }
            fieldToMember.put(field, member);
            field.setOffset(member.getOffset());
        }
        int size = allocated.length();
        CompoundType.Member[] membersArray = fieldToMember.values().toArray(CompoundType.Member[]::new);
        Arrays.sort(membersArray);
        List<CompoundType.Member> membersList = List.of(membersArray);
        CompoundType compoundType = ts.getCompoundType(CompoundType.Tag.STRUCT, "statics." + type.getInternalName().replace('/', '.'), size, minAlignment, () -> membersList);
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
