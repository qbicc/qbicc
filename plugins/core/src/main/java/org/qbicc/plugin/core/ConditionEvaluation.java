package org.qbicc.plugin.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Locatable;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.OS;
import org.qbicc.machine.arch.Platform;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.ArrayAnnotationValue;
import org.qbicc.type.annotation.ClassAnnotationValue;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * An evaluator for determining whether conditional annotations should be applied.
 */
public final class ConditionEvaluation {
    private static final AttachmentKey<ConditionEvaluation> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;

    private final Map<DefinedTypeDefinition, Boolean> cachedResults = new ConcurrentHashMap<>();
    private final Map<String, Boolean> predefinedResults;

    private ConditionEvaluation(final CompilationContext ctxt) {
        this.ctxt = ctxt;
        Platform platform = ctxt.getPlatform();
        predefinedResults = Map.ofEntries(
            // Operating systems
            Map.entry("org/qbicc/runtime/Build$Target$IsAix", Boolean.FALSE /* todo */),
            Map.entry("org/qbicc/runtime/Build$Target$IsApple", Boolean.valueOf(platform.getOs() == OS.DARWIN)),
            Map.entry("org/qbicc/runtime/Build$Target$IsLinux", Boolean.valueOf(platform.getOs() == OS.LINUX)),
            Map.entry("org/qbicc/runtime/Build$Target$IsMacOs", Boolean.valueOf(platform.getOs() == OS.DARWIN)),
            Map.entry("org/qbicc/runtime/Build$Target$IsPosix", Boolean.valueOf(platform.getOs() != OS.WIN32)),
            Map.entry("org/qbicc/runtime/Build$Target$IsUnix", Boolean.valueOf(platform.getOs() != OS.WIN32)),
            Map.entry("org/qbicc/runtime/Build$Target$IsWasi", Boolean.valueOf(platform.getOs() == OS.WASI)),

            // CPU architectures
            Map.entry("org/qbicc/runtime/Build$Target$IsAmd64", Boolean.valueOf(platform.getCpu() == Cpu.X86_64)),
            Map.entry("org/qbicc/runtime/Build$Target$IsArm", Boolean.valueOf(platform.getCpu() == Cpu.ARM)),
            Map.entry("org/qbicc/runtime/Build$Target$IsWasm", Boolean.valueOf(platform.getCpu() == Cpu.WASM32))
        );
    }

    public static ConditionEvaluation get(CompilationContext ctxt) {
        ConditionEvaluation self = ctxt.getAttachment(KEY);
        if (self == null) {
            ConditionEvaluation appearing = ctxt.putAttachmentIfAbsent(KEY, self = new ConditionEvaluation(ctxt));
            if (appearing != null) {
                self = appearing;
            }
        }
        return self;
    }

    public boolean evaluateConditions(ClassContext classContext, Locatable locatable, Annotation conditionalAnnotation) {
        if (conditionalAnnotation == null) {
            return true;
        }
        ArrayAnnotationValue whenValue = (ArrayAnnotationValue) conditionalAnnotation.getValue("when");
        ArrayAnnotationValue unlessValue = (ArrayAnnotationValue) conditionalAnnotation.getValue("unless");

        if (whenValue != null) {
            for (int i = 0; i < whenValue.getElementCount(); i ++) {
                if (! evaluate(classContext, locatable, (ClassAnnotationValue) whenValue.getValue(i), true)) {
                    return false;
                }
            }
        }
        if (unlessValue != null) {
            for (int i = 0; i < unlessValue.getElementCount(); i ++) {
                if (evaluate(classContext, locatable, (ClassAnnotationValue) unlessValue.getValue(i), false)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean evaluate(final ClassContext classContext, final Locatable locatable, final ClassAnnotationValue value, boolean errorCondition) {
        TypeDescriptor rawDescriptor = value.getDescriptor();
        if (rawDescriptor instanceof ClassTypeDescriptor descriptor) {
            String internalName = descriptor.getPackageName() + "/" + descriptor.getClassName();
            if (predefinedResults.containsKey(internalName)) {
                return predefinedResults.get(internalName).booleanValue();
            }
            String niceName = internalName.replace('/', '.');
            DefinedTypeDefinition definedType = classContext.findDefinedType(internalName);
            if (definedType == null) {
                ctxt.error(locatable.getLocation(), "Annotation conditional class `%s` could not be found", niceName);
                return errorCondition;
            }
            Boolean result = cachedResults.get(definedType);
            if (result != null) {
                return result.booleanValue();
            }
            // otherwise, compute
            LoadedTypeDefinition loaded = definedType.load();
            if (loaded.isInterface()) {
                ctxt.error(locatable.getLocation(), "Conditional class `%s` must be a class, not an interface", niceName);
                return errorCondition;
            }
            final LoadedTypeDefinition booleanSupplier = getGetAsBoolean().getEnclosingType().load();
            if (! loaded.getClassType().isSubtypeOf(booleanSupplier.getInterfaceType())) {
                ctxt.error(locatable.getLocation(), "Conditional class `%s` must implement `BooleanSupplier`", niceName);
                return errorCondition;
            }
            // get the constructor
            int ci = loaded.findConstructorIndex(MethodDescriptor.VOID_METHOD_DESCRIPTOR);
            if (ci == -1) {
                ctxt.error(locatable.getLocation(), "Conditional class `%s` has no suitable constructor", niceName);
                return errorCondition;
            }
            ConstructorElement constructor = loaded.getConstructor(ci);
            VmClass vmClass = loaded.getVmClass();
            VmThread vmThread = Vm.requireCurrentThread();
            Vm vm = vmThread.getVM();
            // construct the new instance
            try {
                VmObject obj = vm.newInstance(vmClass, constructor, List.of());
                result = (Boolean) vm.invokeVirtual(getGetAsBoolean(), obj, List.of());
            } catch (Thrown t) {
                ctxt.error(locatable.getLocation(), "Failed to evaluate condition class `%s`: %s", t.getMessage());
                return errorCondition;
            }
            Boolean appearing = cachedResults.putIfAbsent(definedType, result);
            if (appearing != null) {
                // always go with first result in case they don't agree
                result = appearing;
            }
            return result.booleanValue();
        } else {
            ctxt.error(locatable.getLocation(), "Annotation conditional class `%s` is not of a valid type", rawDescriptor);
            return errorCondition;
        }
    }

    private MethodElement getGetAsBoolean() {
        DefinedTypeDefinition bsDef = ctxt.getBootstrapClassContext().findDefinedType("java/util/function/BooleanSupplier");
        if (bsDef == null) {
            ctxt.error("Unable to load JDK interface `BooleanSupplier`");
            throw new IllegalStateException();
        }
        LoadedTypeDefinition booleanSupplier = bsDef.load();
        int mi = booleanSupplier.findMethodIndex(me -> me.getName().equals("getAsBoolean"));
        if (mi == -1) {
            ctxt.error("Unable to locate JDK interface method `BooleanSupplier#getAsBoolean`");
            throw new IllegalStateException();
        }
        return booleanSupplier.getMethod(mi);
    }
}
