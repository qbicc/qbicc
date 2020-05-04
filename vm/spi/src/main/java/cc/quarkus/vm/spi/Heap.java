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

    size_t sizeOf(Oop obj);

    Oop allocate(size_t size);

    Oop loadOopFromObj(Oop obj, ptrdiff_t offset);

    void onThreadAttach();
    void onThreadDetach();

    void onThreadCreate();
    void onThreadDestroy();
}
