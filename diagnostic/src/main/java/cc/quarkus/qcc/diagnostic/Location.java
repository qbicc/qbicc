package cc.quarkus.qcc.diagnostic;

import java.util.Locale;

/**
 *
 */
public final class Location {
    private final Location parent;
    private final LinkType parentLink;
    private final String fileName;
    private final String memberName;
    private final int lineNumber;
    private final int byteCodeIndex;

    public Location(final String fileName, final int lineNumber) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        parent = null;
        parentLink = null;
        memberName = null;
        byteCodeIndex = -1;
    }

    public Location(final Location parent, final LinkType parentLink, final String fileName, final int lineNumber) {
        this.parent = parent;
        this.parentLink = parentLink;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        memberName = null;
        byteCodeIndex = -1;
    }

    public Location getParent() {
        return parent;
    }

    public LinkType getParentLink() {
        return parentLink;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getMemberName() {
        return memberName;
    }

    public int getByteCodeIndex() {
        return byteCodeIndex;
    }

    public String toString() {
        return toString(new StringBuilder(), null).toString();
    }

    private StringBuilder toString(final StringBuilder b, LinkType parentLink) {
        b.append("\t");
        if (parentLink != null) {
            b.append(parentLink).append(' ');
        }
        b.append("at");
        b.append(' ');
        if (memberName != null) {
            b.append(memberName).append(' ');
        }
        if (byteCodeIndex != -1) {
            b.append("bci: ").append(byteCodeIndex).append(' ');
        }
        b.append(fileName);
        if (lineNumber != -1) {
            b.append(':');
            b.append(lineNumber);
        }
        if (parent != null) {
            b.append('\n');
            parent.toString(b, this.parentLink);
        }
        return b;
    }

    public enum LinkType {
        INCLUDED,
        DEFINED,
        REFERENCED,
        SOURCE,
        ;

        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
