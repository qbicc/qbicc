package org.qbicc.plugin.constants;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public final class Constants {
    private static final AttachmentKey<Constants> KEY = new AttachmentKey<>();

    private final ConcurrentMap<FieldElement, Value> constants = new ConcurrentHashMap<>(128);

    Constants() {}

    public static Constants get(CompilationContext ctxt) {
        Constants constants = ctxt.getAttachment(KEY);
        if (constants == null) {
            constants = new Constants();
            Constants appearing = ctxt.putAttachmentIfAbsent(KEY, constants);
            if (appearing != null) {
                constants = appearing;
            }
        }
        return constants;
    }

    public boolean registerConstant(FieldElement element, Value constantValue) {
        return constants.putIfAbsent(element, constantValue) == null;
    }

    public Value getConstantValue(FieldElement element) {
        return constants.get(element);
    }
}
