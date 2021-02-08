package cc.quarkus.qcc.machine.tool.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class CharSequenceInputSource extends InputSource {
    private final CharSequence charSequence;
    private final Charset charset;

    CharSequenceInputSource(final CharSequence charSequence, final Charset charset) {
        this.charSequence = charSequence;
        this.charset = charset;
    }

    public void transferTo(final OutputDestination destination) throws IOException {
        CharSequence charSequence = this.charSequence;
        if (charSequence instanceof String) {
            destination.transferFrom(new StringReader((String) charSequence), charset);
        } else {
            destination.transferFrom(new CharSequenceReader(charSequence), charset);
        }
    }

    ProcessBuilder.Redirect getInputRedirect() {
        return ProcessBuilder.Redirect.PIPE;
    }

    void transferTo(OutputStream os) throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(os, charset)) {
            osw.append(charSequence);
        }
    }

    void transferTo(final Appendable destination) throws IOException {
        destination.append(charSequence);
    }

    InputStream openStream() {
        return new CharBufferInputStream(CharBuffer.wrap(charSequence), charset.newEncoder());
    }

    void writeTo(final Path path) throws IOException {
        Files.writeString(path, charSequence, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public String toString() {
        return "<string>";
    }
}
