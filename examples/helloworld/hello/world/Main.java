// Basic helloworld example with the commands to execute it. 
//
// Compile the example with:
// $ javac -cp ~/.m2/repository/cc/quarkus/qcc-runtime-api/1.0.0-SNAPSHOT/qcc-runtime-api-1.0.0-SNAPSHOT.jar hello/world/Main.java
//
// Jar the Main.class
// $ jar cvf main.jar hello/world/Main.class
//
// Build the native executable in /tmp/output with:
// $ jbang cc.quarkus:qcc-main:1.0.0-SNAPSHOT --boot-module-path $HOME/.m2/repository/cc/quarkus/qccrt-java.base/11.0.1-SNAPSHOT/qccrt-java.base-11.0.1-SNAPSHOT.jar:$HOME/.m2/repository/cc/quarkus/qcc-runtime-api/1.0.0-SNAPSHOT/qcc-runtime-api-1.0.0-SNAPSHOT.jar:$HOME/.m2/repository/cc/quarkus/qcc-runtime-unwind/1.0.0-SNAPSHOT/qcc-runtime-unwind-1.0.0-SNAPSHOT.jar:main.jar --output-path /tmp/output hello.world.Main
//
// Run the executable
// $ /tmp/output/a.out

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

