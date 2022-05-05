package org.qbicc.plugin.lowering;

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
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.object.Data;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Linkage;
import org.qbicc.object.ModuleSection;
import org.qbicc.plugin.constants.Constants;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeSignature;

public class Lowering {
    public static final String GLOBAL_REFERENCES = "QBICC_GLOBALS";

    private static final AttachmentKey<Lowering> KEY = new AttachmentKey<>();

    private final Map<FieldElement, GlobalVariableElement> staticFields = new ConcurrentHashMap<>();
    private final Map<ExecutableElement, LinkedHashSet<LocalVariableElement>> usedVariables = new ConcurrentHashMap<>();

    private Lowering() {}

    public static Lowering get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, Lowering::new);
    }

    public GlobalVariableElement getGlobalForStaticField(FieldElement field) {
        GlobalVariableElement global = staticFields.get(field);
        if (global != null) {
            return global;
        }
        DefinedTypeDefinition typeDef = field.getEnclosingType();
        ClassContext classContext = typeDef.getContext();
        CompilationContext ctxt = classContext.getCompilationContext();
        StringBuilder b = new StringBuilder(64);
        // todo: consider class loader
        b.append(typeDef.getInternalName().replace('/', '.'));
        b.append('.');
        b.append(field.getName());
        TypeDescriptor fieldDesc = field.getTypeDescriptor();
        String globalName = b.toString();
        final GlobalVariableElement.Builder builder = GlobalVariableElement.builder(globalName, fieldDesc);
        ValueType globalType = widenBoolean(field.getType());
        builder.setType(globalType);
        builder.setSignature(TypeSignature.synthesize(classContext, fieldDesc));
        builder.setModifiers(field.getModifiers());
        builder.setEnclosingType(typeDef);
        String sectionName = CompilationContext.IMPLICIT_SECTION_NAME;
        builder.setSection(sectionName);
        global = builder.build();
        GlobalVariableElement appearing = staticFields.putIfAbsent(field, global);
        if (appearing != null) {
            return appearing;
        }
        // Sleazy hack.  The static fields of the InitialHeap class will be declared during serialization.
        if (typeDef.internalPackageAndNameEquals("org/qbicc/runtime/main", "InitialHeap")) {
            return global;
        }
        // we added it, so we must add the definition as well
        LiteralFactory lf = ctxt.getLiteralFactory();
        ModuleSection section = ctxt.getOrAddProgramModule(typeDef).getOrAddSection(sectionName);
        Value initialValue;
        boolean hasValue;
        if (field.getRunTimeInitializer() != null) {
            initialValue = lf.zeroInitializerLiteralOfType(globalType);
            hasValue = false;
        } else {
            initialValue = field.getReplacementValue(ctxt);
            if (initialValue == null) {
                initialValue = typeDef.load().getInitialValue(field);
            }
            if (initialValue == null) {
                initialValue = Constants.get(ctxt).getConstantValue(field);
                if (initialValue == null) {
                    initialValue = lf.zeroInitializerLiteralOfType(globalType);
                    hasValue = false;
                } else {
                    hasValue = true;
                }
            } else {
                hasValue = true;
            }
        }
        if (initialValue instanceof OffsetOfField oof) {
            // special case: the field holds an offset which is really an integer
            FieldElement offsetField = oof.getFieldElement();
            LayoutInfo instanceLayout = Layout.get(ctxt).getInstanceLayoutInfo(offsetField.getEnclosingType());
            if (offsetField.isStatic()) {
                initialValue = lf.literalOf(0);
            } else {
                initialValue = lf.literalOf(instanceLayout.getMember(offsetField).getOffset());
            }
        }
        if (initialValue.getType() instanceof BooleanType && globalType instanceof IntegerType it) {
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
            BuildtimeHeap bth = BuildtimeHeap.get(ctxt);
            bth.serializeVmObject(ol.getValue());
            initialValue = bth.referToSerializedVmObject(ol.getValue(), ol.getType(), section.getProgramModule());
        }
        final Data data = section.addData(field, globalName, initialValue);
        data.setLinkage(hasValue ? Linkage.EXTERNAL : Linkage.COMMON);
        data.setDsoLocal();
        return global;
    }

    private ValueType widenBoolean(ValueType type) {
        // todo: n-bit booleans
        if (type instanceof BooleanType) {
            TypeSystem ts = type.getTypeSystem();
            return ts.getUnsignedInteger8Type();
        } else if (type instanceof ArrayType arrayType) {
            TypeSystem ts = type.getTypeSystem();
            ValueType elementType = arrayType.getElementType();
            ValueType widened = widenBoolean(elementType);
            return elementType == widened ? type : ts.getArrayType(widened, arrayType.getElementCount());
        } else {
            return type;
        }
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
