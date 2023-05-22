package org.qbicc.type;

import io.smallrye.common.constraint.Assert;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A structured type, which comprises zero or more non-overlapping members.
 */
public final class StructType extends ValueType {

    private static final VarHandle sizeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "size", VarHandle.class, StructType.class, long.class);
    private static final VarHandle alignHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "align", VarHandle.class, StructType.class, int.class);

    private final Tag tag;
    private final String name;
    @SuppressWarnings("FieldMayBeFinal") // sizeHandle
    private volatile long size;
    @SuppressWarnings("FieldMayBeFinal") // alignHandle
    private volatile int align;
    private final boolean complete;
    private volatile Supplier<List<Member>> membersResolver;
    private volatile List<Member> members;
    private volatile List<Member> paddedMembers;
    private volatile Map<String, Member> membersByName;

    StructType(final TypeSystem typeSystem, final Tag tag, final String name, final Supplier<List<Member>> membersResolver, final long size, final int overallAlign) {
        super(typeSystem, Objects.hash(StructType.class, name, size, overallAlign));
        // tag does not contribute to hash or equality
        this.tag = tag;
        this.name = name;
        // todo: assert size ≥ end of last member w/alignment etc.
        this.size = size;
        assert Integer.bitCount(overallAlign) == 1;
        this.align = overallAlign;
        this.membersResolver = membersResolver;
        this.complete = true;
    }

    StructType(final TypeSystem typeSystem, final Tag tag, final String name, final Supplier<List<Member>> membersResolver) {
        super(typeSystem, Objects.hash(StructType.class, name, -1, -1));
        // tag does not contribute to hash or equality
        this.tag = tag;
        this.name = name;
        this.size = -1;
        this.align = -1;
        this.membersResolver = membersResolver;
        this.complete = true;
    }

    StructType(final TypeSystem typeSystem, final Tag tag, final String name) {
        super(typeSystem, Objects.hash(StructType.class, name, 0, 1));
        // tag does not contribute to hash or equality
        this.tag = tag;
        this.name = name;
        this.size = 0;
        this.align = 1;
        this.complete = false;
        this.members = List.of();
        this.paddedMembers = List.of();
        this.membersByName = Map.of();
    }

    public boolean isAnonymous() {
        return name == null;
    }

    public String getName() {
        final String name = this.name;
        return name == null ? "<anon>" : name;
    }

    public List<Member> getMembers() {
        List<Member> members = this.members;
        if (members == null) {
            synchronized (this) {
                members = this.members;
                if (members == null) {
                    members = this.members = membersResolver.get();
                    membersResolver = null;
                }
            }
        }
        return members;
    }

    /**
     * Get the members of this structure, with aligned, atomic-friendly padding members inserted in between any gaps.
     *
     * @return the padded members (not {@code null})
     */
    public List<Member> getPaddedMembers() {
        List<Member> paddedMembers = this.paddedMembers;
        if (paddedMembers == null) {
            synchronized (this) {
                paddedMembers = this.paddedMembers;
                if (paddedMembers == null) {
                    paddedMembers = new ArrayList<>(getMembers());
                    // must be sorted ascending by offset
                    paddedMembers.sort(Comparator.naturalOrder());
                    // fill with padding, if needed
                    int offs = 0;
                    final ListIterator<Member> iterator = paddedMembers.listIterator();
                    TypeSystem ts = getTypeSystem();
                    UnsignedIntegerType u8 = ts.getUnsignedInteger8Type();
                    UnsignedIntegerType u16 = ts.getUnsignedInteger16Type();
                    UnsignedIntegerType u32 = ts.getUnsignedInteger32Type();
                    UnsignedIntegerType u64 = ts.getUnsignedInteger64Type();
                    // this only works if...
                    assert u8.getAlign() == 1;
                    assert u16.getAlign() == 2;
                    assert u32.getAlign() == 4;
                    assert u64.getAlign() == 8;
                    // iterate every member and insert padding as needed
                    while (iterator.hasNext()) {
                        Member member = iterator.next();
                        iterator.previous(); // back up to insert
                        offs = padTo(offs, iterator, ts, u8, u16, u32, u64, member.getOffset());
                        iterator.next(); // skip to the next
                        offs += (int) member.getType().getSize();
                    }
                    // finally, insert any trailing padding
                    offs = padTo(offs, iterator, ts, u8, u16, u32, u64, (int) getSize());
                    assert offs == getSize();
                    this.paddedMembers = paddedMembers;
                }
            }
        }
        return paddedMembers;
    }

    private int padTo(int offs, final ListIterator<Member> iterator, final TypeSystem ts, final UnsignedIntegerType u8, final UnsignedIntegerType u16, final UnsignedIntegerType u32, final UnsignedIntegerType u64, final int nextOffset) {
        if (nextOffset >= offs + 1 && (offs & 0b1) != 0) {
            iterator.add(new Member("(padding)", u8, offs, 1));
            offs++;
        }
        // now aligned to 2 bytes
        if (nextOffset >= offs + 2 && (offs & 0b10) != 0) {
            iterator.add(new Member("(padding)", u16, offs, 2));
            offs += 2;
        }
        // now aligned to 4 bytes
        if (nextOffset >= offs + 4 && (offs & 0b100) != 0) {
            iterator.add(new Member("(padding)", u32, offs, 4));
            offs += 4;
        }
        // now aligned to 8 bytes
        int wordBytes = (nextOffset & ~0b111) - offs;
        if (wordBytes > 8) {
            iterator.add(new Member("(padding)", ts.getArrayType(u64, wordBytes >> 3), offs, 8));
            offs += wordBytes;
        } else if (wordBytes == 8) {
            iterator.add(new Member("(padding)", u64, offs, 8));
            offs += 8;
        }
        // next insert de-aligning padding if the member itself is unaligned
        if (nextOffset >= offs + 4) {
            iterator.add(new Member("(padding)", u32, offs, 4));
            offs += 4;
        }
        if (nextOffset >= offs + 2) {
            iterator.add(new Member("(padding)", u16, offs, 2));
            offs += 2;
        }
        if (nextOffset >= offs + 1) {
            iterator.add(new Member("(padding)", u8, offs, 1));
            offs++;
        }
        assert nextOffset == offs;
        return offs;
    }

    public Map<String, Member> getMembersByName() {
        Map<String, Member> membersByName = this.membersByName;
        if (membersByName == null) {
            synchronized (this) {
                membersByName = this.membersByName;
                if (membersByName == null) {
                    membersByName = this.membersByName = createMembersByName();
                }
            }
        }
        return membersByName;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Member> createMembersByName() {
        List<Member> members = getMembers();
        Iterator<Member> iterator = members.iterator();
        if (! iterator.hasNext()) {
            return Map.of();
        }
        Member member0 = iterator.next();
        String name0 = member0.getName();
        if (! iterator.hasNext()) {
            return Map.of(name0, member0);
        }
        Member member1 = iterator.next();
        String name1 = member1.getName();
        if (! iterator.hasNext()) {
            return Map.of(name0, member0, name1, member1);
        }
        Member member2 = iterator.next();
        String name2 = member2.getName();
        if (! iterator.hasNext()) {
            return Map.of(name0, member0, name1, member1, name2, member2);
        }
        // lots of entries, I guess
        List<Map.Entry<String, Member>> entries = new ArrayList<>(members.size());
        entries.add(Map.entry(name0, member0));
        entries.add(Map.entry(name1, member1));
        entries.add(Map.entry(name2, member2));
        while (iterator.hasNext()) {
            Member member = iterator.next();
            entries.add(Map.entry(member.getName(), member));
        }
        return Map.ofEntries(entries.toArray(Map.Entry[]::new));
    }

    public Tag getTag() {
        return tag;
    }

    public int getMemberCount() {
        return getMembers().size();
    }

    public Member getMember(int index) throws IndexOutOfBoundsException {
        return getMembers().get(index);
    }

    public Member getMemberByOffset(int offset) {
        List<Member> list = getMembers();
        int size = list.size();

        // the usual binary search
        int low = 0;
        int high = size - 1;

        Member member;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            member = list.get(mid);
            int cmp = Integer.compare(member.getOffset(), offset);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                // found it exactly
                return member;
            }
        }
        if (low >= size) {
            // it's past the end
            return null;
        } else {
            // see if the offset is within the next-lower member
            member = list.get(low);
            if (offset < member.getOffset() + member.getType().getSize()) {
                return member;
            } else {
                // it fell into padding
                return null;
            }
        }
    }

    public Member getMember(String name) {
        Assert.assertFalse(isAnonymous()); /* anonymous structs do not have member names */
        Member member = getMembersByName().get(name);
        if (member != null) {
            return member;
        }
        throw new NoSuchElementException("No member named '" + name + "' found in " + this.toFriendlyString());
    }

    public boolean hasMember(String name) {
        Assert.assertFalse(isAnonymous()); /* anonymous structs do not have member names */
        return getMembersByName().containsKey(name);
    }

    public boolean isComplete() {
        return complete;
    }

    public long getSize() {
        long size = this.size;
        if (size == -1) {
            // computed dynamically from members
            List<Member> members = getMembers();
            size = 0;
            for (Member member : members) {
                size = Math.max(size, member.getOffset() + member.getType().getSize());
            }
            long witness = (long) sizeHandle.compareAndExchange(this, -1L, size);
            if (witness != -1 && witness != size) {
                // size changed to something unexpected! should be impossible though...
                throw new IllegalStateException();
            }
        }
        return size;
    }

    public int getAlign() {
        int align = this.align;
        if (align == -1) {
            // computed dynamically from members
            List<Member> members = getMembers();
            align = 1;
            for (Member member : members) {
                align = Math.max(align, member.getType().getAlign());
            }
            long witness = (long) alignHandle.compareAndExchange(this, -1, align);
            if (witness != -1 && witness != align) {
                // align changed to something unexpected! should be impossible though...
                throw new IllegalStateException();
            }
        }
        return align;
    }

    public boolean equals(final ValueType other) {
        return other instanceof StructType st && equals(st);
    }

    @Override
    public ValueType getTypeAtOffset(long offset) {
        if (offset > getSize()) {
            return getTypeSystem().getVoidType();
        }
        StructType.Member member = getMemberByOffset((int) offset);
        if (member == null) {
            return getTypeSystem().getVoidType();
        }
        return member.getType().getTypeAtOffset(offset - member.getOffset());
    }

    public boolean equals(final StructType other) {
        return this == other || super.equals(other) && Objects.equals(name, other.name) && size == other.size && align == other.align && getMembers().equals(other.getMembers());
    }

    public StringBuilder toString(final StringBuilder b) {
        super.toString(b);
        b.append("compound ");
        if (tag != Tag.NONE) {
            b.append(tag).append(' ');
        }
        return b.append(getName());
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append(getName());
    }

    public static StructType.Builder builder(TypeSystem typeSystem) {
        return new Builder(typeSystem);
    }

    public static final class Member implements Comparable<Member> {
        private final int hashCode;
        private final String name;
        private final ValueType type;
        private final int offset;
        private final int align;

        Member(final String name, final ValueType type, final int offset, final int align) {
            this.name = name;
            this.type = type;
            this.offset = offset;
            this.align = Math.max(align, type.getAlign());
            assert Integer.bitCount(align) == 1;
            hashCode = (Objects.hash(name, type) * 19 + offset) * 19 + Integer.numberOfTrailingZeros(align);
        }

        public String getName() {
            return name;
        }

        public ValueType getType() {
            return type;
        }

        public <T extends ValueType> T getType(Class<T> expected) {
            return expected.cast(getType());
        }

        public int getOffset() {
            return offset;
        }

        public int getAlign() {
            return align;
        }

        public int hashCode() {
            return hashCode;
        }

        public String toString() {
            return toString(new StringBuilder()).toString();
        }

        public StringBuilder toString(final StringBuilder b) {
            type.toString(b).append(' ').append(name).append('@').append(offset);
            if (align > 1) {
                b.append(" align=").append(align);
            }
            return b;
        }

        public boolean equals(final Object obj) {
            return obj instanceof Member m && equals(m);
        }

        public boolean equals(final Member other) {
            return other == this || other != null && hashCode == other.hashCode && offset == other.offset && align == other.align && Objects.equals(name, other.name) && type.equals(other.type);
        }

        public int compareTo(final Member o) {
            // compare offset
            int res = Integer.compare(offset, o.offset);
            // at same offset? if so, compare size
            if (res == 0) res = Long.compare(o.type.getSize(), type.getSize());
            // at same offset *and* same size? if so, strive for *some* predictable order...
            if (res == 0) res = Integer.compare(hashCode, o.hashCode);
            return res;
        }
    }

    public enum Tag {
        NONE("untagged"),
        CLASS("class"),
        STRUCT("struct"),
        ;
        private final String string;

        Tag(final String string) {
            this.string = string;
        }

        public String toString() {
            return string;
        }
    }

    /**
     * StructType Builders are explicitly not thread-safe
     * and should not be shared between threads.
     * 
     * The resulting StructType can be shared.
     */
    public static final class Builder {
        final TypeSystem typeSystem;
        Tag tag;
        String name;

        long size;
        int offset;
        int overallAlign;

        ArrayList<StructType.Member> members = new ArrayList<>();

        StructType completeType;

        Builder(final TypeSystem typeSystem) {
            this.typeSystem = typeSystem;
            this.tag = Tag.NONE;
        }

        public Builder setName(String name) {
            // Don't need to check for null as StructType's ctor
            // will assign a name to anything without one.
            this.name = name;
            return this;
        }

        public Builder setTag(Tag tag) {
            this.tag = Objects.requireNonNull(tag);
            return this;
        }

        public Builder setOverallAlignment(int align) {
            if (align < 0) { 
                throw new IllegalStateException("Align must be positive");
            }
            overallAlign = align;
            return this;
        }

        public Builder addNextMember(final ValueType type) {
            Assert.assertTrue(name == null);
            return addNextMember("", type, type.getAlign());
        }

        public Builder addNextMember(final String name, final ValueType type) {
            return addNextMember(name, type, type.getAlign());
        }

        public Builder addNextMember(final String name, final ValueType type, final int align) {
            int thisOffset = nextMemberOffset(offset, align);
            Member m = typeSystem.getStructTypeMember(name, type, thisOffset, align);
            // Equivalent to Max(overallAign, Max(type.getAlign(), align))
            overallAlign = Math.max(overallAlign, m.getAlign());
            
            // Update offset to point to the end of the reserved space
            offset = thisOffset + (int)type.getSize();
            members.add(m);
            return this;
        }

        public Member getLastAddedMember() {
            // not ideal but `addNextMember` returns `Builder`
            return members.get(members.size() - 1);
        }

        public int getMemberCountSoFar() {
            return members.size();
        }

        private int nextMemberOffset(int offset, int align) {
            return (offset + (align - 1)) & -align;
        }

        public StructType build() {
            if (members.isEmpty()) {
                throw new IllegalStateException("StructType has no members");
            }
            if (completeType == null) {
                int size = (offset + (overallAlign - 1)) & -overallAlign;  // Offset points to the end of the structure, align the size to overall alignment
                completeType =  typeSystem.getStructType(tag, name, size, overallAlign, () -> members);
            }
            return completeType;
        }
    }
}
