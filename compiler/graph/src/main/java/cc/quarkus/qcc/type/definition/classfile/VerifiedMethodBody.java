package cc.quarkus.qcc.type.definition.classfile;

import cc.quarkus.qcc.graph.Type;

/**
 *
 */
abstract class VerifiedMethodBody {
    final DefinedMethodBody definedBody;
    final Type[] parameterTypes;
    final Type[][] variableTypes;

    VerifiedMethodBody(final DefinedMethodBody definedBody, final Type[] parameterTypes) {
        this.definedBody = definedBody;
        this.parameterTypes = parameterTypes;
        int maxLocals = definedBody.getMaxLocals();
        Type[][] variableTypes = new Type[maxLocals][];
        for (int i = 0; i < maxLocals; i ++) {
            int cnt = definedBody.getLocalVarEntryCount(i);
            Type[] array = variableTypes[i] = new Type[cnt];
            for (int j = 0; j < cnt; j ++) {
                int idx = definedBody.getLocalVarDescriptorIndex(i, j);
                if (idx == 0) {
                    throw new MissingLocalVariableDescriptorException();
                }
                array[j] = definedBody.getClassFile().resolveSingleDescriptor(idx);
            }
        }
        this.variableTypes = variableTypes;
    }

    DefinedMethodBody getDefinedBody() {
        return definedBody;
    }

    Type getLocalVarType(int varIdx, int entryIdx) {
        return variableTypes[varIdx][entryIdx];
    }
}
