package org.qbicc.plugin.nativeimage;

import com.oracle.svm.core.BuildPhaseProvider;
import com.oracle.svm.core.jdk.Resources;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;
import org.graalvm.nativeimage.impl.RuntimeReflectionSupport;
import org.qbicc.context.CompilationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class FeatureProcessor {
    public static void processBuildFeature(CompilationContext ctxt, List<String> features, ClassLoader cl) {
        if (features.isEmpty()) {
            return;
        }

        // Fake up enough of the GraalVM Support classes to let us execute the
        // beforeAnalysis method of the Feature and perform the corresponding qbicc actions.
        Feature.BeforeAnalysisAccess qbiccProxy = new QbiccBeforeAnalysisAccess(ctxt);
        QbiccImageSingletonsSupport qiss = new QbiccImageSingletonsSupport();
        qiss.add(RuntimeReflectionSupport.class, new QbiccRuntimeReflectionSupport(ctxt));
        qiss.add(RuntimeClassInitializationSupport.class, new QbiccRuntimeClassInitializationSupport(ctxt));
        try {
            Class<BuildPhaseProvider> buildPhaseProviderClass = (Class<BuildPhaseProvider>)Class.forName("com.oracle.svm.core.BuildPhaseProvider");
            Constructor<BuildPhaseProvider> bc = buildPhaseProviderClass.getDeclaredConstructor();
            bc.setAccessible(true);
            BuildPhaseProvider buildPhaseProviderInstance = bc.newInstance();
            qiss.add(BuildPhaseProvider.class, buildPhaseProviderInstance);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            ctxt.error("Unable to instantiate com.oracle.svm.core.BuildPhaseProvider");
            return;
        }
        try {
            Class<Resources> resourcesClass = (Class<Resources>)Class.forName("com.oracle.svm.core.jdk.Resources");
            Constructor<Resources> rc = resourcesClass.getDeclaredConstructor();
            rc.setAccessible(true);
            Resources resourcesInstance = rc.newInstance();
            qiss.add(Resources.class, resourcesInstance);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            ctxt.error("Unable to instantiate com.oracle.svm.core.jdk.Resources");
            return;
        }

        // Set up the current Thread to load application classes into the hostVM via its CCL
        ClassLoader savedCCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            for (String feature : features) {
                ctxt.info("Processing build feature %s", feature);
                Feature featureInstance;
                Class<Feature> featureClass;
                try {
                    featureClass = (Class<Feature>) cl.loadClass(feature);
                    featureInstance = featureClass.getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException e) {
                    ctxt.error("Failed to load feature %s", feature);
                    continue;
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    ctxt.error("Failed to instantiate feature %s", feature);
                    continue;
                }

                try {
                    Method beforeAnalysis = featureClass.getDeclaredMethod("beforeAnalysis", Feature.BeforeAnalysisAccess.class);
                    beforeAnalysis.invoke(featureInstance, qbiccProxy);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    ctxt.error("Failed to execute beforeAnalysis for %s", feature);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(savedCCL);
        }
    }
}
