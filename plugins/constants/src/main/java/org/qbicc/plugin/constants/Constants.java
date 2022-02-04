package org.qbicc.plugin.constants;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import io.smallrye.common.function.Functions;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public final class Constants {
    private static final AttachmentKey<Constants> KEY = new AttachmentKey<>();

    private final ConcurrentMap<FieldElement, Supplier<Value>> constants = new ConcurrentHashMap<>(128);

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

    public boolean registerConstant(FieldElement element, Supplier<Value> factory) {
        return constants.putIfAbsent(element, new ConstantFactory(element, factory)) == null;
    }

    public Value getConstantValue(FieldElement element) {
        Supplier<Value> supplier = constants.get(element);
        return supplier == null ? null : supplier.get();
    }

    final class ConstantFactory implements Supplier<Value> {
        private final FieldElement fieldElement;
        private final Supplier<Value> probe;
        private volatile Value value;

        ConstantFactory(FieldElement fieldElement, Supplier<Value> probe) {
            this.fieldElement = fieldElement;
            this.probe = probe;
        }

        @Override
        public Value get() {
            Value value = this.value;
            if (value == null) {
                synchronized (this) {
                    value = this.value;
                    if (value == null) {
                        value = probe.get();
                        this.value = value;
                        // release the factory
                        constants.replace(fieldElement, this, Functions.constantSupplier(value));
                    }
                }
            }
            return value;
        }
    }
}
