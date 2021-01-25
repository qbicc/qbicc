package cc.quarkus.qcc.machine.llvm.debuginfo;

public enum DIFlags {
    Private("DIFlagPrivate"),
    Protected("DIFlagProtected"),
    Public("DIFlagPublic"),
    FwdDecl("DIFlagFwdDecl"),
    AppleBlock("DIFlagAppleBlock"),
    Virtual("DIFlagVirtual"),
    Artificial("DIFlagArtificial"),
    Explicit("DIFlagExplicit"),
    Prototyped("DIFlagPrototyped"),
    ObjcClassComplete("DIFlagObjcClassComplete"),
    ObjectPointer("DIFlagObjectPointer"),
    Vector("DIFlagVector"),
    StaticMember("DIFlagStaticMember"),
    LValueReference("DIFlagLValueReference"),
    RValueReference("DIFlagRValueReference"),
    ExportSymbols("DIFlagExportSymbols"),
    SingleInheritance("DIFlagSingleInheritance"),
    MultipleInheritance("DIFlagMultipleInheritance"),
    VirtualInheritance("DIFlagVirtualInheritance"),
    IntroducedVirtual("DIFlagIntroducedVirtual"),
    BitField("DIFlagBitField"),
    NoReturn("DIFlagNoReturn"),
    TypePassByValue("DIFlagTypePassByValue"),
    TypePassByReference("DIFlagTypePassByReference"),
    EnumClass("DIFlagEnumClass"),
    Thunk("DIFlagThunk"),
    NonTrivial("DIFlagNonTrivial"),
    BigEndian("DIFlagBigEndian"),
    LittleEndian("DIFlagLittleEndian"),
    AllCallsDescribed("DIFlagAllCallsDescribed");

    public final String name;

    DIFlags(final String name) {
        this.name = name;
    }
}
