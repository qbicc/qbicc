package org.qbicc.context;

import io.smallrye.common.constraint.Assert;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Something which has a program location.
 */
public interface ProgramLocatable extends Locatable {
    int lineNumber();
    int bytecodeIndex();
    ExecutableElement element();
    ProgramLocatable callSite();

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

    default Frozen freeze() {
        return new Frozen(lineNumber(), bytecodeIndex(), element(), callSite());
    }

    default ProgramLocatable withUnderlyingCallSite(ProgramLocatable callSite) {
        if (callSite == null) {
            return this;
        }
        ProgramLocatable ourCallSite = callSite();
        if (ourCallSite == null) {
            return new Frozen(lineNumber(), bytecodeIndex(), element(), callSite);
        } else {
            return new Frozen(lineNumber(), bytecodeIndex(), element(), ourCallSite.withUnderlyingCallSite(callSite));
        }
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

    /**
     * A {@code ProgramLocatable} which is immutable and is suitable for usage as a map key.
     *
     * @param lineNumber the line number
     * @param bytecodeIndex the bci
     * @param element the element (must not be {@code null})
     * @param callSite the call site, or {@code null} if none
     */
    record Frozen(int lineNumber, int bytecodeIndex, ExecutableElement element, ProgramLocatable callSite) implements ProgramLocatable {
        public Frozen {
            Assert.checkNotNullParam("element", element);
        }

        @Override
        public Frozen freeze() {
            return this;
        }
    }
}
