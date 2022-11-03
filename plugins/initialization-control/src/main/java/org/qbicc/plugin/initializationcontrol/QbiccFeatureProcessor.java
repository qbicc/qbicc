package org.qbicc.plugin.initializationcontrol;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.qbicc.context.CompilationContext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public class QbiccFeatureProcessor {
    public static void process(CompilationContext ctxt, List<Path> features) {
        if (features.isEmpty()) {
            return;
        }
        FeaturePatcher fp = FeaturePatcher.get(ctxt);
        RuntimeResourceManager rm = RuntimeResourceManager.get(ctxt);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


        for (Path feature: features) {
            QbiccFeature qf;
            try {
                InputStream fs = new FileInputStream(feature.toFile());
                qf = mapper.readValue(fs, QbiccFeature.class);
            } catch (FileNotFoundException e) {
                ctxt.error("Failed to open qbicc-feature %s", feature);
                continue;
            } catch (IOException e) {
                ctxt.error(e, "Failed to parse qbicc-feature %s ",feature);
                continue;
            }

            ctxt.info("Processing build feature %s", feature);
            if (qf.initializeAtRuntime != null) {
                for (String className : qf.initializeAtRuntime) {
                    String internalName = className.replace('.', '/');
                    fp.addRuntimeInitializedClass(internalName);
                }
            }
            if (qf.runtimeResource != null) {
                for (String name : qf.runtimeResource) {
                    rm.addResource(name);
                }
            }
            if (qf.runtimeResources != null) {
                for (String name : qf.runtimeResources) {
                    rm.addResources(name);
                }
            }
            if (qf.reflectiveConstructors != null) {
                for (QbiccFeature.Constructor c : qf.reflectiveConstructors) {
                    fp.addReflectiveConstructor(c.declaringClass, c.descriptor);
                }
            }
            if (qf.reflectiveFields != null) {
                for (QbiccFeature.Field f : qf.reflectiveFields) {
                    fp.addReflectiveField(f.declaringClass, f.name);
                }
            }
            if (qf.reflectiveMethods != null) {
                for (QbiccFeature.Method meth : qf.reflectiveMethods) {
                    fp.addReflectiveMethod(meth.declaringClass, meth.name, meth.descriptor);
                }
            }
        }
    }
}
