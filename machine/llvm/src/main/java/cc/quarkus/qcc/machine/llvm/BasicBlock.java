package cc.quarkus.qcc.machine.llvm;

import cc.quarkus.qcc.machine.llvm.op.Assignment;
import cc.quarkus.qcc.machine.llvm.op.AtomicRmwInstruction;
import cc.quarkus.qcc.machine.llvm.op.Binary;
import cc.quarkus.qcc.machine.llvm.op.Branch;
import cc.quarkus.qcc.machine.llvm.op.Call;
import cc.quarkus.qcc.machine.llvm.op.ExactBinary;
import cc.quarkus.qcc.machine.llvm.op.Fence;
import cc.quarkus.qcc.machine.llvm.op.Load;
import cc.quarkus.qcc.machine.llvm.op.NuwNswBinary;
import cc.quarkus.qcc.machine.llvm.op.OrderingConstraint;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.machine.llvm.op.Return;
import cc.quarkus.qcc.machine.llvm.op.Select;
import cc.quarkus.qcc.machine.llvm.op.Store;

/**
 *
 */
public interface BasicBlock extends Value {

    BasicBlock name(String name);

    FunctionDefinition functionDefinition();

    // not starter, not terminator

    Phi phi(Value type);

    // terminators

    Branch br(BasicBlock dest);

    Branch br(Value cond, BasicBlock ifTrue, BasicBlock ifFalse);

    Return ret();

    Return ret(Value type, Value val);

    void unreachable();

    // starters

    Assignment assign(Value value);

    Select select(Value condType, Value cond, Value valueType, Value trueValue, Value falseValue);

    NuwNswBinary add(Value type, Value arg1, Value arg2);

    NuwNswBinary sub(Value type, Value arg1, Value arg2);

    NuwNswBinary mul(Value type, Value arg1, Value arg2);

    NuwNswBinary shl(Value type, Value arg1, Value arg2);

    ExactBinary udiv(Value type, Value arg1, Value arg2);

    ExactBinary sdiv(Value type, Value arg1, Value arg2);

    ExactBinary lshr(Value type, Value arg1, Value arg2);

    ExactBinary ashr(Value type, Value arg1, Value arg2);

    Binary icmp(IntCondition cond, Value type, Value arg1, Value arg2);

    Binary and(Value type, Value arg1, Value arg2);

    Binary or(Value type, Value arg1, Value arg2);

    Binary xor(Value type, Value arg1, Value arg2);

    Binary urem(Value type, Value arg1, Value arg2);

    Binary srem(Value type, Value arg1, Value arg2);

    Call call(Value type, Value function);

    Call invoke(Value type, Value function, BasicBlock normal, BasicBlock unwind);

    Load load(Value type, Value pointeeType, Value pointer);

    Store store(Value type, Value value, Value pointeeType, Value pointer);

    Fence fence(OrderingConstraint ordering);

    AtomicRmwInstruction atomicrmw();

    // create more blocks

    BasicBlock createBlock();
}
