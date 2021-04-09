package org.qbicc.machine.llvm.impl;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.debuginfo.DIFlags;
import org.qbicc.machine.llvm.debuginfo.DISPFlags;
import org.qbicc.machine.llvm.debuginfo.DISubprogram;
import org.qbicc.machine.llvm.debuginfo.Virtuality;
import io.smallrye.common.constraint.Assert;

import java.io.IOException;
import java.util.EnumSet;

public class DISubprogramImpl extends AbstractMetadataNode implements DISubprogram {
    private final String name;
    private String linkageName;
    private AbstractValue scope;
    private AbstractValue file;
    private int line;
    private final AbstractValue type;
    private boolean isLocal;
    private boolean isDefinition = true;
    private int scopeLine;
    private AbstractValue containingType;
    private Virtuality virtuality = Virtuality.None;
    private int virtualIndex;
    private EnumSet<DIFlags> flags = EnumSet.noneOf(DIFlags.class);
    private EnumSet<DISPFlags> spFlags = EnumSet.noneOf(DISPFlags.class);
    private boolean isOptimized;
    private final AbstractValue unit;
    private AbstractValue templateParams;
    private AbstractValue declaration;
    private AbstractValue retainedNodes;
    private AbstractValue thrownTypes;

    DISubprogramImpl(final int index, final String name, final AbstractValue type, final AbstractValue unit) {
        super(index);
        this.name = name;
        this.type = type;
        this.unit = unit;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        super.appendTo(target);

        target.append("distinct !DISubprogram(name: ");
        appendEscapedString(target, name);

        if (linkageName != null) {
            target.append(", linkageName: ");
            appendEscapedString(target, linkageName);
        }

        if (scope != null) {
            target.append(", scope: ");
            scope.appendTo(target);
        }

        if (file != null) {
            target.append(", file: ");
            file.appendTo(target);
            target.append(", line: ");
            appendDecimal(target, line);
        }

        target.append(", type: ");
        type.appendTo(target);

        target.append(", isLocal: ");
        target.append(isLocal ? "true" : "false");

        target.append(", isDefinition: ");
        target.append(isDefinition ? "true" : "false");

        if (file != null) {
            target.append(", scopeLine: ");
            appendDecimal(target, scopeLine);
        }

        if (containingType != null) {
            target.append(", containingType: ");
            containingType.appendTo(target);
        }

        target.append(", virtuality: ");
        target.append(virtuality.name);

        if (virtuality != Virtuality.None) {
            target.append(", virtualIndex: ");
            appendDecimal(target, virtualIndex);
        }

        if (!flags.isEmpty()) {
            target.append(", flags: ");
            appendDiFlags(target, flags);
        }

        if (!spFlags.isEmpty()) {
            target.append(", spFlags: ");
            appendDiSpFlags(target, spFlags);
        }

        target.append(", isOptimized: ");
        target.append(isOptimized ? "true" : "false");

        target.append(", unit: ");
        unit.appendTo(target);

        if (templateParams != null) {
            target.append(", templateParams: ");
            templateParams.appendTo(target);
        }

        if (declaration != null) {
            target.append(", declaration: ");
            declaration.appendTo(target);
        }

        if (retainedNodes != null) {
            target.append(", retainedNodes: ");
            retainedNodes.appendTo(target);
        }

        if (thrownTypes != null) {
            target.append(", thrownTypes: ");
            thrownTypes.appendTo(target);
        }

        target.append(')');
        return appendTrailer(target);
    }

    public DISubprogram linkageName(final String linkageName) {
        this.linkageName = linkageName;
        return this;
    }

    public DISubprogram scope(final LLValue scope) {
        this.scope = (AbstractValue) scope;
        return this;
    }

    public DISubprogram location(final LLValue file, final int line, final int scopeLine) {
        this.file = (AbstractValue) file;
        this.line = line;
        this.scopeLine = scopeLine;
        return this;
    }

    public DISubprogram isLocal(final boolean isLocal) {
        this.isLocal = isLocal;
        return this;
    }

    public DISubprogram isDefinition(final boolean isDefinition) {
        this.isDefinition = isDefinition;
        return this;
    }

    public DISubprogram containingType(final LLValue containingType) {
        this.containingType = (AbstractValue) containingType;
        return this;
    }

    public DISubprogram virtuality(final Virtuality virtuality, final int virtualIndex) {
        Assert.checkNotNullParam("virtuality", virtuality);
        this.virtuality = virtuality;
        this.virtualIndex = virtualIndex;
        return this;
    }

    public DISubprogram flags(final EnumSet<DIFlags> flags, final EnumSet<DISPFlags> spFlags) {
        Assert.checkNotNullParam("flags", flags);
        Assert.checkNotNullParam("spFlags", spFlags);
        this.flags = flags;
        this.spFlags = spFlags;
        return this;
    }

    public DISubprogram isOptimized(final boolean isOptimized) {
        this.isOptimized = isOptimized;
        return this;
    }

    public DISubprogram templateParams(final LLValue templateParams) {
        this.templateParams = (AbstractValue) templateParams;
        return this;
    }

    public DISubprogram declaration(final LLValue declaration) {
        this.declaration = (AbstractValue) declaration;
        return this;
    }

    public DISubprogram retainedNodes(final LLValue retainedNodes) {
        this.retainedNodes = (AbstractValue) retainedNodes;
        return this;
    }

    public DISubprogram thrownTypes(final LLValue thrownTypes) {
        this.thrownTypes = (AbstractValue) thrownTypes;
        return this;
    }

    public DISubprogram comment(final String comment) {
        return (DISubprogram) super.comment(comment);
    }
}
