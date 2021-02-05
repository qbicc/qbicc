package cc.quarkus.qcc.plugin.native_;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.object.Data;
import cc.quarkus.qcc.object.Linkage;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.object.ThreadLocalMode;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.VoidType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.AnnotationValue;
import cc.quarkus.qcc.type.annotation.ArrayAnnotationValue;
import cc.quarkus.qcc.type.annotation.StringAnnotationValue;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.FieldResolver;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodBodyFactory;
import cc.quarkus.qcc.type.definition.MethodResolver;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.FunctionElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import org.jboss.logging.Logger;

/**
 * A delegating type builder which handles interactions with {@code @extern} and {@code @export} methods and fields.
 */
public class ExternExportTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private static final Logger log = Logger.getLogger("cc.quarkus.qcc.plugin.native_");

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

    public void setVisibleAnnotations(final List<Annotation> annotations) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        for (Annotation annotation : annotations) {
            ClassTypeDescriptor desc = annotation.getDescriptor();
            if (desc.getPackageName().equals(Native.NATIVE_PKG)) {
                String annClassName = desc.getClassName();
                if (annClassName.equals(Native.ANN_INCLUDE) || annClassName.equals(Native.ANN_INCLUDE_LIST)) {
                    hasInclude = true;
                }
                if (annClassName.equals(Native.ANN_LIB)) {
                    nativeInfo.registerLibrary(((StringAnnotationValue) annotation.getValue("value")).getString());
                } else if (annClassName.equals(Native.ANN_LIB_LIST)) {
                    ArrayAnnotationValue array = (ArrayAnnotationValue) annotation.getValue("value");
                    int cnt = array.getElementCount();
                    for (int j = 0; j < cnt; j ++) {
                        Annotation element = (Annotation) array.getValue(j);
                        nativeInfo.registerLibrary(((StringAnnotationValue) element.getValue("value")).getString());
                    }
                }
            }
        }
        getDelegate().setVisibleAnnotations(annotations);
    }

    public void addField(final FieldResolver resolver, final int index) {
        delegate.addField(new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing) {
                NativeInfo nativeInfo = NativeInfo.get(ctxt);
                FieldElement resolved = resolver.resolveField(index, enclosing);
                // look for annotations
                for (Annotation annotation : resolved.getVisibleAnnotations()) {
                    ClassTypeDescriptor desc = annotation.getDescriptor();
                    if (desc.getPackageName().equals(Native.NATIVE_PKG)) {
                        if (desc.getClassName().equals(Native.ANN_EXTERN)) {
                            AnnotationValue nameVal = annotation.getValue("withName");
                            String name = nameVal == null ? resolved.getName() : ((StringAnnotationValue) nameVal).getString();
                            if (! resolved.isStatic()) {
                                ctxt.error(resolved, "External (imported) fields must be `static`");
                            }
                            // register as an external data object
                            addExtern(nativeInfo, resolved, name);
                            // all done
                            return resolved;
                        } else if (desc.getClassName().equals(Native.ANN_EXPORT)) {
                            // immediately generate the call-in stub
                            AnnotationValue nameVal = annotation.getValue("withName");
                            String name = nameVal == null ? resolved.getName() : ((StringAnnotationValue) nameVal).getString();
                            // register it
                            addExport(nativeInfo, resolved, name);
                            // and define it
                            Section section = ctxt.getOrAddProgramModule(enclosing).getOrAddSection(CompilationContext.IMPLICIT_SECTION_NAME);
                            ValueType fieldType = resolved.getType(List.of());
                            Data data = section.addData(resolved, name, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(fieldType));
                            if (resolved.hasAllModifiersOf(ClassFile.I_ACC_THREAD_LOCAL)) {
                                data.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
                            }
                            data.setLinkage(Linkage.COMMON);
                            // all done
                            return resolved;
                        }
                    }
                }
                return resolved;
            }

            private void addExtern(final NativeInfo nativeInfo, final FieldElement resolved, final String name) {
                ValueType fieldType = resolved.getType(List.of());
                nativeInfo.registerFieldInfo(
                    resolved.getEnclosingType().getDescriptor(),
                    resolved.getName(),
                    new NativeDataInfo(resolved, false, fieldType, ctxt.getLiteralFactory().literalOfSymbol(name, fieldType.getPointer()))
                );
            }

            private void addExport(final NativeInfo nativeInfo, final FieldElement resolved, final String name) {
                ValueType fieldType = resolved.getType(List.of());
                nativeInfo.registerFieldInfo(
                    resolved.getEnclosingType().getDescriptor(),
                    resolved.getName(),
                    new NativeDataInfo(resolved, true, fieldType, ctxt.getLiteralFactory().literalOfSymbol(name, fieldType.getPointer()))
                );
            }
        }, index);
    }

    public void addMethod(final MethodResolver resolver, final int index) {
        delegate.addMethod(new MethodResolver() {
            public MethodElement resolveMethod(final int index, final DefinedTypeDefinition enclosing) {
                NativeInfo nativeInfo = NativeInfo.get(ctxt);
                MethodElement origMethod = resolver.resolveMethod(index, enclosing);
                // look for annotations that indicate that this method requires special handling
                for (Annotation annotation : origMethod.getVisibleAnnotations()) {
                    ClassTypeDescriptor desc = annotation.getDescriptor();
                    if (desc.getPackageName().equals(Native.NATIVE_PKG)) {
                        if (desc.getClassName().equals(Native.ANN_EXTERN)) {
                            AnnotationValue nameVal = annotation.getValue("withName");
                            String name = nameVal == null ? origMethod.getName() : ((StringAnnotationValue) nameVal).getString();
                            // register as a function
                            addExtern(nativeInfo, origMethod, name);
                            // all done
                            return origMethod;
                        } else if (desc.getClassName().equals(Native.ANN_EXPORT)) {
                            // immediately generate the call-in stub
                            AnnotationValue nameVal = annotation.getValue("withName");
                            String name = nameVal == null ? origMethod.getName() : ((StringAnnotationValue) nameVal).getString();
                            addExport(nativeInfo, origMethod, name);
                            // all done
                            return origMethod;
                        }
                    }
                }
                boolean isNative = origMethod.hasAllModifiersOf(ClassFile.ACC_NATIVE);
                if (isNative) {
                    if (hasInclude) {
                        // treat it as extern with the default name
                        addExtern(nativeInfo, origMethod, origMethod.getName());
                    } else {
                        // check to see there are native bindings for it
                        DefinedTypeDefinition enclosingType = origMethod.getEnclosingType();
                        String internalName = enclosingType.getInternalName();
                        DefinedTypeDefinition nativeType = classCtxt.findDefinedType(internalName + "$_native");
                        if (nativeType != null) {
                            // found it
                            ValidatedTypeDefinition loadedNativeType = nativeType.validate();
                            boolean isStatic = origMethod.hasAllModifiersOf(ClassFile.ACC_STATIC);
                            // bound native methods are always static, but bindings for instance methods take
                            //   the receiver as the first argument
                            MethodElement nativeMethod;
                            if (isStatic) {
                                nativeMethod = loadedNativeType.resolveMethodElementExact(origMethod.getName(), origMethod.getDescriptor());
                            } else {
                                // munge the descriptor
                                MethodDescriptor origDescriptor = origMethod.getDescriptor();
                                List<TypeDescriptor> parameterTypes = origDescriptor.getParameterTypes();
                                nativeMethod = loadedNativeType.resolveMethodElementExact(origMethod.getName(),
                                    MethodDescriptor.synthesize(classCtxt,
                                        origMethod.getDescriptor().getReturnType(),
                                        Native.copyWithPrefix(parameterTypes, enclosingType.getDescriptor(), TypeDescriptor[]::new)));
                            }
                            if (nativeMethod != null) {
                                // there's a match
                                if (nativeMethod.hasAllModifiersOf(ClassFile.ACC_STATIC)) {
                                    nativeInfo.registerNativeBinding(origMethod, nativeMethod);
                                } else {
                                    classCtxt.getCompilationContext().error(nativeMethod, "Native bound methods must be declared `static`");
                                }
                            } else {
                                log.debugf("No match found for native method %s in bindings class %s", origMethod, nativeType.getInternalName());
                            }
                        }
                    }
                }
                return origMethod;
            }

            private void addExtern(final NativeInfo nativeInfo, final MethodElement origMethod, final String name) {
                FunctionType type = origMethod.getType(List.of(/*todo*/));
                nativeInfo.registerFunctionInfo(
                    origMethod.getEnclosingType().getDescriptor(),
                    origMethod.getName(),
                    origMethod.getDescriptor(),
                    new NativeFunctionInfo(origMethod, ctxt.getLiteralFactory().literalOfSymbol(name, type))
                );
            }

            private void addExport(final NativeInfo nativeInfo, final MethodElement origMethod, final String name) {
                FunctionElement.Builder builder = new FunctionElement.Builder();
                builder.setName(name);
                builder.setEnclosingType(origMethod.getEnclosingType());
                builder.setDescriptor(origMethod.getDescriptor());
                builder.setSignature(origMethod.getSignature());
                FunctionType fnType = origMethod.getType(List.of(/*todo*/));
                builder.setType(fnType);
                builder.setSourceFileName(origMethod.getSourceFileName());
                builder.setParameters(origMethod.getParameters());
                builder.setMethodBodyFactory(new MethodBodyFactory() {
                    @Override
                    public MethodBody createMethodBody(int index, ExecutableElement element) {
                        FunctionElement elem = (FunctionElement) element;
                        BasicBlockBuilder gf = classCtxt.newBasicBlockBuilder(element);
                        BlockLabel entry = new BlockLabel();
                        gf.begin(entry);
                        int pcnt = elem.getParameters().size();
                        List<Value> args = new ArrayList<>(pcnt);
                        for (int i = 0; i < pcnt; i ++) {
                            args.add(gf.parameter(fnType.getParameterType(i), i));
                        }
                        if (fnType.getReturnType() instanceof VoidType) {
                            gf.invokeStatic(origMethod, args);
                            gf.return_();
                        } else {
                            gf.return_(gf.invokeValueStatic(origMethod, args));
                        }
                        gf.finish();
                        BasicBlock entryBlock = BlockLabel.getTargetOf(entry);
                        return MethodBody.of(entryBlock, Schedule.forMethod(entryBlock), null, args);
                    }
                }, 0);
                FunctionElement function = builder.build();
                nativeInfo.registerFunctionInfo(
                    origMethod.getEnclosingType().getDescriptor(),
                    name,
                    origMethod.getDescriptor(),
                    new NativeFunctionInfo(function, ctxt.getLiteralFactory().literalOfSymbol(name, fnType)));
                ctxt.registerEntryPoint(function);
            }
        }, index);
    }
}
