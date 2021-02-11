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

    private final Map<ValidatedTypeDefinition, IdAndRange> typeids = new ConcurrentHashMap<>();

    /** 
     * This class embodies the typeid for a class and the
     * maximumSubclassID so that instanceof checks can be
     * done efficiently for primary classes by validating
     * the following relation holds:
     * `target.typeid < o.typeid < target.maximumSubtypeId`
     * 
     * TypeIDs are assigned to classes using a pre-order
     * traversal of the set reachable classes and their
     * subclasses starting from Object.
     * 
     * Interface typeid's are assigned after class's get their
     * typeids assigned - see code in SuperDisplayBuilder.java
     * 
     * Interfaces also get assigned a bit as their index into
     * the implemented interfaces bit array.  If a class
     * implements interface I, it will have a `1` in the 
     * interface_bits[] at I.interfaceIndexBit.
     * 
     * We have perfect knowledge of the implemented interfaces
     * so we can assign these bits up front and the array should
     * stay reasonably small.
     * 
     * TODO: Assign array classes their typeids in a meaningful way.
     * TODO: Need to fix up Object's `maximumSubtypeId` to include 
     * the interface typeids.
     */
    static class IdAndRange {
        private static int typeid_index = 1; // avoid using 0;

        private static int interface_index_bit = 0; // represents the next bit for an interface

        public static IdAndRange nextID() {
            return new IdAndRange(typeid_index++);
        }

        public static IdAndRange nextInterfaceID() {
            IdAndRange id = new IdAndRange(typeid_index++);
            id.setNextInterfaceIndexBit();
            return id;
        }

        int typeid;
        int maximumSubtypeId;
        // range is [typeid, maximumSubtypeID]

        int interfaceIndexBit = -1;

        IdAndRange(int id) {
            typeid = id;
            maximumSubtypeId = id;
        }

        public void setMaximumSubtypeId(int id) {
            maximumSubtypeId = Math.max(maximumSubtypeId, id);
        }

        public void setNextInterfaceIndexBit() {
            interfaceIndexBit = interface_index_bit;
            if (interface_index_bit == 0) {
                interface_index_bit = 1;
            } else {
                interface_index_bit <<= 1;
            }
        }

        public String toString() {
            String s = "ID[" + typeid +"] Range["+ typeid +", " + maximumSubtypeId + "]";
            if (interfaceIndexBit != -1) {
                s += " indexBit[" + Integer.toHexString(interfaceIndexBit) + "]";
            }
            return s;
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

        supersLog.debug("typeid and range");
        typeids.entrySet().stream()
            .sorted((a, b) -> a.getValue().typeid - b.getValue().typeid)
            .forEach(es -> {
                ValidatedTypeDefinition vtd = es.getKey();
                IdAndRange idRange = es.getValue();
                supersLog.debug(idRange.toString() + " " + vtd.getInternalName());
            }
        );

        int maxInterfaceBit = IdAndRange.interface_index_bit;
        int bytesPerClass = (int)Math.ceil(Math.log10(maxInterfaceBit));
        supersLog.debug("===============");
        supersLog.debug("Implemented interface bits require " + bytesPerClass + " bytes per class");
        supersLog.debug("classes + interfaces = " + typeids.size());
        supersLog.debug("Interface bits[] space (in bytes): " + (typeids.size() * bytesPerClass));
    }

    void assignTypeID(ValidatedTypeDefinition cls) {
        IdAndRange myID = typeids.computeIfAbsent(cls, theCls -> IdAndRange.nextID());
        log.debug("["+ myID.typeid +"] Class: " + cls.getInternalName());
    }

    void assignMaximumSubtypeId(ValidatedTypeDefinition cls) {
        IdAndRange myID = typeids.get(cls);
        log.debug("Visiting: " + cls.getInternalName() + " " + myID.toString());
        ValidatedTypeDefinition superclass = cls.getSuperClass();
        if (superclass != null) {
            IdAndRange superID = typeids.getOrDefault(superclass, null);
            if (superID != null) {
                superID.setMaximumSubtypeId(myID.maximumSubtypeId);
                log.debug("Setting Super's max subtype id: " + superclass.getInternalName() + " " + superID.toString());
            }
        }
    }

    void assignInterfaceID(ValidatedTypeDefinition cls) {
        int numInterfaces = cls.getInterfaceCount();
        for (int i = 0; i < numInterfaces; i++) {
            ValidatedTypeDefinition interface_i = cls.getInterface(i);
            if (typeids.get(interface_i) == null) {
                typeids.computeIfAbsent(interface_i, theInterface -> IdAndRange.nextInterfaceID());
                // assign IDs to interfaces implemented by this interface
                assignInterfaceID(interface_i);
            }
        }
    }

    void writeTypeIdToClasses() {
        typeids.entrySet().stream()
            .forEach(es -> {
                ValidatedTypeDefinition vtd = es.getKey();
                IdAndRange idRange = es.getValue();
                vtd.assignTypeId(idRange.typeid);
            }
        );
    }
}

