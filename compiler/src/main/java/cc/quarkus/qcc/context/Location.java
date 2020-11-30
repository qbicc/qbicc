package cc.quarkus.qcc.context;

import static java.lang.Math.*;

import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.type.definition.element.BasicElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import io.smallrye.common.constraint.Assert;

/**
 * The location of a diagnostic message.
 */
public final class Location {
    public static final Location NO_LOC = builder().build();

    private final String classSimpleName;
    private final String classInternalName;
    private final String classFilePath;
    private final String sourceFilePath;
    private final MemberKind memberKind;
    private final String memberName;
    private final int lineNumber;
    private final int byteCodeIndex;

    Location(Builder builder) {
        String internalName = builder.classInternalName;
        if (internalName != null && ! internalName.isEmpty()) {
            classInternalName = internalName;
            int i = internalName.lastIndexOf('/');
            if (i >= 0) {
                classSimpleName = internalName.substring(i + 1);
            } else {
                classSimpleName = internalName;
            }
        } else {
            classInternalName = classSimpleName = null;
        }
        classFilePath = builder.classFilePath;
        sourceFilePath = builder.sourceFilePath;
        memberKind = builder.memberKind;
        memberName = builder.memberName;
        lineNumber = max(0, builder.lineNumber);
        byteCodeIndex = max(-1, builder.byteCodeIndex);
    }

    public String getClassSimpleName() {
        return classSimpleName;
    }

    public String getClassInternalName() {
        return classInternalName;
    }

    public boolean hasClassName() {
        return classInternalName != null;
    }

    public String getClassFilePath() {
        return classFilePath;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public MemberKind getMemberKind() {
        return memberKind;
    }

    public String getMemberName() {
        return memberName;
    }

    public boolean hasMemberName() {
        return memberName != null;
    }

    public boolean hasLocation() {
        return sourceFilePath != null || classFilePath != null || classInternalName != null || lineNumber > 0;
    }

    public int getByteCodeIndex() {
        return byteCodeIndex;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        appendBaseString(b);
        if (hasMemberName()) {
            b.append("; ");
            appendMemberString(b);
        }
        if (hasClassName()) {
            b.append("; ");
            appendLocationString(b);
        }
        return b.toString();
    }

    public StringBuilder appendMemberString(final StringBuilder b) {
        b.append("member: ").append(memberKind).append(' ').append(memberName);
        if (byteCodeIndex >= 0) {
            b.append("@ bci ").append(byteCodeIndex);
        }
        return b;
    }

    public StringBuilder appendLocationString(final StringBuilder b) {
        return b.append("location: type ").append(classSimpleName);
    }

    public StringBuilder appendBaseString(final StringBuilder b) {
        if (sourceFilePath != null) {
            b.append(sourceFilePath);
        } else if (classFilePath != null) {
            b.append(classFilePath);
        } else if (classInternalName != null) {
            b.append(classInternalName).append(".class");
        } else {
            b.append("<no location>");
        }
        if (lineNumber > 0) {
            b.append(':');
            b.append(lineNumber);
        }
        return b;
    }

    public enum MemberKind {
        NONE("member"),
        FIELD("field"),
        METHOD("method"),
        VARIABLE("variable"),
        FUNCTION("function"),
        ;

        final String str;

        MemberKind(final String str) {
            this.str = str;
        }

        public String toString() {
            return str;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Location fromStackTrace(Throwable t) {
        StackTraceElement[] elements = t.getStackTrace();
        if (elements.length >= 1) {
            StackTraceElement element = elements[0];
            return builder()
                .setSourceFilePath(element.getFileName())
                .setClassInternalName(element.getClassName().replace('.', '/'))
                .setLineNumber(element.getLineNumber())
                .setMemberName(element.getMethodName())
                .setMemberKind(MemberKind.METHOD)
                .build();
        } else {
            return NO_LOC;
        }
    }

    public static final class Builder {
        private String classInternalName;
        private String classFilePath;
        private String sourceFilePath;
        private MemberKind memberKind = MemberKind.NONE;
        private String memberName;
        private int lineNumber = 0;
        private int byteCodeIndex = -1;

        Builder() {}

        public String getClassInternalName() {
            return classInternalName;
        }

        public Builder setClassInternalName(final String classInternalName) {
            this.classInternalName = classInternalName;
            return this;
        }

        public String getClassFilePath() {
            return classFilePath;
        }

        public Builder setClassFilePath(final String classFilePath) {
            this.classFilePath = classFilePath;
            return this;
        }

        public String getSourceFilePath() {
            return sourceFilePath;
        }

        public Builder setSourceFilePath(final String sourceFilePath) {
            this.sourceFilePath = sourceFilePath;
            return this;
        }

        public MemberKind getMemberKind() {
            return memberKind;
        }

        public Builder setMemberKind(final MemberKind memberKind) {
            this.memberKind = Assert.checkNotNullParam("memberKind", memberKind);
            return this;
        }

        public String getMemberName() {
            return memberName;
        }

        public Builder setMemberName(final String memberName) {
            this.memberName = memberName;
            return this;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public Builder setLineNumber(final int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public int getByteCodeIndex() {
            return byteCodeIndex;
        }

        public Builder setByteCodeIndex(final int byteCodeIndex) {
            this.byteCodeIndex = byteCodeIndex;
            return this;
        }

        public Builder setElement(BasicElement element) {
            setMemberKind(element instanceof MethodElement ? Location.MemberKind.METHOD : element instanceof FieldElement ? Location.MemberKind.FIELD : Location.MemberKind.NONE);
            setMemberName(element.toString());
            setSourceFilePath(element.getSourceFileName());
            setClassInternalName(element.getEnclosingType().getInternalName());
            return this;
        }

        public Builder setNode(Node node) {
            setLineNumber(node.getSourceLine());
            setByteCodeIndex(node.getBytecodeIndex());
            return this;
        }

        public Location build() {
            return new Location(this);
        }
    }
}
