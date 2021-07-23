package org.qbicc.plugin.native_;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.object.Data;
import org.qbicc.object.Linkage;
import org.qbicc.object.Section;
import org.qbicc.object.ThreadLocalMode;
import org.qbicc.type.ArrayType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.AnnotationValue;
import org.qbicc.type.annotation.ArrayAnnotationValue;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.jboss.logging.Logger;

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
                            ValueType fieldType = resolved.getType();
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
                ValueType fieldType = resolved.getType();
                SymbolLiteral literal;
                if (fieldType instanceof ArrayType) {
                    literal = ctxt.getLiteralFactory().literalOfSymbol(name, fieldType);
                } else {
                    literal = ctxt.getLiteralFactory().literalOfSymbol(name, fieldType.getPointer());
                }
                nativeInfo.registerFieldInfo(
                    resolved.getEnclosingType().getDescriptor(),
                    resolved.getName(),
                    new NativeDataInfo(resolved, false, fieldType, literal)
                );
            }

            private void addExport(final NativeInfo nativeInfo, final FieldElement resolved, final String name) {
                ValueType fieldType = resolved.getType();
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
                            LoadedTypeDefinition loadedNativeType = nativeType.load();
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
                FunctionType origType = origMethod.getType();
                TypeSystem ts = classCtxt.getTypeSystem();
                FunctionType type;
                int pc = origType.getParameterCount();
                if (origMethod.isVarargs() && pc > 1) {
                    ValueType[] argTypes = new ValueType[pc];
                    for (int i = 0; i < pc - 1; i ++) {
                        argTypes[i] = origType.getParameterType(i);
                    }
                    argTypes[pc - 1] = ts.getVariadicType();
                    type = ts.getFunctionType(origType.getReturnType(), argTypes);
                } else {
                    type = origType;
                }
                nativeInfo.registerFunctionInfo(
                    origMethod.getEnclosingType().getDescriptor(),
                    origMethod.getName(),
                    origMethod.getDescriptor(),
                    new ExternalFunctionInfo(origMethod.getEnclosingType(), name, type)
                );
            }

            private void addExport(final NativeInfo nativeInfo, final MethodElement origMethod, final String name) {
                if (origMethod.isVarargs()) {
                    ctxt.error(origMethod, "Exported vararg functions not yet supported");
                    return;
                }
                FunctionElement.Builder builder = FunctionElement.builder();
                builder.setName(name);
                builder.setModifiers(origMethod.getModifiers());
                builder.setEnclosingType(origMethod.getEnclosingType());
                builder.setDescriptor(origMethod.getDescriptor());
                builder.setSignature(origMethod.getSignature());
                FunctionType fnType = origMethod.getType();
                builder.setType(fnType);
                builder.setSourceFileName(origMethod.getSourceFileName());
                builder.setParameters(origMethod.getParameters());
                builder.setMinimumLineNumber(origMethod.getMinimumLineNumber());
                builder.setMaximumLineNumber(origMethod.getMaximumLineNumber());
                builder.setMethodBodyFactory(origMethod.getMethodBodyFactory(), origMethod.getMethodBodyFactoryIndex());
                FunctionElement function = builder.build();
                nativeInfo.registerFunctionInfo(
                    origMethod.getEnclosingType().getDescriptor(),
                    name,
                    origMethod.getDescriptor(),
                    new ExportedFunctionInfo(function)
                );
                ctxt.registerEntryPoint(function);
            }
        }, index);
    }
}
