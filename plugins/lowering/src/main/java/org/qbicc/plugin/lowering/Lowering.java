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
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.object.Data;
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
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeSignature;

public class Lowering {
    private static final AttachmentKey<Lowering> KEY = new AttachmentKey<>();

    private final Map<ExecutableElement, LinkedHashSet<LocalVariableElement>> usedVariables = new ConcurrentHashMap<>();

    private Lowering() {}

    public static Lowering get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, Lowering::new);
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
