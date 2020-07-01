package cc.quarkus.qcc.graph.schedule;

import java.util.BitSet;

// This is a mostly exact transliteration of "A Fast Algorithm for Finding Dominators in a Flowgraph"
//    by Lengauer and Tarjan.  At some point, it'd probably be worth optimizing a bit.
//
// procedure DOMINATORS(integer set array succ(1 :: n); integer r, n; integer array dom(1 :: n));
//   begin
//     integer array parent, ancestor, [ child, ] vertex(1 :: n);
//     integer array label, semi [, size](0 :: n);
//     integer set array pred, bucket(1 :: n);
//     integer u, v, x;
final class DominatorFinder {
    // global parameters
    final int graphSize;
    int n;
    final BitSet[] succ;

    // global variables
    final int[] parent, ancestor, child, vertex;
    final int[] label, semi, size;
    final BitSet[] pred;

    DominatorFinder(int graphSize) {
        this.graphSize = graphSize;
        succ = new BitSet[graphSize];
        parent = new int[graphSize];
        ancestor = new int[graphSize];
        child = new int[graphSize];
        vertex = new int[graphSize];
        label = new int[graphSize];
        semi = new int[graphSize];
        size = new int[graphSize];
        pred = new BitSet[graphSize];
    }

    //     procedure DFS(integer v);
    //       begin
    //         semi(v) := n := n + 1;
    //         vertex(n) := label(v) := v;
    //         ancestor(v) := [ child(v) := ] 0;
    //         [ size(v) := 1; ]
    //         for each w ∈ succ(v) do
    //           if semi(w) = 0 then
    //             parent(w) := v; DFS(w);
    //           fi;
    //           add v to pred(w);
    //         od
    //       end DFS;
    void DFS(int v) {
        semi[v] = ++n;
        vertex[n] = label[v] = v;
        ancestor[v] = child[v] = 0;
        size[v] = 1;
        int w = succ[v].nextSetBit(0);
        while (w != -1) {
            if (semi[w] == 0) {
                parent[w] = v;
                DFS(w);
            }
            pred[w].set(v);
            w = succ[v].nextSetBit(w);
        }
    }

    //     procedure COMPRESS(integer v);
    //       if ancestor(ancestor(v)) ≠ 0 then
    //         COMPRESS(ancestor(v));
    //         if semi(label(ancestor(v))) < semi(label(v)) then
    //           label(v) := label(ancestor(v))
    //         fi;
    //         ancestor(v) := ancestor(ancestor(v))
    //       fi;
    void COMPRESS(int v) {
        if (ancestor[ancestor[v]] != 0) {
            COMPRESS(ancestor[v]);
            if (semi[label[ancestor[v]]] < semi[label[v]]) {
                label[v] = label[ancestor[v]];
            }
            ancestor[v] = ancestor[ancestor[v]];
        }
    }

    //   [ integer procedure EVAL(integer v);
    //       if ancestor(v) = 0 then
    //         EVAL := label(v)
    //       else
    //         COMPRESS(v)
    //         EVAL := if semi(label(ancestor(v))) ≥ semi(label(v))
    //                 then label(v)
    //                 else label(ancestor(v)) fi
    //       fi ]
    int EVAL(int v) {
        if (ancestor[v] == 0) {
            return label[v];
        } else {
            COMPRESS(v);
            return semi[label[ancestor[v]]] >= semi[label[v]] ? label[v] : label[ancestor[v]];
        }
    }

    //   [ procedure LINK(integer v, w);
    //       begin
    //         integer s;
    //         s := w;
    //         while semi(label(w)) < semi(label(child(s))) do
    //           if size(s) + size(child(child(s))) ≥ 2 * size(child(s)) then
    //             ancestor(child(s)) := s;
    //             child(s) := child(child(s));
    //           else
    //             size(child(s)) := size(s);
    //             s := ancestor(s) := child(s);
    //           fi
    //         od
    //         label(s) := label(w);
    //         size(v) := size(v) + size(w);
    //         if size(v) < 2 * size(w) then
    //           s, child(v) := child(v), s
    //         fi
    //         while s ≠ 0 do
    //           ancestor(s) := v;
    //           s := child(s)
    //         od
    //       end LINK; ]
    void LINK(int v, int w) {
        int s = w;
        while (semi[label[w]] < semi[label[child[s]]]) {
            if (size[s] + size[child[child[s]]] >= size[child[s]] << 1) {
                ancestor[child[s]] = s;
                child[s] = child[child[s]];
            } else {
                size[child[s]] = size[s];
                s = ancestor[s] = child[s];
            }
        }
        label[s] = label[w];
        size[v] += size[w];
        if (size[v] < size[w] << 1) {
            int tmp = child[v];
            child[v] = s;
            s = tmp;
        }
        while (s != 0) {
            ancestor[s] = v;
            s = child[s];
        }
    }

    //step1:
    //     for v := 1 until n do
    //       pred(v) := bucket(v) := ∅;
    //       semi(v) := 0;
    //     od;
    //     n := 0;
    //     DFS(r);
    //     [ size(0) := label(0) := semi(0) := 0; ]
    //     for i := n by -1 until 2 do
    //       w := vertex(i);
    //step2:
    //       for each v ∈ pred(w) do
    //         u := EVAL(v);
    //         if semi(u) < semi(w) then
    //           semi(w) := semi(u)
    //         fi
    //       od;
    //       add w to bucket(vertex(semi(w)));
    //       LINK(parent(w), w);
    //step3: for each v ∈ bucket(parent(w)) do
    //         delete v from bucket(parent(w));
    //         u := EVAL(v);
    //         dom(v) := if semi(u) < semi(v) then u else parent(w) fi
    //       od
    //     od
    //step4:
    //     for i := 2 until n do
    //       w := vertex(i);
    //       if dom(w) ≠ vertex(semi(w)) then
    //         dom(w) := dom(dom(w))
    //       fi
    //     od
    //     dom(r) := 0
    //   end DOMINATORS
    void main(final BlockInfo[] allBlocks) {
        BitSet[] bucket = new BitSet[graphSize];
        // step 1
        for (int v = 1; v < graphSize; v ++) {
            pred[v] = new BitSet();
            bucket[v] = new BitSet();
            semi[v] = 0;
        }
        n = 0;
        DFS(0); // r == 0
        size[0] = label[0] = semi[0] = 0;
        for (int i = n; i >= 2; i --) {
            int w = vertex[i];
            // step 2
            int v = pred[w].nextSetBit(0);
            while (v != -1) {
                int u = EVAL(v);
                if (semi[u] < semi[w]) {
                    semi[w] = semi[u];
                }
                v = pred[w].nextSetBit(0);
            }
            bucket[vertex[semi[w]]].set(w);
            LINK(parent[w], w);
            // step 3
            v = bucket[parent[w]].nextSetBit(0);
            while (v != -1) {
                bucket[parent[w]].clear(v);
                int u = EVAL(v);
                allBlocks[v].dominator = allBlocks[semi[u] < semi[v] ? u : parent[w]];
                v = bucket[parent[w]].nextSetBit(0);
            }
        }
        // step 4
        for (int i = 2; i < n; i ++) {
            int w = vertex[i];
            if (allBlocks[w].dominator.index != vertex[semi[w]]) {
                allBlocks[w].dominator = allBlocks[allBlocks[w].dominator.index];
            }
        }
        allBlocks[0].dominator = null;
    }
}
