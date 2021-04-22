package org.qbicc.type;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 *
 */
public final class CompoundType extends ValueType {

    private final Tag tag;
    private final String name;
    private final long size;
    private final int align;
    private final boolean complete;
    private volatile Supplier<List<Member>> membersResolver;
    private volatile List<Member> members;

    CompoundType(final TypeSystem typeSystem, final Tag tag, final String name, final Supplier<List<Member>> membersResolver, final long size, final int overallAlign) {
        super(typeSystem, (int) size * 19 + Integer.numberOfTrailingZeros(overallAlign));
        // name/tag do not contribute to hash or equality
        this.tag = tag;
        this.name = name == null ? "<anon>" : name;
        // todo: assert size ≥ end of last member w/alignment etc.
        this.size = size;
        assert Integer.bitCount(overallAlign) == 1;
        this.align = overallAlign;
        this.membersResolver = membersResolver;
        this.complete = true;
    }

    CompoundType(final TypeSystem typeSystem, final Tag tag, final String name) {
        super(typeSystem, 0);
        this.tag = tag;
        this.name = name;
        this.size = 0;
        this.align = 1;
        this.complete = false;
        this.members = List.of();
    }

    public String getName() {
        return name;
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

    public Tag getTag() {
        return tag;
    }

    public int getMemberCount() {
        return getMembers().size();
    }

    public Member getMember(int index) throws IndexOutOfBoundsException {
        return getMembers().get(index);
    }

    public Member getMember(String name) {
        List<Member> members = getMembers();
        for (Member m : members) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new NoSuchElementException("No member named '" + name + "' found in " + this.toFriendlyString());
    }

    public boolean isComplete() {
        return complete;
    }

    public long getSize() {
        return size;
    }

    public int getAlign() {
        return align;
    }

    public boolean equals(final ValueType other) {
        return other instanceof CompoundType && equals((CompoundType) other);
    }

    public boolean equals(final CompoundType other) {
        return this == other || super.equals(other) && name.equals(other.name) && size == other.size && align == other.align && getMembers().equals(other.getMembers());
    }

    public StringBuilder toString(final StringBuilder b) {
        super.toString(b);
        b.append("compound ");
        if (tag != Tag.NONE) {
            b.append(tag).append(' ');
        }
        return b.append(name);
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        b.append("compound.");
        if (tag == Tag.NONE) {
            b.append("unt.");
        } else if (tag == Tag.STRUCT) {
            b.append("struct.");
        } else {
            assert tag == Tag.UNION;
            b.append("union.");
        }
        b.append(name);
        return b;
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
            return obj instanceof Member && equals((Member) obj);
        }

        public boolean equals(final Member other) {
            return other == this || other != null && hashCode == other.hashCode && offset == other.offset && align == other.align && name.equals(other.name) && type.equals(other.type);
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
        STRUCT("struct"),
        UNION("union"),
        ;
        private final String string;

        Tag(final String string) {
            this.string = string;
        }

        public String toString() {
            return string;
        }
    }

    public static class CompoundTypeBuilder {
        final TypeSystem typeSystem;
        final Tag tag;
        final String name;

        long size = 0;
        int offset = 0;
        int overallAlign;

        ArrayList<CompoundType.Member> members = new ArrayList<>();

        public CompoundTypeBuilder(final TypeSystem typeSystem, final Tag tag, final String name, int overallAlign) {
            this.typeSystem = typeSystem;
            this.tag = tag;
            this.name = name;
            this.overallAlign = overallAlign;
        }

        public CompoundTypeBuilder addNextMember(final String name, final ValueType type) {
            return addNextMember(name, type, type.getAlign());
        }

        public CompoundTypeBuilder addNextMember(final String name, final ValueType type, final int align) {
            int thisOffset = nextMemberOffset(offset, align);
            Member m = typeSystem.getCompoundTypeMember(name, type, thisOffset, align);

            // Update offset to point to the end of the reserved space
            offset = thisOffset + (int)type.getSize();
            members.add(m);
            return this;
        }

        private int nextMemberOffset(int offset, int align) {
            return (offset + (align - 1)) & -align;
        }

        public CompoundType build() {
            if (members.isEmpty()) {
                throw new IllegalStateException("CompoundType has no members");
            }

            Member last = members.get(members.size() -1);
            int size = last.getOffset() + (int)last.getType().getSize();
            return typeSystem.getCompoundType(tag, name, size, overallAlign, () -> members);
        }
    }
}
