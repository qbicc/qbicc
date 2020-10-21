package cc.quarkus.qcc.driver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cc.quarkus.qcc.context.AdditivePhaseContext;
import cc.quarkus.qcc.context.AnalyticPhaseContext;
import cc.quarkus.qcc.context.Diagnostic;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.interpreter.JavaVM;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.CCompiler;
import cc.quarkus.qcc.tool.llvm.LlvmTool;
import cc.quarkus.qcc.type.TypeSystem;

/**
 * A simple driver to run all the stages of compilation.
 */
public class Driver {

    final BaseContext baseContext;
    final AdditivePhaseContext additivePhaseContext;
    final AnalyticPhaseContext analyticPhaseContext;
    final List<Consumer<? super AdditivePhaseContext>> preAdditiveHooks;
    final List<Consumer<? super AdditivePhaseContext>> postAdditiveHooks;
    final List<Consumer<? super AnalyticPhaseContext>> preAnalyticHooks;
    final List<Consumer<? super AnalyticPhaseContext>> postAnalyticHooks;

    /*
        Reachability (Run Time)

        A class is reachable when any instance of that class can exist at run time.  This can happen only
        when either its constructor is reachable at run time, or when an instance of that class
        is reachable via the heap from an entry point.  The existence of a variable of a class type
        is not sufficient to cause the class to be reachable (the variable could be null-only) - there
        must be an actual value.

        A non-virtual method is reachable only when it can be directly called by another reachable method.

        A virtual method is reachable when it (or a method that the virtual method overrides) can be called
        by a reachable method and when its class is reachable.

        A static field is reachable when it can be accessed by a reachable method.
     */

    Driver(final Builder builder) {
        // type system
        final TypeSystem typeSystem = builder.typeSystem;
        final LiteralFactory literalFactory = LiteralFactory.create(typeSystem);

        // this is shared by all phases
        final BaseContext baseContext = new BaseContext(typeSystem, literalFactory);
        this.baseContext = baseContext;

        // phase contexts

        ArrayList<BasicBlockBuilder.Factory> additivePhaseFactories = new ArrayList<>();
        for (List<BasicBlockBuilder.Factory> list : builder.factories.getOrDefault(Phase.ADDITIVE, Map.of()).values()) {
            additivePhaseFactories.addAll(list);
        }
        // last of all...
        additivePhaseFactories.add((ctxt, delegate) -> BasicBlockBuilder.simpleBuilder(ctxt.getTypeSystem()));
        Collections.reverse(additivePhaseFactories);
        additivePhaseFactories.trimToSize();
        additivePhaseContext = new AdditivePhaseContextImpl(baseContext, additivePhaseFactories);

        ArrayList<BasicBlockBuilder.Factory> analyticPhaseFactories = new ArrayList<>();
        for (List<BasicBlockBuilder.Factory> list : builder.factories.getOrDefault(Phase.ANALYTIC, Map.of()).values()) {
            analyticPhaseFactories.addAll(list);
        }
        // last of all...
        analyticPhaseFactories.add((ctxt, delegate) -> BasicBlockBuilder.simpleBuilder(ctxt.getTypeSystem()));
        Collections.reverse(analyticPhaseFactories);
        analyticPhaseFactories.trimToSize();
        analyticPhaseContext = new AnalyticPhaseContextImpl(baseContext, analyticPhaseFactories);

        // hooks

        preAdditiveHooks = List.copyOf(builder.preAdditiveHooks);
        postAdditiveHooks = List.copyOf(builder.postAdditiveHooks);
        preAnalyticHooks = List.copyOf(builder.preAnalyticHooks);
        postAnalyticHooks = List.copyOf(builder.postAnalyticHooks);
    }

    /**
     * Execute the compilation.
     *
     * @return {@code true} if compilation succeeded, {@code false} otherwise
     */
    public boolean execute() {
        // TODO: start VM *here*
        try {
            for (Consumer<? super AdditivePhaseContext> hook : preAdditiveHooks) {
                try {
                    hook.accept(additivePhaseContext);
                } catch (Throwable t) {
                    baseContext.msg(new Diagnostic(null, null, Diagnostic.Level.ERROR, "Pre-additive hook failed: %s", t));
                }
                if (baseContext.errors() > 0) {
                    // bail out
                    return false;
                }
            }
            // TODO: trace *here*
            if (baseContext.errors() > 0) {
                // bail out
                return false;
            }
            for (Consumer<? super AdditivePhaseContext> hook : postAdditiveHooks) {
                try {
                    hook.accept(additivePhaseContext);
                } catch (Throwable t) {
                    baseContext.msg(new Diagnostic(null, null, Diagnostic.Level.ERROR, "Post-additive hook failed: %s", t));
                }
                if (baseContext.errors() > 0) {
                    // bail out
                    return false;
                }
            }
        } finally {
            // TODO: exit VM *here*
        }
        if (baseContext.errors() > 0) {
            // bail out
            return false;
        }
        for (Consumer<? super AnalyticPhaseContext> hook : preAnalyticHooks) {
            try {
                hook.accept(analyticPhaseContext);
            } catch (Throwable t) {
                baseContext.msg(new Diagnostic(null, null, Diagnostic.Level.ERROR, "Pre-analytic hook failed: %s", t));
            }
            if (baseContext.errors() > 0) {
                // bail out
                return false;
            }
        }
        // TODO: re-trace/copy classes *here*
        if (baseContext.errors() > 0) {
            // bail out
            return false;
        }
        for (Consumer<? super AnalyticPhaseContext> hook : postAnalyticHooks) {
            try {
                hook.accept(analyticPhaseContext);
            } catch (Throwable t) {
                baseContext.msg(new Diagnostic(null, null, Diagnostic.Level.ERROR, "Post-analytic hook failed: %s", t));
            }
            if (baseContext.errors() > 0) {
                // bail out
                return false;
            }
        }
        // TODO: trace/emit *here*
        return baseContext.errors() == 0;
    }

    /**
     * Construct a new builder.
     *
     * @return the new builder (not {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    public Iterable<Diagnostic> getDiagnostics() {
        return baseContext.getDiagnostics();
    }

    public static final class Builder {
        final List<Path> bootModules = new ArrayList<>();
        final Map<Phase, Map<Stage, List<BasicBlockBuilder.Factory>>> factories = new EnumMap<>(Phase.class);
        final List<Consumer<? super AdditivePhaseContext>> preAdditiveHooks = new ArrayList<>();
        final List<Consumer<? super AdditivePhaseContext>> postAdditiveHooks = new ArrayList<>();
        final List<Consumer<? super AnalyticPhaseContext>> preAnalyticHooks = new ArrayList<>();
        final List<Consumer<? super AnalyticPhaseContext>> postAnalyticHooks = new ArrayList<>();

        Platform targetPlatform;
        TypeSystem typeSystem;
        JavaVM vm;
        CCompiler toolChain;
        LlvmTool llvmTool;

        Builder() {}

        public Builder addBootModule(Path modulePath) {
            if (modulePath != null) {
                bootModules.add(modulePath);
            }
            return this;
        }

        public Builder addBlockBuilderFactory(Phase phase, Stage stage, BasicBlockBuilder.Factory factory) {
            factories.computeIfAbsent(phase, p -> new EnumMap<>(Stage.class)).computeIfAbsent(stage, s -> new ArrayList<>()).add(factory);
            return this;
        }

        public Builder addPreAdditiveHook(Consumer<? super AdditivePhaseContext> hook) {
            if (hook != null) {
                preAdditiveHooks.add(hook);
            }
            return this;
        }

        public Builder addPostAdditiveHook(Consumer<? super AdditivePhaseContext> hook) {
            if (hook != null) {
                preAdditiveHooks.add(hook);
            }
            return this;
        }

        public Builder addPreAnalyticHook(Consumer<? super AnalyticPhaseContext> hook) {
            if (hook != null) {
                preAnalyticHooks.add(hook);
            }
            return this;
        }

        public Builder addPostAnalyticHook(Consumer<? super AnalyticPhaseContext> hook) {
            if (hook != null) {
                preAnalyticHooks.add(hook);
            }
            return this;
        }

        public Platform getTargetPlatform() {
            return targetPlatform;
        }

        public Builder setTargetPlatform(final Platform targetPlatform) {
            this.targetPlatform = targetPlatform;
            return this;
        }

        public TypeSystem getTypeSystem() {
            return typeSystem;
        }

        public Builder setTypeSystem(final TypeSystem typeSystem) {
            this.typeSystem = typeSystem;
            return this;
        }

        public JavaVM getVm() {
            return vm;
        }

        public Builder setVm(final JavaVM vm) {
            this.vm = vm;
            return this;
        }

        public CCompiler getToolChain() {
            return toolChain;
        }

        public Builder setToolChain(final CCompiler toolChain) {
            this.toolChain = toolChain;
            return this;
        }

        public LlvmTool getLlvmTool() {
            return llvmTool;
        }

        public Builder setLlvmTool(final LlvmTool llvmTool) {
            this.llvmTool = llvmTool;
            return this;
        }

        public Driver build() {
            return new Driver(this);
        }
    }
}
