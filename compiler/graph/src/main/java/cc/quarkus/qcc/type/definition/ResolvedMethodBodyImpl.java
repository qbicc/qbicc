package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.GraphBuilder;
import cc.quarkus.qcc.graph.ParameterValue;
import org.objectweb.asm.tree.MethodNode;

final class ResolvedMethodBodyImpl implements ResolvedMethodBody {

    private final VerifiedMethodBodyImpl delegate;
    private final List<ParameterValue> parameters;
    private final BasicBlock entryBlock;

    ResolvedMethodBodyImpl(final VerifiedMethodBodyImpl delegate) {
        this.delegate = delegate;
        DefinedMethodDefinitionImpl methodDefinition = delegate.getMethodDefinition();
        MethodNode node = ((VerifiedTypeDefinitionImpl) methodDefinition.getEnclosingTypeDefinition().verify()).getMethodNode(methodDefinition.getIndex());
        GraphBuilder graphBuilder = new GraphBuilder(methodDefinition.getModifiers(), methodDefinition.getName(), node.desc, methodDefinition.getEnclosingTypeDefinition().verify().resolve());
        node.accept(graphBuilder);
        parameters = graphBuilder.getParameters();
        entryBlock = graphBuilder.getEntryBlock();
    }

    @Override
    public DefinedMethodDefinitionImpl getMethodDefinition() {
        return delegate.getMethodDefinition();
    }

    @Override
    public List<ParameterValue> getParameters() {
        return this.parameters;
    }

    @Override
    public BasicBlock getEntryBlock() {
        return this.entryBlock;
    }
}
