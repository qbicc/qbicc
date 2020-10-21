package cc.quarkus.qcc.driver;

import java.util.List;

import cc.quarkus.qcc.context.AnalyticPhaseContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;

/**
 *
 */
final class AnalyticPhaseContextImpl extends AbstractPhaseContext implements AnalyticPhaseContext {
    AnalyticPhaseContextImpl(final BaseContext baseContext, final List<BasicBlockBuilder.Factory> factories) {
        super(baseContext, factories);
    }

    public DefinedTypeDefinition findDefinedType(final String typeName) {
        return null;
    }

    public DefinedTypeDefinition resolveDefinedTypeLiteral(final TypeIdLiteral typeId) {
        return null;
    }
}
