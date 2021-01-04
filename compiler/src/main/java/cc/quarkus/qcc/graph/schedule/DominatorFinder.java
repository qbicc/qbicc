package cc.quarkus.qcc.graph.schedule;

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
    int n;

    DominatorFinder() {
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
    void DFS(BlockInfo[] infos, int v) {
        info(infos, v).semi = ++n;
        info(infos, n).vertex = info(infos, v).label = v;
        info(infos, v).ancestor = info(infos, v).child = 0;
        info(infos, v).size = 1;
        int w = info(infos, v).succ.nextSetBit(0) + 1; // one-based arrays
        while (w != 0) {
            if (semi(infos, w) == 0) {
                info(infos, w).parent = v;
                DFS(infos, w);
            }
            info(infos, w).pred.set(v - 1); // one-based arrays, zero-based bitset
            w = info(infos, v).succ.nextSetBit(w) + 1; // w is already increased by one
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
    void COMPRESS(BlockInfo[] infos, int v) {
        if (info(infos, info(infos, v).ancestor).ancestor != 0) {
            COMPRESS(infos, info(infos, v).ancestor);
            if (semi(infos, label(infos, info(infos, v).ancestor)) < semi(infos, label(infos, v))) {
                info(infos, v).label = label(infos, info(infos, v).ancestor);
            }
            info(infos, v).ancestor = info(infos, info(infos, v).ancestor).ancestor;
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
    int EVAL(BlockInfo[] infos, int v) {
        if (info(infos, v).ancestor == 0) {
            return label(infos, v);
        } else {
            COMPRESS(infos, v);
            return semi(infos, label(infos, info(infos, v).ancestor)) >= semi(infos, label(infos, v)) ? label(infos, v) : label(infos, info(infos, v).ancestor);
        }
    }

    //   [ procedure LINK(integer v, w);
    //       begin
    //         comment this procedure assumes for convenience that
    //           size(0) = label(0) = semi(0) = 0;
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
    void LINK(BlockInfo[] infos, int v, int w) {
        int s = w;
        while (semi(infos, label(infos, w)) < semi(infos, label(infos, child(infos, s)))) {
            if (size(infos, s) + size(infos, child(infos, child(infos, s))) >= 2 * size(infos, child(infos, s))) {
                info(infos, child(infos, s)).ancestor = s;
                info(infos, s).child = child(infos, child(infos, s));
            } else {
                info(infos, child(infos, s)).size = size(infos, s);
                s = info(infos, s).ancestor = child(infos, s);
            }
        }
        info(infos, s).label = label(infos, w);
        info(infos, v).size += size(infos, w);
        if (size(infos, v) < 2 * size(infos, w)) {
            int tmp = child(infos, v);
            info(infos, v).child = s;
            s = tmp;
        }
        while (s != 0) {
            info(infos, s).ancestor = v;
            s = child(infos, s);
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
    void main(final BlockInfo[] infos) {
        // step 1
        n = 0;
        DFS(infos, 1); // r == 1
        //     [ size(0) := label(0) := semi(0) := 0; ] (implicit)
        for (int i = infos.length - 1; i >= 2; i --) {
            int w = vertex(infos, i);
            // step 2
            int v = info(infos, w).pred.nextSetBit(0) + 1; // one-based arrays
            while (v != 0) {
                int u = EVAL(infos, v);
                if (semi(infos, u) < semi(infos, w)) {
                    info(infos, w).semi = semi(infos, u);
                }
                v = info(infos, w).pred.nextSetBit(v) + 1; // v is already increased by one
            }
            info(infos, vertex(infos, semi(infos, w))).bucket.set(w - 1);
            LINK(infos, parent(infos, w), w);
            // step 3
            v = info(infos, parent(infos, w)).bucket.nextSetBit(0) + 1; // one-based arrays
            while (v != 0) {
                info(infos, parent(infos, w)).bucket.clear(v - 1); // one-based arrays, zero-based bitset
                int u = EVAL(infos, v);
                info(infos, v).dominator = semi(infos, u) < semi(infos, v) ? u : parent(infos, w);
                v = info(infos, parent(infos, w)).bucket.nextSetBit(v) + 1; // v is already increased by 1
            }
        }
        // step 4
        for (int i = 2; i <= n; i ++) {
            int w = vertex(infos, i);
            if (dominator(infos, w) != vertex(infos, semi(infos, w))) {
                info(infos, w).dominator = dominator(infos, w);
            }
        }
        info(infos, 1).dominator = 0;
    }

    private BlockInfo info(final BlockInfo[] infos, final int i) {
        return infos[i - 1];
    }

    private int parent(final BlockInfo[] infos, final int i) {
        return i == 0 ? 0 : info(infos, i).parent;
    }

    private int dominator(final BlockInfo[] infos, final int i) {
        return i == 0 ? 0 : info(infos, i).dominator;
    }

    private int vertex(final BlockInfo[] infos, final int i) {
        return i == 0 ? 0 : info(infos, i).vertex;
    }

    private int child(final BlockInfo[] infos, final int i) {
        return i == 0 ? 0 : info(infos, i).child;
    }

    private int label(final BlockInfo[] infos, final int i) {
        return i == 0 ? 0 : info(infos, i).label;
    }

    private int semi(final BlockInfo[] infos, final int i) {
        return i == 0 ? 0 : info(infos, i).semi;
    }

    private int size(final BlockInfo[] infos, final int i) {
        return i == 0 ? 0 : info(infos, i).size;
    }

}
