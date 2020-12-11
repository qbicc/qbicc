package cc.quarkus.qcc.plugin.native_;

import java.io.IOException;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.driver.Driver;
import cc.quarkus.qcc.machine.probe.CProbe;
import cc.quarkus.qcc.machine.probe.Qualifier;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.StringAnnotationValue;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ConstructorResolver;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
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
            if (superClassInternalName.equals(Native.OBJECT_INT_NAME) || superClassInternalName.equals(Native.WORD_INT_NAME)) {
                // probe native object type
                isNative = true;
            }
        }
        getDelegate().setSuperClassName(superClassInternalName);
    }

    public void addConstructor(final ConstructorResolver resolver, final int index) {
        // native types cannot be constructed the normal way
        if (! isNative) {
            delegate.addConstructor(resolver, index);
        }
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
                    if (ProbeUtils.processCommonAnnotation(pb, annotation)) {
                        continue;
                    }
                    if (annDesc.getClassName().equals(Native.ANN_NAME)) {
                        simpleName = ((StringAnnotationValue) annotation.getValue("value")).getString();
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
