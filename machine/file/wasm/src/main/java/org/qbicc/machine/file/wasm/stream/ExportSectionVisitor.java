package org.qbicc.machine.file.wasm.stream;

/**
 *
 */
public class ExportSectionVisitor<E extends Exception> extends Visitor<E> {
    public void visitFunctionExport(final String name, final int funcIdx) throws E {
    }

    public void visitTableExport(final String name, final int tableIdx) throws E {
    }

    public void visitMemoryExport(final String name, final int memIdx) throws E {
    }

    public void visitGlobalExport(final String name, final int globalIdx) throws E {
    }
}
