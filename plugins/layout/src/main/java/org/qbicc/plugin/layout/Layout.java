package org.qbicc.plugin.layout;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ObjectType;
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

    @Deprecated
    public FieldElement getArrayContentField(final ObjectType arrayObjType) {
        return CoreClasses.get(ctxt).getArrayContentField(arrayObjType);
    }

    @Deprecated
    public LoadedTypeDefinition getArrayLoadedTypeDefinition(String arrayType) {
        return CoreClasses.get(ctxt).getArrayLoadedTypeDefinition(arrayType);
    }

    /**
     * Get the object field which holds the run time type identifier.
     *
     * @return the type identifier field
     */
    @Deprecated
    public FieldElement getObjectTypeIdField() {
        return CoreClasses.get(ctxt).getObjectTypeIdField();
    }

    /**
     * Get the field on {@code Class} which holds the type identifier of its corresponding instance type.
     *
     * @return the class type identifier field
     */
    @Deprecated
    public FieldElement getClassTypeIdField() {
        return CoreClasses.get(ctxt).getClassTypeIdField();
    }

    @Deprecated
    public FieldElement getClassDimensionField() {
        return CoreClasses.get(ctxt).getClassDimensionField();
    }

    @Deprecated
    public FieldElement getArrayLengthField() {
        return CoreClasses.get(ctxt).getArrayLengthField();
    }

    @Deprecated
    public FieldElement getRefArrayElementTypeIdField() {
        return CoreClasses.get(ctxt).getRefArrayElementTypeIdField();
    }

    @Deprecated
    public FieldElement getRefArrayDimensionsField() {
        return CoreClasses.get(ctxt).getRefArrayDimensionsField();
    }

    @Deprecated
    public FieldElement getRefArrayContentField() {
        return CoreClasses.get(ctxt).getRefArrayContentField();
    }

    @Deprecated
    public FieldElement getBooleanArrayContentField() {
        return CoreClasses.get(ctxt).getBooleanArrayContentField();
    }

    @Deprecated
    public FieldElement getByteArrayContentField() {
        return CoreClasses.get(ctxt).getByteArrayContentField();
    }

    @Deprecated
    public FieldElement getShortArrayContentField() {
        return CoreClasses.get(ctxt).getShortArrayContentField();
    }

    @Deprecated
    public FieldElement getIntArrayContentField() {
        return CoreClasses.get(ctxt).getIntArrayContentField();
    }

    @Deprecated
    public FieldElement getLongArrayContentField() {
        return CoreClasses.get(ctxt).getLongArrayContentField();
    }

    @Deprecated
    public FieldElement getCharArrayContentField() {
        return CoreClasses.get(ctxt).getCharArrayContentField();
    }

    @Deprecated
    public FieldElement getFloatArrayContentField() {
        return CoreClasses.get(ctxt).getFloatArrayContentField();
    }

    @Deprecated
    public FieldElement getDoubleArrayContentField() {
        return CoreClasses.get(ctxt).getDoubleArrayContentField();
    }

    public LayoutInfo getInstanceLayoutInfo(DefinedTypeDefinition type) {
        return getInstanceLayoutInfoHelper(type, false);
    }

    public LayoutInfo getInstanceLayoutInfoForNativeType(DefinedTypeDefinition type) {
        return getInstanceLayoutInfoHelper(type, true);
    }

    private LayoutInfo getInstanceLayoutInfoHelper(DefinedTypeDefinition type, boolean isNativeType) {
        if (type.isInterface()) {
            throw new IllegalArgumentException("Interfaces have no instance layout");
        }
        LoadedTypeDefinition validated = type.load();
        LayoutInfo layoutInfo = instanceLayouts.get(validated);
        if (layoutInfo != null) {
            return layoutInfo;
        }
        // ignore super class layout for native types
        LoadedTypeDefinition superClass = isNativeType ? null : validated.getSuperClass();
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
            allocated.or(superLayout.allocated);
        }
        int cnt = validated.getFieldCount();
        Map<FieldElement, CompoundType.Member> fieldToMember = superLayout == null ? new HashMap<>(cnt) : new HashMap<>(superLayout.fieldToMember);
        for (int i = 0; i < cnt; i ++) {
            // todo: skip unused fields?
            FieldElement field = validated.getField(i);
            if (field.isStatic()) {
                continue;
            }
            CompoundType.Member member = computeMember(allocated, field);
            if (member.getAlign() > minAlignment) {
                minAlignment = member.getAlign();
            }
            fieldToMember.put(field, member);
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
        if (fieldType instanceof BooleanType) {
            // widen booleans to 8 bits
            fieldType = ts.getUnsignedInteger8Type();
        }
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
            i = bitSet.nextClearBit(n);
        }
    }

    public static final class LayoutInfo {
        private final BitSet allocated;
        private final CompoundType compoundType;
        private final Map<FieldElement, CompoundType.Member> fieldToMember;

        LayoutInfo(final BitSet allocated, final CompoundType compoundType, final Map<FieldElement, CompoundType.Member> fieldToMember) {
            this.allocated = allocated;
            this.compoundType = compoundType;
            this.fieldToMember = fieldToMember;
        }

        public CompoundType getCompoundType() {
            return compoundType;
        }

        public CompoundType.Member getMember(FieldElement element) {
            return fieldToMember.get(element);
        }
    }
}
