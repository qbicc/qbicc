package cc.quarkus.qcc.plugin.native_;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
final class NativeInfo {
    static final AttachmentKey<NativeInfo> KEY = new AttachmentKey<>();

    final Map<MethodElement, NativeFunctionInfo> nativeFunctions = new ConcurrentHashMap<>();
    final Map<DefinedTypeDefinition, ValueType> nativeTypes = new ConcurrentHashMap<>();

    static NativeInfo get(final CompilationContext ctxt) {
        NativeInfo nativeInfo = ctxt.getAttachment(KEY);
        if (nativeInfo == null) {
            NativeInfo appearing = ctxt.putAttachmentIfAbsent(KEY, nativeInfo = new NativeInfo());
            if (appearing != null) {
                nativeInfo = appearing;
            }
        }
        return nativeInfo;
    }
}
