package org.qbicc.machine.file.wasm.stream;

import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.ValType;

/**
 *
 */
public class ImportSectionVisitor<E extends Exception> extends Visitor<E> {
    public void visitFunctionImport(final String moduleName, final String name, final int typeIdx) throws E {
    }

    public void visitTableImport(final String moduleName, final String name, final RefType type, final int min) throws E {
    }

    public void visitTableImport(final String moduleName, final String name, final RefType type, final int min, final int max, boolean shared) throws E {
    }

    public void visitMemoryImport(final String moduleName, final String name, final int min) throws E {
    }

    public void visitMemoryImport(final String moduleName, final String name, final int min, final int max, boolean shared) throws E {
    }

    public void visitGlobalImport(final String moduleName, final String name, final ValType type, final Mutability mut) throws E {
    }
}
