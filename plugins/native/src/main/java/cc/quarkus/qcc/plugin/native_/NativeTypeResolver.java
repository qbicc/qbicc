package cc.quarkus.qcc.plugin.native_;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.DescriptorTypeResolver;

/**
 * This type resolver is responsible for translating Java reference types such as {@code CNative.c_int} into the
 * corresponding {@code ValueType} in field, method, and constructor declarations.
 */
public class NativeTypeResolver implements DescriptorTypeResolver.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DescriptorTypeResolver delegate;

    public NativeTypeResolver(final ClassContext classCtxt, final DescriptorTypeResolver delegate) {
        this.classCtxt = classCtxt;
        ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DescriptorTypeResolver getDelegate() {
        return delegate;
    }

    public ValueType resolveTypeFromClassName(final String packageName, final String internalName) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        DefinedTypeDefinition definedType = classCtxt.findDefinedType(packageName + "/" + internalName);
        if (definedType == null) {
            return delegate.resolveTypeFromClassName(packageName, internalName);
        }
        ValueType valueType = nativeInfo.nativeTypes.get(definedType);
        return valueType == null ? delegate.resolveTypeFromClassName(packageName, internalName) : valueType;
    }

    private ValueType translateType(NativeInfo nativeInfo, ValueType type) {
        if (type instanceof ReferenceType) {
            TypeIdLiteral upperBound = ((ReferenceType) type).getUpperBound();
            if (upperBound instanceof ClassTypeIdLiteral) {
                ClassTypeIdLiteral classId = (ClassTypeIdLiteral) upperBound;
                DefinedTypeDefinition def = classCtxt.resolveDefinedTypeLiteral(classId);
                ValueType nativeType = nativeInfo.nativeTypes.get(def);
                if (nativeType != null) {
                    return nativeType;
                }
            }
        }
        // not a native type
        return type;
    }
}
