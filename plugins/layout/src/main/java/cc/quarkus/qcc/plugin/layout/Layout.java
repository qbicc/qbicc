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
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.generic.BaseTypeSignature;

/**
 *
 */
public final class Layout {
    private static final AttachmentKey<Layout> KEY = new AttachmentKey<>();

    private final Map<ValidatedTypeDefinition, LayoutInfo> instanceLayouts = new ConcurrentHashMap<>();
    private final CompilationContext ctxt;
    private final FieldElement objectClassField;
    private final FieldElement classTypeIdField;

    private Layout(final CompilationContext ctxt) {
        this.ctxt = ctxt;
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        DefinedTypeDefinition jlcDef = classContext.findDefinedType("java/lang/Class");
        ValidatedTypeDefinition jlo = jloDef.validate();
        ValidatedTypeDefinition jlc = jlcDef.validate();
        // inject a field of ClassObjectType to hold the object class
        FieldElement.Builder builder = FieldElement.builder();
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_HIDDEN);
        builder.setName("class");
        builder.setEnclosingType(jloDef);
        // void for now, but this is cheating terribly
        builder.setDescriptor(BaseTypeDescriptor.V);
        builder.setSignature(BaseTypeSignature.V);
        // the type is really the self-type, but we don't have one of those so use j.l.Object
        builder.setType(jlo.getClassType().getTypeType());
        FieldElement field = builder.build();
        jlo.injectField(field);
        objectClassField = field;
        // now inject a field of ClassObjectType into Class to hold the corresponding run time type
        builder = FieldElement.builder();
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_HIDDEN);
        builder.setName("id");
        builder.setEnclosingType(jlcDef);
        // void for now, but this is cheating terribly
        builder.setDescriptor(BaseTypeDescriptor.V);
        builder.setSignature(BaseTypeSignature.V);
        builder.setType(jlo.getClassType().getTypeType());
        field = builder.build();
        jlc.injectField(field);
        classTypeIdField = field;
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
     * Get the object field which holds the run time type identifier.
     *
     * @return the type identifier field
     */
    public FieldElement getObjectClassField() {
        return objectClassField;
    }

    /**
     * Get the field on {@code Class} which holds the type identifier of its corresponding instance type.
     *
     * @return the class type identifier field
     */
    public FieldElement getClassTypeIdField() {
        return classTypeIdField;
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

        for (int i = 0; i < cnt; i ++) {
            // todo: skip unused fields?
            FieldElement field = validated.getField(i);
            if (field.isStatic()) {
                continue;
            }
            allMembers[i] = instanceMembers[ic++] = computeMember(allocated, field);
        }
        int size = allocated.length();
        List<CompoundType.Member> fieldIndexToMember = Arrays.asList(allMembers);
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
