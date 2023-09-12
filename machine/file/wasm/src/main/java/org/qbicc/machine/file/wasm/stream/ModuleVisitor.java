package org.qbicc.machine.file.wasm.stream;

/**
 * A streaming visitor for WASM modules.
 */
public class ModuleVisitor<E extends Exception> extends Visitor<E> {
    public TypeSectionVisitor<E> visitTypeSection() throws E {
        return null;
    }

    public ImportSectionVisitor<E> visitImportSection() throws E {
        return null;
    }

    public FunctionSectionVisitor<E> visitFunctionSection() throws E {
        return null;
    }

    public TableSectionVisitor<E> visitTableSection() throws E {
        return null;
    }

    public MemorySectionVisitor<E> visitMemorySection() throws E {
        return null;
    }

    public GlobalSectionVisitor<E> visitGlobalSection() throws E {
        return null;
    }

    public ExportSectionVisitor<E> visitExportSection() throws E {
        return null;
    }

    public StartSectionVisitor<E> visitStartSection() throws E {
        return null;
    }

    public ElementSectionVisitor<E> visitElementSection() throws E {
        return null;
    }

    public DataCountSectionVisitor<E> visitDataCountSection() throws E {
        return null;
    }

    public CodeSectionVisitor<E> visitCodeSection() throws E {
        return null;
    }

    public DataSectionVisitor<E> visitDataSection() throws E {
        return null;
    }

    public TagSectionVisitor<E> visitTagSection() throws E {
        return null;
    }
}
