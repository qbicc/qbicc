// Basic helloworld example with the commands to execute it. 
//
// Compile the example with jbang (0.65.1+):
// $ jbang build examples/helloworld/hello/world/Main.java
//
// Build the native executable in /tmp/output with:
// $ jbang org.qbicc:qbicc-main:0.1.0-SNAPSHOT --boot-module-path $(jbang info classpath --deps org.qbicc.rt:qbicc-rt-java.base:11.0.1-SNAPSHOT --deps org.qbicc:qbicc-runtime-main:0.1.0-SNAPSHOT --deps org.qbicc:qbicc-runtime-unwind:0.1.0-SNAPSHOT --deps org.qbicc:qbicc-runtime-gc-nogc:0.1.0-SNAPSHOT --deps org.qbicc:qbicc-runtime-deserialization:0.1.0-SNAPSHOT examples/helloworld/hello/world/Main.java) --output-path /tmp/output hello.world.Main
//
// Run the executable
// $ /tmp/output/a.out
//
//DEPS org.qbicc:qbicc-runtime-api:0.1.0-SNAPSHOT
package hello.world;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
public class Main {
    @extern
    public static native int putchar(int arg);

    public static void main(String[] args) {
        Object o = new Object();
        putchar('h');
        putchar('e');
        putchar('l');
        putchar('l');
        putchar('o');
        putchar(' ');
        putchar('w');
        putchar('o');
        putchar('r');
        synchronized (o) {
        }
        putchar('l');
        putchar('d');
        putchar('\n');
    }
}

