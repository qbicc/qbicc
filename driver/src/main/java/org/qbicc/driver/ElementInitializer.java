package org.qbicc.driver;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InitializerElement;

/**
 *
 */
public class ElementInitializer implements Consumer<ExecutableElement> {
    public ElementInitializer() {
    }

    @Override
    public void accept(ExecutableElement element) {
        if (element instanceof InitializerElement && element.hasNoModifiersOf(ClassFile.I_ACC_RUN_TIME)) {
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
}
