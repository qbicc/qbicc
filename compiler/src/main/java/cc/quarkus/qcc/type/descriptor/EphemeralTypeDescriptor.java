package cc.quarkus.qcc.type.descriptor;

import cc.quarkus.qcc.graph.type.CompletionToken;
import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.graph.type.IfToken;
import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.graph.type.StartToken;
import cc.quarkus.qcc.graph.type.ThrowToken;
import cc.quarkus.qcc.type.QType;

public class EphemeralTypeDescriptor<T extends QType> implements TypeDescriptor<T> {

    public static final TypeDescriptor<StartToken> START_TOKEN = new cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor<>(StartToken.class);
    public static final TypeDescriptor<EndToken> END_TOKEN = new cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor<>(EndToken.class);

    public static final TypeDescriptor<InvokeToken> INVOKE_TOKEN = new cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor<>(InvokeToken.class);
    public static final TypeDescriptor<ThrowToken> THROW_TOKEN = new cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor<>(ThrowToken.class);

    public static final TypeDescriptor<IfToken> IF_TOKEN = new cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor<>(IfToken.class);
    public static final TypeDescriptor<ControlToken> CONTROL_TOKEN = new cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor<>(ControlToken.class);
    public static final TypeDescriptor<IOToken> IO_TOKEN = new cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor<>(IOToken.class);
    public static final TypeDescriptor<MemoryToken> MEMORY_TOKEN = new cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor<>(MemoryToken.class);

    public static final TypeDescriptor<CompletionToken> COMPLETION_TOKEN = new cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor<>(CompletionToken.class);

    private EphemeralTypeDescriptor(Class<T> valueType) {
        this.valueType = valueType;
    }

    @Override
    public Class<T> type() {
        return this.valueType;
    }

    @Override
    public String label() {
        return this.valueType.getSimpleName();
    }

    private final Class<T> valueType;
}
