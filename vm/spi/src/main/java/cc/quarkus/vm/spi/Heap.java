package cc.quarkus.vm.spi;

import static cc.quarkus.c_native.stdc.Stddef.*;
import static cc.quarkus.c_native.api.CNative.*;

import java.util.function.Consumer;


/**
 *
 */
public interface Heap<Oop extends object> {
    void forEach(Consumer<Oop> oopHandler);

    Oop pin(Oop original);

    Oop unpin(Oop pinned);

    ptr<?> ptr(Oop pinned);

    size_t sizeOfObject(Oop oop);

    Oop allocate(size_t size);

    Oop readOopField(Oop oop, ptrdiff_t offset) throws NullPointerException;
    long readLongField(Oop oop, ptrdiff_t offset) throws NullPointerException;
    int readIntField(Oop oop, ptrdiff_t offset) throws NullPointerException;
    short readShortField(Oop oop, ptrdiff_t offset) throws NullPointerException;
    byte readByteField(Oop oop, ptrdiff_t offset) throws NullPointerException;
    char readCharField(Oop oop, ptrdiff_t offset) throws NullPointerException;
    boolean readBooleanField(Oop oop, ptrdiff_t offset) throws NullPointerException;

    void onThreadAttach();
    void onThreadDetach();

    void onThreadCreate();
    void onThreadDestroy();
}
