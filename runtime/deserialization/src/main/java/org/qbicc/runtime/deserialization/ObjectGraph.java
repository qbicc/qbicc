package org.qbicc.runtime.deserialization;

public class ObjectGraph {
    private final Object[] objects; // Tempting to make this ArrayList<Object>, but that makes 1300 more classes reachable
    private int nextId;

    public ObjectGraph(int expected) {
        objects = new Object[expected];
    }

    public void recordObject(Object obj) {
        objects[nextId] = obj;
        nextId += 1;
    }

    public Object resolveBackref(int backref) {
        int index = nextId - backref;
        return objects[index];
    }

    public Object getObject(int index) {
        return objects[index];
    }
}
