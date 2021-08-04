package org.qbicc.plugin.methodinfo;

import org.qbicc.type.definition.element.ExecutableElement;

final class InstructionMap {
    private int offset;
    private int sourceCodeIndex;
    private ExecutableElement function;

    InstructionMap(int offset, int sourceCodeIndex, ExecutableElement element) {
        this.offset = offset;
        this.sourceCodeIndex = sourceCodeIndex;
        this.function = element;
    }
    public int getOffset() {
        return offset;
    }

    public int getSourceCodeIndex() {
        return sourceCodeIndex;
    }

    public ExecutableElement getFunction() {
        return function;
    }
}
