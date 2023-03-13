package org.qbicc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

import io.smallrye.common.constraint.Assert;

/**
 * A type comprising multiple overlapping objects of any type.
 */
public final class UnionType extends ValueType {

    private static final VarHandle sizeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "size", VarHandle.class, UnionType.class, long.class);
    private static final VarHandle alignHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "align", VarHandle.class, UnionType.class, int.class);

    private final Tag tag;
    private final String name;
    @SuppressWarnings("FieldMayBeFinal") // sizeHandle
    private volatile long size;
    @SuppressWarnings("FieldMayBeFinal") // alignHandle
    private volatile int align;
    private final boolean complete;
    private volatile Supplier<List<Member>> membersResolver;
    private volatile List<Member> members;
    private volatile Map<String, Member> membersByName;

    UnionType(final TypeSystem typeSystem, final Tag tag, final String name, final Supplier<List<Member>> membersResolver, final long size, final int overallAlign) {
        super(typeSystem, Objects.hash(UnionType.class, name));
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

    UnionType(final TypeSystem typeSystem, final Tag tag, final String name, final Supplier<List<Member>> membersResolver) {
        super(typeSystem, Objects.hash(UnionType.class, name));
        // tag does not contribute to hash or equality
        this.tag = tag;
        this.name = name;
        this.size = -1;
        this.align = -1;
        this.membersResolver = membersResolver;
        this.complete = true;
    }

    UnionType(final TypeSystem typeSystem, final Tag tag, final String name) {
        super(typeSystem, Objects.hash(UnionType.class, name));
        // tag does not contribute to hash or equality
        this.tag = tag;
        this.name = name;
        this.size = 0;
        this.align = 1;
        this.complete = false;
        this.members = List.of();
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

    public Member getMember(String name) {
        Assert.assertFalse(isAnonymous()); /* anonymous unions do not have member names */
        Member member = getMembersByName().get(name);
        if (member != null) {
            return member;
        }
        throw new NoSuchElementException("No member named '" + name + "' found in " + this.toFriendlyString());
    }

    public boolean hasMember(String name) {
        Assert.assertFalse(isAnonymous()); /* anonymous unions do not have member names */
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
                size = Math.max(size, member.getType().getSize());
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
        return other instanceof UnionType ct && equals(ct);
    }

    @Override
    public ValueType getTypeAtOffset(long offset) {
        return offset == 0 ? this : super.getTypeAtOffset(offset);
    }

    public boolean equals(final UnionType other) {
        return this == other || super.equals(other) && Objects.equals(name, other.name) && size == other.size && align == other.align && getMembers().equals(other.getMembers());
    }

    public StringBuilder toString(final StringBuilder b) {
        super.toString(b);
        b.append("union ");
        if (tag != Tag.NONE) {
            b.append(tag).append(' ');
        }
        return b.append(getName());
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append(getName());
    }

    public static final class Member {
        private final int hashCode;
        private final String name;
        private final ValueType type;

        Member(final String name, final ValueType type) {
            this.name = name;
            this.type = type;
            hashCode = Objects.hash(name, type);
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

       public int hashCode() {
            return hashCode;
        }

        public String toString() {
            return toString(new StringBuilder()).toString();
        }

        public StringBuilder toString(final StringBuilder b) {
            type.toString(b).append(' ').append(name);
            return b;
        }

        public boolean equals(final Object obj) {
            return obj instanceof Member m && equals(m);
        }

        public boolean equals(final Member other) {
            return other == this || other != null && hashCode == other.hashCode && Objects.equals(name, other.name) && type.equals(other.type);
        }
    }

    public enum Tag {
        NONE("untagged"),
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
}
