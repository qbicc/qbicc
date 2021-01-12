// Basic instanceof example with the commands to execute it. 
//
// Compile the example with:
// $ javac -cp ~/.m2/repository/cc/quarkus/qcc-runtime-api/1.0.0-SNAPSHOT/qcc-runtime-api-1.0.0-SNAPSHOT.jar instance/Main.java
//
// Jar the Main.class
// $ jar cvf main.jar instance/Main.class
//
// Build the native executable in /tmp/output with:
// $ jbang cc.quarkus:qcc-main:1.0.0-SNAPSHOT --boot-module-path ~/.m2/repository/cc/quarkus/qccrt-java.base/11.0.1-SNAPSHOT/qccrt-java.base-11.0.1-SNAPSHOT.jar:main.jar:~/.m2/repository/cc/quarkus/qcc-runtime-main/1.0.0-SNAPSHOT/qcc-runtime-main-1.0.0-SNAPSHOT.jar --output-path /tmp/output instance.Main
//
// Run the executable
// $ /tmp/output/a.out

package instance;

import static cc.quarkus.qcc.runtime.CNative.*;

/**
 *
 */
public class Main {
    @extern
    public static native int putchar(int arg);

    @export
    public static int main() {
	if (Main.class instanceof Object) {
        putchar('T');
	} else {
        putchar('F');
	}
        return 0;
    }

    public static void main(String[] args) {
        // make driver happy
    }
}

