package org.qbicc.plugin.lowering;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.OffsetOfField;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.object.Data;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Linkage;
import org.qbicc.object.Section;
import org.qbicc.plugin.constants.Constants;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.LongAnnotationValue;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;

public class Lowering {
    public static final String GLOBAL_REFERENCES = "QBICC_GLOBALS";

    private static final AttachmentKey<Lowering> KEY = new AttachmentKey<>();

    private final Map<LoadedTypeDefinition, GlobalVariableElement> globals = new ConcurrentHashMap<>();
    private final Map<ExecutableElement, LinkedHashSet<LocalVariableElement>> usedVariables = new ConcurrentHashMap<>();

    private Lowering() {}

    public static Lowering get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, Lowering::new);
    }

    public GlobalVariableElement getStaticsGlobalForType(LoadedTypeDefinition typeDef) {
        GlobalVariableElement global = globals.get(typeDef);
        if (global != null) {
            return global;
        }
        ClassContext classContext = typeDef.getContext();
        CompilationContext ctxt = classContext.getCompilationContext();
        String itemName = "statics-" + typeDef.getInternalName().replace('/', '.');
        LayoutInfo layoutInfo = Layout.get(ctxt).getStaticLayoutInfo(typeDef);
        final GlobalVariableElement.Builder builder = GlobalVariableElement.builder(itemName, BaseTypeDescriptor.V);
        CompoundType staticsType = layoutInfo.getCompoundType();
        builder.setType(staticsType);
        builder.setSignature(BaseTypeSignature.V);
        builder.setModifiers(typeDef.getModifiers());
        builder.setEnclosingType(typeDef);
        final String sectionName = CompilationContext.IMPLICIT_SECTION_NAME;
        builder.setSection(sectionName);
        global = builder.build();
        GlobalVariableElement appearing = globals.putIfAbsent(typeDef, global);
        if (appearing != null) {
            return appearing;
        }

        Section section = ctxt.getOrAddProgramModule(typeDef).getOrAddSection(sectionName);
        // initialize values for all fields
        int cnt = typeDef.getFieldCount();
        LiteralFactory lf = ctxt.getLiteralFactory();
        boolean hasValue = false;
        Map<CompoundType.Member, Literal> valueMap = new HashMap<>(cnt);
        for (int i = 0; i < cnt; i ++) {
            FieldElement field = typeDef.getField(i);
            if (field.isStatic()) {
                Value initialValue;
                if (field.getRunTimeInitializer() != null) {
                    initialValue = lf.zeroInitializerLiteralOfType(field.getType());
                } else {
                    initialValue = field.getReplacementValue(ctxt);
                    if (initialValue == null) {
                        initialValue = ((DefinedTypeDefinition) typeDef).load().getInitialValue(field);
                    }
                    if (initialValue == null) {
                        initialValue = Constants.get(ctxt).getConstantValue(field);
                        if (initialValue == null) {
                            initialValue = lf.zeroInitializerLiteralOfType(field.getType());
                        } else {
                            hasValue = true;
                        }
                    } else {
                        hasValue = true;
                    }
                }
                CompoundType.Member member = layoutInfo.getMember(field);
                if (initialValue instanceof OffsetOfField) {
                    // special case: the field holds an offset which is really an integer
                    FieldElement offsetField = ((OffsetOfField) initialValue).getFieldElement();
                    LayoutInfo instanceLayout = Layout.get(ctxt).getInstanceLayoutInfo(offsetField.getEnclosingType());
                    if (offsetField.isStatic()) {
                        initialValue = lf.literalOf(-1);
                    } else {
                        initialValue = lf.literalOf(instanceLayout.getMember(offsetField).getOffset());
                    }
                }
                if (initialValue.getType() instanceof BooleanType && member.getType() instanceof IntegerType it) {
                    // widen the initial value
                    if (initialValue instanceof BooleanLiteral) {
                        initialValue = lf.literalOf(it, ((BooleanLiteral) initialValue).booleanValue() ? 1 : 0);
                    } else if (initialValue instanceof ZeroInitializerLiteral) {
                        initialValue = lf.literalOf(it, 0);
                    } else {
                        throw new IllegalArgumentException("Cannot initialize boolean field");
                    }
                }
                if (initialValue instanceof ObjectLiteral ol) {
                    ProgramObjectLiteral objLit = BuildtimeHeap.get(ctxt).serializeVmObject(ol.getValue());
                    DataDeclaration decl = section.declareData(objLit.getProgramObject());
                    decl.setAddrspace(1);
                    ProgramObjectLiteral refToLiteral = lf.literalOf(decl);
                    initialValue = lf.valueConvertLiteral(refToLiteral, ol.getType());
                }
                valueMap.put(member, (Literal) initialValue);
            }
        }

        final Data data = section.addData(null, itemName, hasValue ? lf.literalOf(staticsType, valueMap) : lf.zeroInitializerLiteralOfType(staticsType));
        data.setLinkage(hasValue ? Linkage.EXTERNAL : Linkage.COMMON);
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
