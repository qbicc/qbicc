package org.qbicc.plugin.instanceofcheckcast;

import java.util.function.Consumer;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.reachability.RTAInfo;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.InitializerElement;

/**
 * Register all reachable class's class initalizer methods as entrypoints
 * so they survive to be lowered into functions.
 * This is required as we lose info after ReachabilityBlockBuilder required
 * to otherwise keep them alive.
 * 
 * Eventually, this will need to skip an class initializer that was run as
 * part of the build process.
 */
public class ClassInitializerRegister implements Consumer<CompilationContext>  {

    @Override
    public void accept(CompilationContext ctxt) {
        RTAInfo rtaInfo = RTAInfo.get(ctxt);
		
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        LoadedTypeDefinition jlo = jloDef.load();

        // Code below uses #hasMethodBody() to filter out the empty
        // class initializers and to avoid processing internal arrays

        // Register Object's <clinit> if it exists
        InitializerElement object_clinit = jlo.getInitializer();
        if (object_clinit != null && object_clinit.hasMethodBody()) {
            ctxt.registerEntryPoint(object_clinit);
        }
        // Visit all live subclasses and register their <clinit>
		rtaInfo.visitLiveSubclassesPreOrder(jlo, sc -> {
            InitializerElement initializer = sc.getInitializer();
            if (initializer != null && initializer.hasMethodBody()) {
                ctxt.registerEntryPoint(initializer);
            }
        });
        // Visit all live interfaces and register their <clinit>
        rtaInfo.visitLiveInterfaces(sc -> {
            InitializerElement initializer = sc.getInitializer();
            if (initializer != null && initializer.hasMethodBody()) {
                ctxt.registerEntryPoint(initializer);
            }
        });
    }
}