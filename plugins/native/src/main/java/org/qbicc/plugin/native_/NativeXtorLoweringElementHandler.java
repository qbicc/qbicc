package org.qbicc.plugin.native_;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.object.Function;
import org.qbicc.object.ProgramModule;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Lower declared ctors/dtors to the program module.
 */
public class NativeXtorLoweringElementHandler implements Consumer<ExecutableElement> {
    private final Set<DefinedTypeDefinition> visited = ConcurrentHashMap.newKeySet();

    public NativeXtorLoweringElementHandler() {
    }

    @Override
    public void accept(ExecutableElement executableElement) {
        if (executableElement.hasMethodBody()) {
            DefinedTypeDefinition enclosingType = executableElement.getEnclosingType();
            if (visited.add(enclosingType)) {
                ClassContext classContext = enclosingType.getContext();
                CompilationContext ctxt = classContext.getCompilationContext();
                ProgramModule programModule = ctxt.getProgramModule(enclosingType);
                if (programModule == null) {
                    ctxt.error(executableElement, "No program module for reachable element");
                    return;
                }
                NativeInfo nativeInfo = NativeInfo.get(ctxt);
                List<NativeInfo.FunctionAndPriority> xtors = nativeInfo.getGlobalConstructors(enclosingType);
                for (NativeInfo.FunctionAndPriority xtor : xtors) {
                    Function fn = ctxt.getExactFunctionIfExists(xtor.function());
                    if (fn == null) {
                        ctxt.error(executableElement, "No lowered function for entry point element");
                        continue;
                    }
                    programModule.addConstructor(fn, xtor.priority());
                }
                xtors = nativeInfo.getGlobalDestructors(enclosingType);
                for (NativeInfo.FunctionAndPriority xtor : xtors) {
                    Function fn = ctxt.getExactFunctionIfExists(xtor.function());
                    if (fn == null) {
                        ctxt.error(executableElement, "No lowered function for entry point element");
                        continue;
                    }
                    programModule.addDestructor(fn, xtor.priority());
                }
            }
        }
    }
}
