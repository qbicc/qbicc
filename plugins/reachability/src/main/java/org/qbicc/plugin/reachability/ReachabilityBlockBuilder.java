package org.qbicc.plugin.reachability;

import java.util.HashSet;

import org.qbicc.context.CompilationContext;
import org.qbicc.facts.Facts;
import org.qbicc.graph.Action;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.Field;
import org.qbicc.graph.FunctionElementHandle;
import org.qbicc.graph.InitCheck;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.Load;
import org.qbicc.graph.MultiNewArray;
import org.qbicc.graph.New;
import org.qbicc.graph.NewReferenceArray;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Store;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.pointer.InstanceMethodPointer;
import org.qbicc.pointer.ReferenceAsPointer;
import org.qbicc.pointer.RootPointer;
import org.qbicc.pointer.StaticFieldPointer;
import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.definition.element.StaticMethodElement;

/**
 * A block builder stage which traverses the final set of reachable Nodes
 * for an ExecutableElement and invokes a ReachabilityAnalysis them.
 * As a result of the analysis, new ExecutableElements may become reachable and
 * this be enqueued for compilation.
 */
public class ReachabilityBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<Void, Void> {
    private final CompilationContext ctxt;

    public ReachabilityBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public void finish() {
        BasicBlock entryBlock = getFirstBlock();
        entryBlock.getTerminator().accept(new ReachabilityVisitor(), new ReachabilityContext(ctxt, getDelegate().getCurrentElement()));
        super.finish();
    }

    static final class ReachabilityContext {
        final CompilationContext ctxt;
        private final ExecutableElement currentElement;
        private final ReachabilityAnalysis analysis;
        final HashSet<Node> visited = new HashSet<>();

        ReachabilityContext(CompilationContext ctxt, ExecutableElement currentElement) {
            this.ctxt = ctxt;
            this.currentElement = currentElement;
            this.analysis = ReachabilityInfo.get(ctxt).getAnalysis();
        }
    }

    static final class ReachabilityVisitor implements NodeVisitor<ReachabilityContext, Void, Void, Void, Void>, RootPointer.Visitor<ReachabilityContext, Void> {
        @Override
        public Void visitUnknown(ReachabilityContext param, Action node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(ReachabilityContext param, Value node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(ReachabilityContext param, ValueHandle node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(ReachabilityContext param, Terminator node) {
            if (visitUnknown(param, (Node) node)) {
                // process reachable successors
                int cnt = node.getSuccessorCount();
                for (int i = 0; i < cnt; i ++) {
                    node.getSuccessor(i).getTerminator().accept(this, param);
                }
                for (Value v: node.getOutboundValues().values()) {
                    v.accept(this, param);
                }
            }
            return null;
        }

        boolean visitUnknown(ReachabilityContext param, Node node) {
            if (param.visited.add(node)) {
                if (node.hasValueHandleDependency()) {
                    node.getValueHandle().accept(this, param);
                }
                int cnt = node.getValueDependencyCount();
                for (int i = 0; i < cnt; i ++) {
                    node.getValueDependency(i).accept(this, param);
                }
                if (node instanceof OrderedNode on) {
                    Node dependency =on.getDependency();
                    if (dependency instanceof Action a) {
                        a.accept(this, param);
                    } else if (dependency instanceof Value v) {
                        v.accept(this, param);
                    } else if (dependency instanceof Terminator t) {
                        t.accept(this, param);
                    } else if (dependency instanceof ValueHandle vh) {
                        vh.accept(this, param);
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public Void visit(ReachabilityContext param, ObjectLiteral value) {
            param.analysis.processReachableObject(value.getValue(), param.currentElement);
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, PointerLiteral value) {
            RootPointer pointer = value.getPointer().getRootPointer();
            pointer.accept(this, param);
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, TypeLiteral value) {
            if (value.getValue() instanceof ClassObjectType cot) {
                param.analysis.processReachableType(cot.getDefinition().load(), param.currentElement);
            } else if (value.getValue() instanceof InterfaceObjectType iot) {
                param.analysis.processReachableType(iot.getDefinition().load(), param.currentElement);
            } else if (value.getValue() instanceof ReferenceArrayObjectType aot) {
                param.analysis.processArrayElementType(aot.getLeafElementType());
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext reachabilityContext, InstanceMethodPointer pointer) {
            InstanceMethodElement target = pointer.getInstanceMethod();
            reachabilityContext.analysis.processReachableExactInvocation(target, reachabilityContext.currentElement);
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, ReferenceAsPointer pointer) {
            param.analysis.processReachableObject(pointer.getReference(), param.currentElement);
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, StaticFieldPointer pointer) {
            StaticFieldElement f = pointer.getStaticField();
            param.analysis.processReachableStaticFieldAccess(f, param.currentElement);
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, StaticMethodPointer pointer) {
            StaticMethodElement target = pointer.getStaticMethod();
            param.analysis.processReachableExactInvocation(target, param.currentElement);
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, ConstructorElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                ConstructorElement target = node.getExecutable();
                param.analysis.processReachableExactInvocation(target, param.currentElement);
                // The lambda meta factory and other reflective machinery in the JDK use Unsafe.allocateInstance
                // instead of a "real" New to instantiate a class.  To compensate, reachability analysis assumes
                // that if a ConstructorElementHandle is invoked and the invocation isn't the super.<init> that starts
                // all non-Object constructors, then the declaring type of the constructor _must_ have been instantiated somehow.
                if (!target.getEnclosingType().load().isAbstract()) {
                    if (!(param.currentElement instanceof ConstructorElement) ||
                        (param.currentElement instanceof ConstructorElement &&
                            (param.currentElement.getEnclosingType().load().getSuperClass() == null ||
                            !param.currentElement.getEnclosingType().load().getSuperClass().equals(target.getEnclosingType())))) {
                        param.analysis.processInstantiatedClass(target.getEnclosingType().load(), false, param.currentElement);
                    }
                }
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, FunctionElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                FunctionElement target = node.getExecutable();
                param.analysis.processReachableExactInvocation(target, param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, ExactMethodElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                param.analysis.processReachableExactInvocation(node.getExecutable(), param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, VirtualMethodElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                param.analysis.processReachableDispatchedInvocation(node.getExecutable(), param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, InterfaceMethodElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                param.analysis.processReachableDispatchedInvocation(node.getExecutable(), param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, StaticMethodElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                MethodElement target = node.getExecutable();
                param.analysis.processReachableExactInvocation(target, param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, New node) {
            if (visitUnknown(param, (Node)node)) {
                LoadedTypeDefinition ltd = node.getClassObjectType().getDefinition().load();
                param.analysis.processInstantiatedClass(ltd, false, param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, NewReferenceArray node) {
            if (visitUnknown(param, (Node)node)) {
                // Force the array's leaf element type to be reachable (and thus assigned a typeId).
                param.analysis.processArrayElementType(node.getArrayType().getLeafElementType());
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, MultiNewArray node) {
            if (visitUnknown(param, (Node)node)) {
                if (node.getArrayType() instanceof ReferenceArrayObjectType at) {
                    // Force the array's leaf element type to be reachable (and thus assigned a typeId).
                    param.analysis.processArrayElementType(at.getLeafElementType());
                }
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, StaticField node) {
            if (visitUnknown(param, (Node)node)) {
                StaticFieldElement f = node.getVariableElement();
                param.analysis.processReachableStaticFieldAccess(f, param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, InitCheck node) {
            if (visitUnknown(param, (Node)node)) {
                param.analysis.processReachableRuntimeInitializer(node.getInitializerElement(), param.currentElement);
                MethodElement run = RuntimeMethodFinder.get(param.ctxt).getMethod("org/qbicc/runtime/main/Once", "run");
                param.analysis.processReachableDispatchedInvocation(run, param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, ClassOf node) {
            if (visitUnknown(param, (Node)node)) {
                MethodElement methodElement = RuntimeMethodFinder.get(param.ctxt).getMethod("getClassFromTypeId");
                param.analysis.processReachableExactInvocation(methodElement, param.currentElement);
            }
            return null;
        }

        // Field reachability

        @Override
        public Void visit(ReachabilityContext param, Load node) {
            if (visitUnknown(param, (Node)node)) {
                if (node.getValueHandle() instanceof Field f) {
                    Facts.get(param.ctxt).discover(f.getVariableElement(), FieldReachabilityFacts.IS_READ);
                }
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, Store node) {
            if (visitUnknown(param, (Node)node)) {
                if (node.getValueHandle() instanceof Field f) {
                    // todo: exclude writes of the field's original constant value (usually null/zero but might be something else)
                    Facts.get(param.ctxt).discover(f.getVariableElement(), FieldReachabilityFacts.IS_WRITTEN);
                }
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, CmpAndSwap node) {
            if (visitUnknown(param, (Node)node)) {
                if (node.getValueHandle() instanceof Field f) {
                    // todo: if `expect` is not equal to the field's original constant value, then only mark a read here
                    Facts.get(param.ctxt).discover(f.getVariableElement(), FieldReachabilityFacts.IS_READ, FieldReachabilityFacts.IS_WRITTEN);
                }
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, ReadModifyWrite node) {
            if (visitUnknown(param, (Node)node)) {
                if (node.getValueHandle() instanceof Field f) {
                    Facts.get(param.ctxt).discover(f.getVariableElement(), FieldReachabilityFacts.IS_READ, FieldReachabilityFacts.IS_WRITTEN);
                }
            }
            return null;
        }
    }
}
