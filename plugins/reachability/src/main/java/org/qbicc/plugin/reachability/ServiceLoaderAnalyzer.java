package org.qbicc.plugin.reachability;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.atomic.AccessModes;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.plugin.apploader.AppClassLoader;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceLoaderAnalyzer {
    private static final AttachmentKey<ServiceLoaderAnalyzer> KEY = new AttachmentKey<>();
    private static final List<LoadedTypeDefinition> NONE = List.of();
    static final String SERVICE_DIR = "META-INF/services";

    private final CompilationContext ctxt;
    private final Map<LoadedTypeDefinition, List<LoadedTypeDefinition>> serviceProviders = new ConcurrentHashMap<>();

    private ServiceLoaderAnalyzer(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static ServiceLoaderAnalyzer get(CompilationContext ctxt) {
        ServiceLoaderAnalyzer info = ctxt.getAttachment(KEY);
        if (info == null) {
            info = new ServiceLoaderAnalyzer(ctxt);
            ServiceLoaderAnalyzer appearing = ctxt.putAttachmentIfAbsent(KEY, info);
            if (appearing != null) {
                info = appearing;
            }
        }
        return info;
    }

    public List<LoadedTypeDefinition> getProviders(LoadedTypeDefinition service) {
        List<LoadedTypeDefinition> cachedProviders = serviceProviders.get(service);
        if (cachedProviders != null) {
            return cachedProviders;
        }
        ArrayList<LoadedTypeDefinition> providers = new ArrayList<>();

        // Step 1: Process module information to find providers
        // TODO: We need to actually implement the module-based lookup!

        // Step 2: Find and process provider configuration files in the classpath
        String pcName = SERVICE_DIR+"/"+service.getInternalName().replace('/', '.');
        findProviders(pcName, ctxt.getBootstrapClassContext(), providers);
        VmClassLoader appCl = AppClassLoader.get(ctxt).getAppClassLoader();
        if (appCl != null) {
            findProviders(pcName, appCl.getClassContext(), providers);
        }

        List<LoadedTypeDefinition> result = providers.isEmpty() ? NONE : List.copyOf(providers);
        serviceProviders.put(service, result);
        return result;
    }

    private static void findProviders(String pcName, ClassContext classContext, ArrayList<LoadedTypeDefinition> providers) {
        List<byte[]> files = classContext.getResources(pcName);
        for (byte[] f: files) {
            String pc = new String(f, StandardCharsets.UTF_8);
            String[] lines = pc.split(System.lineSeparator());
            for (String line : lines) {
                /* filter '#' comments from line */
                int commentStart = line.indexOf('#');
                if (commentStart != -1) {
                    line = line.substring(0, commentStart).trim();
                }
                line = line.trim();
                if (!line.isBlank()) {
                    DefinedTypeDefinition dtd = classContext.findDefinedType(line.replace('.', '/'));
                    if (dtd != null) {
                        LoadedTypeDefinition ltd = dtd.load();
                        if (!providers.contains(ltd)) {
                            providers.add(ltd);
                        }
                    }
                }
            }
        }
    }

    public void serializeProviderConfig() {
        // Translate the compile-time map to the runtime Class[][]
        VmClass classClass = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load().getVmClass();
        Vm vm = ctxt.getVm();
        ArrayList<VmArray> accumulator = new ArrayList<>();
        for (Map.Entry<LoadedTypeDefinition,List<LoadedTypeDefinition>> e: serviceProviders.entrySet()) {
            if (!e.getValue().isEmpty()) {
                VmObject[] inner = new VmObject[e.getValue().size()+1];
                inner[0] = e.getKey().getVmClass();
                int cursor = 1;
                for (LoadedTypeDefinition t: e.getValue()) {
                    inner[cursor++] = t.getVmClass();
                }
                accumulator.add(vm.newArrayOf(classClass, inner));
            }
        }
        VmArray runtimeArray = vm.newArrayOf(classClass.getArrayClass(), accumulator.toArray(VmObject[]::new));

        // Now updated the static field with the final encoded provider map so that it will be serialized to the runtime heap.
        LoadedTypeDefinition supportType = ctxt.getBootstrapClassContext().findDefinedType("java/util/QbiccServiceLoaderSupport").load();
        VmClass supportClass = supportType.getVmClass();
        FieldElement f = supportType.findField("providerConfigurationMapping");
        supportClass.getStaticMemory().storeRef(supportClass.indexOfStatic(f), runtimeArray, AccessModes.SinglePlain);
    }
}
