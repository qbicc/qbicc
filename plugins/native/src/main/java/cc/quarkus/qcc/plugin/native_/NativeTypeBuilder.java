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
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.VoidType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.AnnotationValue;
import cc.quarkus.qcc.type.annotation.StringAnnotationValue;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodResolver;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public class NativeTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition.Builder delegate;

    public NativeTypeBuilder(final ClassContext classCtxt, final DefinedTypeDefinition.Builder delegate) {
        this.classCtxt = classCtxt;
        this.ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    public void setSuperClassName(final String superClassInternalName) {
        if (superClassInternalName != null) {
            if (superClassInternalName.equals(Native.OBJECT)) {
                // probe object type
            } else if (superClassInternalName.equals(Native.WORD)) {
                // probe word type

            }
        }
        getDelegate().setSuperClassName(superClassInternalName);
    }

    public void addMethod(final MethodResolver resolver, final int index) {
        delegate.addMethod(new MethodResolver() {
            public MethodElement resolveMethod(final int index, final DefinedTypeDefinition enclosing) {
                NativeInfo nativeInfo = NativeInfo.get(ctxt);
                MethodElement origMethod = resolver.resolveMethod(index, enclosing);
                boolean isNative = origMethod.hasAllModifiersOf(ClassFile.ACC_NATIVE);
                int cnt = origMethod.getVisibleAnnotationCount();
                // look for annotations that indicate that this method requires special handling
                for (int i = 0; i < cnt; i ++) {
                    Annotation annotation = origMethod.getVisibleAnnotation(i);
                    if (annotation.getClassInternalName().equals(Native.ANN_EXTERN)) {
                        AnnotationValue nameVal = annotation.getValue("withName");
                        String name = nameVal == null ? origMethod.getName() : ((StringAnnotationValue) nameVal).getString();
                        // register as a function
                        int pc = origMethod.getParameterCount();
                        ValueType[] types = new ValueType[pc];
                        for (int j = 0; j < pc; j ++) {
                            types[j] = origMethod.getDescriptor().getParameterType(j);
                        }
                        nativeInfo.nativeFunctions.put(origMethod, new NativeFunctionInfo(ctxt.getLiteralFactory().literalOfSymbol(
                            name,
                            ctxt.getTypeSystem().getFunctionType(origMethod.getReturnType(), types)
                        )));
                        // all done
                        break;
                    } else if (annotation.getClassInternalName().equals(Native.ANN_EXPORT)) {
                        // immediately generate the call-in stub
                        AnnotationValue nameVal = annotation.getValue("withName");
                        String name = nameVal == null ? origMethod.getName() : ((StringAnnotationValue) nameVal).getString();
                        int pc = origMethod.getParameterCount();
                        ValueType[] types = new ValueType[pc];
                        for (int j = 0; j < pc; j ++) {
                            types[j] = origMethod.getDescriptor().getParameterType(j);
                        }
                        ValueType returnType = origMethod.getReturnType();
                        boolean isVoid = returnType instanceof VoidType;
                        FunctionType fnType = ctxt.getTypeSystem().getFunctionType(returnType, types);
                        Function function = ctxt.getOrAddProgramModule(enclosing).getOrAddSection(IMPLICIT_SECTION_NAME).addFunction(origMethod, name, fnType);
                        BasicBlockBuilder gf = classCtxt.newBasicBlockBuilder(origMethod);
                        BlockLabel entry = new BlockLabel();
                        gf.begin(entry);
                        // for now, thread is null!
                        Function exactFunction = ctxt.getExactFunction(origMethod);
                        LiteralFactory lf = ctxt.getLiteralFactory();
                        SymbolLiteral fn = lf.literalOfSymbol(exactFunction.getName(), exactFunction.getType());
                        List<Value> args = new ArrayList<>(origMethod.getParameterCount() + 1);
                        List<Value> pv = new ArrayList<>(origMethod.getParameterCount());
                        args.add(lf.literalOfNull());
                        for (int j = 0; j < origMethod.getParameterCount(); j ++) {
                            Value parameter = gf.parameter(origMethod.getDescriptor().getParameterType(j), j);
                            pv.add(parameter);
                            args.add(parameter);
                        }
                        Value result = gf.callFunction(fn, args);
                        if (isVoid) {
                            gf.return_();
                        } else {
                            gf.return_(result);
                        }
                        BasicBlock entryBlock = BlockLabel.getTargetOf(entry);
                        function.replaceBody(MethodBody.of(entryBlock, Schedule.forMethod(entryBlock), null, pv));
                        // ensure the method is reachable
                        ctxt.registerEntryPoint(origMethod);
                        // all done
                        break;
                    }
                }
                return origMethod;
            }
        }, index);
    }
}
