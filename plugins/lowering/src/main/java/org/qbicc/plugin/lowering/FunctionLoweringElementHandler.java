package org.qbicc.plugin.lowering;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.object.Function;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;

/**
 * An element handler which lowers each element to a function program object.
 */
public final class FunctionLoweringElementHandler implements Consumer<ExecutableElement> {
    /**
     * Construct a new instance.
     */
    public FunctionLoweringElementHandler() {
    }

    @Override
    public void accept(ExecutableElement element) {
        if (element.hasMethodBody()) {
            // copy to a function.
            ClassContext classContext = element.getEnclosingType().getContext();
            CompilationContext compilationContext = classContext.getCompilationContext();
            BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>> copier = compilationContext.getCopier();
            LoadedTypeDefinition threadClass = compilationContext.getBootstrapClassContext().findDefinedType("java/lang/Thread").load();
            MethodBody original = element.getMethodBody();
            BasicBlock entryBlock = original.getEntryBlock();
            List<ParameterValue> paramValues;
            ParameterValue thisValue;
            BasicBlockBuilder builder = classContext.newBasicBlockBuilder(element);
            if (element instanceof FunctionElement) {
                // it is already a function with a C calling convention and ABI.
                paramValues = original.getParameterValues();
                thisValue = null;
            } else {
                // make a function with a Java calling convention and ABI.
                List<ParameterValue> origParamValues = original.getParameterValues();
                paramValues = new ArrayList<>(origParamValues.size() + 2);
                // first parameter is the current thread.
                paramValues.add(builder.parameter(threadClass.getClassType().getReference(), "thr", 0));
                if (! element.isStatic()) {
                    // instance methods get `this` as an implicit second parameter.
                    thisValue = original.getThisValue();
                    paramValues.add(thisValue);
                } else {
                    thisValue = null;
                }
                paramValues.addAll(origParamValues);
            }
            builder.startMethod(paramValues);
            Function function = compilationContext.getExactFunction(element);
            // perform the copy.
            BasicBlock copyBlock = Node.Copier.execute(entryBlock, builder, compilationContext, copier);
            builder.finish();
            function.replaceBody(MethodBody.of(copyBlock, Schedule.forMethod(copyBlock), thisValue, paramValues));
            element.replaceMethodBody(function.getBody());
        }
    }
}
