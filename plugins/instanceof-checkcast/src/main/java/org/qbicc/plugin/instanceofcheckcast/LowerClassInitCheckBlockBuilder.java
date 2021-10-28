package org.qbicc.plugin.instanceofcheckcast;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * 
 */
public class LowerClassInitCheckBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final ExecutableElement originalElement;

    public LowerClassInitCheckBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        originalElement = delegate.getCurrentElement();
        this.ctxt = ctxt;
    }
 
    public Node classInitCheck(final ObjectType objectType) {
        initCheck(objectType);
        return nop();
    }

    private void initCheck(ObjectType objectType) {
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        
        final BlockLabel callInit = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        final LiteralFactory lf = ctxt.getLiteralFactory();

        Value typeId = lf.literalOfType(objectType);

        GlobalVariableElement clinitStates = tables.getAndRegisterGlobalClinitStateStruct(getCurrentElement());
        CompoundType clinitStates_t = (CompoundType) clinitStates.getType();
        ValueHandle init_state_array = memberOf(globalVariable(clinitStates), clinitStates_t.getMember("init_state"));
        Value state = load(elementOf(init_state_array, typeId), MemoryAtomicityMode.ACQUIRE);

        
        if_(isEq(state, lf.literalOf(0)), callInit, goAhead);
        try {
            begin(callInit);
            MethodElement helper = ctxt.getVMHelperMethod("initialize_class");
            getFirstBuilder().call(getFirstBuilder().staticMethod(helper, helper.getDescriptor(), helper.getType()), List.of(getFirstBuilder().currentThread(), typeId));
            goto_(goAhead);
        } catch (BlockEarlyTermination ignored) {
            //continue
        }
        begin(goAhead);
    }
}
