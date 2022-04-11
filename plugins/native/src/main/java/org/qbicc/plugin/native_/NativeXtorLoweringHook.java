package org.qbicc.plugin.native_;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.object.Function;
import org.qbicc.object.GlobalXtor;
import org.qbicc.object.ProgramModule;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.FunctionElement;

/**
 * Lower declared ctors/dtors to the program module.
 */
public final class NativeXtorLoweringHook {

    private NativeXtorLoweringHook() {}

    public static void process(CompilationContext ctxt) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        processOneList(ctxt, nativeInfo.getGlobalConstructors(), ProgramModule::addConstructor);
        processOneList(ctxt, nativeInfo.getGlobalDestructors(), ProgramModule::addDestructor);
    }

    @FunctionalInterface
    interface AddMethod {
        GlobalXtor add(ProgramModule pm, Function fn, int priority);
    }

    private static void processOneList(CompilationContext ctxt, List<NativeInfo.FunctionAndPriority> xtors, AddMethod method) {
        for (NativeInfo.FunctionAndPriority xtor : xtors) {
            FunctionElement functionElement = xtor.function();
            DefinedTypeDefinition enclosingType = functionElement.getEnclosingType();
            Function fn = ctxt.getExactFunctionIfExists(functionElement);
            if (fn == null) {
                ctxt.error(functionElement, "No lowered function for entry point element");
                continue;
            }
            ProgramModule programModule = ctxt.getOrAddProgramModule(enclosingType);
            if (programModule == null) {
                ctxt.error(functionElement, "No program module for reachable element");
                continue;
            }
            method.add(programModule, fn, xtor.priority());
        }
    }
}
