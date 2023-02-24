package org.qbicc.main;

import java.io.Writer;
import java.util.Arrays;

import org.jboss.logmanager.handlers.WriterHandler;

/**
 * A logging handler that does not upset Maven's test harness.
 */
public class MavenFriendlyConsoleHandler extends WriterHandler {

    public MavenFriendlyConsoleHandler() {
        setWriter(new Writer() {
            @Override
            public void write(int c) {
                System.out.write(c);
            }

            @Override
            public void write(char[] cbuf) {
                System.out.print(cbuf);
            }

            @Override
            public void write(String str) {
                System.out.print(str);
            }

            @Override
            public void write(String str, int off, int len) {
                System.out.append(str, off, len);
            }

            @Override
            public Writer append(char c) {
                System.out.write(c);
                return this;
            }

            @Override
            public void write(char[] cbuf, int off, int len) {
                System.out.print(Arrays.copyOfRange(cbuf, off, off + len));
            }

            @Override
            public void flush() {
                System.out.flush();
            }

            @Override
            public void close() {
                flush();
            }
        });
    }
}
