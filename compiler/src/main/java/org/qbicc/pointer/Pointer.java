package org.qbicc.pointer;

import java.util.Iterator;
import java.util.List;

import org.qbicc.interpreter.Memory;
import org.qbicc.type.ArrayType;
import org.qbicc.type.StructType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.InstanceFieldElement;

/**
 * The base type of a VM pointer value.
 */
public abstract class Pointer {
    private final PointerType type;

    Pointer(PointerType type) {
        this.type = type;
    }

    public long getValue() {
        throw new IllegalArgumentException("Pointer does not have a value");
    }

    public PointerType getType() {
        return type;
    }

    public ValueType getPointeeType() {
        return type.getPointeeType();
    }

    public <T extends ValueType> T getPointeeType(Class<T> expected) {
        return expected.cast(getPointeeType());
    }

    /**
     * Attempt to get a new, correctly-typed pointer which is offset from this one by the given number of bytes.
     * If the byte offset does not correspond to a valid sub-member, an exception is thrown.
     *
     * @param offset the offset, in bytes
     * @param array {@code true} if offsets past the size of this pointer's pointee type should be considered to be sibling array elements,
     *              or {@code false} if they should be considered to be out of bounds
     * @return the correctly-typed pointer value (not {@code null})
     * @throws IllegalArgumentException if the offset does not correspond to a valid pointer
     */
    public Pointer offsetInBytes(long offset, boolean array) throws IllegalArgumentException {
        if (offset == 0) {
            return this;
        }
        ValueType pointeeType = getPointeeType();
        if (pointeeType instanceof ArrayType at) {
            // get the nearest index
            long elementSize = at.getElementSize();
            long index = offset / elementSize;
            long extra = offset % elementSize;
            // assume that this may be a "flexible" array if it's the top level
            return new ElementPointer(this, index).offsetInBytes(extra, array);
        }
        // not an inner array, so check the bounds within the pointee size within an outer array
        if (array) {
            long pointeeSize = pointeeType.getSize();
            if (pointeeSize > 0 && (offset < 0 || offset >= pointeeSize)) {
                long index = offset / pointeeSize;
                long extra = offset % pointeeSize;
                return offsetByElements(index).offsetInBytes(extra, false);
            }
        }
        // structure?
        if (pointeeType instanceof StructType st) {
            // find the most-fitting member
            List<StructType.Member> members = st.getMembers();
            Iterator<StructType.Member> iterator = members.iterator();
            while (iterator.hasNext()) {
                StructType.Member member = iterator.next();
                int memberOffset = member.getOffset();
                if (memberOffset > offset) {
                    // not it
                    continue;
                }
                if (! iterator.hasNext() && member.getType() instanceof ArrayType at && at.getElementCount() == 0) {
                    // flexible array member; it claims all following memory
                    return new MemberPointer(this, member).offsetInBytes(offset - memberOffset, true);
                }
                if ((offset < memberOffset + member.getType().getSize())) {
                    return new MemberPointer(this, member).offsetInBytes(offset - memberOffset, false);
                }
            }
            // invalid pointer/unknown memory location
            return null;
        }
        // field?
        if (pointeeType instanceof PhysicalObjectType pot) {
            LoadedTypeDefinition def = pot.getDefinition().load();
            do {
                int fieldCount = def.getFieldCount();
                for (int i = 0; i < fieldCount; i ++) {
                    if (def.getField(i) instanceof InstanceFieldElement ife) {
                        long fieldOffset = ife.getOffset();
                        if (fieldOffset > offset) {
                            // not it
                            continue;
                        }
                        if (i == fieldCount - 1 && ife.getType() instanceof ArrayType at && at.getElementCount() == 0) {
                            // flexible array member; it claims all following memory
                            return new InstanceFieldPointer(this, ife).offsetInBytes(offset - fieldOffset, true);
                        }
                        if (offset < fieldOffset + ife.getType().getSize()) {
                            return new InstanceFieldPointer(this, ife).offsetInBytes(offset - fieldOffset, false);
                        }
                    }
                }
                def = def.getSuperClass();
            } while (def != null);
            // invalid pointer/unknown memory location (not within target object)
            return null;
        }
        // invalid pointer/unknown memory location (unknown type)
        return null;
    }

    public Pointer offsetByElements(long count) {
        if (count == 0) {
            return this;
        }
        return new OffsetPointer(this, count);
    }

    public abstract RootPointer getRootPointer();

    public abstract long getRootByteOffset();

    public abstract Memory getRootMemoryIfExists();

    public abstract String getRootSymbolIfExists();

    @Override
    public int hashCode() {
        return getRootPointer().hashCode() * 19 + Long.hashCode(getRootByteOffset());
    }

    /**
     * Determine pointer equality as described in {@link #equals(Pointer)}.
     *
     * @param obj the other value
     * @return {@code true} if the other value is equal to this pointer, or {@code false} otherwise
     * @see #equals(Pointer)
     */
    @Override
    public final boolean equals(Object obj) {
        return obj instanceof Pointer p && equals(p);
    }

    /**
     * Determine pointer equality. Two pointers are equal if they share the same root pointer and offset. If this
     * method returns {@code false}, the pointers still may end up being equal at run time.
     *
     * @param other the other pointer
     * @return {@code true} if this pointer is definitely equal to the other pointer, or {@code false} otherwise
     *      (including the case where equality cannot be determined)
     */
    public boolean equals(Pointer other) {
        return this == other || other != null && getRootPointer().equals(other.getRootPointer()) && getRootByteOffset() == other.getRootByteOffset();
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public abstract StringBuilder toString(StringBuilder b);

    public abstract <T, R> R accept(Visitor<T, R> visitor, T t);

    public boolean isNoThrow() {
        return false;
    }

    public boolean isNoSafePoints() {
        return false;
    }

    public boolean isNoReturn() {
        return false;
    }

    public boolean isNoSideEffect() {
        return false;
    }

    public interface Visitor<T, R> extends RootPointer.Visitor<T, R> {
        default R visitAny(T t, Pointer pointer) {
            return null;
        }

        @Override
        default R visitAny(T t, RootPointer pointer) {
            return visitAny(t, (Pointer) pointer);
        }

        default R visit(T t, ElementPointer pointer) {
            return visitAny(t, pointer);
        }

        default R visit(T t, InstanceFieldPointer pointer) {
            return visitAny(t, pointer);
        }

        default R visit(T t, MemberPointer pointer) {
            return visitAny(t, pointer);
        }

        default R visit(T t, OffsetPointer pointer) {
            return visitAny(t, pointer);
        }
    }
}
