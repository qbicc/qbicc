package cc.quarkus.qcc.driver;

import java.util.List;

import cc.quarkus.qcc.context.AdditivePhaseContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;

/**
 *
 */
final class AdditivePhaseContextImpl extends AbstractPhaseContext implements AdditivePhaseContext {
    AdditivePhaseContextImpl(final BaseContext baseContext, final List<BasicBlockBuilder.Factory> factories) {
        super(baseContext, factories);
    }

    public DefinedTypeDefinition findDefinedType(final String typeName) {
        return null;
    }

    public DefinedTypeDefinition resolveDefinedTypeLiteral(final TypeIdLiteral typeId) {
        return null;
    }
}
