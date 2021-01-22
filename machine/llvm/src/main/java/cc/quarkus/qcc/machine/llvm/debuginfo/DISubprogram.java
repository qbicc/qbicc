package cc.quarkus.qcc.machine.llvm.debuginfo;

import cc.quarkus.qcc.machine.llvm.LLValue;

import java.util.EnumSet;

/**
 *
 */
public interface DISubprogram extends MetadataNode {
    DISubprogram linkageName(String linkageName);
    DISubprogram scope(LLValue scope);
    DISubprogram location(LLValue file, int line, int scopeLine);
    DISubprogram isLocal(boolean isLocal);
    DISubprogram isDefinition(boolean isDefinition);
    DISubprogram containingType(LLValue containingType);
    DISubprogram virtuality(Virtuality virtuality, int virtualIndex);
    DISubprogram flags(EnumSet<DIFlags> flags, EnumSet<DISPFlags> spFlags);
    DISubprogram isOptimized(boolean isOptimized);
    DISubprogram templateParams(LLValue templateParams);
    DISubprogram declaration(LLValue declaration);
    DISubprogram retainedNodes(LLValue retainedNodes);
    DISubprogram thrownTypes(LLValue thrownTypes);

    DISubprogram comment(String comment);
}
