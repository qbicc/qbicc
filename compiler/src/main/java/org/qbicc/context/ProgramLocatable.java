package org.qbicc.context;

import org.qbicc.graph.Node;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Something which has a program location.
 */
public interface ProgramLocatable extends Locatable {
    int lineNumber();
    int bytecodeIndex();
    ExecutableElement element();
    Node callSite();

    default String sourceFileName() {
        ExecutableElement element = element();
        return element == null ? null : element.getSourceFileName();
    }

    default DefinedTypeDefinition enclosingType() {
        ExecutableElement element = element();
        return element == null ? null : element.getEnclosingType();
    }

    default Location getLocation() {
        ExecutableElement element = element();
        Location.Builder lb = Location.builder();
        if (element != null) {
            lb.setElement(element);
        } else {
            String name = sourceFileName();
            if (name != null) {
                lb.setSourceFilePath(name);
            }
            DefinedTypeDefinition enclosingType = enclosingType();
            if (enclosingType != null) {
                lb.setClassInternalName(enclosingType.getInternalName());
            }
        }
        int line = lineNumber();
        if (line > 0) {
            lb.setLineNumber(line);
        }
        int bci = bytecodeIndex();
        if (bci >= 0) {
            lb.setByteCodeIndex(bci);
        }
        // (call site is not used by Location)
        return lb.build();
    }

    static boolean hasSameLocation(ProgramLocatable left, ProgramLocatable right) {
        if (left == null) {
            return right == null;
        } else if (right == null) {
            return false;
        } else {
            return left.lineNumber() == right.lineNumber()
                && left.bytecodeIndex() == right.bytecodeIndex()
                && left.element() == right.element()
                && hasSameLocation(left.callSite(), right.callSite());
        }
    }
}
