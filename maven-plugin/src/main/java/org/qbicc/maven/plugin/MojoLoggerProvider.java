package org.qbicc.maven.plugin;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.logging.LoggerProvider;

/**
 *
 */
public final class MojoLoggerProvider implements LoggerProvider {
    @Override
    public Logger getLogger(String name) {
        return new MojoLogger(name);
    }

    private final ThreadLocal<Map<String, Object>> mdcMap = new ThreadLocal<Map<String, Object>>();

    public void clearMdc() {
        final Map<String, Object> map = mdcMap.get();
        if (map != null) {
            map.clear();
        }
    }

    public Object getMdc(String key) {
        return mdcMap.get() == null ? null : mdcMap.get().get(key);
    }

    public Map<String, Object> getMdcMap() {
        final Map<String, Object> map = mdcMap.get();
        return map == null ? Collections.emptyMap() : map;
    }

    public Object putMdc(String key, Object value) {
        Map<String, Object> map = mdcMap.get();
        if (map == null) {
            map = new HashMap<String, Object>();
            mdcMap.set(map);
        }
        return map.put(key, value);
    }

    public void removeMdc(String key) {
        Map<String, Object> map = mdcMap.get();
        if (map == null)
            return;
        map.remove(key);
    }

    private final ThreadLocal<ArrayDeque<Entry>> ndcStack = new ThreadLocal<ArrayDeque<Entry>>();

    public void clearNdc() {
        ArrayDeque<Entry> stack = ndcStack.get();
        if (stack != null)
            stack.clear();
    }

    public String getNdc() {
        ArrayDeque<Entry> stack = ndcStack.get();
        return stack == null || stack.isEmpty() ? null : stack.peek().merged;
    }

    public int getNdcDepth() {
        ArrayDeque<Entry> stack = ndcStack.get();
        return stack == null ? 0 : stack.size();
    }

    public String peekNdc() {
        ArrayDeque<Entry> stack = ndcStack.get();
        return stack == null || stack.isEmpty() ? "" : stack.peek().current;
    }

    public String popNdc() {
        ArrayDeque<Entry> stack = ndcStack.get();
        return stack == null || stack.isEmpty() ? "" : stack.pop().current;
    }

    public void pushNdc(String message) {
        ArrayDeque<Entry> stack = ndcStack.get();
        if (stack == null) {
            stack = new ArrayDeque<Entry>();
            ndcStack.set(stack);
        }
        stack.push(stack.isEmpty() ? new Entry(message) : new Entry(stack.peek(), message));
    }

    public void setNdcMaxDepth(int maxDepth) {
        final ArrayDeque<Entry> stack = ndcStack.get();
        if (stack != null) while (stack.size() > maxDepth) stack.pop();
    }

    private static class Entry {

        private final String merged;
        private final String current;

        Entry(String current) {
            merged = current;
            this.current = current;
        }

        Entry(Entry parent, String current) {
            merged = parent.merged + ' ' + current;
            this.current = current;
        }
    }
}
