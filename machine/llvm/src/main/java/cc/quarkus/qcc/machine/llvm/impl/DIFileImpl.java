package cc.quarkus.qcc.machine.llvm.impl;

import cc.quarkus.qcc.machine.llvm.debuginfo.DIFile;

import java.io.IOException;

final class DIFileImpl extends AbstractMetadataNode implements DIFile {
    private final String filename;
    private final String directory;

    DIFileImpl(final int index, final String filename, final String directory) {
        super(index);
        this.filename = filename;
        this.directory = directory;
    }

    public Appendable appendTo(Appendable target) throws IOException {
        super.appendTo(target);

        target.append("!DIFile(filename: ");
        appendEscapedString(target, filename);
        target.append(", directory: ");
        appendEscapedString(target, directory);

        target.append(')');
        return appendTrailer(target);
    }

    public DIFile comment(final String comment) {
        return (DIFile) super.comment(comment);
    }
}
