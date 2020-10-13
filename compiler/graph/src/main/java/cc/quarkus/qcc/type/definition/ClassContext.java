package cc.quarkus.qcc.type.definition;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.TypeSystem;

/**
 * A class and interface context, which can either be standalone (static) or can be integrated with an interpreter.  An
 * interpreter should have one instance per class loader.
 */
public interface ClassContext {
    DefinedTypeDefinition findDefinedType(String typeName);

    DefinedTypeDefinition resolveDefinedTypeLiteral(TypeIdLiteral typeId);

    String deduplicate(ByteBuffer buffer, int offset, int length);

    String deduplicate(String original);

    TypeSystem getTypeSystem();

    LiteralFactory getLiteralFactory();
}
