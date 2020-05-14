package cc.quarkus.qcc.graph.invoke;

public class Cat implements Animal {
    @Override
    public String speak() {
        return "meow";
    }
}
