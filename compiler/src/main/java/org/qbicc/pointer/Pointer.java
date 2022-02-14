package org.qbicc.pointer;

import java.util.List;

import org.qbicc.interpreter.Memory;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

/**
 * The base type of a VM pointer value.
 */
public abstract class Pointer {
    private final PointerType type;

    Pointer(PointerType type) {
        this.type = type;
    }

    public PointerType getType() {
        return type;
    }

    public ValueType getPointeeType() {
        return type.getPointeeType();
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
        // not an array, so check the bounds within the pointee size
        long pointeeSize = pointeeType.getSize();
        if (offset < 0 || offset > pointeeSize) {
            if (! array) {
                throw new IllegalArgumentException("Pointer offset is out of bounds");
            }
            long index = offset / pointeeSize;
            long extra = offset % pointeeSize;
            return offsetByElements(index).offsetInBytes(extra, false);
        }
        // structure?
        if (pointeeType instanceof CompoundType ct) {
            // find the most-fitting member
            List<CompoundType.Member> members = ct.getMembers();
            for (CompoundType.Member member : members) {
                int memberOffset = member.getOffset();
                if (memberOffset <= offset) {
                    return new MemberPointer(this, member).offsetInBytes(offset - memberOffset, false);
                }
            }
            throw new IllegalArgumentException("Pointer offset does not correspond to a structure member");
        }
        // field?
        if (pointeeType instanceof PhysicalObjectType pot) {
            LoadedTypeDefinition def = pot.getDefinition().load();
            int fieldCount = def.getFieldCount();
            for (int i = 0; i < fieldCount; i ++) {
                FieldElement field = def.getField(i);
                if (! field.isStatic()) {
                    int fieldOffset = field.getInterpreterOffset();
                    if (offset >= fieldOffset && offset < field.getType().getSize()) {
                        return new InstanceFieldPointer(this, field).offsetInBytes(fieldOffset - offset, false);
                    }
                }
            }
            throw new IllegalArgumentException("Pointer offset does not correspond to a field in the target instance");
        }
        throw new IllegalArgumentException("Cannot determine type of pointer offset");
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

    public abstract <T, R> R accept(Visitor<T, R> visitor, T t);

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
