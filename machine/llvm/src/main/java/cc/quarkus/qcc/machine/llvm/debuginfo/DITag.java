package cc.quarkus.qcc.machine.llvm.debuginfo;

public enum DITag {
    Member("DW_TAG_member"),
    PointerType("DW_TAG_pointer_type"),
    ReferenceType("DW_TAG_reference_type"),
    Typedef("DW_TAG_typedef"),
    Inheritance("DW_TAG_inheritance"),
    PtrToMemberType("DW_TAG_ptr_to_member_type"),
    ConstType("DW_TAG_const_type"),
    Friend("DW_TAG_friend"),
    VolatileType("DW_TAG_volatile_type"),
    RestrictType("DW_TAG_restrict_type"),
    AtomicType("DW_TAG_atomic_type"),
    ArrayType("DW_TAG_array_type"),
    ClassType("DW_TAG_class_type"),
    EnumerationType("DW_TAG_enumeration_type"),
    StructureType("DW_TAG_structure_type"),
    UnionType("DW_TAG_union_type");

    public final String name;

    DITag(final String name) {
        this.name = name;
    }
}
