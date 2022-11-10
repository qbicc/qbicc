package org.qbicc.graph;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.qbicc.type.InvokableType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A catching method or function tail call.
 * The return value of the callee is returned by the caller and thus must be of the same type as, or a subtype of, the caller's return type.
 * Exceptions thrown by the target are caught and delivered to the catch block.
 * If no exception is thrown by the callee, execution resumes in the resume block.
 * Generally this node will translate to a plain {@code invoke} followed by a {@code return} on the back end.
 * This node terminates its block.
 *
 * @see BasicBlockBuilder#tailInvoke(org.qbicc.graph.ValueHandle, java.util.List, org.qbicc.graph.BlockLabel, java.util.Map)
 */
public final class TailInvoke extends AbstractTerminator {
    private final Node dependency;
    private final BasicBlock terminatedBlock;
    private final ValueHandle target;
    private final List<Value> arguments;
    private final InvokableType calleeType;
    private final BlockLabel catchLabel;

    TailInvoke(Node callSite, ExecutableElement element, int line, int bci, final BlockEntry blockEntry, Node dependency, ValueHandle target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        super(callSite, element, line, bci, targetArguments);
        this.dependency = dependency;
        this.terminatedBlock = new BasicBlock(blockEntry, this);
        this.target = target;
        this.arguments = arguments;
        this.catchLabel = catchLabel;
        calleeType = (InvokableType) target.getPointeeType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(TailInvoke.class, dependency, target, arguments);
    }

    @Override
    String getNodeName() {
        return "TailInvoke";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TailInvoke && equals((TailInvoke) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        Iterator<Value> itr = arguments.iterator();
        if (itr.hasNext()) {
            itr.next().toReferenceString(b);
            while (itr.hasNext()) {
                b.append(',');
                itr.next().toReferenceString(b);
            }
        }
        b.append(')');
        return b;
    }

    public boolean equals(TailInvoke other) {
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target) && arguments.equals(other.arguments);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public InvokableType getCalleeType() {
        return calleeType;
    }

    public List<Value> getArguments() {
        return arguments;
    }

    @Override
    public int getValueDependencyCount() {
        return arguments.size();
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return arguments.get(index);
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return target;
    }

    @Override
    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    public BlockLabel getCatchLabel() {
        return catchLabel;
    }

    public BasicBlock getCatchBlock() {
        return BlockLabel.getTargetOf(catchLabel);
    }

    @Override
    public int getSuccessorCount() {
        return 1;
    }

    @Override
    public BasicBlock getSuccessor(int index) {
        return index == 0 ? getCatchBlock() : Util.throwIndexOutOfBounds(index);
    }

    public boolean isImplicitOutboundArgument(final Slot slot, final BasicBlock block) {
        return slot == Slot.thrown() && block == getCatchBlock();
    }

    @Override
    public <T, R> R accept(TerminatorVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
