package cc.quarkus.qcc.plugin.native_;

import static cc.quarkus.qcc.context.CompilationContext.*;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.VoidType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.AnnotationValue;
import cc.quarkus.qcc.type.annotation.StringAnnotationValue;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodResolver;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;

/**
 * A delegating type builder which handles calls to and from {@code @extern} and {@code @export} methods.
 */
public class ExternExportTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition.Builder delegate;

    public ExternExportTypeBuilder(final ClassContext classCtxt, final DefinedTypeDefinition.Builder delegate) {
        this.classCtxt = classCtxt;
        this.ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    public void addMethod(final MethodResolver resolver, final int index) {
        delegate.addMethod(new MethodResolver() {
            public MethodElement resolveMethod(final int index, final DefinedTypeDefinition enclosing) {
                NativeInfo nativeInfo = NativeInfo.get(ctxt);
                MethodElement origMethod = resolver.resolveMethod(index, enclosing);
                // look for annotations that indicate that this method requires special handling
                for (Annotation annotation : origMethod.getVisibleAnnotations()) {
                    ClassTypeDescriptor desc = annotation.getDescriptor();
                    if (desc.getPackageName().equals(Native.NATIVE_PKG)) {
                        if (desc.getClassName().equals(Native.ANN_EXTERN)) {
                            AnnotationValue nameVal = annotation.getValue("withName");
                            String name = nameVal == null ? origMethod.getName() : ((StringAnnotationValue) nameVal).getString();
                            // register as a function
                            FunctionType type = origMethod.getType(List.of(/*todo*/));
                            nativeInfo.registerFunctionInfo(
                                origMethod.getEnclosingType().getDescriptor(),
                                origMethod.getName(),
                                origMethod.getDescriptor(),
                                new NativeFunctionInfo(origMethod, ctxt.getLiteralFactory().literalOfSymbol(name, type))
                            );
                            // all done
                            break;
                        } else if (desc.getClassName().equals(Native.ANN_EXPORT)) {
                            // immediately generate the call-in stub
                            AnnotationValue nameVal = annotation.getValue("withName");
                            String name = nameVal == null ? origMethod.getName() : ((StringAnnotationValue) nameVal).getString();
                            Function exactFunction = ctxt.getExactFunction(origMethod);
                            FunctionType fnType = origMethod.getType(List.of(/*todo*/));
                            Function function = ctxt.getOrAddProgramModule(enclosing).getOrAddSection(IMPLICIT_SECTION_NAME).addFunction(origMethod, name, fnType);
                            BasicBlockBuilder gf = classCtxt.newBasicBlockBuilder(origMethod);
                            BlockLabel entry = new BlockLabel();
                            gf.begin(entry);
                            LiteralFactory lf = ctxt.getLiteralFactory();
                            SymbolLiteral fn = lf.literalOfSymbol(exactFunction.getName(), exactFunction.getType());
                            int pcnt = origMethod.getParameters().size();
                            List<Value> args = new ArrayList<>(pcnt + 1);
                            List<Value> pv = new ArrayList<>(pcnt);
                            // for now, thread is null!
                            // todo: insert prolog here
                            args.add(lf.literalOfNull());
                            for (int j = 0; j < pcnt; j ++) {
                                Value parameter = gf.parameter(fnType.getParameterType(j), j);
                                pv.add(parameter);
                                args.add(parameter);
                            }
                            Value result = gf.callFunction(fn, args);
                            if (fnType.getReturnType() instanceof VoidType) {
                                gf.return_();
                            } else {
                                gf.return_(result);
                            }
                            gf.finish();
                            BasicBlock entryBlock = BlockLabel.getTargetOf(entry);
                            function.replaceBody(MethodBody.of(entryBlock, Schedule.forMethod(entryBlock), null, pv));
                            // ensure the method is reachable
                            ctxt.registerEntryPoint(origMethod);
                            // all done
                            break;
                        }
                    }
                }
                return origMethod;
            }
        }, index);
    }
}
