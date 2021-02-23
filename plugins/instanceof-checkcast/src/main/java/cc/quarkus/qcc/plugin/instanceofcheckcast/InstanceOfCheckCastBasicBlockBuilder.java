package cc.quarkus.qcc.plugin.instanceofcheckcast;

import java.util.List;

import org.jboss.logging.Logger;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.NullType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A BasicBlockBuilder which replaces instanceof/checkcast operations with calls to
 * RuntimeHelper APIs.
 */
public class InstanceOfCheckCastBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final Logger log = Logger.getLogger("cc.quarkus.qcc.plugin.instanceofcheckcast");
    private static final Logger supersLog = Logger.getLogger("cc.quarkus.qcc.plugin.instanceofcheckcast.supers");
    
    private final CompilationContext ctxt;

    static final boolean PLUGIN_DISABLED = true; // don't commit yet

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

    public Value instanceOf(final Value input, final ValueType expectedType) {
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

        // TODO: missing null check on the input
        final BlockLabel nullPath = new BlockLabel();
        final BlockLabel notNull = new BlockLabel();
        final BlockLabel after = new BlockLabel();
        final NullLiteral nullLiteral = lf.literalOfNull();
        //PhiValue phi = phi(ctxt.getTypeSystem().getBooleanType(), after);
        if_(isEq(input, nullLiteral), nullPath, notNull);
        begin(nullPath);
        //PhiValue phi = phi(ctxt.getTypeSystem().getBooleanType(), after);
        // (final CompilationContext ctxt, final Element element, final BlockLabel input, final Value value)
        //phi.setValueForBlock(ctxt, getCurrentElement(), nullPath, lf.literalOf(false));
        goto_(after);
        begin(notNull);

        // 3 cases:
        // 1 - expectedType statically known to be a class
        // 2 - expectedType statically known to be an interface
        // 3 - expectedType statically known to be an array class
Value result = null;
        if (expectedType instanceof ReferenceType) {
            ReferenceType refExpectedType = (ReferenceType) expectedType;
            ClassObjectType cotExpectedType = (ClassObjectType)refExpectedType.getUpperBound();
            ValidatedTypeDefinition vtdExpectedType = cotExpectedType.getDefinition().validate();
            TypeDescriptor descriptor = vtdExpectedType.getDescriptor();
            if (descriptor instanceof ArrayTypeDescriptor) {
                // an array
            } else {
                if (vtdExpectedType.isInterface()) {
                    // interface
                } else {
                    // class
                    // Two cases here: leaf classes and those with subclasses
                    Value inputTypeId = typeIdOf(referenceHandle(input));
                    final int typeId = vtdExpectedType.getTypeId();
                    final int maxSubId = vtdExpectedType.getMaximumSubtypeId();
                    Literal vtdTypeId = ctxt.getLiteralFactory().literalOf(typeId);
                    if (typeId == maxSubId) {
                        supersLog.debug("instanceof: leaf class: direct compare: (" 
                        + typeId
                        + ") " + vtdExpectedType.getInternalName());
                        // "leaf" class case - use direct comparison
                        //return super.isEq(inputTypeId, vtdTypeId);
                        //phi.setValueForBlock(ctxt, getCurrentElement(), notNull, super.isEq(inputTypeId, vtdTypeId));
                        result = super.isEq(inputTypeId, vtdTypeId);
                    } else {
                        // "non-leaf" class case
                        // (instanceId - castClassId <= (castClassId.range - castClassId)
                        IntegerLiteral allowedRange = ctxt.getLiteralFactory().literalOf(maxSubId - typeId);
                        supersLog.debug("instanceof: non-leaf class: allowedRange: (" 
                            + maxSubId + " - " + typeId + ") = " + allowedRange.longValue()
                            + " " + vtdExpectedType.getInternalName());
                        
                        Value subtract = sub(inputTypeId, vtdTypeId);
                        //return super.isLe(subtract, allowedRange);
                        //phi.setValueForBlock(ctxt, getCurrentElement(), notNull, super.isLe(subtract, allowedRange));
                        result = super.isLe(subtract, allowedRange);
                    }
                }
            }
            goto_(after);
            begin(after);
            PhiValue phi = phi(ctxt.getTypeSystem().getBooleanType(), after);
            phi.setValueForBlock(ctxt, getCurrentElement(), nullPath, lf.literalOf(false));
            phi.setValueForBlock(ctxt, getCurrentElement(), notNull, result);
            return phi;
        }


        if (PLUGIN_DISABLED) {
            return super.instanceOf(input, expectedType);
        }
        // This code is not yet enabled.  Committing in this state so it's available
        // and so the plugin is included in the list of plugins.


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
