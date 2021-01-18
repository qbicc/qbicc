package cc.quarkus.qcc.interpreter.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@SuppressWarnings("serial")
class ConcurrentTrie<K, V> extends ConcurrentHashMap<K, ConcurrentTrie<K, V>> {
    private static final VarHandle valueHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "value", VarHandle.class, ConcurrentTrie.class, Object.class);

    private volatile V value;

    public V get() {
        return value;
    }

    public boolean compareAndSet(V expected, V update) {
        return valueHandle.compareAndSet(this, expected, update);
    }
}
