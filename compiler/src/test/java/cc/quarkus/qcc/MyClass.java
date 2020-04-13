package cc.quarkus.qcc;

public class MyClass {

    public int sum(int a, int b) {
        if (a < b) {
            return foo();
        }
        return 2;
    }

    public int foo() {
        return 89;
    }
}
