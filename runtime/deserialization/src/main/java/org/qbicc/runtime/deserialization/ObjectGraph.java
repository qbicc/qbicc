package org.qbicc.runtime.deserialization;

import java.util.ArrayList;

public class ObjectGraph {
    private final ArrayList<Object> objects = new ArrayList<>();
    private int nextId;

    public void recordObject(Object obj) {
        objects.add(nextId, obj);
        nextId += 1;
    }

    public Object resolveBackref(int backref) {
        int index = nextId - backref;
        return objects.get(index);
    }

    public Object getObject(int index) {
        return objects.get(index);
    }
}
