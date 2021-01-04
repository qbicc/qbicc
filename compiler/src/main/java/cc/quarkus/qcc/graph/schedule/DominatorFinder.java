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
        infos[v].semi = ++n;
        infos[n].vertex = infos[v].label = v;
        infos[v].ancestor = infos[v].child = 0;
        infos[v].size = 1;
        int w = infos[v].succ.nextSetBit(0);
        while (w != -1) {
            if (infos[w].semi == 0) {
                infos[w].parent = v;
                DFS(infos, w);
            }
            infos[w].pred.set(v);
            w = infos[v].succ.nextSetBit(w + 1);
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
        if (infos[infos[v].ancestor].ancestor != 0) {
            COMPRESS(infos, infos[v].ancestor);
            if (infos[infos[infos[v].ancestor].label].semi < infos[infos[v].label].semi) {
                infos[v].label = infos[infos[v].ancestor].label;
            }
            infos[v].ancestor = infos[infos[v].ancestor].ancestor;
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
        if (infos[v].ancestor == 0) {
            return infos[v].label;
        } else {
            COMPRESS(infos, v);
            return infos[infos[infos[v].ancestor].label].semi >= infos[infos[v].label].semi ? infos[v].label : infos[infos[v].ancestor].label;
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
    void LINK(BlockInfo[] infos, int v, int w) {
        int s = w;
        while (infos[infos[w].label].semi < infos[infos[infos[s].child].label].semi) {
            if (infos[s].size + infos[infos[infos[s].child].child].size >= 2 * infos[infos[s].child].size) {
                infos[infos[s].child].ancestor = s;
                infos[s].child = infos[infos[s].child].child;
            } else {
                infos[infos[s].child].size = infos[s].size;
                s = infos[s].ancestor = infos[s].child;
            }
        }
        infos[s].label = infos[w].label;
        infos[v].size += infos[w].size;
        if (infos[v].size < 2 * infos[w].size) {
            int tmp = infos[v].child;
            infos[v].child = s;
            s = tmp;
        }
        while (s != 0) {
            infos[s].ancestor = v;
            s = infos[s].child;
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
        DFS(infos, 0); // r == 0
        //     [ size(0) := label(0) := semi(0) := 0; ] (implicit)
        for (int i = infos.length - 1; i >= 1; i --) {
            int w = infos[i].vertex;
            // step 2
            int v = infos[w].pred.nextSetBit(0);
            while (v != -1) {
                int u = EVAL(infos, v);
                if (infos[u].semi < infos[w].semi) {
                    infos[w].semi = infos[u].semi;
                }
                v = infos[w].pred.nextSetBit(v + 1);
            }
            infos[infos[infos[w].semi].vertex].bucket.set(w);
            LINK(infos, infos[w].parent, w);
            // step 3
            v = infos[infos[w].parent].bucket.nextSetBit(0);
            while (v != -1) {
                infos[infos[w].parent].bucket.clear(v);
                int u = EVAL(infos, v);
                infos[v].dominator = infos[infos[u].semi < infos[v].semi ? u : infos[w].parent];
                v = infos[infos[w].parent].bucket.nextSetBit(v + 1);
            }
        }
        // step 4
        for (int i = 2; i < n; i ++) {
            int w = infos[i].vertex;
            if (infos[w].dominator.index != infos[infos[w].semi].vertex) {
                infos[w].dominator = infos[infos[w].dominator.index];
            }
        }
        infos[0].dominator = null;
    }
}
