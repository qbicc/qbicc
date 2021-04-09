package org.qbicc.machine.llvm.impl;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.debuginfo.DICompileUnit;
import org.qbicc.machine.llvm.debuginfo.DebugEmissionKind;

import java.io.IOException;

final class DICompileUnitImpl extends AbstractMetadataNode implements DICompileUnit {
    private final String language;
    private final AbstractValue file;
    private String producer;
    private boolean isOptimized;
    private String flags;
    private int runtimeVersion;
    private String splitDebugFilename;
    private final DebugEmissionKind emissionKind;
    private AbstractValue enums;
    private AbstractValue retainedTypes;
    private AbstractValue globals;
    private AbstractValue imports;
    private AbstractValue macros;

    DICompileUnitImpl(final int index, final String language, final AbstractValue file, final DebugEmissionKind emissionKind) {
        super(index);

        this.language = language;
        this.file = file;
        this.emissionKind = emissionKind;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);

        target.append("distinct !DICompileUnit(language: ");
        target.append(language);

        target.append(", file: ");
        file.appendTo(target);

        if (producer != null) {
            target.append(", producer: ");
            appendEscapedString(target, producer);
        }

        target.append(", isOptimized: ");
        target.append(isOptimized ? "true" : "false");

        if (flags != null) {
            target.append(", flags: ");
            appendEscapedString(target, flags);
        }

        target.append(", runtimeVersion: ");
        appendDecimal(target, runtimeVersion);

        if (splitDebugFilename != null) {
            target.append(", splitDebugFilename: ");
            appendEscapedString(target, splitDebugFilename);
        }

        target.append(", emissionKind: ");
        target.append(emissionKind.name());

        if (enums != null) {
            target.append(", enums: ");
            enums.appendTo(target);
        }

        if (retainedTypes != null) {
            target.append(", retainedTypes: ");
            retainedTypes.appendTo(target);
        }

        if (globals != null) {
            target.append(", globals: ");
            globals.appendTo(target);
        }

        if (imports != null) {
            target.append(", imports: ");
            imports.appendTo(target);
        }

        if (macros != null) {
            target.append(", macros: ");
            macros.appendTo(target);
        }

        target.append(')');
        return appendTrailer(target);
    }

    public DICompileUnit producer(final String producer) {
        this.producer = producer;
        return this;
    }

    public DICompileUnit isOptimized(final boolean isOptimized) {
        this.isOptimized = isOptimized;
        return this;
    }

    public DICompileUnit flags(final String flags) {
        this.flags = flags;
        return this;
    }

    public DICompileUnit runtimeVersion(final int runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
        return this;
    }

    public DICompileUnit splitDebugFilename(final String splitDebugFilename) {
        this.splitDebugFilename = splitDebugFilename;
        return this;
    }

    public DICompileUnit enums(final LLValue enums) {
        this.enums = (AbstractValue) enums;
        return this;
    }

    public DICompileUnit retainedTypes(final LLValue retainedTypes) {
        this.retainedTypes = (AbstractValue) retainedTypes;
        return this;
    }

    public DICompileUnit globals(final LLValue globals) {
        this.globals = (AbstractValue) globals;
        return this;
    }

    public DICompileUnit imports(final LLValue imports) {
        this.imports = (AbstractValue) imports;
        return this;
    }

    public DICompileUnit macros(final LLValue macros) {
        this.macros = (AbstractValue) macros;
        return this;
    }

    public DICompileUnit comment(final String comment) {
        return (DICompileUnit) super.comment(comment);
    }
}
