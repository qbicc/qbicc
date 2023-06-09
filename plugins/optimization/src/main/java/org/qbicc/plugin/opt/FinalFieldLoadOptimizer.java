package org.qbicc.plugin.opt;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.TypeIdType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * This optimization is run during the ANALYZE phase and replaces loads
 * of reallyFinal fields with their constant values using the build-time
 * heap constructed by the interpreter during the ADD phase.
 *
 * It optimizes static field and instance fields on ObjectLiterals.
 */
public class FinalFieldLoadOptimizer extends DelegatingBasicBlockBuilder {
    final CompilationContext ctxt;

    public FinalFieldLoadOptimizer(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value load(Value pointer, ReadAccessMode accessMode) {
        if (pointer instanceof StaticFieldLiteral sfl && sfl.getVariableElement().isReallyFinal()) {
            StaticFieldElement fieldElement = sfl.getVariableElement();
            Value contents = fieldElement.getEnclosingType().load().getInitialValue(fieldElement);
            if (contents instanceof Literal) {
                // ctxt.info("Replacing getstatic of "+fieldElement+" in "+getDelegate().getCurrentElement());
                return contents;
            }
        } else if (pointer instanceof InstanceFieldOf ifo && ifo.getVariableElement().isReallyFinal() && ifo.getInstance() instanceof ObjectLiteral ol) {
            InstanceFieldElement fieldElement = ifo.getVariableElement();
            VmClass vmClass = ol.getValue().getVmClass();
            int offset = vmClass.indexOf(fieldElement);
            Memory mem = ol.getValue().getMemory();
            TypeDescriptor desc = fieldElement.getTypeDescriptor();
            Literal contents;
            if (desc.equals(BaseTypeDescriptor.Z)) {
                int val = mem.load8(offset, SinglePlain);
                contents = ctxt.getLiteralFactory().literalOf(val != 0);
            } else if (desc.equals(BaseTypeDescriptor.B)) {
                contents = ctxt.getLiteralFactory().literalOf((byte) mem.load8(offset, SinglePlain));
            } else if (desc.equals(BaseTypeDescriptor.S)) {
                contents = ctxt.getLiteralFactory().literalOf((short) mem.load16(offset, SinglePlain));
            } else if (desc.equals(BaseTypeDescriptor.C)) {
                contents = ctxt.getLiteralFactory().literalOf((char) mem.load16(offset, SinglePlain));
            } else if (desc.equals(BaseTypeDescriptor.I)) {
                contents = ctxt.getLiteralFactory().literalOf(mem.load32(offset, SinglePlain));
            } else if (desc.equals(BaseTypeDescriptor.F)) {
                contents =  ctxt.getLiteralFactory().literalOf(mem.loadFloat(offset, SinglePlain));
            } else if (desc.equals(BaseTypeDescriptor.J)) {
                contents =  ctxt.getLiteralFactory().literalOf(mem.load64(offset, SinglePlain));
            } else if (desc.equals(BaseTypeDescriptor.D)) {
                contents =  ctxt.getLiteralFactory().literalOf(mem.loadDouble(offset, SinglePlain));
            } else {
               if (ifo.getPointeeType() instanceof TypeIdType) {
                   ValueType tt = mem.loadType(offset, SinglePlain);
                   contents = ctxt.getLiteralFactory().literalOfType(tt);
               } else {
                   VmObject value = mem.loadRef(offset, SinglePlain);
                   if (value == null) {
                       contents =  ctxt.getLiteralFactory().zeroInitializerLiteralOfType(fieldElement.getType());
                   } else {
                       contents =  ctxt.getLiteralFactory().literalOf(value);
                   }
               }
            }
            // ctxt.info("Replacing getfield of "+fieldElement+" in "+getDelegate().getCurrentElement());
            return contents;
        }

        return getDelegate().load(pointer, accessMode);
    }
}
