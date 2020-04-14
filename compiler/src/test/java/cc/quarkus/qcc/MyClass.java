package cc.quarkus.qcc;

public class MyClass {

    public int sum(int a, int b) {
        if (a < b) {
            //return foo();
            throw new NullPointerException("dang");
            //a++;
        }
        return 2;
    }

    public int foo() {
        return 89;
    }
}
