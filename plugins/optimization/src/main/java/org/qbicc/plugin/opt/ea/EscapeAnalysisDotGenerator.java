package org.qbicc.plugin.opt.ea;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.GraphGenConfig;
import org.qbicc.driver.Phase;
import org.qbicc.plugin.dot.DotGenerator;
import org.qbicc.type.definition.element.BasicElement;
import org.qbicc.type.definition.element.ExecutableElement;

import java.util.function.Consumer;

public class EscapeAnalysisDotGenerator implements Consumer<CompilationContext> {

    private final DotGenerator dotGenerator;

    public EscapeAnalysisDotGenerator(GraphGenConfig graphGenConfig) {
        this.dotGenerator = new DotGenerator(Phase.ANALYZE, "analyze-inter", graphGenConfig).addVisitorFactory(EscapeAnalysisDotVisitor::new);
    }

    @Override
    public void accept(CompilationContext ctxt) {
        final EscapeAnalysisState state = EscapeAnalysisState.get(ctxt);
        state.getMethodsVisited().forEach(element -> process(element, ctxt));
    }

    private void process(ExecutableElement element, CompilationContext ctxt) {
        dotGenerator.visitUnknown(ctxt, (BasicElement) element);
    }
}
