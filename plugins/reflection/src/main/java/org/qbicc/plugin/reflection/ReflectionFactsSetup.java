package org.qbicc.plugin.reflection;

import org.qbicc.context.CompilationContext;
import org.qbicc.facts.Condition;
import org.qbicc.facts.Facts;
import org.qbicc.plugin.reachability.TypeReachabilityFacts;

public class ReflectionFactsSetup {

    private ReflectionFactsSetup() {}

    public static void setupAdd(CompilationContext ctxt) {
        Facts facts = Facts.get(ctxt);
        facts.registerAction(Condition.when(TypeReachabilityFacts.HAS_CLASS), (ltd, f) -> Reflection.get(ctxt).generateReflectiveData(ltd));
    }
}
