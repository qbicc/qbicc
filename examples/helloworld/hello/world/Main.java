// Basic helloworld example with the commands to execute it. 
//
// Compile the example with jbang (0.65.1+):
// $ jbang build hello/world/Main.java
//
// Build the native executable in /tmp/output with:
// $ jbang cc.quarkus:qcc-main:1.0.0-SNAPSHOT --boot-module-path $(jbang info classpath --deps cc.quarkus:qccrt-java.base:11.0.1-SNAPSHOT --deps cc.quarkus:qcc-runtime-main:1.0.0-SNAPSHOT --deps cc.quarkus:qcc-runtime-unwind:1.0.0-SNAPSHOT --deps cc.quarkus:qcc-runtime-gc-nogc:1.0.0-SNAPSHOT examples/helloworld/hello/world/Main.java) --output-path /tmp/output hello.world.Main
//
// Run the executable
// $ /tmp/output/a.out
//
//DEPS cc.quarkus:qcc-runtime-api:1.0.0-SNAPSHOT
package hello.world;

import static cc.quarkus.qcc.runtime.CNative.*;

/**
 *
 */
public class Main {
    @extern
    public static native int putchar(int arg);

    @export
    public static int main() {
        putchar('h');
        putchar('e');
        putchar('l');
        putchar('l');
        putchar('o');
        putchar(' ');
        putchar('w');
        putchar('o');
        putchar('r');
        putchar('l');
        putchar('d');
        putchar('\n');
        return 0;
    }

    public static void main(String[] args) {
        // make driver happy
    }
}

