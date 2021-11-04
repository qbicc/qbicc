package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class VaArg extends AbstractYieldingInstruction {
    private final AbstractValue vaListType;
    private final AbstractValue vaList;
    private final AbstractValue outputType;

    VaArg(BasicBlockImpl block, AbstractValue vaListType, AbstractValue vaList, AbstractValue outputType) {
        super(block);
        this.vaListType = vaListType;
        this.vaList = vaList;
        this.outputType = outputType;
    }

    @Override
    Appendable appendTrailer(Appendable target) throws IOException {
        vaListType.appendTo(target);
        target.append(' ');
        vaList.appendTo(target);
        target.append(',');
        target.append(' ');
        outputType.appendTo(target);
        return super.appendTrailer(target);
    }
}
