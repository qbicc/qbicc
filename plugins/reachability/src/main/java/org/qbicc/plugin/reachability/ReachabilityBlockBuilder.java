package org.qbicc.plugin.reachability;

import java.util.HashSet;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Action;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.ClassOf;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.FunctionElementHandle;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.MultiNewArray;
import org.qbicc.graph.NewArray;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.MethodElement;

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
        private final ExecutableElement originalElement;
        private final ReachabilityAnalysis analysis;
        final HashSet<Node> visited = new HashSet<>();

        ReachabilityContext(CompilationContext ctxt, ExecutableElement originalElement) {
            this.ctxt = ctxt;
            this.originalElement = originalElement;
            this.analysis = ReachabilityInfo.get(ctxt).getAnalysis();
        }
    }

    static final class ReachabilityVisitor implements NodeVisitor<ReachabilityContext, Void, Void, Void, Void> {
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
        public Void visit(ReachabilityContext param, ConstructorElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                ConstructorElement target = node.getExecutable();
                LoadedTypeDefinition ltd = target.getEnclosingType().load();
                param.analysis.processReachableConstructorInvoke(ltd, target, param.originalElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, FunctionElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                FunctionElement target = node.getExecutable();
                param.analysis.processReachableStaticInvoke(target, param.originalElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, ExactMethodElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                param.analysis.processReachableInstanceMethodInvoke(node.getExecutable(), param.originalElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, VirtualMethodElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                param.analysis.processReachableInstanceMethodInvoke(node.getExecutable(), param.originalElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, InterfaceMethodElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                param.analysis.processReachableInstanceMethodInvoke(node.getExecutable(), param.originalElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, StaticMethodElementHandle node) {
            if (visitUnknown(param, (Node)node)) {
                MethodElement target = node.getExecutable();
                param.analysis.processStaticElementInitialization(target.getEnclosingType().load(), target, param.originalElement);
                param.analysis.processReachableStaticInvoke(target, param.originalElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, NewArray node) {
            if (visitUnknown(param, (Node)node)) {
                if (node.getArrayType() instanceof ReferenceArrayObjectType at) {
                    // Force the array's leaf element type to be reachable (and thus assigned a typeId).
                    param.analysis.processArrayElementType(at.getLeafElementType());
                }
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
                FieldElement f = node.getVariableElement();
                param.analysis.processStaticElementInitialization(f.getEnclosingType().load(), f, param.originalElement);
            }
            return null;
        }

        @Override
        public Void visit(ReachabilityContext param, ClassOf node) {
            if (visitUnknown(param, (Node)node)) {
                MethodElement methodElement = param.ctxt.getVMHelperMethod("classof_from_typeid");
                param.ctxt.enqueue(methodElement);
                if (node.getInput() instanceof TypeLiteral tl && tl.getValue() instanceof ClassObjectType cot) {
                    param.analysis.processClassInitialization(cot.getDefinition().load());
                }
            }
            return null;
        }
    }
}
