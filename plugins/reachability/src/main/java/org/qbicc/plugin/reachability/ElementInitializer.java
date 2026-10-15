package org.qbicc.plugin.reachability;

import org.qbicc.context.CompilationContext;
import org.qbicc.facts.Condition;
import org.qbicc.facts.Facts;
import org.qbicc.facts.core.ExecutableReachabilityFacts;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.InitializerElement;

/**
 *
 */
public class ElementInitializer {
    private ElementInitializer() {}

    private static void doInit(InitializerElement element) {
        if (element.hasNoModifiersOf(ClassFile.I_ACC_RUN_TIME)) {
            if (element.hasMethodBody()) {
                Vm vm = Vm.requireCurrent();
                VmClass vmClass = element.getEnclosingType().load().getVmClass();
                try {
                    vm.initialize(vmClass);
                } catch (Thrown thrown) {
                    VmThrowable throwable = thrown.getThrowable();
                    String className = throwable.getVmClass().getName();
                    String message = throwable.getMessage();
                    CompilationContext ctxt = vm.getCompilationContext();
                    String warningMessage;
                    if (message != null) {
                        warningMessage = String.format("Failed to initialize %s: %s: %s", vmClass.getName(), className, message);
                    } else {
                        warningMessage = String.format("Failed to initialize %s: %s", vmClass.getName(), className);
                    }
                    VmThrowable cause = throwable.getCause();
                    while (cause != null) {
                        message = cause.getMessage();
                        if (message != null) {
                            warningMessage += String.format(" caused by %s: %s", cause.getVmClass().getName(), message);
                        } else {
                            warningMessage += String.format(" caused by %s", cause.getVmClass().getName());
                        }
                        cause = cause.getCause();
                    }
                    ctxt.warning(warningMessage);
                }
            }
        }
    }

    public static void register(final CompilationContext ctxt) {
        Facts facts = Facts.get(ctxt);
        facts.registerInlineAction(Condition.when(InitializerReachabilityFacts.NEEDS_INITIALIZATION), (init, f) -> f.discover(init, ExecutableReachabilityFacts.NEEDS_COMPILATION));
        facts.registerAction(Condition.whenAll(InitializerReachabilityFacts.NEEDS_INITIALIZATION, ExecutableReachabilityFacts.IS_COMPILED), ElementInitializer::doInit);
    }
}
