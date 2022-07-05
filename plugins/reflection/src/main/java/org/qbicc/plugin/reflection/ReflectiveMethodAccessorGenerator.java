package org.qbicc.plugin.reflection;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.reachability.ReachabilityRoots;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;

import java.util.function.Consumer;

/**
 * Recognizes any ExecutableElement that needs to be reflectively invokable at runtime
 * and ensures that the necessary Accessor code is generated. These elements may have
 * either been annotated with @ReflectivelyAccessed or been programmatically registered
 * using some other mechanism (eg GraalVM BuildFeature) during an ADD preHook.
 */
public class ReflectiveMethodAccessorGenerator implements Consumer<ExecutableElement> {
    public ReflectiveMethodAccessorGenerator() {
    }

    @Override
    public void accept(ExecutableElement executableElement) {
        CompilationContext ctxt = executableElement.getEnclosingType().getContext().getCompilationContext();
        if (ReachabilityRoots.get(ctxt).getReflectiveMethods().contains(executableElement)) {
            Reflection r = Reflection.get(ctxt);
            if (executableElement instanceof MethodElement me) {
                r.makeAvailableForRuntimeReflection(me);
            } else if (executableElement instanceof ConstructorElement ce) {
                r.makeAvailableForRuntimeReflection(ce);
            }
        }
    }
}
