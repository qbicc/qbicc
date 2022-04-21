package org.qbicc.plugin.correctness;

import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * An element handler which detects methods annotated with BuildTimeOnly
 * and rewrites their method body to call a runtime error method.
 */
public class BuildTimeOnlyElementHandler implements Consumer<ExecutableElement> {

    public BuildTimeOnlyElementHandler() {
    }

    @Override
    public void accept(ExecutableElement element) {
        // rewrite the method body
        if (element.hasMethodBody() && element.hasAllModifiersOf(ClassFile.I_ACC_BUILD_TIME_ONLY)) {
            ClassContext classContext = element.getEnclosingType().getContext();
            CompilationContext compilationContext = classContext.getCompilationContext();
            BasicBlockBuilder builder = classContext.newBasicBlockBuilder(element);
            MethodBody original = element.getMethodBody();

            builder.startMethod(original.getParameterValues());
            BlockLabel entryLabel = new BlockLabel();
            builder.begin(entryLabel);
            try {
                MethodElement helper = RuntimeMethodFinder.get(compilationContext).getMethod("raiseUnreachableCodeError");
                Literal msg = compilationContext.getLiteralFactory().literalOf(element.toString(), classContext.findDefinedType("java/lang/String").load().getObjectType().getReference());
                builder.callNoReturn(builder.staticMethod(helper), List.of(msg));
            } catch (BlockEarlyTermination ignored) {}
            builder.finish();
            BasicBlock entryBlock = BlockLabel.getTargetOf(entryLabel);
            element.replaceMethodBody(MethodBody.of(entryBlock, Schedule.forMethod(entryBlock), original.getThisValue(), original.getParameterValues()));
        }
    }
}
