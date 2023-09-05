package org.qbicc.machine.file.wasm.stream;

/**
 *
 */
public class ElementSectionVisitor<E extends Exception> extends Visitor<E> {
    public ElementVisitor<E> visitPassiveElement() throws E {
        return null;
    }

    public ActiveElementVisitor<E> visitActiveElement() throws E {
        return null;
    }

    public ElementVisitor<E> visitDeclarativeElement() throws E {
        return null;
    }
}
