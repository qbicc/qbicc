package cc.quarkus.qcc.plugin.instanceofcheckcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

/**
 * Build Cohen's display of accessible super types.
 * This is initially just the array of the supertypes of Class C
 * including itself.
 */
public class SupersDisplayTables {
    private static final AttachmentKey<SupersDisplayTables> KEY = new AttachmentKey<>();
    private static final ValidatedTypeDefinition[] INVALID_DISPLAY = new ValidatedTypeDefinition[0];
    public static final boolean DEBUG_SUPERSDISPLAY = true;

    private final CompilationContext ctxt;
    private final Map<ValidatedTypeDefinition, ValidatedTypeDefinition[]> supers = new ConcurrentHashMap<>();

    private int maxDisplaySizeElements;

    private SupersDisplayTables(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static SupersDisplayTables get(CompilationContext ctxt) {
        SupersDisplayTables dt = ctxt.getAttachment(KEY);
        if (dt == null) {
            dt = new SupersDisplayTables(ctxt);
            SupersDisplayTables appearing = ctxt.putAttachmentIfAbsent(KEY, dt);
            if (appearing != null) {
                dt = appearing;
            }
        }
        return dt;
    }

    public ValidatedTypeDefinition[] getSupersDisplay(ValidatedTypeDefinition cls) {
        if (cls.getSuperClass() == null) {
            // java/lang/Object case
            return supers.computeIfAbsent(cls, theCls -> new ValidatedTypeDefinition[] { theCls });
        } else if (cls.isInterface()) {
            // Interfaces only have Object as their superclass
            // TODO: Should the interface be in the display?  no for the Click paper
            return supers.computeIfAbsent(cls, theCls -> new ValidatedTypeDefinition[] { theCls });
        }
        // Display should have been built before this point so return the built one
        // or an easy to identify invalid one.
        return supers.getOrDefault(cls, INVALID_DISPLAY);
    }

    void buildSupersDisplay(ValidatedTypeDefinition cls) {
        if (DEBUG_SUPERSDISPLAY) ctxt.debug("Building SupersDisplay for: " + cls.getDescriptor());
        ValidatedTypeDefinition[] supersArray = getSupersDisplay(cls);
        if (supersArray == INVALID_DISPLAY) {
            RTAInfo info = RTAInfo.get(ctxt);
            ArrayList<ValidatedTypeDefinition> superDisplay = new ArrayList<>();
            ValidatedTypeDefinition next = cls;
            do {
                superDisplay.add(next);
                if (!info.isLiveClass(next)) {
                    // TODO - can we optimize here if RTA doesn't see this class as live? Can that happen?
                    if (DEBUG_SUPERSDISPLAY) ctxt.debug("Found RTA non-live super: " + cls.getDescriptor());
                }
                next = next.getSuperClass();
            } while (next != null);
            Collections.reverse(superDisplay);
            // TODO some kind of assert that display size == depth
            // TODO: ValidatedTypeDefinition needs to have its depth set
            maxDisplaySizeElements = Math.max(maxDisplaySizeElements, superDisplay.size());
            supersArray = superDisplay.toArray(INVALID_DISPLAY); // Use this to ensure toArray result has right type
            supers.put(cls, supersArray); 
        }
        if (DEBUG_SUPERSDISPLAY) ctxt.debug("Display size: " + supersArray.length);
    }

    public void statistics() {
        HashMap<Integer, Integer> histogram = new HashMap<>();
        supers.values().stream().forEach(vtd -> {
            Integer column = Integer.valueOf(vtd.length);
            Integer count = histogram.getOrDefault(column, 0);
            count += 1;
            histogram.put(column, count);
        });
        ctxt.debug("Supers display statistics: [size, occurrance]");
        histogram.entrySet().stream().forEach(es -> {
            ctxt.debug("\t["+ es.getKey() +", " + es.getValue()+ "]");
        });
        int numClasses = supers.size();
        ctxt.debug("Classes: "+ numClasses);
        ctxt.debug("Max display size: "+ maxDisplaySizeElements);
        ctxt.debug("Slots of storage: " + numClasses * maxDisplaySizeElements);
        int emptySlots = histogram.entrySet().stream().flatMapToInt(es -> {
            int waste = maxDisplaySizeElements - es.getKey(); // max - needed number
            waste *= es.getValue(); // * number of classes in bucket
            return IntStream.of(waste);
        }).sum();
        ctxt.debug("Slots of waste: " + emptySlots);
        
    }
}

