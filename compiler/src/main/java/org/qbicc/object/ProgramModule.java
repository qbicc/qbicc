package org.qbicc.object;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.DefinedTypeDefinition;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class ProgramModule {
    final DefinedTypeDefinition typeDefinition;
    final TypeSystem typeSystem;
    final LiteralFactory literalFactory;
    final Map<String, Section> sections = new ConcurrentHashMap<>();

    public ProgramModule(final DefinedTypeDefinition typeDefinition, final TypeSystem typeSystem, final LiteralFactory literalFactory) {
        this.typeDefinition = typeDefinition;
        this.typeSystem = Assert.checkNotNullParam("typeSystem", typeSystem);
        this.literalFactory = Assert.checkNotNullParam("literalFactory", literalFactory);
    }

    public DefinedTypeDefinition getTypeDefinition() {
        return typeDefinition;
    }

    public Section getSectionIfExists(String name) {
        return sections.get(name);
    }

    public Section getOrAddSection(String name) {
        return sections.computeIfAbsent(name, n -> new Section(n, literalFactory.literalOfSymbol(name, typeSystem.getVoidType().getPointer()), this));
    }

    public Iterable<Section> sections() {
        return sections.values();
    }
}
