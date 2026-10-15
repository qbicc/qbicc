package org.qbicc.plugin.reachability;

import org.qbicc.context.CompilationContext;
import org.qbicc.facts.Condition;
import org.qbicc.facts.Facts;
import org.qbicc.facts.core.ExecutableReachabilityFacts;
import org.qbicc.graph.atomic.AccessModes;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.InitializerElement;

/**
 * Core facts utility class.
 */
public final class ReachabilityFactsSetup {
    private ReachabilityFactsSetup() {}

    public static void setupAdd(CompilationContext ctxt) {
        Facts facts = Facts.get(ctxt);
        setupEarlyReachability(facts);
        setupReachability(facts);
    }

    public static void setupAnalyze(CompilationContext ctxt) {
        Facts facts = Facts.get(ctxt);
        setupEarlyReachability(facts);
        setupReachability(facts);
        setupValidate(facts);
    }

    public static void setupLower(CompilationContext ctxt) {
        Facts facts = Facts.get(ctxt);
        setupReachability(facts);
        setupValidate(facts);
    }

    public static void setupGenerate(CompilationContext ctxt) {
        Facts facts = Facts.get(ctxt);
        setupReachability(facts);
        setupValidate(facts);
    }

    private static void setupEarlyReachability(final Facts facts) {
        facts.registerAction(Condition.when(TypeReachabilityFacts.HAS_CLASS), (ltd, f) -> ReachabilityInfo.get(f.getCompilationContext()).getAnalysis().processReachableType(ltd, null));
    }

    private static void setupReachability(final Facts facts) {
        facts.registerInlineAction(Condition.when(TypeReachabilityFacts.IS_INSTANTIATED), ReachabilityFactsSetup::markTypeAsOnHeap);
        facts.registerInlineAction(Condition.when(TypeReachabilityFacts.IS_INSTANTIATED), (type, f) -> requestInitialization(type, f));
        facts.registerInlineAction(Condition.when(FieldReachabilityFacts.IS_READ), (field, f) -> {
            if (field.isStatic()) {
                requestInitialization(field.getEnclosingType().load(), f);
            }
        });
        facts.registerInlineAction(Condition.when(FieldReachabilityFacts.IS_WRITTEN), (field, f) -> {
            if (field.isStatic()) {
                requestInitialization(field.getEnclosingType().load(), f);
            }
        });
        facts.registerInlineAction(Condition.when(TypeReachabilityFacts.IS_ON_HEAP), ReachabilityFactsSetup::markEachMethodAsInstantiated);
        facts.registerInlineAction(Condition.when(InstanceMethodReachabilityFacts.IS_PROVISIONALLY_INVOKED), ReachabilityFactsSetup::markEnclosingTypeAsProvisionallyInvoked);
        facts.registerInlineAction(Condition.when(InstanceMethodReachabilityFacts.IS_PROVISIONALLY_DISPATCH_INVOKED), ReachabilityFactsSetup::markEnclosingTypeAsProvisionallyDispatched);
        facts.registerInlineAction(Condition.whenAll(InstanceMethodReachabilityFacts.EXACT_RECEIVER_IS_ON_HEAP, InstanceMethodReachabilityFacts.IS_PROVISIONALLY_INVOKED), ReachabilityFactsSetup::markAsInvoked);
        facts.registerInlineAction(Condition.whenAll(InstanceMethodReachabilityFacts.DISPATCH_RECEIVER_IS_ON_HEAP, InstanceMethodReachabilityFacts.IS_PROVISIONALLY_DISPATCH_INVOKED), ReachabilityFactsSetup::markAsDispatchInvoked);
        facts.registerInlineAction(Condition.when(ObjectReachabilityFacts.IS_REACHABLE), ReachabilityFactsSetup::markObjectTypeDefAsOnHeap);
        facts.registerAction(Condition.when(ExecutableReachabilityFacts.IS_INVOKED), ReachabilityFactsSetup::handleEnclosing);
        // TODO: generate DISPATCH_RECEIVER_IS_ON_HEAP
    }

    private static void setupValidate(final Facts facts) {
        facts.registerInlineAction(Condition.when(ExecutableReachabilityFacts.IS_INVOKED), ReachabilityFactsSetup::validateWasInvoked);
    }

    // actions

    private static void validateWasInvoked(ExecutableElement me, Facts facts) {
        if (!facts.hadFact(me, ExecutableReachabilityFacts.IS_INVOKED)) {
            facts.getCompilationContext().error(me, "Element cannot become reachable after being unreachable in the previous phase");
        }
    }

    private static void markEachMethodAsInstantiated(LoadedTypeDefinition ltd, Facts facts) {
        ltd.forEachNonStaticMethod(facts, ReachabilityFactsSetup::markMethodWithOnHeapReceiver);
    }

    private static void markMethodWithOnHeapReceiver(Facts facts, InstanceMethodElement me) {
        facts.discover(me, InstanceMethodReachabilityFacts.EXACT_RECEIVER_IS_ON_HEAP);
    }

    private static void markEnclosingTypeAsProvisionallyInvoked(InstanceMethodElement me, Facts facts) {
        facts.discover(me.getEnclosingType().load(), TypeReachabilityFacts.ELEMENT_IS_PROVISIONALLY_INVOKED);
    }

    private static void markEnclosingTypeAsProvisionallyDispatched(InstanceMethodElement me, Facts facts) {
        facts.discover(me.getEnclosingType().load(), TypeReachabilityFacts.ELEMENT_IS_PROVISIONALLY_DISPATCH_INVOKED);
    }

    private static void markAsInvoked(InstanceMethodElement me, Facts facts) {
        facts.discover(me, ExecutableReachabilityFacts.IS_INVOKED);
    }

    private static void markAsDispatchInvoked(InstanceMethodElement me, Facts facts) {
        facts.discover(me, InstanceMethodReachabilityFacts.IS_DISPATCH_INVOKED);
    }

    private static void handleEnclosing(final ExecutableElement e, final Facts facts) {
        LoadedTypeDefinition type = e.getEnclosingType().load();
        if (e instanceof ConstructorElement) {
            facts.discover(type, TypeReachabilityFacts.IS_INSTANTIATED, TypeReachabilityFacts.HAS_CLASS);
        } else {
            facts.discover(type, TypeReachabilityFacts.HAS_CLASS);
        }
        if (e.isStatic() && !(e instanceof InitializerElement)) {
            requestInitialization(type, facts);
        }
    }

    /**
     * Request initialization for a given class/type and its superclasses and interface default initializers.
     *
     * @param ltd the type definition to initialize
     * @param facts the facts context to discover initializers in
     */
    private static void requestInitialization(LoadedTypeDefinition ltd, Facts facts) {
        if (ltd.hasSuperClass()) {
            LoadedTypeDefinition superClass = ltd.getSuperClass();
            if (superClass != null) {
                requestInitialization(superClass, facts);
            }
        }
        maybeInitializeInterfaces(ltd, facts);
        InitializerElement initializer = ltd.getInitializer();
        if (initializer != null) {
            facts.discover(initializer, InitializerReachabilityFacts.NEEDS_INITIALIZATION);
        }
    }

    /**
     * Recursively initialize interfaces that declare default methods.
     *
     * @param ltd the type definition whose interfaces are inspected
     * @param facts the facts context
     */
    private static void maybeInitializeInterfaces(LoadedTypeDefinition ltd, Facts facts) {
        int cnt = ltd.getInterfaceCount();
        for (int i = 0; i < cnt; i ++) {
            LoadedTypeDefinition interfaceLtd = ltd.getInterface(i);
            if (interfaceLtd.declaresDefaultMethods()) {
                requestInitialization(interfaceLtd, facts);
            } else {
                maybeInitializeInterfaces(interfaceLtd, facts);
            }
        }
    }

    private static void markTypeAsOnHeap(LoadedTypeDefinition ltd, Facts facts) {
        facts.discover(ltd, TypeReachabilityFacts.IS_ON_HEAP);
    }

    private static void markObjectTypeDefAsOnHeap(VmObject obj, Facts facts) {
        facts.discover(obj.getVmClass().getTypeDefinition(), TypeReachabilityFacts.IS_ON_HEAP);
        // iterate fields of object to recursively mark them reachable
        PhysicalObjectType objectType = obj.getObjectType();
        LoadedTypeDefinition def = objectType.getDefinition().load();
        int fc = def.getFieldCount();
        for (int i = 0; i < fc; i++) {
            if (def.getField(i) instanceof InstanceFieldElement fe && fe.getType() instanceof ReferenceType) {
                VmObject nested = obj.getMemory().loadRef(fe.getOffset(), AccessModes.SinglePlain);
                if (nested != null) {
                    facts.discover(nested, ObjectReachabilityFacts.IS_REACHABLE);
                }
            }
        }
    }
}
