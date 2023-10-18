package org.qbicc.plugin.native_;

import java.io.IOException;
import java.util.List;

import org.jboss.logging.Logger;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Driver;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.object.Data;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Linkage;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.ThreadLocalMode;
import org.qbicc.plugin.core.ConditionEvaluation;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.FunctionType;
import org.qbicc.type.MethodType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.ArrayAnnotationValue;
import org.qbicc.type.annotation.EnumConstantAnnotationValue;
import org.qbicc.type.annotation.IntAnnotationValue;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * A delegating type builder which handles interactions with {@code @extern} and {@code @export} methods and fields.
 */
public class ExternExportTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private static final Logger log = Logger.getLogger("org.qbicc.plugin.native_");

    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition.Builder delegate;
    private boolean hasInclude;

    public ExternExportTypeBuilder(final ClassContext classCtxt, final DefinedTypeDefinition.Builder delegate) {
        this.classCtxt = classCtxt;
        this.ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    public void setInvisibleAnnotations(final List<Annotation> annotations) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        ConditionEvaluation conditionEvaluation = ConditionEvaluation.get(ctxt);
        for (Annotation annotation : annotations) {
            ClassTypeDescriptor desc = annotation.getDescriptor();
            if (desc.getPackageName().equals(Native.NATIVE_PKG)) {
                String annClassName = desc.getClassName();
                if (annClassName.equals(Native.ANN_INCLUDE) || annClassName.equals(Native.ANN_INCLUDE_LIST)) {
                    hasInclude = true;
                }
                if (annClassName.equals(Native.ANN_LIB)) {
                    if (conditionEvaluation.evaluateConditions(classCtxt, this, annotation)) {
                        nativeInfo.registerLibrary(((StringAnnotationValue) annotation.getValue("value")).getString());
                    }
                } else if (annClassName.equals(Native.ANN_LIB_LIST)) {
                    ArrayAnnotationValue array = (ArrayAnnotationValue) annotation.getValue("value");
                    int cnt = array.getElementCount();
                    for (int j = 0; j < cnt; j ++) {
                        Annotation element = (Annotation) array.getValue(j);
                        if (conditionEvaluation.evaluateConditions(classCtxt, this, element)) {
                            nativeInfo.registerLibrary(((StringAnnotationValue) element.getValue("value")).getString());
                        }
                    }
                }
            }
        }
        getDelegate().setInvisibleAnnotations(annotations);
    }

    public void addField(final FieldResolver resolver, final int index, String name, TypeDescriptor descriptor) {
        delegate.addField(new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                NativeInfo nativeInfo = NativeInfo.get(ctxt);
                ConditionEvaluation conditionEvaluation = ConditionEvaluation.get(ctxt);
                FieldElement resolved = resolver.resolveField(index, enclosing, builder);
                String name = resolved.getName();
                // look for annotations that indicate that this method requires special handling
                boolean nameOverridden = false;
                boolean isExtern = false;
                boolean isExport = false;
                for (Annotation annotation : resolved.getInvisibleAnnotations()) {
                    ClassTypeDescriptor desc = annotation.getDescriptor();
                    if (desc.getPackageName().equals(Native.NATIVE_PKG)) {
                        if (desc.getClassName().equals(Native.ANN_NAME) && ! nameOverridden) {
                            if (annotation.getValue("value") instanceof StringAnnotationValue sav) {
                                if (conditionEvaluation.evaluateConditions(classCtxt, resolved, annotation)) {
                                    name = sav.getString();
                                    nameOverridden = true;
                                }
                            }
                        } else if (desc.getClassName().equals(Native.ANN_NAME_LIST) && ! nameOverridden) {
                            if (annotation.getValue("value") instanceof ArrayAnnotationValue aav) {
                                int cnt = aav.getElementCount();
                                for (int i = 0; i < cnt; i ++) {
                                    if (aav.getValue(i) instanceof Annotation nested) {
                                        ClassTypeDescriptor nestedDesc = nested.getDescriptor();
                                        if (nestedDesc.packageAndClassNameEquals(Native.NATIVE_PKG, Native.ANN_NAME)) {
                                            if (nested.getValue("value") instanceof StringAnnotationValue sav) {
                                                if (conditionEvaluation.evaluateConditions(classCtxt, resolved, annotation)) {
                                                    name = sav.getString();
                                                    nameOverridden = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (desc.getClassName().equals(Native.ANN_EXTERN)) {
                            if (isExport) {
                                ctxt.error(resolved, "Field cannot have both `@extern` and `@export");
                                return resolved;
                            }
                            isExtern = true;
                        } else if (desc.getClassName().equals(Native.ANN_EXPORT)) {
                            if (isExtern) {
                                ctxt.error(resolved, "Field cannot have both `@extern` and `@export");
                                return resolved;
                            }
                            isExport = true;
                        }
                    }
                }
                if (isExtern) {
                    if (! resolved.isStatic()) {
                        ctxt.error(resolved, "External (imported) fields must be `static`");
                    }
                    // declare it
                    ProgramModule programModule = ctxt.getOrAddProgramModule(enclosing);
                    ValueType fieldType = resolved.getType();
                    DataDeclaration decl = programModule.declareData(resolved, name, fieldType);
                    if (resolved.hasAllModifiersOf(ClassFile.I_ACC_THREAD_LOCAL)) {
                        decl.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
                    }
                    decl.setLinkage(Linkage.EXTERNAL);
                    // and register as an external data object
                    addExtern(nativeInfo, resolved, decl);
                } else if (isExport) {
                    if (! resolved.isStatic()) {
                        ctxt.error(resolved, "Exported fields must be `static`");
                    }
                    // define it
                    ModuleSection section = ctxt.getImplicitSection(enclosing);
                    ValueType fieldType = resolved.getType();
                    Data data = section.addData(resolved, name, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(fieldType));
                    if (resolved.hasAllModifiersOf(ClassFile.I_ACC_THREAD_LOCAL)) {
                        data.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
                    }
                    data.setLinkage(Linkage.EXTERNAL);
                    // and register it
                    addExport(nativeInfo, resolved, data);
                }
                return resolved;
            }

            private void addExtern(final NativeInfo nativeInfo, final FieldElement resolved, final DataDeclaration decl) {
                ValueType fieldType = resolved.getType();
                nativeInfo.registerFieldInfo(
                    resolved.getEnclosingType().getDescriptor(),
                    resolved.getName(),
                    new NativeDataInfo(resolved, fieldType, ctxt.getLiteralFactory().literalOf(decl))
                );
            }

            private void addExport(final NativeInfo nativeInfo, final FieldElement resolved, final Data data) {
                ValueType fieldType = resolved.getType();
                nativeInfo.registerFieldInfo(
                    resolved.getEnclosingType().getDescriptor(),
                    resolved.getName(),
                    new NativeDataInfo(resolved, fieldType, ctxt.getLiteralFactory().literalOf(data))
                );
            }
        }, index, name, descriptor);
    }

    public void addMethod(final MethodResolver resolver, final int index, String name, MethodDescriptor descriptor) {
        delegate.addMethod(new MethodResolver() {
            public MethodElement resolveMethod(final int index, final DefinedTypeDefinition enclosing, MethodElement.Builder builder) {
                NativeInfo nativeInfo = NativeInfo.get(ctxt);
                ConditionEvaluation conditionEvaluation = ConditionEvaluation.get(ctxt);
                MethodElement origMethod = resolver.resolveMethod(index, enclosing, builder);
                String name = origMethod.getName();
                // look for annotations that indicate that this method requires special handling
                boolean nameOverridden = false;
                boolean isExtern = false;
                boolean isExport = false;
                for (Annotation annotation : origMethod.getInvisibleAnnotations()) {
                    ClassTypeDescriptor desc = annotation.getDescriptor();
                    if (desc.getPackageName().equals(Native.NATIVE_PKG)) {
                        if (desc.getClassName().equals(Native.ANN_NAME) && ! nameOverridden) {
                            if (annotation.getValue("value") instanceof StringAnnotationValue sav) {
                                if (conditionEvaluation.evaluateConditions(classCtxt, origMethod, annotation)) {
                                    name = sav.getString();
                                    nameOverridden = true;
                                }
                            }
                        } else if (desc.getClassName().equals(Native.ANN_NAME_LIST) && ! nameOverridden) {
                            if (annotation.getValue("value") instanceof ArrayAnnotationValue aav) {
                                int cnt = aav.getElementCount();
                                for (int i = 0; i < cnt; i ++) {
                                    if (aav.getValue(i) instanceof Annotation nested) {
                                        ClassTypeDescriptor nestedDesc = nested.getDescriptor();
                                        if (nestedDesc.packageAndClassNameEquals(Native.NATIVE_PKG, Native.ANN_NAME)) {
                                            if (nested.getValue("value") instanceof StringAnnotationValue sav) {
                                                if (conditionEvaluation.evaluateConditions(classCtxt, origMethod, annotation)) {
                                                    name = sav.getString();
                                                    nameOverridden = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (desc.getClassName().equals(Native.ANN_EXTERN)) {
                            if (isExport) {
                                ctxt.error(origMethod, "Method cannot have both `@extern` and `@export");
                                return origMethod;
                            }
                            isExtern = true;
                        } else if (desc.getClassName().equals(Native.ANN_EXPORT)) {
                            if (isExtern) {
                                ctxt.error(origMethod, "Method cannot have both `@extern` and `@export");
                                return origMethod;
                            }
                            isExport = true;
                        } else if (desc.getClassName().equals(Native.ANN_MACRO)) {
                            name = getFunctionNameFromMacro(origMethod);
                        }
                    }
                }
                if (! isExtern && ! isExport && origMethod.hasAllModifiersOf(ClassFile.ACC_NATIVE) && hasInclude) {
                    // treat it as extern
                    isExtern = true;
                }
                if (name != null) {
                    if (isExtern) {
                        addExtern(nativeInfo, origMethod, name);
                    } else if (isExport) {
                        addExport(nativeInfo, origMethod, name);
                    }
                }
                return origMethod;
            }

            private void addExtern(final NativeInfo nativeInfo, final MethodElement origMethod, final String name) {
                MethodType origType = origMethod.getType();
                TypeSystem ts = classCtxt.getTypeSystem();
                FunctionType type;
                int pc = origType.getParameterCount();
                if (origMethod.isVarargs() && pc > 1) {
                    ValueType[] argTypes = new ValueType[pc];
                    for (int i = 0; i < pc - 1; i ++) {
                        argTypes[i] = origType.getParameterType(i);
                    }
                    argTypes[pc - 1] = ts.getVariadicType();
                    type = ts.getFunctionType(origType.getReturnType(), List.of(argTypes));
                } else {
                    type = ts.getFunctionType(origType.getReturnType(), origType.getParameterTypes());
                }
                nativeInfo.registerFunctionInfo(
                    origMethod.getEnclosingType().getDescriptor(),
                    origMethod.getName(),
                    origMethod.getDescriptor(),
                    new ExternalFunctionInfo(origMethod.getEnclosingType(), name, type, origMethod)
                );
            }

            private void addExport(final NativeInfo nativeInfo, final MethodElement origMethod, final String name) {
                boolean constructor = false;
                int constructorPriority = 0;
                boolean destructor = false;
                int destructorPriority = 0;
                // default for functions
                SafePointBehavior safePointBehavior = SafePointBehavior.ENTER;
                int safePointSetBits = 0;
                int safePointClearBits = 0;
                for (Annotation annotation : origMethod.getInvisibleAnnotations()) {
                    ClassTypeDescriptor desc = annotation.getDescriptor();
                    if (desc.getPackageName().equals(Native.NATIVE_PKG)) {
                        IntAnnotationValue priority = (IntAnnotationValue) annotation.getValue("priority");
                        if (desc.getClassName().equals(Native.ANN_CONSTRUCTOR)) {
                            constructor = true;
                            constructorPriority = priority == null ? 1000 : priority.intValue();
                        } else if (desc.getClassName().equals(Native.ANN_DESTRUCTOR)) {
                            destructor = true;
                            destructorPriority = priority == null ? 1000 : priority.intValue();
                        } else if (desc.getClassName().equals(Native.ANN_SAFEPOINT)) {
                            // explicit behavior
                            EnumConstantAnnotationValue behaviorValue = (EnumConstantAnnotationValue) annotation.getValue("value");
                            if (behaviorValue != null) {
                                safePointBehavior = SafePointBehavior.valueOf(behaviorValue.getValueName());
                            }
                            IntAnnotationValue setBitsValue = (IntAnnotationValue) annotation.getValue("setBits");
                            if (setBitsValue != null) {
                                safePointSetBits = setBitsValue.intValue();
                            }
                            IntAnnotationValue clearBitsValue = (IntAnnotationValue) annotation.getValue("clearBits");
                            if (clearBitsValue != null) {
                                safePointClearBits = clearBitsValue.intValue();
                            }
                        }
                    }
                }
                FunctionElement.Builder builder = FunctionElement.builder(name, origMethod.getDescriptor(), origMethod.getIndex());
                builder.setModifiers(origMethod.getModifiers());
                builder.setEnclosingType(origMethod.getEnclosingType());
                builder.setSignature(origMethod.getSignature());
                builder.setSourceFileName(origMethod.getSourceFileName());
                builder.setParameters(origMethod.getParameters());
                builder.setMinimumLineNumber(origMethod.getMinimumLineNumber());
                builder.setMaximumLineNumber(origMethod.getMaximumLineNumber());
                builder.setMethodBodyFactory(origMethod.getMethodBodyFactory(), origMethod.getMethodBodyFactoryIndex());
                builder.setSafePointBehavior(safePointBehavior);
                builder.setSafePointSetBits(safePointSetBits);
                builder.setSafePointClearBits(safePointClearBits);
                FunctionElement function = builder.build();
                nativeInfo.registerFunctionInfo(
                    origMethod.getEnclosingType().getDescriptor(),
                    name,
                    origMethod.getDescriptor(),
                    new ExportedFunctionInfo(function)
                );
                ctxt.establishExactFunction(origMethod, function);
                ctxt.registerEntryPoint(function);
                if (constructor) {
                    nativeInfo.registerGlobalConstructor(function, constructorPriority);
                }
                if (destructor) {
                    nativeInfo.registerGlobalDestructor(function, destructorPriority);
                }
            }

            private String getFunctionNameFromMacro(final MethodElement origMethod) {
                CProbe.Builder builder = CProbe.builder();
                ProbeUtils.ProbeProcessor pp = new ProbeUtils.ProbeProcessor(classCtxt, origMethod.getEnclosingType());
                for (Annotation annotation : origMethod.getEnclosingType().getInvisibleAnnotations()) {
                    pp.processAnnotation(annotation);
                }
                pp.accept(builder);
                pp = new ProbeUtils.ProbeProcessor(classCtxt, origMethod);
                for (Annotation annotation : origMethod.getInvisibleAnnotations()) {
                    pp.processAnnotation(annotation);
                }
                pp.accept(builder);
                builder.probeMacroFunctionName(origMethod.getName(), origMethod.getSourceFileName(), 0);
                CProbe probe = builder.build();
                CProbe.Result result;
                try {
                    result = probe.run(ctxt.getAttachment(Driver.C_TOOL_CHAIN_KEY), ctxt.getAttachment(Driver.OBJ_PROVIDER_TOOL_KEY), null);
                    if (result == null) {
                        return null;
                    }
                } catch (IOException e) {
                    return null;
                }
                CProbe.FunctionInfo functionInfo = result.getFunctionInfo(origMethod.getName());
                final String resolvedName = functionInfo.getResolvedName();
                return resolvedName == null || resolvedName.isBlank() ? origMethod.getName() : resolvedName;
            }
        }, index, name, descriptor);
    }
}
