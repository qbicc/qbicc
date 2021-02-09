package cc.quarkus.qcc.plugin.instanceofcheckcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;

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
    private static final Logger log = Logger.getLogger("cc.quarkus.qcc.plugin.instanceofcheckcast");
    private static final Logger supersLog = Logger.getLogger("cc.quarkus.qcc.plugin.instanceofcheckcast.supers");
    
    private static final AttachmentKey<SupersDisplayTables> KEY = new AttachmentKey<>();
    private static final ValidatedTypeDefinition[] INVALID_DISPLAY = new ValidatedTypeDefinition[0];

    private final CompilationContext ctxt;
    private final Map<ValidatedTypeDefinition, ValidatedTypeDefinition[]> supers = new ConcurrentHashMap<>();

    private final Map<ValidatedTypeDefinition, IdAndBounds> typeids = new ConcurrentHashMap<>();

    static class IdAndBounds {
        private static int typeid_index = 1; // avoid using 0;

        public static IdAndBounds nextID() {
            return new IdAndBounds(typeid_index++);
        }

        int typeid;
        int upperBound;
        // lower bound is == typeid

        IdAndBounds(int id) {
            typeid = id;
            upperBound = id;
        }

        public void setUpperBound(int id) {
            upperBound = Math.max(upperBound, id);
        }

        public String toString() {
            return "ID[" + typeid +"] Bound["+ typeid +", "+upperBound+"]";
        }
    }

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
        log.debug("Building SupersDisplay for: " + cls.getDescriptor());
        ValidatedTypeDefinition[] supersArray = getSupersDisplay(cls);
        if (supersArray == INVALID_DISPLAY) {
            RTAInfo info = RTAInfo.get(ctxt);
            ArrayList<ValidatedTypeDefinition> superDisplay = new ArrayList<>();
            ValidatedTypeDefinition next = cls;
            do {
                superDisplay.add(next);
                if (!info.isLiveClass(next)) {
                    // TODO - can we optimize here if RTA doesn't see this class as live? Can that happen?
                    log.debug("Found RTA non-live super: " + cls.getDescriptor());
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
        log.debug("Display size: " + supersArray.length);
    }

    public void statistics() {
        HashMap<Integer, Integer> histogram = new HashMap<>();
        supers.values().stream().forEach(vtd -> {
            Integer column = Integer.valueOf(vtd.length);
            Integer count = histogram.getOrDefault(column, 0);
            count += 1;
            histogram.put(column, count);
        });
        supersLog.debug("Supers display statistics: [size, occurrance]");
        histogram.entrySet().stream().forEach(es -> {
            supersLog.debug("\t["+ es.getKey() +", " + es.getValue()+ "]");
        });
        int numClasses = supers.size();
        supersLog.debug("Classes: "+ numClasses);
        supersLog.debug("Max display size: "+ maxDisplaySizeElements);
        supersLog.debug("Slots of storage: " + numClasses * maxDisplaySizeElements);
        int emptySlots = histogram.entrySet().stream().flatMapToInt(es -> {
            int waste = maxDisplaySizeElements - es.getKey(); // max - needed number
            waste *= es.getValue(); // * number of classes in bucket
            return IntStream.of(waste);
        }).sum();
        supersLog.debug("Slots of waste: " + emptySlots);

        supersLog.debug("typeid and bounds");
        typeids.entrySet().stream()
            .sorted((a, b) -> a.getValue().typeid - b.getValue().typeid)
            .forEach(es -> {
                ValidatedTypeDefinition vtd = es.getKey();
                IdAndBounds idBounds = es.getValue();
                supersLog.debug(idBounds.toString() + " " + vtd.getInternalName());
            }
        );
    }

    void assignTypeID(ValidatedTypeDefinition cls) {
        IdAndBounds myID = typeids.computeIfAbsent(cls, theCls -> IdAndBounds.nextID());
        log.debug("["+ myID.typeid +"] Class: " + cls.getInternalName());
    }

    void assignBounds(ValidatedTypeDefinition cls) {
        IdAndBounds myID = typeids.get(cls);
        log.debug("Visiting: " + cls.getInternalName() + " " + myID.toString());
        ValidatedTypeDefinition superclass = cls.getSuperClass();
        if (superclass != null) {
            IdAndBounds superBounds = typeids.getOrDefault(superclass, null);
            if (superBounds != null) {
                superBounds.setUpperBound(myID.upperBound);
                log.debug("Setting Super's bound: " + superclass.getInternalName() + " " + superBounds.toString());
            }
        }
    }
}

