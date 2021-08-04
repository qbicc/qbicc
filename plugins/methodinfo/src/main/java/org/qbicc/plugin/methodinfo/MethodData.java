package org.qbicc.plugin.methodinfo;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class MethodData {
    private final IndexableSet<MethodInfo> methodInfoSet = new IndexableSet<>();
    private final IndexableSet<SourceCodeInfo> sourceCodeInfoSet = new IndexableSet<>();
    private InstructionMap[] instructionMapList;

    MethodData(int instructionsCount) {
        this.instructionMapList = new InstructionMap[instructionsCount];
    }

    int add(SourceCodeInfo sourceCodeInfo) {
        return sourceCodeInfoSet.addIfAbsent(sourceCodeInfo);
    }

    int add(MethodInfo methodInfo) {
        return methodInfoSet.addIfAbsent(methodInfo);
    }

    void add(int index, InstructionMap instructionMap) {
        instructionMapList[index] = instructionMap;
    }

    MethodInfo[] getMethodInfoTable() {
        return methodInfoSet.toArray(new MethodInfo[0]);
    }

    SourceCodeInfo[] getSourceCodeInfoTable() {
        return sourceCodeInfoSet.toArray(new SourceCodeInfo[0]);
    }

    InstructionMap[] getInstructionMapList() {
        InstructionMap[] map = new InstructionMap[instructionMapList.length];
        System.arraycopy(instructionMapList, 0, map, 0, map.length);
        return map;
    }

    /**
     * A set implementation that allows to retrieve index of the items in the order they are added to the set.
     * Note that user should synchronize on the set when traversing it via Iterator.
     * @param <T>
     */
    private static final class IndexableSet<T> extends AbstractSet<T> implements Set<T> {
        final Map<T, Integer> map;
        int lastIndex;

        IndexableSet() {
            map = new LinkedHashMap<>();
            lastIndex = -1;
        }

        @Override
        public synchronized int size() {
            return map.size();
        }

        @Override
        public synchronized boolean isEmpty() {
            return lastIndex == -1;
        }

        @Override
        public synchronized Iterator iterator() {
            return map.keySet().iterator();
        }

        @Override
        public synchronized Object[] toArray() {
            return map.keySet().toArray();
        }

        @Override
        public synchronized <T> T[] toArray(T[] t) {
            return super.toArray(t);
        }

        @Override
        public synchronized boolean contains(Object o) {
            return super.contains(o);
        }

        @Override
        public synchronized boolean containsAll(Collection<?> c) {
            return super.containsAll(c);
        }

        @Override
        public synchronized boolean add(T t) {
            if (map.containsKey(t)) {
                return false;
            } else {
                map.put(t, lastIndex++);
            }
            return true;
        }

        public synchronized int addIfAbsent(T t) {
            return map.computeIfAbsent(t, k -> ++lastIndex);
        }

        @Override
        public boolean remove(Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized void clear() {
            map.clear();
            lastIndex = 0;
        }

        @Override
        public synchronized String toString() {
            return super.toString();
        }
    }
}
