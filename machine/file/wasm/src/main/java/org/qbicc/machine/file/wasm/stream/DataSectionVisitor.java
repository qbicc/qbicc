package org.qbicc.machine.file.wasm.stream;

/**
 *
 */
public class DataSectionVisitor<E extends Exception> extends Visitor<E> {
    public DataVisitor<E> visitPassiveSegment() throws E {
        return null;
    }

    public ActiveDataVisitor<E> visitActiveSegment(int memIdx) throws E {
        return null;
    }
}
