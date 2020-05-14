package cc.quarkus.qcc.graph.invoke;

public class Dog implements Animal {
    @Override
    public String speak() {
        return "woof";
    }
}
