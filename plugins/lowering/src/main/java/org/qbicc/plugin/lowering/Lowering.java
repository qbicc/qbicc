package org.qbicc.plugin.lowering;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.OffsetOfField;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.object.Data;
import org.qbicc.object.Linkage;
import org.qbicc.object.Section;
import org.qbicc.plugin.constants.Constants;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.BooleanType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.LocalVariableElement;

public class Lowering {
    public static final String GLOBAL_REFERENCES = "QBICC_GLOBALS";

    private static final AttachmentKey<Lowering> KEY = new AttachmentKey<>();

    private final Map<FieldElement, GlobalVariableElement> globals = new ConcurrentHashMap<>();
    private final Map<ExecutableElement, LinkedHashSet<LocalVariableElement>> usedVariables = new ConcurrentHashMap<>();

    private Lowering() {}

    public static Lowering get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, Lowering::new);
    }

    public GlobalVariableElement getGlobalForField(FieldElement fieldElement) {
        GlobalVariableElement global = globals.get(fieldElement);
        if (global != null) {
            return global;
        }
        DefinedTypeDefinition enclosingType = fieldElement.getEnclosingType();
        ClassContext classContext = enclosingType.getContext();
        CompilationContext ctxt = classContext.getCompilationContext();
        ValueType fieldType = fieldElement.getType();
        String itemName = "static-" + enclosingType.getInternalName().replace('/', '.') + "-" + fieldElement.getName() + "-" + fieldElement.getIndex();
        final GlobalVariableElement.Builder builder = GlobalVariableElement.builder();
        builder.setName(itemName);
        final ValueType varType = fieldType instanceof BooleanType ? ctxt.getTypeSystem().getUnsignedInteger8Type() : fieldType;
        builder.setType(varType);
        builder.setDescriptor(fieldElement.getTypeDescriptor());
        builder.setSignature(fieldElement.getTypeSignature());
        builder.setModifiers(fieldElement.getModifiers());
        builder.setEnclosingType(enclosingType);
        final String sectionName;
        if (fieldElement.getType() instanceof ReferenceType) {
            sectionName = CompilationContext.IMPLICIT_SECTION_NAME; // TODO: GLOBAL_REFERENCES;
        } else {
            sectionName = CompilationContext.IMPLICIT_SECTION_NAME;
        }
        builder.setSection(sectionName);
        global = builder.build();
        GlobalVariableElement appearing = globals.putIfAbsent(fieldElement, global);
        if (appearing != null) {
            return appearing;
        }

        Section section = ctxt.getOrAddProgramModule(enclosingType).getOrAddSection(sectionName);
        Value initialValue = enclosingType.load().getInitialValue(fieldElement);
        if (initialValue == null) {
            initialValue = Constants.get(ctxt).getConstantValue(fieldElement);
        }
        if (initialValue == null) {
            initialValue = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(varType);
        }
        if (initialValue instanceof OffsetOfField) {
            // special case: the field holds an offset which is really an integer
            FieldElement offsetField = ((OffsetOfField) initialValue).getFieldElement();
            LayoutInfo layoutInfo = Layout.get(ctxt).getInstanceLayoutInfo(offsetField.getEnclosingType());
            if (offsetField.isStatic()) {
                initialValue = ctxt.getLiteralFactory().literalOf(-1);
            } else {
                initialValue = ctxt.getLiteralFactory().literalOf(layoutInfo.getMember(offsetField).getOffset());
            }
        }
        if (initialValue.getType() instanceof BooleanType) {
            assert varType instanceof IntegerType;
            // widen the initial value
            if (initialValue instanceof BooleanLiteral) {
                initialValue = ctxt.getLiteralFactory().literalOf((IntegerType) varType, ((BooleanLiteral) initialValue).booleanValue() ? 1 : 0);
            } else if (initialValue instanceof ZeroInitializerLiteral) {
                initialValue = ctxt.getLiteralFactory().literalOf((IntegerType) varType, 0);
            } else {
                throw new IllegalArgumentException("Cannot initialize boolean field");
            }
        }
        if (initialValue instanceof ObjectLiteral) {
            SymbolLiteral objLit = BuildtimeHeap.get(ctxt).serializeVmObject(((ObjectLiteral) initialValue).getValue());
            section.declareData(null, objLit.getName(), objLit.getType()).setAddrspace(1);
            SymbolLiteral refToLiteral = ctxt.getLiteralFactory().literalOfSymbol(objLit.getName(), objLit.getType().getPointer().asCollected());
            initialValue = ctxt.getLiteralFactory().bitcastLiteral(refToLiteral, ((ObjectLiteral) initialValue).getType());
        }

        final Data data = section.addData(fieldElement, itemName, initialValue);
        data.setLinkage(initialValue instanceof ZeroInitializerLiteral ? Linkage.COMMON : Linkage.EXTERNAL);
        data.setDsoLocal();
        return global;
    }


    LinkedHashSet<LocalVariableElement> createUsedVariableSet(ExecutableElement element) {
        final LinkedHashSet<LocalVariableElement> set = new LinkedHashSet<>();
        usedVariables.put(element, set);
        return set;
    }

    LinkedHashSet<LocalVariableElement> removeUsedVariableSet(ExecutableElement element) {
        final LinkedHashSet<LocalVariableElement> set = usedVariables.remove(element);
        if (set == null) {
            throw new NoSuchElementException();
        }
        return set;
    }
}
