package org.qbicc.plugin.opt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Action;
import org.qbicc.graph.Add;
import org.qbicc.graph.AddressOf;
import org.qbicc.graph.And;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.Call;
import org.qbicc.graph.CallNoSideEffects;
import org.qbicc.graph.CallNoReturn;
import org.qbicc.graph.Cmp;
import org.qbicc.graph.CmpG;
import org.qbicc.graph.CmpL;
import org.qbicc.graph.Convert;
import org.qbicc.graph.Div;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.Extend;
import org.qbicc.graph.ExtractElement;
import org.qbicc.graph.ExtractMember;
import org.qbicc.graph.Fence;
import org.qbicc.graph.GlobalVariable;
import org.qbicc.graph.Goto;
import org.qbicc.graph.If;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.InvokeNoReturn;
import org.qbicc.graph.IsEq;
import org.qbicc.graph.IsGe;
import org.qbicc.graph.IsGt;
import org.qbicc.graph.IsLe;
import org.qbicc.graph.IsLt;
import org.qbicc.graph.IsNe;
import org.qbicc.graph.Load;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Mod;
import org.qbicc.graph.Multiply;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.Neg;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.NotNull;
import org.qbicc.graph.Or;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.Return;
import org.qbicc.graph.Select;
import org.qbicc.graph.Shl;
import org.qbicc.graph.Shr;
import org.qbicc.graph.StackAllocation;
import org.qbicc.graph.Store;
import org.qbicc.graph.Sub;
import org.qbicc.graph.Switch;
import org.qbicc.graph.TailCall;
import org.qbicc.graph.TailInvoke;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Unreachable;
import org.qbicc.graph.Unschedulable;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueReturn;
import org.qbicc.graph.Xor;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;

import org.jboss.logging.Logger;


public class FenceAnalyzerVisitor implements NodeVisitor<Void, Value, Node, Void, ValueHandle> {
    private final CompilationContext ctxt;
    static final Logger logger = Logger.getLogger("org.qbicc.plugin.opt.fence");
    private Set<BasicBlock> tracedBlocks;
    private Set<Node> visitedActions;
    private Set<Node> visitedValues;
    private static final ConcurrentMap<String, FunctionInfo> functionInfoMap = new ConcurrentHashMap<>();
    private static FunctionInfo functionInfo;
    private ConcurrentMap<BasicBlock, BlockInfo> blockInfoMap;
    private BlockInfo blockInfo;
    private final Queue<PhiValue> phiQueue = new ArrayDeque<>();
    private String functionName;
    private int depth;

    public FenceAnalyzerVisitor(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static Map<String, FunctionInfo> getAnalysis() {
        return functionInfoMap;
    }

    private void init(BasicBlock entryBlock, String functionName) {
        this.functionName = functionName;
        tracedBlocks = new HashSet<BasicBlock>();
        visitedActions = new HashSet<Node>();
        visitedValues = new HashSet<Node>();
        blockInfoMap = new ConcurrentHashMap<BasicBlock, BlockInfo>();
        blockInfo = new BlockInfo();
        blockInfoMap.put(entryBlock, blockInfo);
        functionInfo = new FunctionInfo(blockInfoMap);
        functionInfoMap.put(functionName, functionInfo);
        depth = 0;
    }

    /**
     * This method is invoked for each function
     *
     */
    public void execute(BasicBlock entryBlock, String functionName) {
        init(entryBlock, functionName);
        entryBlock.getTerminator().accept(this, null);

        PhiValue phi;
        while ((phi = phiQueue.poll()) != null) {
            BasicBlock ourBlock = phi.getPinnedBlock();
            Set<Node> incomingSet = getIncoming(ourBlock, functionName);
            BlockInfo blockInfo = blockInfoMap.get(ourBlock);
            blockInfo.addIncoming(incomingSet);
            blockInfoMap.put(ourBlock, blockInfo);
        }
    }

    /**
     * @return a set of nodes if it succeeded in collecting incoming nodes
     *         correctly, {@code null} otherwise.
     *
     */
    public Set<Node> getIncoming(BasicBlock block, String functionName) {
        Set<Node> incomingSet = new HashSet<Node>();

        if (!getIncoming(block, functionName, incomingSet, new ArrayList<BasicBlock>())) {
            return null;
        }

        return incomingSet;
    }

    /**
     * @return {@code true} if it succeeded in collecting incoming nodes correctly,
     *         {@code false} otherwise.
     *
     */
    private boolean getIncoming(BasicBlock block, String functionName, Set<Node> incomingSet, List<BasicBlock> trace) {
        if (trace.contains(block)) {
            return true;
        }
        trace.add(block);

        Object[] blocks = block.getIncoming().toArray();
        for (int i = 0; i < blocks.length; ++i) {
            BasicBlock bb = (BasicBlock) blocks[i];
            Map<BasicBlock, BlockInfo> blockInfoMap = functionInfoMap.get(functionName).getMap();
            BlockInfo blockInfo = blockInfoMap.get(bb);

            if (blockInfo == null // Not get this block information yet
                    || blockInfo.isFailed()) {
                return false;
            }

            List<Node> list = blockInfo.getList();
            if (list.size() == 0) {
                if (!getIncoming(bb, functionName, incomingSet, trace)) {
                    return false;
                }
            } else {
                incomingSet.add(list.get(list.size() - 1));
            }
        }

        return true;
    }

    private void map(BasicBlock block) {
        if (tracedBlocks.contains(block)) {
            return;
        }
        tracedBlocks.add(block);

        blockInfo = new BlockInfo();
        blockInfo.addIncoming(getIncoming(block, functionName));
        blockInfoMap.put(block, blockInfo);
        block.getTerminator().accept(this, null);
    }

    private void map(Node unknown) {
        if (depth++ > 500) {
            throw new TooBigException();
        }

        if (unknown instanceof Action) {
            if (visitedActions.add(unknown)) {
                ((Action) unknown).accept(this, null);
            }
        } else if (unknown instanceof Value) {
            if (visitedValues.add(unknown)) {
                ((Value) unknown).accept(this, null);
            }
        } else {
            throw new IllegalStateException();
        }

        depth--;
    }

    // terminators

    public Void visit(final Void param, final Goto node) {
        map(node.getDependency());

        BlockLabel label = ((Goto) node).getResumeTargetLabel();
        map(BlockLabel.getTargetOf(label));

        return null;
    }

    public Void visit(final Void param, final If node) {
        map(node.getDependency());

        If ifnode = (If) node;
        map(ifnode.getCondition());
        map(ifnode.getTrueBranch());
        map(ifnode.getFalseBranch());

        return null;
    }

    public Void visit(final Void param, final Return node) {
        map(node.getDependency());

        blockInfo.setReturnBlock();

        return null;
    }

    public Void visit(final Void param, final Unreachable node) {
        map(node.getDependency());

        return null;
    }

    public Void visit(final Void param, final Switch node) {
        map(node.getDependency());

        map(node.getSwitchValue());
        map(node.getDefaultTarget());

        for (int i = 0; i < node.getNumberOfValues(); i++) {
            map(node.getTargetForIndex(i));
        }

        return null;
    }

    public Void visit(final Void param, final ValueReturn node) {
        map(node.getDependency());

        blockInfo.setReturnBlock();

        return null;
    }

    public Node visit(final Void param, Fence node) {
        blockInfo.add(node);
        logger.debugf("Fence %s with mode %s", node, node.getAtomicityMode());
        map(node.getDependency());

        return node;
    }

    public Node visit(final Void param, BlockEntry node) {
        return node;
    }

    public Node visit(final Void param, Store node) {
        if (node.getMode() == MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT) {
            logger.debugf("Store %s with mode %s", node, node.getMode());
            blockInfo.add(node);
        }
        map(node.getDependency());

        return node;
    }

    public Value visit(final Void param, Load node) {
        if (node.getMode() == MemoryAtomicityMode.ACQUIRE) {
            logger.debugf("Load %s with mode %s", node, node.getMode());
            blockInfo.add(node);
        }
        map(node.getDependency());

        return node;
    }

    public Value visit(final Void param, final Add node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final AddressOf node) {
        node.getValueHandle().accept(this, null);

        return node;
    }

    public Value visit(final Void param, final And node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(Void param, Cmp node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(Void param, CmpG node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(Void param, CmpL node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final IsEq node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final IsNe node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final IsLt node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final IsLe node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final IsGt node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final IsGe node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final Or node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final Xor node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final Multiply node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final Select node) {
        map(node.getCondition());

        return node;
    }

    public Value visit(final Void param, final PhiValue node) {
        phiQueue.add(node);

        return node;
    }

    public Value visit(final Void param, final Neg node) {
        map(node.getInput());

        return node;
    }

    public Value visit(Void param, NotNull node) {
        map(node.getInput());

        return node;
    }

    public Value visit(final Void param, final Shr node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final Shl node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final Sub node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final Div node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final Mod node) {
        map(node.getLeftInput());
        map(node.getRightInput());

        return node;
    }

    public Value visit(final Void param, final BitCast node) {
        map(node.getInput());

        return node;
    }

    public Value visit(final Void param, final Convert node) {
        map(node.getInput());

        return node;
    }

    public Value visit(final Void param, final Extend node) {
        map(node.getInput());

        return node;
    }

    public Value visit(final Void param, final ExtractElement node) {
        map(node.getArrayValue());

        return node;
    }

    public Value visit(final Void param, final ExtractMember node) {
        map(node.getCompoundValue());

        return node;
    }

    public Value visit(final Void param, final Invoke.ReturnValue node) {
        map(node.getDependency());

        return node;
    }

    public Value visit(final Void param, final CheckCast node) {
        map(node.getDependency());
        map(node.getInput());

        return node;
    }

    public Value visit(final Void param, final Truncate node) {
        map(node.getInput());

        return node;
    }

    public Value visit(final Void param, final StackAllocation node) {
        return node;
    }

    // calls
    public Value visit(final Void param, final Call node) {
        logger.debugf("Call %s", node);
        blockInfo.add(node);

        map(node.getDependency());
        List<Value> arguments = node.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            map(arguments.get(i));
        }

        return node;
    }

    public Value visit(final Void param, final CallNoSideEffects node) {
        logger.debugf("CallNoSideEffects %s", node);
        blockInfo.add(node);

        List<Value> arguments = node.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            map(arguments.get(i));
        }

        return null;
    }

    public Void visit(final Void param, final CallNoReturn node) {
        logger.debugf("CallNoReturn %s", node);
        blockInfo.add(node);

        map(node.getDependency());
        List<Value> arguments = node.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            map(arguments.get(i));
        }

        return null;
    }

    public Void visit(final Void param, final TailCall node) {
        logger.debugf("TailCall %s", node);
        blockInfo.add(node);

        map(node.getDependency());
        List<Value> arguments = node.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            map(arguments.get(i));
        }

        return null;
    }

    public Void visit(final Void param, final Invoke node) {
        logger.debugf("Invoke %s", node);
        blockInfo.add(node);

        map(node.getDependency());
        List<Value> arguments = node.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            map(arguments.get(i));
        }

        return null;
    }

    public Void visit(final Void param, final InvokeNoReturn node) {
        logger.debugf("InvokeNoReturn %s", node);
        blockInfo.add(node);

        map(node.getDependency());
        List<Value> arguments = node.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            map(arguments.get(i));
        }

        return null;
    }

    public Void visit(final Void param, final TailInvoke node) {
        logger.debugf("TailInvoke %s", node);
        blockInfo.add(node);

        map(node.getDependency());
        List<Value> arguments = node.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            map(arguments.get(i));
        }

        return null;
    }

    class FunctionInfo {
        private Set<Node> tailNodes;
        private Set<Node> unweakened;
        private final Map<BasicBlock, BlockInfo> blockInfoMap;
        private boolean isFailed;
        private boolean weakening;

        public FunctionInfo(Map<BasicBlock, BlockInfo> blockInfoMap) {
            this.blockInfoMap = blockInfoMap;
        }

        public synchronized void setWeakening() {
            weakening = true;
        }

        public synchronized void setWeakened() {
            weakening = false;
        }

        public boolean isWeakening() {
            return weakening;
        }

        public Map<BasicBlock, BlockInfo> getMap() {
            return blockInfoMap;
        }

        public void addTailNode(Node n) {
            if (isFailed) {
                return;
            }

            if (tailNodes == null) {
                tailNodes = new HashSet<Node>();
            }

            tailNodes.add(n);
        }

        public void addTailNode(Set<Node> set) {
            if (isFailed) {
                return;
            }

            if (tailNodes == null) {
                tailNodes = new HashSet<Node>();
            }

            tailNodes.addAll(set);
        }

        public boolean resolved() {
            return isFailed || tailNodes != null;
        }

        public Set<Node> getTailNodes() {
            return tailNodes;
        }

        public void setFailed() {
            isFailed = true;
            tailNodes = null;
        }

        public boolean isFailed() {
            return isFailed;
        }
    }

    /**
     * Information on a basic block about the existence of Fence, ordered Load, ordered
     * Store, and Call/Invoke reateld nodes. It holds 1) a list of these nodes stored in
     * the program order and 2) a set of these nodes in the incoming basic blocks.
     *
     **/
    class BlockInfo {
        private Set<Node> incoming;
        private List<Node> list; // can be null
        private boolean isFailed;
        private boolean isReturnBlock;

        public BlockInfo() {
            this.incoming = new HashSet<Node>();
            this.list = new ArrayList<Node>();
        }

        public boolean isFailed() {
            return isFailed;
        }

        public void setFail() {
            isFailed = true;
            list = null;
        }

        public Set<Node> getIncoming() {
            return incoming;
        }

        public void add(Node node) {
            if (!isFailed()) {
                list.add(0, node);
            }
        }

        public void addIncoming(Set<Node> subSet) {
            if (subSet != null) {
                incoming.addAll(subSet);
            }
        }

        public List<Node> getList() {
            return list;
        }

        public void addList(List<Node> subList) {
            list.addAll(subList);
        }

        public boolean isReturnBlock() {
            return isReturnBlock;
        }

        public void setReturnBlock() {
            isReturnBlock = true;
        }
    }

    class TooBigException extends RuntimeException {
    }    
}
