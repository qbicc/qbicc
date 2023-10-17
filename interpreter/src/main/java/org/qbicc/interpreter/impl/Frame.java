package org.qbicc.interpreter.impl;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.Action;
import org.qbicc.graph.ActionVisitor;
import org.qbicc.graph.Add;
import org.qbicc.graph.And;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BinaryValue;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BitReverse;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.ByteOffsetPointer;
import org.qbicc.graph.ByteSwap;
import org.qbicc.graph.Call;
import org.qbicc.graph.CallNoReturn;
import org.qbicc.graph.CallNoSideEffects;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.Cmp;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.CmpG;
import org.qbicc.graph.CmpL;
import org.qbicc.graph.Comp;
import org.qbicc.graph.CountLeadingZeros;
import org.qbicc.graph.CountTrailingZeros;
import org.qbicc.graph.CurrentThread;
import org.qbicc.graph.DebugAddressDeclaration;
import org.qbicc.graph.DebugValueDeclaration;
import org.qbicc.graph.DecodeReference;
import org.qbicc.graph.Div;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.EncodeReference;
import org.qbicc.graph.EnterSafePoint;
import org.qbicc.graph.ExitSafePoint;
import org.qbicc.graph.Extend;
import org.qbicc.graph.ExtractMember;
import org.qbicc.graph.Fence;
import org.qbicc.graph.FpToInt;
import org.qbicc.graph.Goto;
import org.qbicc.graph.If;
import org.qbicc.graph.InitCheck;
import org.qbicc.graph.InitializeClass;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.InstanceOf;
import org.qbicc.graph.IntToFp;
import org.qbicc.graph.InterfaceMethodLookup;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.InvokeNoReturn;
import org.qbicc.graph.IsEq;
import org.qbicc.graph.IsGe;
import org.qbicc.graph.IsGt;
import org.qbicc.graph.IsLe;
import org.qbicc.graph.IsLt;
import org.qbicc.graph.IsNe;
import org.qbicc.graph.Load;
import org.qbicc.graph.Max;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.MemberOfUnion;
import org.qbicc.graph.Min;
import org.qbicc.graph.Mod;
import org.qbicc.graph.MonitorEnter;
import org.qbicc.graph.MonitorExit;
import org.qbicc.graph.MultiNewArray;
import org.qbicc.graph.Multiply;
import org.qbicc.graph.Neg;
import org.qbicc.graph.New;
import org.qbicc.graph.NewArray;
import org.qbicc.graph.NewReferenceArray;
import org.qbicc.graph.Node;
import org.qbicc.graph.NotNull;
import org.qbicc.graph.OffsetOfField;
import org.qbicc.graph.OffsetPointer;
import org.qbicc.graph.Or;
import org.qbicc.graph.PointerDifference;
import org.qbicc.graph.PollSafePoint;
import org.qbicc.graph.PopCount;
import org.qbicc.graph.Reachable;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Ret;
import org.qbicc.graph.Return;
import org.qbicc.graph.Rol;
import org.qbicc.graph.Ror;
import org.qbicc.graph.Select;
import org.qbicc.graph.Shl;
import org.qbicc.graph.Shr;
import org.qbicc.graph.StackAllocation;
import org.qbicc.graph.Store;
import org.qbicc.graph.Sub;
import org.qbicc.graph.Switch;
import org.qbicc.graph.TailCall;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.TerminatorVisitor;
import org.qbicc.graph.Throw;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Unreachable;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.graph.VirtualMethodLookup;
import org.qbicc.graph.Xor;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ConstructorLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.GlobalVariableLiteral;
import org.qbicc.graph.literal.InstanceMethodLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.graph.literal.StaticMethodLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeIdLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.interpreter.InvalidMemoryAccessException;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmInvokable;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.pointer.ConstructorPointer;
import org.qbicc.pointer.ElementPointer;
import org.qbicc.pointer.ExecutableElementPointer;
import org.qbicc.pointer.GlobalPointer;
import org.qbicc.pointer.InstanceFieldPointer;
import org.qbicc.pointer.InstanceMethodPointer;
import org.qbicc.pointer.IntegerAsPointer;
import org.qbicc.pointer.MemberPointer;
import org.qbicc.pointer.MemoryPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.ReferenceAsPointer;
import org.qbicc.pointer.RootPointer;
import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.StructType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeIdType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.descriptor.MethodDescriptor;

final strictfp class Frame implements ActionVisitor<VmThreadImpl, Void>, ValueVisitor<VmThreadImpl, Object>, TerminatorVisitor<VmThreadImpl, BasicBlock> {
    static final Object MISSING = new Object();

    /**
     * The calling frame.
     */
    final Frame enclosing;

    /**
     * The current call depth.
     */
    final int depth;

    /**
     * The element being executed ({@code null} indicates a native frame).
     */
    final ExecutableElement element;

    /**
     * Local variable memory.
     */
    final MemoryPointer memoryPointer;

    /**
     * Frame values.
     */
    final Map<Value, Object> values = new HashMap<>();

    /**
     * Current block.
     */
    BasicBlock block;

    /**
     * Pointer for instruction within block.
     */
    Node ip;

    /**
     * Return value holder.
     */
    Object output;

    /**
     * The set of currently-held locks.
     */
    Set<Lock> heldLocks;

    /**
     * The caught exception.
     */
    VmThrowable exception;

    Frame(Frame enclosing, ExecutableElement element, MemoryPointer memoryPointer) {
        this.enclosing = enclosing;
        this.depth = enclosing == null ? 0 : enclosing.depth + 1;
        this.element = element;
        this.memoryPointer = memoryPointer;
    }

    /////////////////////
    // Execution handler
    /////////////////////

    private void assertSameTypes(BinaryValue val) {
        ValueType leftType = val.getLeftInput().getType();
        ValueType rightType = val.getRightInput().getType();
        if (leftType.getClass() == rightType.getClass()) {
            if (leftType instanceof ReferenceType && rightType instanceof ReferenceType) {
                // references of any type can be compared
                return;
            }
            if (leftType instanceof TypeIdType && rightType instanceof TypeIdType) {
                // type IDs can be compared
                return;
            }
            if (leftType.equals(rightType)) {
                // identical types can be compared
                return;
            }
        }
        throw new IllegalStateException("Node type mismatch");
    }

    private static IllegalStateException badInputType() {
        return new IllegalStateException("Bad input type");
    }

    ////////
    // Stack
    ////////

    public ProgramLocatable[] getBackTrace() {
        int depth = 0;
        Frame frame = this;
        while (frame != null) {
            ProgramLocatable ip = frame.ip;
            while (ip != null) {
                if (ip.element().hasNoModifiersOf(ClassFile.I_ACC_HIDDEN)) {
                    depth++;
                }
                ip = ip.callSite();
            }
            frame = frame.enclosing;
        }
        frame = this;
        ProgramLocatable[] backTrace = new Node[depth];
        depth = 0;
        while (frame != null) {
            ProgramLocatable ip = frame.ip;
            while (ip != null) {
                if (ip.element().hasNoModifiersOf(ClassFile.I_ACC_HIDDEN)) {
                    backTrace[depth++] = ip;
                }
                ip = ip.callSite();
            }
            frame = frame.enclosing;
        }
        return backTrace;
    }

    //////////
    // Values
    //////////

    @Override
    public Object visitUnknown(VmThreadImpl thread, Value node) {
        throw illegalInstruction();
    }

    @Override
    public Object visit(VmThreadImpl thread, Add node) {
        ValueType inputType = node.getLeftInput().getType();
        assertSameTypes(node);
        if (isInt64(inputType)) {
            Object raw = require(node.getLeftInput());
            if (raw instanceof Pointer p) {
                // pointer arithmetic
                return p.offsetInBytes(unboxLong(node.getRightInput()), true);
            } else {
                // long math
                return box(unboxLong(node.getLeftInput()) + unboxLong(node.getRightInput()), node.getType());
            }
        } else if (isInteger(inputType)) {
            // truncated integer math
            return box(unboxInt(node.getLeftInput()) + unboxInt(node.getRightInput()), node.getType());
        } else if (isFloat32(inputType)) {
            return box(unboxFloat(node.getLeftInput()) + unboxFloat(node.getRightInput()), node.getType());
        } else if (isFloat64(inputType)) {
            return box(unboxDouble(node.getLeftInput()) + unboxDouble(node.getRightInput()), node.getType());
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, And node) {
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        assertSameTypes(node);
        if (isInt64(inputType)) {
            // TODO: Remove this when late-scheduling can be enabled.
            //       This hacks around early scheduling of a bitwise and in runtime-only code in the DirectByteBuffer constructor
            if (require(left) instanceof GlobalPointer) {
                return box(0L, node.getType());
            }
            return box(unboxLong(left) & unboxLong(right), node.getType());
        } else if (isInt32(inputType)) {
            return box(unboxInt(left) & unboxInt(right), node.getType());
        } else if (isBool(inputType)) {
            return Boolean.valueOf(unboxBool(left) & unboxBool(right));
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, BitCast node) {
        return bitCast(node.getInput(), node.getType());
    }

    private Object bitCast(final Value input, final WordType outputType) {
        WordType inputType = (WordType) input.getType();
        if (inputType.equals(outputType)) {
            return require(input);
        } else if (isRef(inputType) && isRef(outputType)) {
            if (isRef(outputType)) {
                return require(input);
            } else if (isPointer(outputType)) {
                Object obj = require(input);
                return obj == null ? null : new ReferenceAsPointer((VmObject) obj);
            }
        } else if (isPointer(inputType)) {
            if (isPointer(outputType)) {
                return require(input);
            } else if (isInt64(outputType) && inputType.getMinBits() == 64) {
                Object val = require(input);
                if (val instanceof IntegerAsPointer iap) {
                    return Long.valueOf(iap.getValue());
                } else {
                    return val;
                }
            } else if (isInt32(outputType) && inputType.getMinBits() == 32) {
                Object val = require(input);
                if (val instanceof IntegerAsPointer iap) {
                    return Integer.valueOf((int) iap.getValue());
                } else {
                    return val;
                }
            }
        } else if (isInt32(inputType)) {
            if (isPointer(outputType) && outputType.getMinBits() == 32) {
                Object val = require(input);
                if (val instanceof Pointer ptr) {
                    return ptr;
                } else if (val instanceof Number num) {
                    PointerType voidPtr = outputType.getTypeSystem().getVoidType().getPointer();
                    return new IntegerAsPointer(voidPtr, num.longValue());
                }
            } else if (isInt32(outputType)) {
                return require(input);
            } else if (isFloat32(outputType)) {
                return box(Float.intBitsToFloat(unboxInt(input)), outputType);
            }
        } else if (isInt64(inputType)) {
            if (isPointer(outputType) && outputType.getMinBits() == 64) {
                Object val = require(input);
                if (val instanceof Pointer ptr) {
                    return ptr;
                } else if (val instanceof Number num) {
                    PointerType voidPtr = outputType.getTypeSystem().getVoidType().getPointer();
                    return new IntegerAsPointer(voidPtr, num.longValue());
                }
            } else if (isInt64(outputType)) {
                return require(input);
            } else if (isFloat64(outputType)) {
                return box(Double.longBitsToDouble(unboxLong(input)), outputType);
            }
        } else if (isFloat32(inputType)) {
            if (isInt32(outputType)) {
                return box(Float.floatToRawIntBits(unboxFloat(input)), outputType);
            }
        } else if (isFloat64(inputType)) {
            if (isInt64(outputType)) {
                return box(Double.doubleToRawLongBits(unboxDouble(input)), outputType);
            }
        } else if (isIntSameWidth(inputType, outputType)) {
            return require(input);
        }
        throw new IllegalStateException("Invalid cast");
    }

    @Override
    public Object visit(VmThreadImpl param, BitReverse node) {
        Value input = node.getInput();
        ValueType inputType = input.getType();
        if (isInt64(inputType)) {
            return box(Long.reverse(unboxLong(input)), inputType);
        } else if (isInt32(inputType)) {
            return box(Integer.reverse(unboxInt(input)), inputType);
        } else if (isInt16(inputType)) {
            return box(Integer.reverse(unboxInt(input)) >>> 16, inputType);
        } else if (isInt8(inputType)) {
            return box(Integer.reverse(unboxInt(input)) >>> 24, inputType);
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl vmThread, ByteOffsetPointer node) {
        // allow all supportable combinations of a + b
        Object base = require(node.getBasePointer());
        Object off = require(node.getOffset());
        if (base == null) {
            if (off == null) {
                return null;
            } if (off instanceof Pointer p) {
                return p;
            } else if (off instanceof Number n) {
                long offVal = n.longValue();
                return offVal == 0 ? null : new IntegerAsPointer(node.getType(), offVal);
            } else {
                // ??
                return null;
            }
        } else if (base instanceof Number || base instanceof IntegerAsPointer) {
            long baseVal = base instanceof Number n ? n.longValue() : ((IntegerAsPointer) base).getValue();
            if (off == null) {
                return new IntegerAsPointer(node.getType(), baseVal);
            } else if (off instanceof Number || off instanceof IntegerAsPointer) {
                long offVal = off instanceof Number n ? n.longValue() : ((IntegerAsPointer) off).getValue();
                return new IntegerAsPointer(node.getType(), baseVal + offVal);
            } else if (off instanceof Pointer offPtr) {
                return offPtr.offsetInBytes(baseVal, false);
            } else {
                // ??
                return null;
            }
        } else if (base instanceof Pointer basePtr) {
            if (off == null) {
                return basePtr;
            } else if (off instanceof Number || off instanceof IntegerAsPointer) {
                long offVal = off instanceof Number n ? n.longValue() : ((IntegerAsPointer) off).getValue();
                return basePtr.offsetInBytes(offVal, false);
            } else if (off instanceof Pointer) {
                // pointer + pointer
                return null;
            } else {
                // ??
                return null;
            }
        } else {
            // ??
            return null;
        }
    }

    @Override
    public Object visit(VmThreadImpl param, ByteSwap node) {
        Value input = node.getInput();
        ValueType inputType = input.getType();
        if (isInt64(inputType)) {
            return box(Long.reverseBytes(unboxLong(input)), inputType);
        } else if (isInt32(inputType)) {
            return box(Integer.reverseBytes(unboxInt(input)), inputType);
        } else if (isInt16(inputType)) {
            return box(Short.reverseBytes((short) unboxInt(input)), inputType);
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, CheckCast node) {
        Object input = require(node.getInput());
        if (input instanceof VmObjectImpl) {
            // OK we can do it
            CheckCast.CastType kind = node.getKind();
            ObjectType toType = (ObjectType) require(node.getToType());
            ObjectType objType = ((VmObjectImpl) input).getObjectType();
            for (int dims = unboxInt(node.getToDimensions()); dims > 0; dims --) {
                if (objType instanceof ReferenceArrayObjectType) {
                    objType = ((ReferenceArrayObjectType) objType).getElementObjectType();
                } else {
                    throw failCast(thread, kind);
                }
            }
            if (! objType.isSubtypeOf(toType)) {
                throw failCast(thread, kind);
            }
        }
        // otherwise just ignore it
        return input;
    }

    private Thrown failCast(VmThreadImpl thread, CheckCast.CastType kind) {
        ClassObjectType exType;
        VmImpl vm = thread.getVM();
        ClassContext bcc = vm.getCompilationContext().getBootstrapClassContext();
        LoadedTypeDefinition exDefined;
        if (kind == CheckCast.CastType.Cast) {
            exDefined = bcc.findDefinedType("java/lang/ClassCastException").load();
        } else {
            assert kind == CheckCast.CastType.ArrayStore;
            exDefined = bcc.findDefinedType("java/lang/ArrayStoreException").load();
        }
        exType = exDefined.getClassType();
        VmThrowable obj = (VmThrowable) vm.allocateObject(exType);
        vm.invokeExact(exDefined.resolveConstructorElement(MethodDescriptor.VOID_METHOD_DESCRIPTOR), obj, List.of());
        return new Thrown(obj);
    }

    @Override
    public Object visit(VmThreadImpl thread, ClassOf node) {
        Object type = require(node.getInput());
        VmClassImpl simpleType = getSimpleType(thread, type);
        for (int dimensions = unboxInt(node.getDimensions()); dimensions > 0; dimensions --) {
            simpleType = simpleType.getArrayClass();
        }
        return simpleType;
    }

    private VmClassImpl getSimpleType(final VmThreadImpl thread, final Object type) {
        // TODO: replace these if-trees with new `Primitive` class
        if (type instanceof PrimitiveArrayObjectType) {
            WordType elementType = ((PrimitiveArrayObjectType) type).getElementType();
            if (elementType instanceof BooleanType) {
                return thread.getVM().booleanArrayClass;
            } else if (elementType instanceof SignedIntegerType) {
                SignedIntegerType signed = (SignedIntegerType) elementType;
                if (signed.getMinBits() == 8) {
                    return thread.getVM().byteArrayClass;
                } else if (signed.getMinBits() == 16) {
                    return thread.getVM().shortArrayClass;
                } else if (signed.getMinBits() == 32) {
                    return thread.getVM().intArrayClass;
                } else if (signed.getMinBits() == 64) {
                    return thread.getVM().longArrayClass;
                }
            } else if (elementType instanceof UnsignedIntegerType) {
                UnsignedIntegerType unsigned = (UnsignedIntegerType) elementType;
                if (unsigned.getMinBits() == 16) {
                    return thread.getVM().charArrayClass;
                }
            } else if (elementType instanceof FloatType) {
                FloatType floatType = (FloatType) elementType;
                if (floatType.getMinBits() == 32) {
                    return thread.getVM().floatArrayClass;
                } else  if (floatType.getMinBits() == 64) {
                    return thread.getVM().doubleArrayClass;
                }
            }
        } else if (type instanceof ObjectType) {
            return (VmClassImpl) ((ObjectType) type).getDefinition().load().getVmClass();
        } else if (type instanceof BooleanType) {
            return thread.getVM().booleanClass;
        } else if (type instanceof SignedIntegerType) {
            SignedIntegerType signed = (SignedIntegerType) type;
            if (signed.getMinBits() == 8) {
                return thread.getVM().byteClass;
            } else if (signed.getMinBits() == 16) {
                return thread.getVM().shortClass;
            } else if (signed.getMinBits() == 32) {
                return thread.getVM().intClass;
            } else if (signed.getMinBits() == 64) {
                return thread.getVM().longClass;
            }
        } else if (type instanceof UnsignedIntegerType) {
            UnsignedIntegerType unsigned = (UnsignedIntegerType) type;
            if (unsigned.getMinBits() == 16) {
                return thread.getVM().charClass;
            }
        } else if (type instanceof FloatType) {
            FloatType floatType = (FloatType) type;
            if (floatType.getMinBits() == 32) {
                return thread.getVM().floatClass;
            } else  if (floatType.getMinBits() == 64) {
                return thread.getVM().doubleClass;
            }
        } else if (type instanceof VoidType) {
            return thread.getVM().voidClass;
        }
        throw new IllegalStateException("Invalid type argument for ClassOf()");
    }

    @Override
    public Object visit(VmThreadImpl thread, Cmp node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        if (isUnsigned(inputType)) {
            if (isInt8(inputType)) {
                return Integer.valueOf(Byte.compareUnsigned((byte) unboxInt(left), (byte) unboxInt(right)));
            } else if (isInt16(inputType)) {
                return Integer.valueOf(Short.compareUnsigned((short) unboxInt(left), (short) unboxInt(right)));
            } else if (isInt32(inputType)) {
                return Integer.valueOf(Integer.compareUnsigned(unboxInt(left), unboxInt(right)));
            } else if (isInt64(inputType)) {
                return Integer.valueOf(Long.compareUnsigned(unboxLong(left), unboxLong(right)));
            }
        } else if (isSigned(inputType)) {
            if (isInt8(inputType)) {
                return Integer.valueOf(Byte.compare((byte) unboxInt(left), (byte) unboxInt(right)));
            } else if (isInt16(inputType)) {
                return Integer.valueOf(Short.compare((short) unboxInt(left), (short) unboxInt(right)));
            } else if (isInt32(inputType)) {
                return Integer.valueOf(Integer.compare(unboxInt(left), unboxInt(right)));
            } else if (isInt8(inputType)) {
                return Integer.valueOf(Long.compare(unboxLong(left), unboxLong(right)));
            }
        } else if (isFloat32(inputType)) {
            float f1 = unboxFloat(left);
            float f2 = unboxFloat(right);
            if (f1 < f2) {
                return Integer.valueOf(-1);
            } else if (f1 > f2) {
                return Integer.valueOf(1);
            } else {
                return Integer.valueOf(0);
            }
        } else if (isFloat64(inputType)) {
            double f1 = unboxDouble(left);
            double f2 = unboxDouble(right);
            if (f1 < f2) {
                return Integer.valueOf(-1);
            } else if (f1 > f2) {
                return Integer.valueOf(1);
            } else {
                return Integer.valueOf(0);
            }
        }
        throw new IllegalStateException("Invalid cmp");
    }

    @Override
    public Object visit(VmThreadImpl thread, CmpG node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        if (isFloat32(inputType)) {
            float f1 = unboxFloat(left);
            float f2 = unboxFloat(right);
            if (f1 < f2) {
                return Integer.valueOf(-1);
            } else if (f1 == f2) {
                return Integer.valueOf(0);
            } else {
                return Integer.valueOf(1);
            }
        } else if (isFloat64(inputType)) {
            double f1 = unboxDouble(left);
            double f2 = unboxDouble(right);
            if (f1 < f2) {
                return Integer.valueOf(-1);
            } else if (f1 == f2) {
                return Integer.valueOf(0);
            } else {
                return Integer.valueOf(1);
            }
        }
        throw new IllegalStateException("Invalid cmp");
    }

    @Override
    public Object visit(VmThreadImpl thread, CmpL node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        if (isFloat32(inputType)) {
            float f1 = unboxFloat(left);
            float f2 = unboxFloat(right);
            if (f1 > f2) {
                return Integer.valueOf(1);
            } else if (f1 == f2) {
                return Integer.valueOf(0);
            } else {
                return Integer.valueOf(-1);
            }
        } else if (isFloat64(inputType)) {
            double f1 = unboxDouble(left);
            double f2 = unboxDouble(right);
            if (f1 > f2) {
                return Integer.valueOf(1);
            } else if (f1 == f2) {
                return Integer.valueOf(0);
            } else {
                return Integer.valueOf(-1);
            }
        }
        throw new IllegalStateException("Invalid cmp");
    }

    @Override
    public Object visit(VmThreadImpl param, CountLeadingZeros node) {
        Value input = node.getInput();
        ValueType inputType = input.getType();
        if (isInt64(inputType)) {
            return box(Long.numberOfLeadingZeros(unboxLong(input)), node.getType());
        } else if (isInt32(inputType)) {
            return box(Integer.numberOfLeadingZeros(unboxInt(input)), node.getType());
        } else if (isInt16(inputType)) {
            return box(Integer.numberOfLeadingZeros(unboxInt(input) << 16 | 0x0000ffff), node.getType());
        } else if (isInt8(inputType)) {
            return box(Integer.numberOfLeadingZeros(unboxInt(input) << 24 | 0x00ffffff), node.getType());
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl param, CountTrailingZeros node) {
        Value input = node.getInput();
        ValueType inputType = input.getType();
        if (isInt64(inputType)) {
            return box(Long.numberOfTrailingZeros(unboxLong(input)), node.getType());
        } else if (isInt32(inputType)) {
            return box(Integer.numberOfTrailingZeros(unboxInt(input)), node.getType());
        } else if (isInt16(inputType)) {
            return box(Integer.numberOfTrailingZeros(unboxInt(input) | 0xffff0000), node.getType());
        } else if (isInt8(inputType)) {
            return box(Integer.numberOfTrailingZeros(unboxInt(input) | 0xffffff00), node.getType());
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl vmThread, CurrentThread node) {
        return vmThread.getSelfPointer();
    }

    @Override
    public Object visit(VmThreadImpl vmThread, DecodeReference node) {
        Object obj = require(node.getInput());
        if (obj == null) {
            return null;
        }
        return new ReferenceAsPointer((VmObject) obj);
    }

    @Override
    public Object visit(VmThreadImpl vmThread, EncodeReference node) {
        Object obj = require(node.getInput());
        if (obj instanceof ReferenceAsPointer rap) {
            return rap.getReference();
        } else if (obj == null) {
            return null;
        } else {
            throw badInputType();
        }
    }

    @Override
    public Object visit(VmThreadImpl thread, Div node) {
        ValueType inputType = node.getLeftInput().getType();
        assertSameTypes(node);
        if (isSigned(inputType)) {
            if (isInt64(inputType)) {
                // long math
                return box(unboxLong(node.getLeftInput()) / unboxLong(node.getRightInput()), node.getType());
            } else if (isInteger(inputType)) {
                // truncated integer math
                return box(unboxInt(node.getLeftInput()) / unboxInt(node.getRightInput()), node.getType());
            }
        } else if (isUnsigned(inputType)) {
            if (isInt64(inputType)) {
                return box(Long.divideUnsigned(unboxLong(node.getLeftInput()), unboxLong(node.getRightInput())), node.getType());
            } else if (isInt32(inputType)) {
                return box(Integer.divideUnsigned(unboxInt(node.getLeftInput()), unboxInt(node.getRightInput())), node.getType());
            } else if (isInt16(inputType)) {
                return box((short) (Short.toUnsignedInt((short) unboxInt(node.getLeftInput())) / Short.toUnsignedInt((short) unboxInt(node.getRightInput()))), node.getType());
            } else if (isInt8(inputType)) {
                return box((byte) (Byte.toUnsignedInt((byte) unboxInt(node.getLeftInput())) / Byte.toUnsignedInt((byte) unboxInt(node.getRightInput()))), node.getType());
            }
        } else if (isFloat32(inputType)) {
            return box(unboxFloat(node.getLeftInput()) / unboxFloat(node.getRightInput()), node.getType());
        } else if (isFloat64(inputType)) {
            return box(unboxDouble(node.getLeftInput()) / unboxDouble(node.getRightInput()), node.getType());
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, ElementOf node) {
        int index = unboxInt(node.getIndex());
        Value arrayPointerValue = node.getArrayPointer();
        if (arrayPointerValue instanceof DecodeReference dr && dr.getInput().getType() instanceof ReferenceType referenceType) {
            // array object access?
            PhysicalObjectType physicalBound = referenceType.getUpperBound();
            if (physicalBound instanceof ArrayObjectType aot) {
                CompilationContext ctxt = element.getEnclosingType().getContext().getCompilationContext();
                CoreClasses coreClasses = CoreClasses.get(ctxt);
                InstanceFieldElement field = (InstanceFieldElement) coreClasses.getArrayContentField(aot);
                return new ElementPointer(new InstanceFieldPointer(unboxPointer(dr), field), index);
            } else {
                throw unsupportedType();
            }
        } else {
            Pointer arrayPointer = unboxPointer(arrayPointerValue);
            if (arrayPointer == null) {
                return null;
            } else {
                // todo: fix by removing types from pointers
                while (arrayPointer.getType().getPointeeType() instanceof StructType st) {
                    final StructType.Member zeroMember = st.getMember(0);
                    if (zeroMember.getOffset() != 0) {
                        throw new IllegalStateException();
                    }
                    arrayPointer = new MemberPointer(arrayPointer, zeroMember);
                }
                return new ElementPointer(arrayPointer, index);
            }
        }
    }

    @Override
    public Object visit(VmThreadImpl thread, Extend node) {
        Value input = node.getInput();
        WordType inputType = (WordType) input.getType();
        WordType outputType = node.getType();
        if (isSigned(inputType)) {
            if (isInt8(inputType)) {
                return box((byte)unboxInt(input), outputType);
            } else if (isInt16(inputType)) {
                return box((short)unboxInt(input), outputType);
            } else if (isInt32(inputType)) {
                return box((int)unboxLong(input), outputType);
            } else if (isInt64(inputType)) {
                return box(unboxLong(input), outputType);
            }
        } else if (isUnsigned(inputType)) {
            if (isInt8(inputType)) {
                return box(unboxInt(input) & 0xff, outputType);
            } else if (isInt16(inputType)) {
                return box(unboxInt(input) & 0xffff, outputType);
            } else if (isInt32(inputType)) {
                return box(unboxInt(input) & 0xffff_ffffL, outputType);
            } else if (isInt64(inputType)) {
                return box(unboxLong(input), outputType);
            }
        } else if (isFloat32(inputType) && isFloat64(outputType)) {
            return box((double) unboxFloat(input), outputType);
        } else if (isBool(inputType)) {
            return box(unboxBool(input) ? 1 : 0, outputType);
        }
        throw new IllegalStateException("Invalid extend");
    }

    @Override
    public Object visit(VmThreadImpl param, ExtractMember node) {
        Value input = node.getStructValue();
        Memory compound = (Memory) require(input);
        ValueType resultType = node.getType();
        int offset = node.getMember().getOffset();
        if (isInt8(resultType)) {
            return box(compound.load8(offset, SinglePlain), resultType);
        } else if (isInt16(resultType)) {
            return box(compound.load16(offset, SinglePlain), resultType);
        } else if (isInt32(resultType)) {
            return box(compound.load32(offset, SinglePlain), resultType);
        } else if (isInt64(resultType)) {
            return box(compound.load64(offset, SinglePlain), resultType);
        } else if (isFloat32(resultType)) {
            return box(Float.intBitsToFloat(compound.load32(offset, SinglePlain)), resultType);
        } else if (isFloat64(resultType)) {
            return box(Double.longBitsToDouble(compound.load64(offset, SinglePlain)), resultType);
        } else if (isBool(resultType)) {
            return Boolean.valueOf((compound.load8(offset, SinglePlain) & 1) != 0);
        } else if (isRef(resultType)) {
            return compound.loadRef(offset, SinglePlain);
        } else if (resultType instanceof PointerType) {
            return compound.loadPointer(offset, SinglePlain);
        } else {
            throw new IllegalStateException("Invalid type for extract");
        }
    }

    @Override
    public Object visit(VmThreadImpl vmThread, FpToInt node) {
        Value input = node.getInput();
        FloatType inputType = (FloatType) input.getType();
        IntegerType outputType = node.getType();
        if (isFloat32(inputType)) {
            if (isSigned(outputType)) {
                if (isInt64(outputType)) {
                    return box((long) unboxFloat(input), outputType);
                } else if (isInt32(outputType)) {
                    return box((int) unboxFloat(input), outputType);
                } else if (isInt16(outputType)) {
                    return box((short) unboxFloat(input), outputType);
                } else if (isInt8(outputType)) {
                    return box((byte) unboxFloat(input), outputType);
                }
            } else if (isUnsigned(outputType)) {
                if (isInt64(outputType)) {
                    throw new UnsupportedOperationException("Unsupported conversion (will implement later)");
                } else if (isInt32(outputType)) {
                    throw new UnsupportedOperationException("Unsupported conversion (will implement later)");
                } else if (isInt16(outputType)) {
                    return box((char) unboxFloat(input), outputType);
                } else if (isInt8(outputType)) {
                    throw new UnsupportedOperationException("Unsupported conversion (will implement later)");
                }
            }
        } else if (isFloat64(inputType)) {
            if (isSigned(outputType)) {
                if (isInt64(outputType)) {
                    return box((long) unboxDouble(input), outputType);
                } else if (isInt32(outputType)) {
                    return box((int) unboxDouble(input), outputType);
                } else if (isInt16(outputType)) {
                    return box((short) unboxDouble(input), outputType);
                } else if (isInt8(outputType)) {
                    return box((byte) unboxDouble(input), outputType);
                }
            } else if (isUnsigned(outputType)) {
                if (isInt64(outputType)) {
                    throw new UnsupportedOperationException("Unsupported conversion (will implement later)");
                } else if (isInt32(outputType)) {
                    throw new UnsupportedOperationException("Unsupported conversion (will implement later)");
                } else if (isInt16(outputType)) {
                    return box((char) unboxDouble(input), outputType);
                } else if (isInt8(outputType)) {
                    throw new UnsupportedOperationException("Unsupported conversion (will implement later)");
                }
            }
        }
        throw new IllegalStateException("Invalid cast");
    }

    @Override
    public Object visit(VmThreadImpl vmThread, InstanceFieldOf node) {
        Value instance = node.getInstance();
        Pointer pointer = unboxPointer(instance);
        if (pointer == null) {
            return null;
        }
        return new InstanceFieldPointer(pointer, node.getVariableElement());
    }

    @Override
    public Object visit(VmThreadImpl thread, InstanceOf node) {
        Value instance = node.getInstance();
        Object value = require(instance);
        if (value instanceof VmObject) {
            VmObject obj = (VmObject) value;
            ObjectType checkType = node.getCheckType();
            ObjectType objType = obj.getObjectType();
            for (int dims = node.getCheckDimensions(); dims > 0; dims --) {
                if (objType instanceof ReferenceArrayObjectType) {
                    objType = ((ReferenceArrayObjectType) objType).getElementObjectType();
                } else {
                    return false;
                }
            }
            return Boolean.valueOf(objType.isSubtypeOf(checkType));
        }
        return Boolean.FALSE;
    }

    @Override
    public Object visit(VmThreadImpl vmThread, IntToFp node) {
        Value input = node.getInput();
        IntegerType inputType = (IntegerType) input.getType();
        FloatType outputType = node.getType();
        if (isSigned(inputType)) {
            if (isInt64(inputType)) {
                if (isFloat32(outputType)) {
                    return box((float) unboxLong(input), outputType);
                } else if (isFloat64(outputType)) {
                    return box((double) unboxLong(input), outputType);
                }
            } else if (isInteger(inputType)) {
                if (isFloat32(outputType)) {
                    return box((float) unboxInt(input), outputType);
                } else if (isFloat64(outputType)) {
                    return box((double) unboxInt(input), outputType);
                }
            }
        } else if (isUnsigned(inputType)) {
            if (isInt64(inputType)) {
                // todo: this may or may not be right re: rounding...
                long inLong = unboxLong(input);
                if (isFloat32(outputType)) {
                    return box(Math.fma((float) (inLong >>> 1L), 2f, (float) (inLong & 1)), outputType);
                } else if (isFloat64(outputType)) {
                    return box(Math.fma((double) (inLong >>> 1L), 2f, (double) (inLong & 1)), outputType);
                }
            } else if (isInt32(inputType)) {
                if (isFloat32(outputType)) {
                    return box((float) (unboxLong(input) & 0xffff_ffffL), outputType);
                } else if (isFloat64(outputType)) {
                    return box((double) (unboxLong(input) & 0xffff_ffffL), outputType);
                }
            } else if (isInt16(inputType)) {
                if (isFloat32(outputType)) {
                    return box((float) (unboxInt(input) & 0xffff), outputType);
                } else if (isFloat64(outputType)) {
                    return box((double) (unboxInt(input) & 0xffff), outputType);
                }
            } else if (isInt8(inputType)) {
                if (isFloat32(outputType)) {
                    return box((float) (unboxInt(input) & 0xff), outputType);
                } else if (isFloat64(outputType)) {
                    return box((double) (unboxInt(input) & 0xff), outputType);
                }
            }
        }
        throw new IllegalStateException("Invalid cast");
    }

    @Override
    public Object visit(VmThreadImpl thread, IsEq node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        if (isFloat32(inputType)) {
            return Boolean.valueOf(unboxFloat(left) == unboxFloat(right));
        } else if (isFloat64(inputType)) {
            return Boolean.valueOf(unboxDouble(left) == unboxDouble(right));
        } else if (isInt64(inputType)) {
            return Boolean.valueOf(unboxLong(left) == unboxLong(right));
        } else if (isInteger(inputType)) {
            return Boolean.valueOf(unboxInt(left) == unboxInt(right));
        } else if (isBool(inputType)) {
            return Boolean.valueOf(unboxBool(left) == unboxBool(right));
        } else if (isRef(inputType) || inputType instanceof PointerType) {
            return Boolean.valueOf(require(left) == require(right));
        } else if (isTypeId(inputType)) {
            return Boolean.valueOf(unboxType(left).equals(unboxType(right)));
        }
        throw new IllegalStateException("Invalid is*");
    }

    @Override
    public Object visit(VmThreadImpl thread, IsNe node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        if (isFloat32(inputType)) {
            return Boolean.valueOf(unboxFloat(left) != unboxFloat(right));
        } else if (isFloat64(inputType)) {
            return Boolean.valueOf(unboxDouble(left) != unboxDouble(right));
        } else if (isInt64(inputType)) {
            // Allow "null check" idiom of comparing a Pointer to 0
            Object leftRaw = require(left);
            if (leftRaw instanceof Pointer && unboxLong(right) == 0) {
                return Boolean.valueOf(true);
            }
            return Boolean.valueOf(unboxLong(left) != unboxLong(right));
        } else if (isInteger(inputType)) {
            return Boolean.valueOf(unboxInt(left) != unboxInt(right));
        } else if (isBool(inputType)) {
            return Boolean.valueOf(unboxBool(left) != unboxBool(right));
        } else if (isRef(inputType) || inputType instanceof PointerType) {
            return Boolean.valueOf(require(left) != require(right));
        } else if (isTypeId(inputType)) {
            return Boolean.valueOf(! unboxType(left).equals(unboxType(right)));
        }
        throw new IllegalStateException("Invalid is*");
    }

    @Override
    public Object visit(VmThreadImpl thread, IsGe node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        if (isFloat32(inputType)) {
            return Boolean.valueOf(unboxFloat(left) >= unboxFloat(right));
        } else if (isFloat64(inputType)) {
            return Boolean.valueOf(unboxDouble(left) >= unboxDouble(right));
        } else if (isInt64(inputType)) {
            return Boolean.valueOf(isSigned(inputType) ? compareLongSigned(left, right, -1) >= 0 : compareLongUnsigned(left, right, -1) >= 0);
        } else if (isInteger(inputType)) {
            return Boolean.valueOf(isSigned(inputType) ? unboxInt(left) >= unboxInt(right) : Integer.compareUnsigned(unboxInt(left), unboxInt(right)) >= 0);
        }
        throw new IllegalStateException("Invalid is*");
    }

    @Override
    public Object visit(VmThreadImpl thread, IsGt node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        if (isFloat32(inputType)) {
            return Boolean.valueOf(unboxFloat(left) > unboxFloat(right));
        } else if (isFloat64(inputType)) {
            return Boolean.valueOf(unboxDouble(left) > unboxDouble(right));
        } else if (isInt64(inputType)) {
            return Boolean.valueOf(isSigned(inputType) ? compareLongSigned(left, right, -1) > 0 : compareLongUnsigned(left, right, -1) > 0);
        } else if (isInteger(inputType)) {
            return Boolean.valueOf(isSigned(inputType) ? unboxInt(left) > unboxInt(right) : Integer.compareUnsigned(unboxInt(left), unboxInt(right)) > 0);
        }
        throw new IllegalStateException("Invalid is*");
    }

    @Override
    public Object visit(VmThreadImpl thread, IsLe node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        if (isFloat32(inputType)) {
            return Boolean.valueOf(unboxFloat(left) <= unboxFloat(right));
        } else if (isFloat64(inputType)) {
            return Boolean.valueOf(unboxDouble(left) <= unboxDouble(right));
        } else if (isInt64(inputType)) {
            return Boolean.valueOf(isSigned(inputType) ? compareLongSigned(left, right, 1) <= 0 : compareLongUnsigned(left, right, 1) <= 0);
        } else if (isInteger(inputType)) {
            return Boolean.valueOf(isSigned(inputType) ? unboxInt(left) <= unboxInt(right) : Integer.compareUnsigned(unboxInt(left), unboxInt(right)) <= 0);
        }
        throw new IllegalStateException("Invalid is*");
    }

    @Override
    public Object visit(VmThreadImpl thread, IsLt node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        if (isFloat32(inputType)) {
            return Boolean.valueOf(unboxFloat(left) < unboxFloat(right));
        } else if (isFloat64(inputType)) {
            return Boolean.valueOf(unboxDouble(left) < unboxDouble(right));
        } else if (isInt64(inputType)) {
            return Boolean.valueOf(isSigned(inputType) ? compareLongSigned(left, right, 1) < 0 : compareLongUnsigned(left, right, 1) < 0);
        } else if (isInteger(inputType)) {
            return Boolean.valueOf(isSigned(inputType) ? unboxInt(left) < unboxInt(right) : Integer.compareUnsigned(unboxInt(left), unboxInt(right)) < 0);
        }
        throw new IllegalStateException("Invalid is*");
    }

    private int compareLongSigned(Value left, Value right, int undef) {
        Object leftObj = require(left);
        Object rightObj = require(right);
        if (leftObj == rightObj) {
            return 0;
        } else if (leftObj == null) {
            if (rightObj instanceof Number rightLong) {
                return Long.compare(0, rightLong.longValue());
            } else if (rightObj instanceof IntegerAsPointer rightPointer) {
                return Long.compare(0, rightPointer.getValue());
            } else if (rightObj instanceof Pointer) {
                return -1; // null < pointer
            } else {
                // treat comparison as undefined
                return undef;
            }
        } else if (leftObj instanceof Number leftLong) {
            if (rightObj == null) {
                return Long.compare(leftLong.longValue(), 0);
            } else if (rightObj instanceof Number rightLong) {
                return Long.compare(leftLong.longValue(), rightLong.longValue());
            } else if (rightObj instanceof IntegerAsPointer rightPointer) {
                return Long.compare(leftLong.longValue(), rightPointer.getValue());
            } else {
                // treat comparison as undefined
                return undef;
            }
        } else if (leftObj instanceof IntegerAsPointer leftPointer) {
            if (rightObj == null) {
                return Long.compare(leftPointer.getValue(), 0);
            } else if (rightObj instanceof Number rightLong) {
                return Long.compare(leftPointer.getValue(), rightLong.longValue());
            } else if (rightObj instanceof IntegerAsPointer rightPointer) {
                return Long.compare(leftPointer.getValue(), rightPointer.getValue());
            } else {
                // treat comparison as undefined
                return undef;
            }
        } else if (leftObj instanceof Pointer leftPointer) {
            if (rightObj == null) {
                return 1; // pointer > null
            } else if (rightObj instanceof Pointer rightPointer) {
                Memory leftMemory = leftPointer.getRootMemoryIfExists();
                Memory rightMemory = rightPointer.getRootMemoryIfExists();
                if (leftMemory != null && rightMemory == leftMemory) {
                    return Long.compareUnsigned(leftPointer.getRootByteOffset(), rightPointer.getRootByteOffset());
                } else {
                    return undef;
                }
            } else {
                return undef;
            }
        } else {
            // treat comparison as undefined
            return undef;
        }
    }

    private int compareLongUnsigned(Value left, Value right, int undef) {
        Object leftObj = require(left);
        Object rightObj = require(right);
        if (leftObj == rightObj) {
            return 0;
        } else if (leftObj == null) {
            if (rightObj instanceof Number rightLong) {
                return Long.compareUnsigned(0, rightLong.longValue());
            } else if (rightObj instanceof IntegerAsPointer rightPointer) {
                return Long.compareUnsigned(0, rightPointer.getValue());
            } else if (rightObj instanceof Pointer) {
                return -1; // null < pointer
            } else {
                // treat comparison as undefined
                return undef;
            }
        } else if (leftObj instanceof Number leftLong) {
            if (rightObj == null) {
                return Long.compareUnsigned(leftLong.longValue(), 0);
            } else if (rightObj instanceof Number rightLong) {
                return Long.compareUnsigned(leftLong.longValue(), rightLong.longValue());
            } else if (rightObj instanceof IntegerAsPointer rightPointer) {
                return Long.compareUnsigned(leftLong.longValue(), rightPointer.getValue());
            } else {
                // treat comparison as undefined
                return undef;
            }
        } else if (leftObj instanceof IntegerAsPointer leftPointer) {
            if (rightObj == null) {
                return Long.compareUnsigned(leftPointer.getValue(), 0);
            } else if (rightObj instanceof Number rightLong) {
                return Long.compareUnsigned(leftPointer.getValue(), rightLong.longValue());
            } else if (rightObj instanceof IntegerAsPointer rightPointer) {
                return Long.compareUnsigned(leftPointer.getValue(), rightPointer.getValue());
            } else {
                // treat comparison as undefined
                return undef;
            }
        } else if (leftObj instanceof Pointer leftPointer) {
            if (rightObj == null) {
                return 1; // pointer > null
            } else if (rightObj instanceof Pointer rightPointer) {
                Memory leftMemory = leftPointer.getRootMemoryIfExists();
                Memory rightMemory = rightPointer.getRootMemoryIfExists();
                if (leftMemory != null && rightMemory == leftMemory) {
                    return Long.compareUnsigned(leftPointer.getRootByteOffset(), rightPointer.getRootByteOffset());
                } else {
                    return undef;
                }
            } else {
                return undef;
            }
        } else {
            // treat comparison as undefined
            return undef;
        }
    }

    @Override
    public Object visit(VmThreadImpl thread, MemberOf node) {
        Pointer structPointer = unboxPointer(node.getStructurePointer());
        if (structPointer == null) {
            return null;
        } else {
            return new MemberPointer(structPointer, node.getMember());
        }
    }

    @Override
    public Object visit(VmThreadImpl thread, MemberOfUnion node) {
        return unboxPointer(node.getUnionPointer());
    }

    @Override
    public Object visit(VmThreadImpl thread, Max node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType type = left.getType();
        if (isSigned(type)) {
            if (isInt64(type)) {
                return box(Math.max(unboxLong(left), unboxLong(right)), type);
            } else {
                return box(Math.max(unboxInt(left), unboxInt(right)), type);
            }
        } else if (isUnsigned(type)) {
            long leftLong = unboxLong(left);
            long rightLong = unboxLong(right);
            int cmp = Long.compareUnsigned(leftLong, rightLong);
            return box(cmp < 0 ? rightLong : leftLong, type);
        } else if (isFloat32(type)) {
            return box(Math.max(unboxFloat(left), unboxFloat(right)), type);
        } else if (isFloat64(type)) {
            return box(Math.max(unboxDouble(left), unboxDouble(right)), type);
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, Min node) {
        assertSameTypes(node);
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType type = left.getType();
        if (isSigned(type)) {
            if (isInt64(type)) {
                return box(Math.min(unboxLong(left), unboxLong(right)), type);
            } else {
                return box(Math.min(unboxInt(left), unboxInt(right)), type);
            }
        } else if (isUnsigned(type)) {
            long leftLong = unboxLong(left);
            long rightLong = unboxLong(right);
            int cmp = Long.compareUnsigned(leftLong, rightLong);
            return box(cmp < 0 ? leftLong : rightLong, type);
        } else if (isFloat32(type)) {
            return box(Math.min(unboxFloat(left), unboxFloat(right)), type);
        } else if (isFloat64(type)) {
            return box(Math.min(unboxDouble(left), unboxDouble(right)), type);
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, Mod node) {
        ValueType inputType = node.getLeftInput().getType();
        assertSameTypes(node);
        if (isSigned(inputType)) {
            if (isInt64(inputType)) {
                // TODO: Remove this when late-scheduling can be enabled.
                //       This hacks around early scheduling of a mod in runtime-only code in the DirectByteBuffer constructor
                if (require(node.getLeftInput()) instanceof GlobalPointer) {
                    return box(0L, node.getType());
                }
                // long math
                return box(unboxLong(node.getLeftInput()) % unboxLong(node.getRightInput()), node.getType());
            } else if (isInteger(inputType)) {
                // truncated integer math
                return box(unboxInt(node.getLeftInput()) % unboxInt(node.getRightInput()), node.getType());
            }
        } else if (isUnsigned(inputType)) {
            if (isInt64(inputType)) {
                return box(Long.remainderUnsigned(unboxLong(node.getLeftInput()), unboxLong(node.getRightInput())), node.getType());
            } else if (isInt32(inputType)) {
                return box(Integer.remainderUnsigned(unboxInt(node.getLeftInput()), unboxInt(node.getRightInput())), node.getType());
            } else if (isInt16(inputType)) {
                return box((short) (Short.toUnsignedInt((short) unboxInt(node.getLeftInput())) % Short.toUnsignedInt((short) unboxInt(node.getRightInput()))), node.getType());
            } else if (isInt8(inputType)) {
                return box((byte) (Byte.toUnsignedInt((byte) unboxInt(node.getLeftInput())) % Byte.toUnsignedInt((byte) unboxInt(node.getRightInput()))), node.getType());
            }
        } else if (isFloat32(inputType)) {
            return box(unboxFloat(node.getLeftInput()) % unboxFloat(node.getRightInput()), node.getType());
        } else if (isFloat64(inputType)) {
            return box(unboxDouble(node.getLeftInput()) % unboxDouble(node.getRightInput()), node.getType());
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, Multiply node) {
        ValueType inputType = node.getLeftInput().getType();
        assertSameTypes(node);
        if (isInt64(inputType)) {
            // long math
            return box(unboxLong(node.getLeftInput()) * unboxLong(node.getRightInput()), node.getType());
        } else if (isInteger(inputType)) {
            // truncated integer math
            return box(unboxInt(node.getLeftInput()) * unboxInt(node.getRightInput()), node.getType());
        } else if (isFloat32(inputType)) {
            return box(unboxFloat(node.getLeftInput()) * unboxFloat(node.getRightInput()), node.getType());
        } else if (isFloat64(inputType)) {
            return box(unboxDouble(node.getLeftInput()) * unboxDouble(node.getRightInput()), node.getType());
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, Neg node) {
        Value input = node.getInput();
        ValueType inputType = input.getType();
        if (isSigned(inputType)) {
            return box(-unboxLong(input), inputType);
        } else if (isFloat32(inputType)) {
            return box(-unboxFloat(input), inputType);
        } else if (isFloat64(inputType)) {
            return box(-unboxDouble(input), inputType);
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl param, NotNull node) {
        return require(node.getInput());
    }

    @Override
    public Object visit(VmThreadImpl thread, Or node) {
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        assertSameTypes(node);
        if (isInt64(inputType)) {
            return box(unboxLong(left) | unboxLong(right), node.getType());
        } else if (isInt32(inputType)) {
            return box(unboxInt(left) | unboxInt(right), node.getType());
        } else if (isBool(inputType)) {
            return Boolean.valueOf(unboxBool(left) | unboxBool(right));
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl vmThread, PointerDifference node) {
        long pointeeSize = node.getLeftInput().getPointeeType().getSize();
        Pointer leftPtr = unboxPointer(node.getLeftInput());
        Pointer rightPtr = unboxPointer(node.getRightInput());
        if (leftPtr instanceof IntegerAsPointer leftIap) {
            if (rightPtr instanceof IntegerAsPointer rightIap) {
                return Long.valueOf((leftIap.getValue() - rightIap.getValue()) / pointeeSize);
            } else if (rightPtr == null) {
                return Long.valueOf(leftIap.getValue());
            } else {
                // we cannot negate rightPtr
                throw badInputType();
            }
        } else if (leftPtr == null) {
            if (rightPtr instanceof IntegerAsPointer rightIap) {
                return Long.valueOf((- rightIap.getValue()) / pointeeSize);
            } else if (rightPtr == null) {
                return Long.valueOf(0);
            } else {
                // we cannot negate rightPtr
                throw badInputType();
            }
        } else {
            // left isn't null and isn't an integer
            if (rightPtr == null || rightPtr instanceof IntegerAsPointer) {
                // cannot represent leftPtr as an integer
                throw badInputType();
            } else {
                // ptr - ptr only works if the base pointers are the same
                RootPointer leftRoot = leftPtr.getRootPointer();
                RootPointer rightRoot = rightPtr.getRootPointer();
                if (leftRoot.equals(rightRoot)) {
                    return Long.valueOf(leftPtr.getRootByteOffset() - rightRoot.getRootByteOffset());
                } else {
                    throw badInputType();
                }
            }
        }
    }

    @Override
    public Object visit(VmThreadImpl param, PopCount node) {
        Value input = node.getInput();
        ValueType inputType = input.getType();
        if (isInt64(inputType)) {
            return box(Long.bitCount(unboxLong(input)), node.getType());
        } else if (isInt32(inputType)) {
            return box(Integer.bitCount(unboxInt(input)), node.getType());
        } else if (isInt16(inputType)) {
            return box(Integer.bitCount(unboxInt(input) & 0x0000ffff), node.getType());
        } else if (isInt8(inputType)) {
            return box(Integer.bitCount(unboxInt(input) & 0x000000ff), node.getType());
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, Rol node) {
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        assertSameTypes(node);
        if (isInt64(inputType)) {
            return box(Long.rotateLeft(unboxLong(left), unboxInt(right)), node.getType());
        } else if (isInt32(inputType)) {
            return box(Integer.rotateLeft(unboxInt(left), unboxInt(right)), node.getType());
        } else if (isInt16(inputType)) {
            int leftInt = unboxInt(left);
            int rightInt = unboxInt(right);
            return box(leftInt << (rightInt & 0xf) | leftInt >>> (-rightInt & 0xf), inputType);
        } else if (isInt8(inputType)) {
            int leftInt = unboxInt(left);
            int rightInt = unboxInt(right);
            return box(leftInt << (rightInt & 0x7) | leftInt >>> (-rightInt & 0x7), inputType);
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, Ror node) {
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        assertSameTypes(node);
        if (isInt64(inputType)) {
            return box(Long.rotateRight(unboxLong(left), unboxInt(right)), node.getType());
        } else if (isInt32(inputType)) {
            return box(Integer.rotateRight(unboxInt(left), unboxInt(right)), node.getType());
        } else if (isInt16(inputType)) {
            int leftInt = unboxInt(left);
            int rightInt = unboxInt(right);
            return box(leftInt >>> (rightInt & 0xf) | leftInt << (-rightInt & 0xf), inputType);
        } else if (isInt8(inputType)) {
            int leftInt = unboxInt(left);
            int rightInt = unboxInt(right);
            return box(leftInt >>> (rightInt & 0x7) | leftInt << (-rightInt & 0x7), inputType);
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, Select node) {
        Boolean cond = (Boolean) require(node.getCondition());
        return cond.booleanValue() ? require(node.getTrueValue()) : require(node.getFalseValue());
    }

    @Override
    public Object visit(VmThreadImpl thread, Shl node) {
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        assertSameTypes(node);
        if (isInt64(inputType)) {
            return box(unboxLong(left) << unboxLong(right), node.getType());
        } else if (isInt32(inputType)) {
            return box(unboxInt(left) << unboxInt(right), node.getType());
        } else if (isInt16(inputType)) {
            return box(unboxInt(left) << (unboxInt(right) & 0xf), node.getType());
        } else if (isInt8(inputType)) {
            return box(unboxInt(left) << (unboxInt(right) & 0x7), node.getType());
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, Shr node) {
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        assertSameTypes(node);
        if (isSigned(inputType)) {
            if (isInt64(inputType)) {
                return box(unboxLong(left) >> unboxLong(right), node.getType());
            } else if (isInt32(inputType)) {
                return box(unboxInt(left) >> unboxInt(right), node.getType());
            } else if (isInt16(inputType)) {
                return box(unboxInt(left) >> (unboxInt(right) & 0xf), node.getType());
            } else if (isInt8(inputType)) {
                return box(unboxInt(left) >> (unboxInt(right) & 0x7), node.getType());
            }
        } else if (isUnsigned(inputType)) {
            if (isInt64(inputType)) {
                return box(unboxLong(left) >>> unboxLong(right), node.getType());
            } else if (isInt32(inputType)) {
                return box(unboxInt(left) >>> unboxInt(right), node.getType());
            } else if (isInt16(inputType)) {
                return box(unboxInt(left) >>> (unboxInt(right) & 0xf), node.getType());
            } else if (isInt8(inputType)) {
                return box(unboxInt(left) >>> (unboxInt(right) & 0x7), node.getType());
            }
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, Sub node) {
        ValueType inputType = node.getLeftInput().getType();
        assertSameTypes(node);
        if (isInt64(inputType)) {
            // TODO: Remove this when late-scheduling can be enabled.
            //       This hacks around early scheduling of a pointer computation in runtime-only code in the DirectByteBuffer constructor
            if (require(node.getLeftInput()) instanceof GlobalPointer) {
                return require(node.getLeftInput());
            }
            // long math
            return box(unboxLong(node.getLeftInput()) - unboxLong(node.getRightInput()), node.getType());
        } else if (isInteger(inputType)) {
            // truncated integer math
            return box(unboxInt(node.getLeftInput()) - unboxInt(node.getRightInput()), node.getType());
        } else if (isFloat32(inputType)) {
            return box(unboxFloat(node.getLeftInput()) - unboxFloat(node.getRightInput()), node.getType());
        } else if (isFloat64(inputType)) {
            return box(unboxDouble(node.getLeftInput()) - unboxDouble(node.getRightInput()), node.getType());
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, Truncate node) {
        Value input = node.getInput();
        WordType inputType = (WordType) input.getType();
        WordType outputType = node.getType();
        if (isSigned(outputType)) {
            if (isInt8(inputType)) {
                return box((byte)unboxInt(input), outputType);
            } else if (isInt16(inputType)) {
                return box((short)unboxInt(input), outputType);
            } else if (isInt32(inputType)) {
                return box((int)unboxLong(input), outputType);
            } else if (isInt64(inputType)) {
                return box(unboxLong(input), outputType);
            }
        } else if (isUnsigned(outputType)) {
            if (isInt8(inputType)) {
                return box(unboxInt(input) & 0xff, outputType);
            } else if (isInt16(inputType)) {
                return box(unboxInt(input) & 0xffff, outputType);
            } else if (isInt32(inputType)) {
                return box(unboxInt(input) & 0xffff_ffffL, outputType);
            } else if (isInt64(inputType)) {
                return box(unboxLong(input), outputType);
            }
        } else if (isFloat64(inputType) && isFloat32(outputType)) {
            return box((float) unboxDouble(input), outputType);
        } else if (isBool(outputType)) {
            return Boolean.valueOf((unboxInt(input) & 0x1) != 0);
        }
        throw new IllegalStateException("Invalid truncate");
    }

    @Override
    public Object visit(VmThreadImpl thread, Xor node) {
        Value left = node.getLeftInput();
        Value right = node.getRightInput();
        ValueType inputType = left.getType();
        assertSameTypes(node);
        if (isInt64(inputType)) {
            return box(unboxLong(left) ^ unboxLong(right), node.getType());
        } else if (isInt32(inputType)) {
            return box(unboxInt(left) ^ unboxInt(right), node.getType());
        } else if (isBool(inputType)) {
            return Boolean.valueOf(unboxBool(left) ^ unboxBool(right));
        }
        throw badInputType();
    }

    //

    ////////////
    // Literals
    ////////////

    @Override
    public Object visit(VmThreadImpl thread, ArrayLiteral node) {
        Memory memory = thread.getVM().allocate(node.getType(), 1);
        List<Literal> nodeValues = node.getValues();
        ValueType elementType = node.getType().getElementType();
        long elementSize = node.getType().getElementSize();
        for (int i = 0; i < nodeValues.size(); i++) {
            Literal value = nodeValues.get(i);
            store(thread, memory, (int) (elementSize * i), elementType, value, SingleUnshared);
        }
        return memory;
    }

    @Override
    public Object visit(VmThreadImpl param, BitCastLiteral node) {
        return bitCast(node.getValue(), node.getType());
    }

    @Override
    public Object visit(VmThreadImpl thread, BooleanLiteral node) {
        return Boolean.valueOf(node.booleanValue());
    }

    @Override
    public Object visit(VmThreadImpl vmThread, ConstructorLiteral literal) {
        return ConstructorPointer.of(literal.getExecutable());
    }

    @Override
    public Object visit(VmThreadImpl thread, FloatLiteral node) {
        return box(node.doubleValue(), node.getType());
    }

    @Override
    public Object visit(VmThreadImpl thread, GlobalVariableLiteral node) {
        return node.getVariableElement().getPointer();
    }

    @Override
    public Object visit(VmThreadImpl vmThread, InstanceMethodLiteral literal) {
        return InstanceMethodPointer.of(literal.getExecutable());
    }

    @Override
    public Object visit(VmThreadImpl thread, IntegerLiteral node) {
        return box(node.longValue(), node.getType());
    }

    @Override
    public Object visit(VmThreadImpl param, NullLiteral node) {
        return null;
    }

    @Override
    public Object visit(VmThreadImpl thread, ObjectLiteral node) {
        return node.getValue();
    }

    @Override
    public Object visit(VmThreadImpl vmThread, ProgramObjectLiteral literal) {
        return literal.getProgramObject().getPointer();
    }

    @Override
    public Object visit(VmThreadImpl thread, StaticFieldLiteral node) {
        return node.getVariableElement().getPointer();
    }

    @Override
    public Object visit(VmThreadImpl vmThread, StaticMethodLiteral literal) {
        return StaticMethodPointer.of(literal.getExecutable());
    }

    @Override
    public Object visit(VmThreadImpl thread, StringLiteral node) {
        return VmImpl.require().intern(node.getValue());
    }

    @Override
    public Object visit(VmThreadImpl param, TypeIdLiteral node) {
        return node.getValue();
    }

    @Override
    public Object visit(VmThreadImpl thread, UndefinedLiteral node) {
        throw new IllegalStateException("Invalid usage of undefined value");
    }

    @Override
    public Object visit(VmThreadImpl thread, ZeroInitializerLiteral node) {
        return VmImpl.require().allocate(node.getType(), 1);
    }

    ///////////////////////////
    // Block parameter
    ///////////////////////////

    @Override
    public Object visit(VmThreadImpl vmThread, BlockParameter node) {
        return require(node);
    }

    ///////////////////////////
    // Memory-affecting Values
    ///////////////////////////

    private Object call(VmThreadImpl thread, Value targetPtr, Value receiverValue, List<Object> arguments) {
        if (depth == 4096) {
            // todo: configure
            VmThrowableClassImpl soeClass = (VmThrowableClassImpl) thread.vm.getBootstrapClassLoader().loadClass("java/lang/StackOverflowError");
            throw new Thrown(soeClass.newInstance());
        }
        Object rawPtr = require(targetPtr);
        if (rawPtr instanceof ExecutableElementPointer ptr) {
            VmObject receiver = receiverValue.getType() instanceof VoidType ? null : (VmObject) require(receiverValue);
            ExecutableElement element = ptr.getExecutableElement();
            DefinedTypeDefinition def = element.getEnclosingType();
            VmClassImpl clazz = (VmClassImpl) def.load().getVmClass();
            clazz.initialize(thread);
            VmInvokable invokable = clazz.getOrCompile(element);
            return invokable.invokeAny(thread, receiver, arguments);
        } else {
            throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid pointer value (" + rawPtr + ")"));
        }
    }

    // Invocation

    @Override
    public Object visit(VmThreadImpl thread, Call node) {
        return call(thread, node.getTarget(), node.getReceiver(), require(node.getArguments()));
    }

    @Override
    public Object visit(VmThreadImpl thread, CallNoSideEffects node) {
        return call(thread, node.getTarget(), node.getReceiver(), require(node.getArguments()));
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, CallNoReturn node) {
        call(thread, node.getTarget(), node.getReceiver(), require(node.getArguments()));
        throw Assert.unreachableCode();
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, Invoke node) {
        try {
            values.put(node.getReturnValue(), call(thread, node.getTarget(), node.getReceiver(), require(node.getArguments())));
            return node.getResumeTarget();
        } catch (Thrown t) {
            this.exception = t.getThrowable();
            return node.getCatchBlock();
        }
    }

    @Override
    public Object visit(VmThreadImpl thread, Invoke.ReturnValue node) {
        return require(node);
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, InvokeNoReturn node) {
        try {
            call(thread, node.getTarget(), node.getReceiver(), require(node.getArguments()));
            throw Assert.unreachableCode();
        } catch (Thrown t) {
            this.exception = t.getThrowable();
            return node.getCatchBlock();
        }
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, TailCall node) {
        output = call(thread, node.getTarget(), node.getReceiver(), require(node.getArguments()));
        return null;
    }

    // Other

    @Override
    public Void visit(VmThreadImpl thread, InitCheck node) {
        // Runtime initialization checks have no build-time effects.
        return null;
    }

    public Object visit(final VmThreadImpl param, final Comp node) {
        Value input = node.getInput();
        ValueType inputType = input.getType();
        if (isInt64(inputType)) {
            return box(unboxLong(input) ^ ~0L, node.getType());
        } else if (isInt32(inputType)) {
            return box(unboxInt(input) ^ ~0, node.getType());
        } else if (isBool(inputType)) {
            return Boolean.valueOf(!unboxBool(input));
        }
        throw badInputType();
    }

    @Override
    public Object visit(VmThreadImpl thread, CmpAndSwap node) {
        Value pointerValue = node.getPointer();
        if (pointerValue instanceof StaticFieldLiteral sf) {
            ((VmClassImpl)sf.getVariableElement().getEnclosingType().load().getVmClass()).initialize(thread);
        }
        Pointer pointer = unboxPointer(pointerValue);
        if (pointer == null) {
            throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid memory access"));
        }
        Memory memory = pointer.getRootMemoryIfExists();
        long offset = pointer.getRootByteOffset();
        ValueType type = pointerValue.getPointeeType();
        Value expect = node.getExpectedValue();
        Value update = node.getUpdateValue();
        ReadAccessMode readAccessMode = node.getReadAccessMode();
        WriteAccessMode writeAccessMode = node.getWriteAccessMode();
        boolean updated;
        StructType resultType = node.getType();
        Memory result = thread.getVM().allocate(resultType, 1);
        if (type instanceof ReferenceType) {
            VmObject expected = (VmObject) require(expect);
            VmObject resultVal = memory.compareAndExchangeRef(offset, expected, (VmObject) require(update), readAccessMode, writeAccessMode);
            updated = expected == resultVal;
            result.storeRef(resultType.getMember(0).getOffset(), resultVal, SinglePlain);
        } else if (type instanceof IntegerType) {
            int bits = ((IntegerType) type).getMinBits();
            if (bits == 8) {
                int expected = unboxInt(expect);
                int unboxedResult = memory.compareAndExchange8(offset, expected, unboxInt(update), readAccessMode, writeAccessMode);
                updated = expected == unboxedResult;
                result.store8(resultType.getMember(0).getOffset(), unboxedResult, SinglePlain);
            } else if (bits == 16) {
                int expected = unboxInt(expect);
                int unboxedResult = memory.compareAndExchange16(offset, expected, unboxInt(update), readAccessMode, writeAccessMode);
                updated = expected == unboxedResult;
                result.store16(resultType.getMember(0).getOffset(), unboxedResult, SinglePlain);
            } else if (bits == 32) {
                int expected = unboxInt(expect);
                int unboxedResult = memory.compareAndExchange32(offset, expected, unboxInt(update), readAccessMode, writeAccessMode);
                updated = expected == unboxedResult;
                result.store32(resultType.getMember(0).getOffset(), unboxedResult, SinglePlain);
            } else {
                assert bits == 64;
                long expected = unboxLong(expect);
                long unboxedResult = memory.compareAndExchange64(offset, expected, unboxLong(update), readAccessMode, writeAccessMode);
                updated = expected == unboxedResult;
                result.store64(resultType.getMember(0).getOffset(), unboxedResult, SinglePlain);
            }
        } else if (type instanceof FloatType) {
            int bits = ((FloatType) type).getMinBits();
            if (bits == 32) {
                int expected = Float.floatToRawIntBits(unboxFloat(expect));
                int unboxedResult = memory.compareAndExchange32(offset, expected, Float.floatToRawIntBits(unboxInt(update)), readAccessMode, writeAccessMode);
                updated = expected == unboxedResult;
                result.store32(resultType.getMember(0).getOffset(), unboxedResult, SinglePlain);
            } else {
                assert bits == 64;
                long expected = Double.doubleToRawLongBits(unboxDouble(expect));
                long unboxedResult = memory.compareAndExchange64(offset, expected, Double.doubleToRawLongBits(unboxDouble(update)), readAccessMode, writeAccessMode);
                updated = expected == unboxedResult;
                result.store64(resultType.getMember(0).getOffset(), unboxedResult, SinglePlain);
            }
        } else if (type instanceof BooleanType) {
            int expected = unboxBool(expect) ? 1 : 0;
            int unboxedResult = memory.compareAndExchange8(offset, expected, unboxBool(update) ? 1 : 0, readAccessMode, writeAccessMode);
            updated = expected == unboxedResult;
            result.store8(resultType.getMember(0).getOffset(), unboxedResult, SinglePlain);
        } else if (type instanceof PointerType) {
            Pointer expected = unboxPointer(expect);
            Pointer resultVal = memory.compareAndExchangePointer(offset, expected, unboxPointer(update), readAccessMode, writeAccessMode);
            updated = expected == resultVal;
            result.storePointer(resultType.getMember(0).getOffset(), resultVal, SinglePlain);
        } else {
            throw unsupportedType();
        }
        result.store8(resultType.getMember(1).getOffset(), updated ? 1 : 0, SinglePlain);
        return result;
    }

    @Override
    public Object visit(VmThreadImpl thread, ReadModifyWrite node) {
        Value pointerValue = node.getPointer();
        if (pointerValue instanceof StaticFieldLiteral sf) {
            ((VmClassImpl)sf.getVariableElement().getEnclosingType().load().getVmClass()).initialize(thread);
        }
        Pointer pointer = unboxPointer(pointerValue);
        if (pointer == null) {
            throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid memory access"));
        }
        Memory memory = pointer.getRootMemoryIfExists();
        long offset = pointer.getRootByteOffset();
        ValueType type = pointerValue.getPointeeType();
        Value update = node.getUpdateValue();
        ReadAccessMode readAccessMode = node.getReadAccessMode();
        WriteAccessMode writeAccessMode = node.getWriteAccessMode();
        switch (node.getOp()) {
            case SET -> {
                if (isBool(type)) {
                    return Boolean.valueOf(memory.getAndSet8(offset, unboxBool(update) ? 1 : 0, readAccessMode, writeAccessMode) != 0);
                } else if (isInt8(type)) {
                    return Byte.valueOf((byte) memory.getAndSet8(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt16(type)) {
                    return Short.valueOf((short) memory.getAndSet16(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt32(type)) {
                    return Integer.valueOf(memory.getAndSet32(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt64(type)) {
                    return Long.valueOf(memory.getAndSet64(offset, unboxLong(update), readAccessMode, writeAccessMode));
                } else if (isFloat32(type)) {
                    return Float.valueOf(Float.intBitsToFloat(memory.getAndSet32(offset, Float.floatToRawIntBits(unboxFloat(update)), readAccessMode, writeAccessMode)));
                } else if (isFloat64(type)) {
                    return Double.valueOf(Double.longBitsToDouble(memory.getAndSet64(offset, Double.doubleToRawLongBits(unboxDouble(update)), readAccessMode, writeAccessMode)));
                } else if (isRef(type)) {
                    return memory.getAndSetRef(offset, (VmObject) require(update), readAccessMode, writeAccessMode);
                } else if (type instanceof PointerType) {
                    return memory.getAndSetPointer(offset, unboxPointer(update), readAccessMode, writeAccessMode);
                } else {
                    throw unsupportedType();
                }
            }
            case ADD -> {
                if (isInt8(type)) {
                    return Byte.valueOf((byte) memory.getAndAdd8(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt16(type)) {
                    return Short.valueOf((short) memory.getAndAdd16(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt32(type)) {
                    return Integer.valueOf(memory.getAndAdd32(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt64(type)) {
                    return Long.valueOf(memory.getAndAdd64(offset, unboxLong(update), readAccessMode, writeAccessMode));
                } else {
                    throw unsupportedType();
                }
            }
            case SUB -> {
                if (isInt8(type)) {
                    return Byte.valueOf((byte) memory.getAndAdd8(offset, -unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt16(type)) {
                    return Short.valueOf((short) memory.getAndAdd16(offset, -unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt32(type)) {
                    return Integer.valueOf(memory.getAndAdd32(offset, -unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt64(type)) {
                    return Long.valueOf(memory.getAndAdd64(offset, -unboxLong(update), readAccessMode, writeAccessMode));
                } else {
                    throw unsupportedType();
                }
            }
            case BITWISE_AND -> {
                if (isInt8(type)) {
                    return Byte.valueOf((byte) memory.getAndBitwiseAnd8(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt16(type)) {
                    return Short.valueOf((short) memory.getAndBitwiseAnd16(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt32(type)) {
                    return Integer.valueOf(memory.getAndBitwiseAnd32(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt64(type)) {
                    return Long.valueOf(memory.getAndBitwiseAnd64(offset, unboxLong(update), readAccessMode, writeAccessMode));
                } else if (isBool(type)) {
                    return Boolean.valueOf((memory.getAndBitwiseAnd8(offset, unboxBool(update) ? 1 : 0, readAccessMode, writeAccessMode) & 1) != 0);
                } else {
                    throw unsupportedType();
                }
            }
            case BITWISE_NAND -> {
                if (isInt8(type)) {
                    return Byte.valueOf((byte) memory.getAndBitwiseNand8(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt16(type)) {
                    return Short.valueOf((short) memory.getAndBitwiseNand16(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt32(type)) {
                    return Integer.valueOf(memory.getAndBitwiseNand32(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt64(type)) {
                    return Long.valueOf(memory.getAndBitwiseNand64(offset, unboxLong(update), readAccessMode, writeAccessMode));
                } else if (isBool(type)) {
                    return Boolean.valueOf((memory.getAndBitwiseNand8(offset, unboxBool(update) ? 1 : 0, readAccessMode, writeAccessMode) & 1) != 0);
                } else {
                    throw unsupportedType();
                }
            }
            case BITWISE_OR -> {
                if (isInt8(type)) {
                    return Byte.valueOf((byte) memory.getAndBitwiseOr8(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt16(type)) {
                    return Short.valueOf((short) memory.getAndBitwiseOr16(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt32(type)) {
                    return Integer.valueOf(memory.getAndBitwiseOr32(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt64(type)) {
                    return Long.valueOf(memory.getAndBitwiseOr64(offset, unboxLong(update), readAccessMode, writeAccessMode));
                } else if (isBool(type)) {
                    return Boolean.valueOf((memory.getAndBitwiseOr8(offset, unboxBool(update) ? 1 : 0, readAccessMode, writeAccessMode) & 1) != 0);
                } else {
                    throw unsupportedType();
                }
            }
            case BITWISE_XOR -> {
                if (isInt8(type)) {
                    return Byte.valueOf((byte) memory.getAndBitwiseXor8(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt16(type)) {
                    return Short.valueOf((short) memory.getAndBitwiseXor16(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt32(type)) {
                    return Integer.valueOf(memory.getAndBitwiseXor32(offset, unboxInt(update), readAccessMode, writeAccessMode));
                } else if (isInt64(type)) {
                    return Long.valueOf(memory.getAndBitwiseXor64(offset, unboxLong(update), readAccessMode, writeAccessMode));
                } else if (isBool(type)) {
                    return Boolean.valueOf((memory.getAndBitwiseXor8(offset, unboxBool(update) ? 1 : 0, readAccessMode, writeAccessMode) & 1) != 0);
                } else {
                    throw unsupportedType();
                }
            }
            case MIN -> {
                if (isSigned(type)) {
                    if (isInt8(type)) {
                        return Byte.valueOf((byte) memory.getAndSetMinSigned8(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt16(type)) {
                        return Short.valueOf((short) memory.getAndSetMinSigned16(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt32(type)) {
                        return Integer.valueOf(memory.getAndSetMinSigned32(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt64(type)) {
                        return Long.valueOf(memory.getAndSetMinSigned64(offset, unboxLong(update), readAccessMode, writeAccessMode));
                    } else {
                        throw unsupportedType();
                    }
                } else {
                    if (isInt8(type)) {
                        return Byte.valueOf((byte) memory.getAndSetMinUnsigned8(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt16(type)) {
                        return Short.valueOf((short) memory.getAndSetMinUnsigned16(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt32(type)) {
                        return Integer.valueOf(memory.getAndSetMinUnsigned32(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt64(type)) {
                        return Long.valueOf(memory.getAndSetMinUnsigned64(offset, unboxLong(update), readAccessMode, writeAccessMode));
                    } else {
                        throw unsupportedType();
                    }
                }
            }
            case MAX -> {
                if (isSigned(type)) {
                    if (isInt8(type)) {
                        return Byte.valueOf((byte) memory.getAndSetMaxSigned8(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt16(type)) {
                        return Short.valueOf((short) memory.getAndSetMaxSigned16(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt32(type)) {
                        return Integer.valueOf(memory.getAndSetMaxSigned32(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt64(type)) {
                        return Long.valueOf(memory.getAndSetMaxSigned64(offset, unboxLong(update), readAccessMode, writeAccessMode));
                    } else {
                        throw unsupportedType();
                    }
                } else {
                    if (isInt8(type)) {
                        return Byte.valueOf((byte) memory.getAndSetMaxUnsigned8(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt16(type)) {
                        return Short.valueOf((short) memory.getAndSetMaxUnsigned16(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt32(type)) {
                        return Integer.valueOf(memory.getAndSetMaxUnsigned32(offset, unboxInt(update), readAccessMode, writeAccessMode));
                    } else if (isInt64(type)) {
                        return Long.valueOf(memory.getAndSetMaxUnsigned64(offset, unboxLong(update), readAccessMode, writeAccessMode));
                    } else {
                        throw unsupportedType();
                    }
                }
            }
            default -> throw Assert.impossibleSwitchCase(node.getOp());
        }
    }

    @Override
    public Object visit(VmThreadImpl thread, Load node) {
        Value pointerValue = node.getPointer();
        if (pointerValue instanceof StaticFieldLiteral sf) {
            ((VmClassImpl)sf.getVariableElement().getEnclosingType().load().getVmClass()).initialize(thread);
        }
        Pointer pointer = unboxPointer(pointerValue);
        if (pointer == null) {
            throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid memory access"));
        }
        Memory memory = pointer.getRootMemoryIfExists();
        if (memory == null) {
            throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid memory access"));
        }
        long offset = pointer.getRootByteOffset();
        ValueType type = node.getType();
        ReadAccessMode mode = node.getAccessMode();
        if (isInt8(type)) {
            return Byte.valueOf((byte) memory.load8(offset, mode));
        } else if (isInt16(type)) {
            return Short.valueOf((short) memory.load16(offset, mode));
        } else if (isInt32(type)) {
            return Integer.valueOf(memory.load32(offset, mode));
        } else if (isInt64(type)) {
            // todo: memory.getTypeAt(offset)
            try {
                pointer = memory.loadPointer(offset, mode);
            } catch (InvalidMemoryAccessException ignored) {
                return Long.valueOf(memory.load64(offset, mode));
            }
            if (pointer == null) {
                return Long.valueOf(memory.load64(offset, mode));
            } else if (pointer instanceof IntegerAsPointer iap) {
                return Long.valueOf(iap.getValue());
            } else {
                return pointer;
            }
        } else if (isFloat32(type)) {
            return Float.valueOf(Float.intBitsToFloat(memory.load32(offset, mode)));
        } else if (isFloat64(type)) {
            return Double.valueOf(Double.longBitsToDouble(memory.load64(offset, mode)));
        } else if (isBool(type)) {
            return Boolean.valueOf(memory.load8(offset, mode) != 0);
        } else if (isRef(type)) {
            return memory.loadRef(offset, mode);
        } else if (isTypeId(type)) {
            return memory.loadType(offset, mode);
        } else if (type instanceof PointerType) {
            return memory.loadPointer(offset, mode);
        } else {
            throw unsupportedType();
        }
    }

    @Override
    public Object visit(VmThreadImpl thread, MultiNewArray node) {
        List<Value> dimList = node.getDimensions();
        int[] dimensions = new int[dimList.size()];
        for (int i = 0; i < dimensions.length; i++) {
            dimensions[i] = unboxInt(dimList.get(i));
        }
        return multiNewArray(thread, node.getArrayType(), 0, dimensions);
    }

    private VmArrayImpl multiNewArray(VmThreadImpl thread, ArrayObjectType type, int dimOffs, int[] dimensions) {
        int size = dimensions[dimOffs];
        VmArrayImpl outer = newArray(thread, type, size);
        if (dimOffs < dimensions.length - 1) {
            // nested arrays to fill
            VmObject[] array = (VmObject[]) outer.getArray();
            for (int i = 0; i < size; i++) {
                array[i] = multiNewArray(thread, (ArrayObjectType) type.getElementType(), dimOffs + 1, dimensions);
            }
        }
        return outer;
    }

    @Override
    public Object visit(VmThreadImpl thread, New node) {
        DefinedTypeDefinition enclosingType = node.element().getEnclosingType();
        VmClassLoaderImpl cl = thread.vm.getClassLoaderForContext(enclosingType.getContext());
        VmClassImpl clazz = (VmClassImpl) node.getClassObjectType().getDefinition().load().getVmClass();
        clazz.initialize(thread);
        return thread.vm.manuallyInitialize(clazz.newInstance());
    }

    @Override
    public Object visit(VmThreadImpl thread, NewArray node) {
        return newArray(thread, node.getArrayType(), unboxInt(node.getSize()));
    }

    @Override
    public Object visit(VmThreadImpl thread, NewReferenceArray node) {
        return newArray(thread, node.getArrayType(), unboxInt(node.getSize()));
    }

    private VmArrayImpl newArray(VmThreadImpl thread, ArrayObjectType arrayType, int size) {
        VmClassImpl clazz = requireClass(arrayType);
        if (clazz instanceof VmArrayClassImpl) {
            return thread.vm.manuallyInitialize(((VmArrayClassImpl) clazz).newInstance(size));
        } else {
            throw unsupportedType();
        }
    }

    @Override
    public Object visit(VmThreadImpl param, OffsetOfField node) {
        FieldElement fieldElement = node.getFieldElement();
        CompilationContext ctxt = element.getEnclosingType().getContext().getCompilationContext();
        Layout layout = Layout.get(ctxt);
        LayoutInfo layoutInfo;
        if (fieldElement.isStatic()) {
            layoutInfo = layout.getStaticLayoutInfo(fieldElement.getEnclosingType());
        } else {
            layoutInfo = layout.getInstanceLayoutInfo(fieldElement.getEnclosingType());
        }
        return Long.valueOf(layoutInfo == null ? 0 : layoutInfo.getMember(fieldElement).getOffset());
    }

    @Override
    public Object visit(VmThreadImpl vmThread, OffsetPointer node) {
        Pointer basePtr = unboxPointer(node.getBasePointer());
        if (basePtr == null) {
            // it's possible that it's a null pointer being added to another pointer, as the JDK sometimes does
            Object offset = require(node.getOffset());
            if (offset instanceof Pointer pv) {
                return pv;
            } else {
                // invalid pointer
                return null;
            }
        }
        long offset = unboxLong(node.getOffset());
        if (offset == 0) {
            return basePtr;
        }
        long pointeeSize = node.getBasePointer().getPointeeType().getSize();
        return basePtr.offsetInBytes(offset * pointeeSize, true);
    }

    @Override
    public Object visit(VmThreadImpl thread, StackAllocation node) {
        return new MemoryPointer(node.getType(), thread.vm.allocate(node.getType().getPointeeType(), unboxLong(node.getCount())));
    }

    @Override
    public Object visit(VmThreadImpl thread, InterfaceMethodLookup node) {
        InstanceMethodElement methodElement = node.getMethod();
        VmObject refVal = (VmObject) require(node.getReference());
        if (refVal == null) {
            throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid memory access"));
        }
        PhysicalObjectType ot = refVal.getObjectType();
        LoadedTypeDefinition baseDef = ot.getDefinition().load();
        InstanceMethodElement result = (InstanceMethodElement) baseDef.resolveMethodElementVirtual(baseDef.getContext(), methodElement.getName(), methodElement.getDescriptor());
        if (result == null) {
            throw new Thrown(thread.vm.noSuchMethodErrorClass.newInstance());
        }
        return InstanceMethodPointer.of(result);
    }

    @Override
    public Object visit(VmThreadImpl thread, VirtualMethodLookup node) {
        InstanceMethodElement methodElement = node.getMethod();
        VmObject refVal = (VmObject) require(node.getReference());
        if (refVal == null) {
            throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid memory access"));
        }
        PhysicalObjectType ot = refVal.getObjectType();
        LoadedTypeDefinition baseDef = ot.getDefinition().load();
        InstanceMethodElement result = (InstanceMethodElement) baseDef.resolveMethodElementVirtual(baseDef.getContext(), methodElement.getName(), methodElement.getDescriptor());
        if (result == null) {
            throw new Thrown(thread.vm.noSuchMethodErrorClass.newInstance());
        }
        return InstanceMethodPointer.of(result);
    }

    ///////////
    // Actions
    ///////////

    @Override
    public Void visitUnknown(VmThreadImpl thread, Action node) {
        throw illegalInstruction();
    }

    @Override
    public Void visit(VmThreadImpl thread, BlockEntry node) {
        return null;
    }

    @Override
    public Void visit(VmThreadImpl thread, DebugAddressDeclaration node) {
        LocalVariableElement lve = node.getVariable();
        Value address = node.getAddress();
        // replace it
        Pointer replacement = memoryPointer.offsetInBytes(lve.getOffset(), false);
        values.put(address, replacement);
        return null;
    }

    @Override
    public Void visit(VmThreadImpl thread, DebugValueDeclaration node) {
        LocalVariableElement lve = node.getVariable();
        store(thread, memoryPointer.getMemory(), lve.getOffset(), lve.getType(), node.getValue(), SingleUnshared);
        return null;
    }

    @Override
    public Void visit(VmThreadImpl thread, Fence node) {
        GlobalAccessMode gam = node.getAccessMode();
        if (GlobalPlain.includes(gam)) {
            // do nothing
        } else if (GlobalLoadLoad.includes(gam)) {
            VarHandle.loadLoadFence();
        } else if (GlobalStoreStore.includes(gam)) {
            VarHandle.storeStoreFence();
        } else if (GlobalAcquire.includes(gam)) {
            VarHandle.acquireFence();
        } else if (GlobalRelease.includes(gam)) {
            VarHandle.releaseFence();
        } else {
            VarHandle.fullFence();
        }
        return null;
    }

    @Override
    public Void visit(VmThreadImpl vmThread, InitializeClass node) {
        VmClassImpl cls = (VmClassImpl)require(node.getInitializeClassValue());
        cls.initialize(vmThread);
        return null;
    }

    @Override
    public Void visit(VmThreadImpl thread, MonitorEnter node) {
        VmObjectImpl obj = (VmObjectImpl) require(node.getInstance());
        Lock lock = obj.getLock();
        Set<Lock> heldLocks = this.heldLocks;
        if (heldLocks == null) {
            heldLocks = this.heldLocks = new HashSet<>();
        }
        lock.lock();
        heldLocks.add(lock);
        return null;
    }

    @Override
    public Void visit(VmThreadImpl thread, MonitorExit node) {
        VmObjectImpl obj = (VmObjectImpl) require(node.getInstance());
        Lock lock = obj.getLock();
        try {
            lock.unlock();
        } catch (IllegalMonitorStateException e) {
            throw new Thrown(/* todo */ null);
        }
        Set<Lock> heldLocks = this.heldLocks;
        if (heldLocks != null) {
            heldLocks.remove(lock);
        }
        return null;
    }

    @Override
    public Void visit(VmThreadImpl vmThread, Reachable node) {
        // no operation needed because we aggressively keep values live
        return null;
    }

    @Override
    public Void visit(VmThreadImpl vmThread, EnterSafePoint node) {
        // no operation
        return null;
    }

    @Override
    public Void visit(VmThreadImpl vmThread, ExitSafePoint node) {
        // no operation
        return null;
    }

    @Override
    public Void visit(VmThreadImpl vmThread, PollSafePoint node) {
        // no operation
        return null;
    }

    @Override
    public Void visit(VmThreadImpl thread, Store node) {
        Value pointer = node.getPointer();
        Value value = node.getValue();
        WriteAccessMode mode = node.getAccessMode();
        if (pointer instanceof StaticFieldLiteral sf) {
            ((VmClassImpl)sf.getVariableElement().getEnclosingType().load().getVmClass()).initialize(thread);
        }
        store(thread, pointer, value, mode);
        return null;
    }

    /**
     * Store a value into the interpreter memory.
     *
     * @param thread the thread (must not be {@code null})
     * @param pointer the store target (must not be {@code null})
     * @param value the value to store
     * @param mode the atomicity mode (must not be {@code null})
     */
    void store(VmThreadImpl thread, final Value pointer, final Value value, final WriteAccessMode mode) {
        Pointer ptr = unboxPointer(pointer);
        if (ptr == null) {
            throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid memory access"));
        }
        Memory memory = ptr.getRootMemoryIfExists();
        long offset = ptr.getRootByteOffset();
        ValueType type = pointer.getPointeeType();
        store(thread, memory, offset, type, value, mode);
    }

    void store(VmThreadImpl thread, final Memory memory, final long offset, final ValueType type, final Value value, final WriteAccessMode mode) {
        if (memory == null) {
            throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid memory access"));
        }
        try {
            if (isInt8(type)) {
                memory.store8(offset, unboxInt(value), mode);
            } else if (isInt16(type)) {
                memory.store16(offset, unboxInt(value), mode);
            } else if (isInt32(type)) {
                memory.store32(offset, unboxInt(value), mode);
            } else if (isInt64(type)) {
                Object rawValue = require(value);
                if (rawValue instanceof Pointer p) {
                    memory.storePointer(offset, p, mode);
                } else if (rawValue == null) {
                    // equivalent to store64(offset, 0, mode);
                    memory.storePointer(offset, null, mode);
                } else {
                    memory.store64(offset, ((Long) rawValue).longValue(), mode);
                }
            } else if (isFloat32(type)) {
                memory.store32(offset, Float.floatToRawIntBits(unboxFloat(value)), mode);
            } else if (isFloat64(type)) {
                memory.store64(offset, Double.doubleToRawLongBits(unboxDouble(value)), mode);
            } else if (isBool(type)) {
                memory.store8(offset, unboxBool(value) ? 1 : 0, mode);
            } else if (isRef(type)) {
                memory.storeRef(offset, (VmObject) require(value), mode);
            } else if (isTypeId(type)) {
                memory.storeType(offset, (ValueType) require(value), mode);
            } else if (type instanceof PointerType) {
                memory.storePointer(offset, unboxPointer(value), mode);
            } else if (type instanceof StructType st) {
                Memory source = (Memory) require(value);
                if (source == null) {
                    throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid memory access"));
                }
                source.typedCopyTo(0, memory, offset, st);
            } else if (type instanceof ArrayType at) {
                Memory source = (Memory) require(value);
                if (source == null) {
                    throw new Thrown(thread.vm.nullPointerException.newInstance("Invalid memory access"));
                }
                source.typedCopyTo(0, memory, offset, at);
            } else {
                throw unsupportedType();
            }
        } catch (InvalidMemoryAccessException e) {
            String message = e.getMessage();
            throw new Thrown(thread.vm.nullPointerException.newInstance(message == null ? "Invalid memory access" : message));
        }
    }

    ///////////////
    // Terminators
    ///////////////

    @Override
    public BasicBlock visitUnknown(VmThreadImpl thread, Terminator node) {
        throw illegalInstruction();
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, Goto node) {
        return node.getResumeTarget();
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, If node) {
        return unboxBool(node.getCondition()) ? node.getTrueBranch() : node.getFalseBranch();
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, Switch node) {
        int sv = unboxInt(node.getSwitchValue());
        // simple binary search for the value
        int low = 0;
        int high = node.getNumberOfValues() - 1;
        while (low <= high) {
            int idx = (low + high) >>> 1;
            int val = node.getValueForIndex(idx);
            if (val < sv) {
                low = idx + 1;
            } else if (val > sv) {
                high = idx - 1;
            } else {
                return node.getTargetForIndex(idx);
            }
        }
        return node.getDefaultTarget();
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, Ret node) {
        return (BasicBlock) require(node.getReturnAddressValue());
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, Throw node) {
        Object raw = require(node.getThrownValue());
        if (raw instanceof VmThrowable throwable) {
            throw new Thrown(throwable);
        }
        // breakpoint
        throw new IllegalStateException();
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, Return node) {
        if (!(node.getReturnValue().getType() instanceof VoidType)) {
            output = require(node.getReturnValue());
        }
        return null;
    }

    @Override
    public BasicBlock visit(VmThreadImpl thread, Unreachable node) {
        throw Assert.unreachableCode();
    }

    private static IllegalArgumentException invalidHandleTypeForOp() {
        return new IllegalArgumentException("Invalid handle type for operation");
    }

    /////////////
    // utilities
    /////////////

    private static boolean isInteger(ValueType type) {
        return type instanceof IntegerType;
    }

    private static boolean isSigned(ValueType type) {
        return type instanceof SignedIntegerType;
    }

    private static boolean isUnsigned(ValueType type) {
        return type instanceof UnsignedIntegerType;
    }

    private static boolean isBool(ValueType type) {
        return type instanceof BooleanType;
    }

    private static boolean isRef(ValueType type) {
        return type instanceof ReferenceType;
    }

    private static boolean isPointer(ValueType type) {
        return type instanceof PointerType;
    }

    private static boolean isTypeId(ValueType type) {
        return type instanceof TypeIdType;
    }

    private static boolean isInt8(ValueType type) {
        return type instanceof IntegerType && ((IntegerType) type).getMinBits() == 8;
    }

    private static boolean isInt16(ValueType type) {
        return type instanceof IntegerType && ((IntegerType) type).getMinBits() == 16;
    }

    private static boolean isInt32(ValueType type) {
        return type instanceof IntegerType && ((IntegerType) type).getMinBits() == 32;
    }

    private static boolean isInt64(ValueType type) {
        return type instanceof IntegerType && ((IntegerType) type).getMinBits() == 64;
    }

    private static boolean isIntSameWidth(ValueType type1, ValueType type2) {
        return type1 instanceof IntegerType && type2 instanceof IntegerType && ((IntegerType) type1).getMinBits() == ((IntegerType) type2).getMinBits();
    }

    private static boolean isFloat32(ValueType type) {
        return type instanceof FloatType && ((FloatType) type).getMinBits() == 32;
    }

    private static boolean isFloat64(ValueType type) {
        return type instanceof FloatType && ((FloatType) type).getMinBits() == 64;
    }

    private Object box(final long longVal, final ValueType type) {
        if (isInt8(type)) {
            return Byte.valueOf((byte) longVal);
        } else if (isInt16(type)) {
            return Short.valueOf((short) longVal);
        } else if (isInt32(type)) {
            return Integer.valueOf((int) longVal);
        } else if (isInt64(type)) {
            return Long.valueOf(longVal);
        } else if (isBool(type)) {
            return Boolean.valueOf(longVal != 0);
        } else if (type instanceof PointerType pt) {
            return new IntegerAsPointer(pt, longVal);
        }
        throw unsupportedType();
    }

    private Object box(final int intVal, final ValueType type) {
        if (isInt8(type)) {
            return Byte.valueOf((byte) intVal);
        } else if (isInt16(type)) {
            return Short.valueOf((short) intVal);
        } else if (isInt32(type)) {
            return Integer.valueOf(intVal);
        } else if (isInt64(type)) {
            return Long.valueOf(intVal);
        } else if (isBool(type)) {
            return Boolean.valueOf(intVal != 0);
        }
        throw unsupportedType();
    }

    private Object box(final float floatVal, final ValueType type) {
        if (isFloat32(type)) {
            return Float.valueOf(floatVal);
        } else if (isFloat64(type)) {
            return Double.valueOf(floatVal);
        }
        throw unsupportedType();
    }

    private Object box(final double doubleVal, final ValueType type) {
        if (isFloat32(type)) {
            return Float.valueOf((float) doubleVal);
        } else if (isFloat64(type)) {
            return Double.valueOf(doubleVal);
        }
        throw unsupportedType();
    }

    private boolean unboxBool(final Value rightInput) {
        Object required = require(rightInput);
        if (required instanceof Number num) {
            return num.byteValue() != 0;
        } else if (required instanceof Boolean boo) {
            return boo.booleanValue();
        } else if (required instanceof Character ch) {
            return (byte)ch.charValue() != 0;
        } else {
            throw unsupportedType();
        }
    }

    private int unboxInt(final Value input) {
        Object required = require(input);
        if (required instanceof Boolean boo) {
            return boo.booleanValue() ? 1 : 0;
        } else if (required instanceof Number num) {
            if (input.getType() instanceof UnsignedIntegerType uit) {
                return switch (uit.getMinBits()) {
                    case 8 -> num.byteValue() & 0xff;
                    case 16 -> num.shortValue() & 0xffff;
                    default -> num.intValue();
                };
            } else {
                // always sign-extends
                return num.intValue();
            }
        } else if (required instanceof Character ch) {
            return ch.charValue();
        } else {
            throw unsupportedType();
        }
    }

    private Pointer unboxPointer(final Value rightInput) {
        Object v = require(rightInput);
        if (v instanceof Number n && n.longValue() == 0) {
            return null;
        }
        return (Pointer) v;
    }

    private long unboxLong(final Value rightInput) {
        Object raw = require(rightInput);
        if (raw instanceof Number num) {
            return num.longValue();
        } else if (raw instanceof IntegerAsPointer iap) {
            return iap.getValue();
        } else if (raw instanceof Boolean boo) {
            return boo.booleanValue() ? 1 : 0;
        } else {
             throw new ClassCastException();
        }
    }

    private float unboxFloat(final Value rightInput) {
        Number obj = (Number) require(rightInput);
        return obj.floatValue();
    }

    private double unboxDouble(final Value rightInput) {
        Number obj = (Number) require(rightInput);
        return obj.doubleValue();
    }

    private List<Object> require(List<? extends Value> values) {
        List<Object> output = new ArrayList<>(values.size());
        for (Value value : values) {
            output.add(require(value));
        }
        return output;
    }

    Object require(Value value) {
        Object v = getOrMissing(value);
        if (v == MISSING) {
            throw missingRequired();
        }
        return v;
    }

    Object getOrMissing(Value value) {
        if (value instanceof Literal) {
            return value.accept(this, null);
        }
        return values.getOrDefault(value, MISSING);
    }

    private static IllegalStateException missingRequired() {
        return new IllegalStateException("Missing required value");
    }

    ValueType unboxType(Value value) {
        return (ValueType) require(value);
    }

    VmClassImpl requireClass(ObjectType objType) {
        if (objType instanceof PrimitiveArrayObjectType) {
            WordType elementType = ((PrimitiveArrayObjectType) objType).getElementType();
            if (elementType instanceof SignedIntegerType) {
                if (elementType.getMinBits() == 8) {
                    return VmImpl.require().byteArrayClass;
                } else if (elementType.getMinBits() == 16) {
                    return VmImpl.require().shortArrayClass;
                } else if (elementType.getMinBits() == 32) {
                    return VmImpl.require().intArrayClass;
                } else if (elementType.getMinBits() == 64) {
                    return VmImpl.require().longArrayClass;
                }
            } else if (elementType instanceof UnsignedIntegerType) {
                if (elementType.getMinBits() == 16) {
                    return VmImpl.require().charArrayClass;
                }
            } else if (elementType instanceof BooleanType) {
                return VmImpl.require().booleanArrayClass;
            } else if (elementType instanceof FloatType) {
                if (elementType.getMinBits() == 32) {
                    return VmImpl.require().floatArrayClass;
                } else if (elementType.getMinBits() == 64) {
                    return VmImpl.require().doubleArrayClass;
                }
            }
            throw Assert.unsupported();
        } else if (objType instanceof ReferenceArrayObjectType) {
            ObjectType elementType = ((ReferenceArrayObjectType) objType).getElementObjectType();
            return requireClass(elementType).getArrayClass();
        } else {
            LoadedTypeDefinition loaded = objType.getDefinition().load();
            VmImpl vm = VmImpl.require();
            VmClassLoaderImpl classLoader = vm.getClassLoaderForContext(loaded.getContext());
            return classLoader.loadClassRunTime(loaded.getInternalName());
        }
    }

    private static UnsupportedOperationException unsupportedType() {
        return new UnsupportedOperationException("Unsupported type");
    }

    private static IllegalStateException illegalInstruction() {
        return new IllegalStateException("Illegal instruction");
    }

    void releaseLocks() {
        Set<Lock> heldLocks = this.heldLocks;
        if (heldLocks != null) for (Lock heldLock : heldLocks) try {
            heldLock.unlock();
        } catch (RuntimeException ignored) {}
    }
}
