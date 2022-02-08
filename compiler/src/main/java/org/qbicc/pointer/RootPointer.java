package org.qbicc.pointer;

import org.qbicc.interpreter.Memory;
import org.qbicc.type.PointerType;

/**
 * A base (terminal) pointer which does not modify another pointer value.
 */
public abstract class RootPointer extends Pointer {
    RootPointer(PointerType type) {
        super(type);
    }

    @Override
    public RootPointer getRootPointer() {
        return this;
    }

    @Override
    public long getRootByteOffset() {
        return 0;
    }

    @Override
    public Memory getRootMemoryIfExists() {
        return null;
    }

    @Override
    public String getRootSymbolIfExists() {
        return null;
    }

    @Override
    public final <T, R> R accept(Pointer.Visitor<T, R> visitor, T t) {
        return accept((Visitor<T, R>) visitor, t);
    }

    public abstract <T, R> R accept(Visitor<T, R> visitor, T t);

    public interface Visitor<T, R> {
        default R visitAny(T t, RootPointer rootPointer) {
            return null;
        }

        default R visit(T t, IntegerAsPointer pointer) {
            return visitAny(t, pointer);
        }

        default R visit(T t, MemoryPointer pointer) {
            return visitAny(t, pointer);
        }

        default R visit(T t, ProgramObjectPointer pointer) {
            return visitAny(t, pointer);
        }

        default R visit(T t, ReferenceAsPointer pointer) {
            return visitAny(t, pointer);
        }

        default R visit(T t, StaticFieldPointer pointer) {
            return visitAny(t, pointer);
        }

        default R visit(T t, StaticMethodPointer pointer) {
            return visitAny(t, pointer);
        }
    }
}
