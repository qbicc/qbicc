package cc.quarkus.qcc.plugin.native_;

import static cc.quarkus.qcc.context.CompilationContext.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.driver.Driver;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.probe.CProbe;
import cc.quarkus.qcc.machine.probe.Qualifier;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.VoidType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.AnnotationValue;
import cc.quarkus.qcc.type.annotation.ArrayAnnotationValue;
import cc.quarkus.qcc.type.annotation.StringAnnotationValue;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodResolver;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public class NativeTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition.Builder delegate;
    private boolean isNative;

    public NativeTypeBuilder(final ClassContext classCtxt, final DefinedTypeDefinition.Builder delegate) {
        this.classCtxt = classCtxt;
        this.ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    public void setSuperClassName(final String superClassInternalName) {
        if (superClassInternalName != null) {
            if (superClassInternalName.equals(Native.OBJECT) || superClassInternalName.equals(Native.WORD)) {
                // probe native object type
                isNative = true;
            }
        }
        getDelegate().setSuperClassName(superClassInternalName);
    }

    public void addMethod(final MethodResolver resolver, final int index) {
        delegate.addMethod(new MethodResolver() {
            public MethodElement resolveMethod(final int index, final DefinedTypeDefinition enclosing) {
                NativeInfo nativeInfo = NativeInfo.get(ctxt);
                MethodElement origMethod = resolver.resolveMethod(index, enclosing);
                boolean isNative = origMethod.hasAllModifiersOf(ClassFile.ACC_NATIVE);
                // look for annotations that indicate that this method requires special handling
                for (Annotation annotation : origMethod.getVisibleAnnotations()) {
                    ClassTypeDescriptor desc = annotation.getDescriptor();
                    if (desc.getPackageName().equals(Native.NATIVE_PKG)) {
                        if (desc.getClassName().equals(Native.ANN_EXTERN)) {
                            AnnotationValue nameVal = annotation.getValue("withName");
                            String name = nameVal == null ? origMethod.getName() : ((StringAnnotationValue) nameVal).getString();
                            // register as a function
                            FunctionType type = origMethod.getType(classCtxt, List.of(/*todo*/));
                            nativeInfo.nativeFunctions.put(origMethod, new NativeFunctionInfo(ctxt.getLiteralFactory().literalOfSymbol(
                                name, type
                            )));
                            // all done
                            break;
                        } else if (desc.getClassName().equals(Native.ANN_EXPORT)) {
                            // immediately generate the call-in stub
                            AnnotationValue nameVal = annotation.getValue("withName");
                            String name = nameVal == null ? origMethod.getName() : ((StringAnnotationValue) nameVal).getString();
                            Function exactFunction = ctxt.getExactFunction(origMethod);
                            FunctionType fnType = exactFunction.getType();
                            Function function = ctxt.getOrAddProgramModule(enclosing).getOrAddSection(IMPLICIT_SECTION_NAME).addFunction(origMethod, name, fnType);
                            BasicBlockBuilder gf = classCtxt.newBasicBlockBuilder(origMethod);
                            BlockLabel entry = new BlockLabel();
                            gf.begin(entry);
                            LiteralFactory lf = ctxt.getLiteralFactory();
                            SymbolLiteral fn = lf.literalOfSymbol(exactFunction.getName(), exactFunction.getType());
                            int pcnt = origMethod.getParameters().size();
                            List<Value> args = new ArrayList<>(pcnt + 1);
                            List<Value> pv = new ArrayList<>(pcnt);
                            // for now, thread is null!
                            // todo: insert prolog here
                            args.add(lf.literalOfNull());
                            for (int j = 0; j < pcnt; j ++) {
                                Value parameter = gf.parameter(fnType.getParameterType(j), j);
                                pv.add(parameter);
                                args.add(parameter);
                            }
                            Value result = gf.callFunction(fn, args);
                            if (fnType.getReturnType() instanceof VoidType) {
                                gf.return_();
                            } else {
                                gf.return_(result);
                            }
                            BasicBlock entryBlock = BlockLabel.getTargetOf(entry);
                            function.replaceBody(MethodBody.of(entryBlock, Schedule.forMethod(entryBlock), null, pv));
                            // ensure the method is reachable
                            ctxt.registerEntryPoint(origMethod);
                            // all done
                            break;
                        }
                    }
                }
                return origMethod;
            }
        }, index);
    }

    public DefinedTypeDefinition build() {
        // wrap up
        DefinedTypeDefinition builtType = getDelegate().build();
        if (isNative && ! builtType.isAbstract()) {
            CProbe.Builder pb = CProbe.builder();
            String simpleName = null;
            Qualifier q = Qualifier.NONE;
            for (Annotation annotation : builtType.getVisibleAnnotations()) {
                ClassTypeDescriptor annDesc = annotation.getDescriptor();
                if (annDesc.getPackageName().equals(Native.NATIVE_PKG)) {
                    if (annDesc.getClassName().equals(Native.ANN_NAME)) {
                        simpleName = ((StringAnnotationValue) annotation.getValue("value")).getString();
                    } else if (annDesc.getClassName().equals(Native.ANN_INCLUDE)) {
                        // include just one
                        String include = ((StringAnnotationValue) annotation.getValue("value")).getString();
                        // todo: when/unless (requires VM)
                        pb.include(include);
                    } else if (annDesc.getClassName().equals(Native.ANN_INCLUDE_LIST)) {
                        ArrayAnnotationValue array = (ArrayAnnotationValue) annotation.getValue("value");
                        int cnt = array.getElementCount();
                        for (int j = 0; j < cnt; j ++) {
                            Annotation nested = (Annotation) array.getValue(j);
                            assert nested.getDescriptor().getClassName().equals(Native.ANN_INCLUDE);
                            String include = ((StringAnnotationValue) annotation.getValue("value")).getString();
                            // todo: when/unless (requires VM)
                            pb.include(include);
                        }
                    } else if (annDesc.getClassName().equals(Native.ANN_DEFINE)) {
                        // define just one
                        String define = ((StringAnnotationValue) annotation.getValue("value")).getString();
                        // todo: when/unless (requires VM)
                        pb.define(define);
                    } else if (annDesc.getClassName().equals(Native.ANN_DEFINE_LIST)) {
                        ArrayAnnotationValue array = (ArrayAnnotationValue) annotation.getValue("value");
                        int cnt = array.getElementCount();
                        for (int j = 0; j < cnt; j ++) {
                            Annotation nested = (Annotation) array.getValue(j);
                            assert nested.getDescriptor().getClassName().equals(Native.ANN_DEFINE);
                            String define = ((StringAnnotationValue) annotation.getValue("value")).getString();
                            // todo: when/unless (requires VM)
                            pb.define(define);
                        }
                    }
                }
                // todo: lib (add to native info)
            }
            if (simpleName == null) {
                String fullName = builtType.getInternalName();
                int idx = fullName.lastIndexOf('/');
                simpleName = idx == -1 ? fullName : fullName.substring(idx + 1);
                idx = simpleName.lastIndexOf('$');
                simpleName = idx == -1 ? simpleName : simpleName.substring(idx + 1);
                if (simpleName.startsWith("struct_")) {
                    q = Qualifier.STRUCT;
                    simpleName = simpleName.substring(7);
                } else if (simpleName.startsWith("union_")) {
                    q = Qualifier.UNION;
                    simpleName = simpleName.substring(6);
                }
            }
            // begin the real work
            ValidatedTypeDefinition vt = builtType.validate();
            NativeInfo nativeInfo = ctxt.getAttachment(NativeInfo.KEY);
            int fc = vt.getFieldCount();
            TypeSystem ts = ctxt.getTypeSystem();
            CProbe.Type.Builder tb = CProbe.Type.builder();
            tb.setName(simpleName);
            tb.setQualifier(q);
            for (int i = 0; i < fc; i ++) {
                ValueType type = vt.getField(i).getType(classCtxt, List.of(/*todo*/));
                // compound type
                tb.addMember(vt.getField(i).getName());
            }
            CProbe.Type probeType = tb.build();
            pb.probeType(probeType);
            CProbe probe = pb.build();
            CompoundType.Tag tag = q == Qualifier.NONE ? CompoundType.Tag.NONE : q == Qualifier.STRUCT ? CompoundType.Tag.STRUCT : CompoundType.Tag.UNION;
            try {
                CProbe.Result result = probe.run(ctxt.getAttachment(Driver.C_TOOL_CHAIN_KEY), ctxt.getAttachment(Driver.OBJ_PROVIDER_TOOL_KEY), ctxt);
                if (result != null) {
                    CProbe.Type.Info typeInfo = result.getTypeInfo(probeType);
                    ValueType realType;
                    long size = typeInfo.getSize();
                    if (typeInfo.isFloating()) {
                        if (size == 4) {
                            realType = ts.getFloat32Type();
                        } else if (size == 8) {
                            realType = ts.getFloat64Type();
                        } else {
                            realType = ts.getCompoundType(tag, simpleName, size, (int) typeInfo.getAlign());
                        }
                    } else if (typeInfo.isSigned()) {
                        if (size == 1) {
                            realType = ts.getSignedInteger8Type();
                        } else if (size == 2) {
                            realType = ts.getSignedInteger16Type();
                        } else if (size == 4) {
                            realType = ts.getSignedInteger32Type();
                        } else if (size == 8) {
                            realType = ts.getSignedInteger64Type();
                        } else {
                            realType = ts.getCompoundType(tag, simpleName, size, (int) typeInfo.getAlign());
                        }
                    } else if (typeInfo.isUnsigned()) {
                        if (size == 1) {
                            realType = ts.getUnsignedInteger8Type();
                        } else if (size == 2) {
                            realType = ts.getUnsignedInteger16Type();
                        } else if (size == 4) {
                            realType = ts.getUnsignedInteger32Type();
                        } else if (size == 8) {
                            realType = ts.getUnsignedInteger64Type();
                        } else {
                            realType = ts.getCompoundType(tag, simpleName, size, (int) typeInfo.getAlign());
                        }
                    } else {
                        CompoundType.Member[] members = new CompoundType.Member[fc];
                        for (int i = 0; i < fc; i ++) {
                            ValueType type = vt.getField(i).getType(classCtxt, List.of(/*todo*/));
                            // compound type
                            String name = vt.getField(i).getName();
                            CProbe.Type.Info member = result.getTypeInfoOfMember(probeType, name);
                            members[i] = ts.getCompoundTypeMember(name, type, (int) member.getOffset(), (int) member.getAlign());
                        }
                        realType = ts.getCompoundType(tag, simpleName, size, (int) typeInfo.getAlign(), members);
                    }
                    nativeInfo.nativeTypes.put(builtType, realType);
                }
            } catch (IOException e) {
                ctxt.error(e, "Failed to define native type " + simpleName);
            }
        }
        return builtType;
    }
}
