package cc.quarkus.qcc.plugin.instanceofcheckcast;

import java.util.List;

import org.jboss.logging.Logger;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.InterfaceObjectType;
import cc.quarkus.qcc.type.NullType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A BasicBlockBuilder which replaces instanceof/checkcast operations with calls to
 * RuntimeHelper APIs.
 */
public class InstanceOfCheckCastBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final Logger log = Logger.getLogger("cc.quarkus.qcc.plugin.instanceofcheckcast");
    
    private final CompilationContext ctxt;

    static final boolean PLUGIN_DISABLED = true;

    public InstanceOfCheckCastBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value narrow(Value value, ValueType toType) {
        if (toType instanceof ReferenceType) {
            ReferenceType refExpectedType = (ReferenceType) toType;
            ValueType actualType = value.getType();
            if (actualType instanceof ReferenceType) {
                if (((ReferenceType) actualType).instanceOf(refExpectedType)) {
                    // the reference type matches statically
                    return value;
                }
            }
        }
        return super.narrow(value, toType);
    }

    public Value instanceOf(final Value input, ObjectType classFileType, final ValueType expectedType) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        // "null" instanceof <X> is always false
        if (NullType.isAlwaysNull(input)) {
            return lf.literalOf(false);
        }
        
        // statically true instanceof checks are equal to x != null
        if (expectedType instanceof ReferenceType) {
            ReferenceType refExpectedType = (ReferenceType) expectedType;
            ValueType actualType = input.getType();
            if (actualType instanceof ReferenceType) {
                if (((ReferenceType) actualType).instanceOf(refExpectedType)) {
                    // the reference type matches statically
                    return super.isNe(input, lf.literalOfNull());
                }
            }
        }

        
        /* Set up the runtime checks here for the 3 major cases:
         * 1 - expectedType statically known to be an array class
         * 2 - expectedType statically known to be an interface
         * 3 - expectedType statically known to be a class
         * The check takes the form of:
         *   boolean result = false;
         *   if (input != null) result = doCheck(input, expectedType);
         *   return result;
         */
        final BlockLabel notNullLabel = new BlockLabel();
        final BlockLabel afterCheckLabel = new BlockLabel();
        final NullLiteral nullLiteral = lf.literalOfNull();
        
        BasicBlock incomingBlock = if_(isNe(input, nullLiteral), notNullLabel, afterCheckLabel);
        begin(notNullLabel);
        Value result = null;
        if (classFileType instanceof ArrayObjectType) {
                // 1 - expectedType statically known to be an array class
            // TODO
            result = lf.literalOf(false);
        } else if (classFileType instanceof InterfaceObjectType) {
            // 2 - expectedType statically known to be an interface
            // TODO
            result = lf.literalOf(false);
        } else {
            // 3 - expectedType statically known to be a class
            // There are two sub cases when dealing with classes:
            // A - leaf classes that have no subclasses can be a direct compare
            // B - non-leaf classes need a subtract + compare
            ClassObjectType cotExpectedType = (ClassObjectType)classFileType;
            ValidatedTypeDefinition vtdExpectedType = cotExpectedType.getDefinition().validate();
            Value inputTypeId = typeIdOf(referenceHandle(input));
            final int typeId = vtdExpectedType.getTypeId();
            final int maxSubId = vtdExpectedType.getMaximumSubtypeId();
            Literal vtdTypeId = lf.literalOf(typeId);
            if (typeId == maxSubId) {
                // "leaf" class case - use direct comparison
                result = super.isEq(inputTypeId, vtdTypeId);
            } else {
                // "non-leaf" class case
                // (instanceId - castClassId <= (castClassId.range - castClassId)
                IntegerLiteral allowedRange = lf.literalOf(maxSubId - typeId);
                Value subtract = sub(inputTypeId, vtdTypeId);
                result = super.isLe(subtract, allowedRange);
            }
        }
        goto_(afterCheckLabel);
        begin(afterCheckLabel);

        PhiValue phi = phi(ctxt.getTypeSystem().getBooleanType(), afterCheckLabel);
        phi.setValueForBlock(ctxt, getCurrentElement(), incomingBlock, lf.literalOf(false));
        phi.setValueForBlock(ctxt, getCurrentElement(), notNullLabel, result);
        return phi;

    }

    Value generateCallToRuntimeHelper(final Value input, ObjectType classFileType, final ValueType expectedType) {
                // This code is not yet enabled.  Committing in this state so it's available
        // and so the plugin is included in the list of plugins.

        if (PLUGIN_DISABLED) {
            return super.instanceOf(input, classFileType, expectedType);
        }
        LiteralFactory lf = ctxt.getLiteralFactory();
        ctxt.info("Lowering instanceof:" + expectedType.getClass());
        // Value result = super.instanceOf(input, expectedType);
        // convert InstanceOf into a new FunctionCall()
        // RuntimeHelpers.fast_instanceof(CurrentThread, Value, ValueType) {
        //  cheap checks for class depth and then probe supers[]
        //  for array cases, etc, call RuntimeHelpers.slow_instanceOf(CurrentThread, Value, ValueType)
        // and let the optimizer inline the 'fast_instanceof' call and hope the rest is removed
        // mark the slow path as @noinline
        // DelegatingBasicBlockBuilder.getLocation() to get the bci & line
        ClassContext bootstrapCC = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition dtd = bootstrapCC.findDefinedType("cc/quarkus/qcc/runtime/main/VMHelpers");
        if (dtd == null) {
            ctxt.error("Can't find runtime library class: " + "cc/quarkus/qcc/runtime/main/VMHelpers");
        }
        ValidatedTypeDefinition resolved = dtd.validate();

        int idx = resolved.findMethodIndex(e -> "fast_instanceof".equals(e.getName()));
        assert(idx != -1);
        MethodElement methodElement = resolved.getMethod(idx);
        ctxt.registerEntryPoint(methodElement);
        Function function = ctxt.getExactFunction(methodElement);
        List<Value> args = List.of(input, lf.literalOfType(expectedType));
        return super.callFunction(lf.literalOfSymbol(function.getName(), function.getType()), args);
    }

    // TODO: Find equivalent checkcast methods to implement here as well
}
