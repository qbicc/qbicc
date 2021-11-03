package org.qbicc.plugin.methodinfo;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.object.Section;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;

public class MethodDataTypes {
    private static final AttachmentKey<MethodDataTypes> KEY = new AttachmentKey<>();

    public static final String QBICC_GLOBAL_METHOD_DATA = "qbicc_global_method_data";

    private final CompilationContext ctxt;

    private GlobalVariableElement globalMethodData;

    private CompoundType methodInfoType;
    private CompoundType sourceCodeInfoType;
    private CompoundType globalMethodDataType;

    public MethodDataTypes(final CompilationContext ctxt) {
        this.ctxt = ctxt;
        TypeSystem ts = ctxt.getTypeSystem();
        LoadedTypeDefinition jls = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load();
        ReferenceType jlsRef = jls.getType().getReference();
        ValueType uint8Type = ts.getUnsignedInteger8Type();
        ValueType uint32Type = ts.getUnsignedInteger32Type();
        ValueType uint64Type = ts.getUnsignedInteger64Type();

        methodInfoType = CompoundType.builder(ts)
            .setTag(CompoundType.Tag.STRUCT)
            .setName("qbicc_method_info")
            .setOverallAlignment(jlsRef.getAlign())
            .addNextMember("fileName", jlsRef)
            .addNextMember("className", jlsRef)
            .addNextMember("methodName", jlsRef)
            .addNextMember("methodDesc", jlsRef)
            .addNextMember("typeId", ts.getUnsignedInteger32Type())
            .build();

        sourceCodeInfoType = CompoundType.builder(ts)
            .setTag(CompoundType.Tag.STRUCT)
            .setName("qbicc_souce_code_info")
            .setOverallAlignment(uint32Type.getAlign())
            .addNextMember("methodInfoIndex", uint32Type)
            .addNextMember("lineNumber", uint32Type)
            .addNextMember("bcIndex", uint32Type)
            .addNextMember("inlinedAtIndex", uint32Type)
            .build();

        globalMethodDataType = CompoundType.builder(ts)
            .setTag(CompoundType.Tag.STRUCT)
            .setName("qbicc_method_data")
            .setOverallAlignment(ts.getPointerSize())
            .addNextMember("methodInfoTable", uint8Type.getPointer())
            .addNextMember("sourceCodeInfoTable", uint8Type.getPointer())
            .addNextMember("sourceCodeIndexTable", uint32Type.getPointer())
            .addNextMember("instructionTable", uint64Type.getPointer())
            .addNextMember("instructionTableSize", uint32Type)
            .build();

        GlobalVariableElement.Builder builder = GlobalVariableElement.builder();
        builder.setName(QBICC_GLOBAL_METHOD_DATA);
        builder.setType(globalMethodDataType);
        builder.setEnclosingType(ctxt.getDefaultTypeDefinition().load());
        builder.setDescriptor(BaseTypeDescriptor.V);
        builder.setSignature(BaseTypeSignature.V);
        globalMethodData = builder.build();
    }

    public static MethodDataTypes get(CompilationContext ctxt) {
        MethodDataTypes dt = ctxt.getAttachment(KEY);
        if (dt == null) {
            dt = new MethodDataTypes(ctxt);
            MethodDataTypes appearing = ctxt.putAttachmentIfAbsent(KEY, dt);
            if (appearing != null) {
                dt = appearing;
            }
        }
        return dt;
    }

    public GlobalVariableElement getAndRegisterGlobalMethodData(ExecutableElement originalElement) {
        Assert.assertNotNull(globalMethodData);
        if (originalElement != null && !globalMethodData.getEnclosingType().equals(originalElement.getEnclosingType())) {
            Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
            section.declareData(null, globalMethodData.getName(), globalMethodData.getType());
        }
        return globalMethodData;
    }

    public CompoundType getMethodInfoType() {
        return methodInfoType;
    }

    public CompoundType getSourceCodeInfoType() {
        return sourceCodeInfoType;
    }

    public CompoundType getGlobalMethodDataType() {
        return globalMethodDataType;
    }
}
