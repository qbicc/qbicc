package cc.quarkus.qcc.driver;

import java.util.function.BiFunction;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.definition.element.BasicElement;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ElementVisitor;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterElement;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class CopyStage implements BiFunction<CompilationContext, ElementVisitor<CompilationContext, Void>, ElementVisitor<CompilationContext, Void>> {

    private final BiFunction<CompilationContext, BasicBlock, BasicBlock> blockCopier;

    CopyStage(final BiFunction<CompilationContext, BasicBlock, BasicBlock> blockCopier) {
        this.blockCopier = blockCopier;
    }

    public ElementVisitor<CompilationContext, Void> apply(final CompilationContext context, final ElementVisitor<CompilationContext, Void> ignored) {
        return new ElementVisitorImpl(blockCopier);
    }

    static class ElementVisitorImpl implements ElementVisitor<CompilationContext, Void> {
        final BiFunction<CompilationContext, BasicBlock, BasicBlock> visitor;

        ElementVisitorImpl(final BiFunction<CompilationContext, BasicBlock, BasicBlock> visitor) {
            this.visitor = visitor;
        }

        public Void visitUnknown(final CompilationContext param, final BasicElement element) {
            throw Assert.unreachableCode();
        }

        public Void visit(final CompilationContext param, final ExecutableElement element) {
            MethodHandle methodBody = element.getMethodBody();
            if (methodBody != null) {
                methodBody.replaceMethodBody(transform(param, methodBody.getOrCreateMethodBody()));
            }
            return null;
        }

        private MethodBody transform(final CompilationContext context, final MethodBody body) {
            BasicBlock entryBlock = body.getEntryBlock();
            int cnt = body.getParameterCount();
            Value thisValue = body.getThisValue();
            Value[] paramValues = cnt == 0 ? Value.NO_VALUES : new Value[cnt];
            for (int i = 0; i < cnt; i ++) {
                paramValues[i] = body.getParameterValue(i);
            }

            BasicBlock newEntryBlock = visitor.apply(context, entryBlock);
            Schedule newSchedule = Schedule.forMethod(newEntryBlock);
            return MethodBody.of(newEntryBlock, newSchedule, thisValue, paramValues);
        }

        public Void visit(final CompilationContext param, final ConstructorElement element) {
            return visit(param, (ExecutableElement) element);
        }

        public Void visit(final CompilationContext param, final MethodElement element) {
            return visit(param, (ExecutableElement) element);
        }

        public Void visit(final CompilationContext param, final InitializerElement element) {
            return visit(param, (ExecutableElement) element);
        }

        public Void visit(final CompilationContext param, final FieldElement element) {
            return null;
        }

        public Void visit(final CompilationContext param, final ParameterElement element) {
            return null;
        }
    }
}
