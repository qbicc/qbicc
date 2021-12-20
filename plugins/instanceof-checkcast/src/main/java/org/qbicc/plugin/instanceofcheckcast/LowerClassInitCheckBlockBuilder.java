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
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.type.CompoundType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

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
 
    public Node initCheck(InitializerElement initializer) {
        if (initializer.hasAllModifiersOf(ClassFile.I_ACC_RUN_TIME)) {
            // generate a run time init check

            // and call the initializer
            call(initializerOf(initializer), List.of());
        }
        // else ignore

        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);

        final BlockLabel callInit = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        final LiteralFactory lf = ctxt.getLiteralFactory();

        Value typeId = lf.literalOfType(initializer.getEnclosingType().load().getType());

        GlobalVariableElement clinitStates = tables.getAndRegisterGlobalClinitStateStruct(getCurrentElement());
        CompoundType clinitStates_t = (CompoundType) clinitStates.getType();
        ValueHandle init_state_array = memberOf(globalVariable(clinitStates), clinitStates_t.getMember("init_state"));
        Value state = load(elementOf(init_state_array, typeId), MemoryAtomicityMode.ACQUIRE);


        if_(isEq(state, lf.literalOf(0)), callInit, goAhead);
        try {
            begin(callInit);
            MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("initializeClass");
            BasicBlockBuilder fb = getFirstBuilder();
            fb.call(fb.staticMethod(helper, helper.getDescriptor(), helper.getType()), List.of(fb.load(fb.currentThread(), SingleUnshared), typeId));
            goto_(goAhead);
        } catch (BlockEarlyTermination ignored) {
            //continue
        }
        begin(goAhead);
        return nop();
    }
}
