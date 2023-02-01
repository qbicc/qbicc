package org.qbicc.plugin.reflection;

import org.qbicc.context.CompilationContext;
import org.qbicc.facts.Condition;
import org.qbicc.facts.Facts;
import org.qbicc.plugin.reachability.ServiceLoaderAnalyzer;
import org.qbicc.plugin.reachability.TypeReachabilityFacts;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.descriptor.MethodDescriptor;

import java.util.List;

public class ReflectionFactsSetup {

    private ReflectionFactsSetup() {}

    public static void setupAdd(CompilationContext ctxt) {
        Facts facts = Facts.get(ctxt);
        facts.registerAction(Condition.when(TypeReachabilityFacts.HAS_CLASS), (ltd, f) -> {
            Reflection.get(ctxt).generateReflectiveData(ltd);

            List<LoadedTypeDefinition> providers = ServiceLoaderAnalyzer.get(ctxt).getProviders(ltd);
            for (LoadedTypeDefinition pc:providers) {
                if (!pc.isInterface()) {
                    int index = pc.findConstructorIndex(MethodDescriptor.VOID_METHOD_DESCRIPTOR);
                    if (index != -1) {
                        ConstructorElement ce = pc.getConstructor(index);
                        ReflectiveElementRegistry.get(ctxt).registerReflectiveConstructor(ce);
                    }
                }
            }
        });
    }
}
