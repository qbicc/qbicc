package org.qbicc.plugin.reachability;

import java.util.HashSet;

import org.qbicc.context.CompilationContext;
import org.qbicc.facts.Facts;
import org.qbicc.graph.Action;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.InitCheck;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.InterfaceMethodLookup;
import org.qbicc.graph.Load;
import org.qbicc.graph.MultiNewArray;
import org.qbicc.graph.New;
import org.qbicc.graph.NewReferenceArray;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Store;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Value;
import org.qbicc.graph.VirtualMethodLookup;
import org.qbicc.graph.literal.ConstructorLiteral;
import org.qbicc.graph.literal.FunctionLiteral;
import org.qbicc.graph.literal.InitializerLiteral;
import org.qbicc.graph.literal.InstanceMethodLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.graph.literal.StaticMethodLiteral;
import org.qbicc.graph.literal.TypeIdLiteral;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;

/**
 * A block builder stage which traverses the final set of reachable Nodes
 * for an ExecutableElement and invokes a ReachabilityAnalysis them.
 * As a result of the analysis, new ExecutableElements may become reachable and
 * this be enqueued for compilation.
 */
public class ReachabilityBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ReachabilityBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public void finish() {
        // finish first so that all blocks are populated
        super.finish();
        BasicBlock entryBlock = getFirstBlock();
        entryBlock.getTerminator().accept(new ReachabilityVisitor(), new ReachabilityContext(ctxt, getDelegate().element()));
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

    static final class ReachabilityVisitor implements NodeVisitor<ReachabilityContext, Void, Void, Void> {
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
        public Void visitUnknown(ReachabilityContext param, Terminator node) {
            if (visitUnknown(param, (Node) node)) {
                // process reachable successors
                int cnt = node.getSuccessorCount();
                for (int i = 0; i < cnt; i ++) {
                    node.getSuccessor(i).getTerminator().accept(this, param);
                }
                for (Slot slot : node.getOutboundArgumentNames()) {
                    node.getOutboundArgument(slot).accept(this, param);
                }
            }
            return null;
        }

        boolean visitUnknown(ReachabilityContext param, Node node) {
            if (param.visited.add(node)) {
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
        public Void visit(ReachabilityContext param, TypeIdLiteral value) {
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
        public Void visit(ReachabilityContext param, ConstructorLiteral node) {
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
        public Void visit(ReachabilityContext param, InitializerLiteral literal) {
            final InitializerElement init = literal.getExecutable();
            if (init.hasAllModifiersOf(ClassFile.I_ACC_RUN_TIME)) {
                param.analysis.processReachableRuntimeInitializer(init, param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, FunctionLiteral node) {
            if (visitUnknown(param, (Node)node)) {
                FunctionElement target = node.getExecutable();
                param.analysis.processReachableExactInvocation(target, param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, InstanceMethodLiteral node) {
            if (visitUnknown(param, (Node)node)) {
                param.analysis.processReachableExactInvocation(node.getExecutable(), param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, VirtualMethodLookup node) {
            if (visitUnknown(param, (Node)node)) {
                param.analysis.processReachableDispatchedInvocation(node.getMethod(), param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, InterfaceMethodLookup node) {
            if (visitUnknown(param, (Node)node)) {
                param.analysis.processReachableDispatchedInvocation(node.getMethod(), param.currentElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, StaticMethodLiteral node) {
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
        public Void visit(ReachabilityContext param, StaticFieldLiteral node) {
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
                FieldElement field;
                if (node.getPointer() instanceof StaticFieldLiteral sfl) {
                    field = sfl.getVariableElement();
                } else if (node.getPointer() instanceof InstanceFieldOf ifo) {
                    field = ifo.getVariableElement();
                } else {
                    return null;
                }
                Facts.get(param.ctxt).discover(field, FieldReachabilityFacts.IS_READ);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, Store node) {
            if (visitUnknown(param, (Node)node)) {
                FieldElement field;
                if (node.getPointer() instanceof StaticFieldLiteral sfl) {
                    field = sfl.getVariableElement();
                } else if (node.getPointer() instanceof InstanceFieldOf ifo) {
                    field = ifo.getVariableElement();
                } else {
                    return null;
                }
                // todo: exclude writes of the field's original constant value (usually null/zero but might be something else)
                Facts.get(param.ctxt).discover(field, FieldReachabilityFacts.IS_WRITTEN);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, CmpAndSwap node) {
            if (visitUnknown(param, (Node)node)) {
                FieldElement field;
                if (node.getPointer() instanceof StaticFieldLiteral sfl) {
                    field = sfl.getVariableElement();
                } else if (node.getPointer() instanceof InstanceFieldOf ifo) {
                    field = ifo.getVariableElement();
                } else {
                    return null;
                }
                // todo: if `expect` is not equal to the field's original constant value, then only mark a read here
                Facts.get(param.ctxt).discover(field, FieldReachabilityFacts.IS_READ, FieldReachabilityFacts.IS_WRITTEN);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, ReadModifyWrite node) {
            if (visitUnknown(param, (Node)node)) {
                FieldElement field;
                if (node.getPointer() instanceof StaticFieldLiteral sfl) {
                    field = sfl.getVariableElement();
                } else if (node.getPointer() instanceof InstanceFieldOf ifo) {
                    field = ifo.getVariableElement();
                } else {
                    return null;
                }
                Facts.get(param.ctxt).discover(field, FieldReachabilityFacts.IS_READ, FieldReachabilityFacts.IS_WRITTEN);
            }
            return null;
        }
    }
}
