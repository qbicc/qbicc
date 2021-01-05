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
    private final BlockInfo[] infos;
    int n;

    DominatorFinder(final BlockInfo[] infos) {
        this.infos = infos;
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
        info(v).semi = ++n;
        info(n).vertex = info(v).label = v;
        info(v).ancestor = info(v).child = 0;
        info(v).size = 1;
        int w = info(v).succ.nextSetBit(0) + 1; // one-based arrays
        while (w != 0) {
            if (semi(w) == 0) {
                info(w).parent = v;
                DFS(w);
            }
            info(w).pred.set(v - 1); // one-based arrays, zero-based bitset
            w = info(v).succ.nextSetBit(w) + 1; // w is already increased by one
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
        if (ancestor(ancestor(v)) != 0) {
            COMPRESS(ancestor(v));
            if (semi(label(ancestor(v))) < semi(label(v))) {
                info(v).label = label(ancestor(v));
            }
            info(v).ancestor = ancestor(ancestor(v));
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
        if (ancestor(v) == 0) {
            return label(v);
        } else {
            COMPRESS(v);
            return semi(label(ancestor(v))) >= semi(label(v)) ? label(v) : label(ancestor(v));
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
    void LINK(int v, int w) {
        int s = w;
        while (semi(label(w)) < semi(label(child(s)))) {
            if (size(s) + size(child(child(s))) >= 2 * size(child(s))) {
                info(child(s)).ancestor = s;
                info(s).child = child(child(s));
            } else {
                info(child(s)).size = size(s);
                s = info(s).ancestor = child(s);
            }
        }
        info(s).label = label(w);
        info(v).size += size(w);
        if (size(v) < 2 * size(w)) {
            int tmp = child(v);
            info(v).child = s;
            s = tmp;
        }
        while (s != 0) {
            info(s).ancestor = v;
            s = child(s);
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
    void main() {
        // step 1
        n = 0;
        DFS(1); // r == 1
        //     [ size(0) := label(0) := semi(0) := 0; ] (implicit)
        for (int i = infos.length - 1; i >= 2; i --) {
            int w = vertex(i);
            // step 2
            int v = info(w).pred.nextSetBit(0) + 1; // one-based arrays
            while (v != 0) {
                int u = EVAL(v);
                if (semi(u) < semi(w)) {
                    info(w).semi = semi(u);
                }
                v = info(w).pred.nextSetBit(v) + 1; // v is already increased by one
            }
            info(vertex(semi(w))).bucket.set(w - 1);
            LINK(parent(w), w);
            // step 3
            v = info(parent(w)).bucket.nextSetBit(0) + 1; // one-based arrays
            while (v != 0) {
                info(parent(w)).bucket.clear(v - 1); // one-based arrays, zero-based bitset
                int u = EVAL(v);
                info(v).dominator = semi(u) < semi(v) ? u : parent(w);
                v = info(parent(w)).bucket.nextSetBit(v) + 1; // v is already increased by 1
            }
        }
        // step 4
        for (int i = 2; i <= n; i ++) {
            int w = vertex(i);
            if (dominator(w) != vertex(semi(w))) {
                info(w).dominator = dominator(dominator(w));
            }
        }
        info(1).dominator = 0;
    }

    private BlockInfo info(final int i) {
        return infos[i - 1];
    }

    private int parent(final int i) {
        return i == 0 ? 0 : info(i).parent;
    }

    private int dominator(final int i) {
        return i == 0 ? 0 : info(i).dominator;
    }

    private int vertex(final int i) {
        return i == 0 ? 0 : info(i).vertex;
    }

    private int child(final int i) {
        return i == 0 ? 0 : info(i).child;
    }

    private int label(final int i) {
        return i == 0 ? 0 : info(i).label;
    }

    private int semi(final int i) {
        return i == 0 ? 0 : info(i).semi;
    }

    private int size(final int i) {
        return i == 0 ? 0 : info(i).size;
    }

    private int ancestor(final int i) {
        return i == 0 ? 0 : info(i).ancestor;
    }
}
