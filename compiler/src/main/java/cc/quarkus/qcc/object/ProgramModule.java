package cc.quarkus.qcc.object;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.type.TypeSystem;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class ProgramModule {
    final TypeSystem typeSystem;
    final LiteralFactory literalFactory;
    final Map<String, Section> sections = new ConcurrentHashMap<>();

    public ProgramModule(final TypeSystem typeSystem, final LiteralFactory literalFactory) {
        this.typeSystem = Assert.checkNotNullParam("typeSystem", typeSystem);
        this.literalFactory = Assert.checkNotNullParam("literalFactory", literalFactory);
    }

    public Section getSectionIfExists(String name) {
        return sections.get(name);
    }

    public Section getOrAddSection(String name) {
        return sections.computeIfAbsent(name, n -> new Section(n, literalFactory.literalOfSymbol(name, typeSystem.getVoidType().getPointer()), this));
    }
}
