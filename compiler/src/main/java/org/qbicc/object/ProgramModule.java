package org.qbicc.object;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.common.constraint.Assert;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.DefinedTypeDefinition;

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
        return sections.computeIfAbsent(name, n -> new Section(n, typeSystem.getVoidType(), this));
    }

    public Iterable<Section> sections() {
        Section[] array = this.sections.values().toArray(Section[]::new);
        Arrays.sort(array, Comparator.comparing(ProgramObject::getName));
        return List.of(array);
    }
}
