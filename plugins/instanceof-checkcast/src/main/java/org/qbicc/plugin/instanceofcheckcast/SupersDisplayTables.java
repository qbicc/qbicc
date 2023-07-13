package org.qbicc.plugin.instanceofcheckcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayTables.IdAndRange.Factory;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.type.ArrayType;
import org.qbicc.type.StructType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;
import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;

/**
 * Build Cohen's display of accessible super types.
 * This is initially just the array of the supertypes of Class C
 * including itself.
 */
public class SupersDisplayTables {
    private static final Logger log = Logger.getLogger("org.qbicc.plugin.instanceofcheckcast");
    private static final Logger supersLog = Logger.getLogger("org.qbicc.plugin.instanceofcheckcast.supers");

    private static final AttachmentKey<SupersDisplayTables> KEY = new AttachmentKey<>();
    private static final LoadedTypeDefinition[] INVALID_DISPLAY = new LoadedTypeDefinition[0];

    private final CompilationContext ctxt;
    private final Map<LoadedTypeDefinition, LoadedTypeDefinition[]> supers = new ConcurrentHashMap<>();

    private final Map<LoadedTypeDefinition, IdAndRange> typeids = new ConcurrentHashMap<>();

    static final String GLOBAL_TYPEID_ARRAY = "qbicc_typeid_array";
    private GlobalVariableElement typeIdArrayGlobal;
    private StructType typeIdStructType;

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
     * [primitive classes]
     * [Object]
     * [primitive arrays]
     * [Object reference array]
     * [Object subclasses]
     * [interfaces]
     */
    static class IdAndRange {

        static class Factory {
            static final int interfaces_per_byte = 8;

            private int typeid_index = 0;

            // interface ids must be contigious and after the class ids
            private int first_interface_typeid = 0;

            public IdAndRange nextID() {
                return new IdAndRange(typeid_index++, this);
            }

            public IdAndRange nextInterfaceID() {
                if (first_interface_typeid == 0) {
                    first_interface_typeid = typeid_index;
                }
                return new IdAndRange(typeid_index++, this);
            }
        }

        private final Factory constants;

        int typeid;
        int maximumSubtypeId;
        // range is [typeid, maximumSubtypeID]

        IdAndRange(int id, Factory factory) {
            typeid = id;
            maximumSubtypeId = id;
            constants = factory;
        }

        public void setMaximumSubtypeId(int id) {
            maximumSubtypeId = Math.max(maximumSubtypeId, id);
        }

        public String toString() {
            String s = "ID[" + typeid + "] Range[" + typeid + ", " + maximumSubtypeId + "]";
            if (typeid >= constants.first_interface_typeid) {
                int bit = (typeid - constants.first_interface_typeid);
                s += " indexBit[" + bit + "]";
                s += " byte[" + implementedInterfaceByteIndex() + "]";
                s += " mask[" + Integer.toBinaryString(1 << (bit & 7)) + "]";
            }
            return s;
        }

        /**
         * Return the index into the implementedInterfaces[]
         * to get the right byte to test for this interface.
         */
        int implementedInterfaceByteIndex() {
            if (typeid >= constants.first_interface_typeid) {
                int bit = (typeid - constants.first_interface_typeid);
                return bit >> 3; /* Equiv to: / interfaces_per_byte */
            }
            return -1;
        }

        /**
         * Return the mask used to test/set this interface
         * in the byte this interface would be in.
         * 
         * ie: interfaceBits[index] & implementedInterfaceBitMask() == implementedInterfaceBitMask()
         * means the interface is implemented
         */
        int implementedInterfaceBitMask() {
            if (typeid >= constants.first_interface_typeid) {
                int bit = (typeid - constants.first_interface_typeid);
                return 1 << (bit & 7);
            }
            return 0;
        }

    }

    private final IdAndRange.Factory idAndRange = new IdAndRange.Factory();

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

    public LoadedTypeDefinition[] getSupersDisplay(LoadedTypeDefinition cls) {
        if (cls.getSuperClass() == null) {
            // java/lang/Object case
            return supers.computeIfAbsent(cls, theCls -> new LoadedTypeDefinition[] { theCls });
        } else if (cls.isInterface()) {
            // Interfaces only have Object as their superclass
            // TODO: Should the interface be in the display? no for the Click paper
            return supers.computeIfAbsent(cls, theCls -> new LoadedTypeDefinition[] { theCls });
        }
        // Display should have been built before this point so return the built one
        // or an easy to identify invalid one.
        return supers.getOrDefault(cls, INVALID_DISPLAY);
    }

    void buildSupersDisplay(LoadedTypeDefinition cls) {
        log.debug("Building SupersDisplay for: " + cls.getDescriptor());
        LoadedTypeDefinition[] supersArray = getSupersDisplay(cls);
        if (supersArray == INVALID_DISPLAY) {
            ReachabilityInfo info = ReachabilityInfo.get(ctxt);
            ArrayList<LoadedTypeDefinition> superDisplay = new ArrayList<>();
            LoadedTypeDefinition next = cls;
            do {
                superDisplay.add(next);
                if (!info.isReachableClass(next)) {
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
            supersLog.debug("\t[" + es.getKey() + ", " + es.getValue() + "]");
        });
        int numClasses = supers.size();
        supersLog.debug("Classes: " + numClasses);
        supersLog.debug("Max display size: " + maxDisplaySizeElements);
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
                LoadedTypeDefinition vtd = es.getKey();
                IdAndRange idRange = es.getValue();
                supersLog.debug(idRange.toString() + " " + vtd.getInternalName());
            }
        );

        int bytesPerClass = getNumberOfBytesInInterfaceBitsArray();
        supersLog.debug("===============");
        supersLog.debug("Implemented interface bits require " + bytesPerClass + " bytes per class");
        supersLog.debug("classes + interfaces = " + typeids.size());
        supersLog.debug("Interface bits[] space (in bytes): " + (typeids.size() * bytesPerClass));
    }

    int assignTypeID(LoadedTypeDefinition cls) {
        IdAndRange myID = typeids.computeIfAbsent(cls, theCls -> idAndRange.nextID());
        log.debug("[" + myID.typeid + "] Class: " + cls.getInternalName());
        return myID.typeid;
    }

    void assignMaximumSubtypeId(LoadedTypeDefinition cls) {
        IdAndRange myID = typeids.get(cls);
        log.debug("Visiting: " + cls.getInternalName() + " " + myID.toString());
        LoadedTypeDefinition superclass = cls.getSuperClass();
        if (superclass != null) {
            IdAndRange superID = typeids.getOrDefault(superclass, null);
            if (superID != null) {
                superID.setMaximumSubtypeId(myID.maximumSubtypeId);
                log.debug("Setting Super's max subtype id: " + superclass.getInternalName() + " " + superID.toString());
            }
        }
    }

    void assignInterfaceId(LoadedTypeDefinition cls) {
        Assert.assertTrue(cls.isInterface());
        typeids.computeIfAbsent(cls, theInterface -> idAndRange.nextInterfaceID());
    }

    void updateJLORange(LoadedTypeDefinition jlo) {
        Assert.assertTrue(jlo.getSuperClass() == null);
        IdAndRange r = typeids.get(jlo);
        // typeid_index is incremented after use so we need
        // subtract 1 here to get the max typeid
        r.maximumSubtypeId = idAndRange.typeid_index - 1;
    }

    void reserveTypeIds(int numToReserve) {
        Assert.assertTrue(numToReserve >= 0);
        idAndRange.typeid_index += numToReserve;
    }

    void writeTypeIdToClasses() {
        typeids.entrySet().stream().forEach(es -> {
            LoadedTypeDefinition vtd = es.getKey();
            IdAndRange idRange = es.getValue();
            vtd.assignTypeId(idRange.typeid);
            vtd.assignMaximumSubtypeId(idRange.maximumSubtypeId);
        });
    }

    public int getFirstInterfaceTypeId() {
        return idAndRange.first_interface_typeid;
    }

    int getNumberOfInterfacesInTypeIds() {
        // + 1 to handle poisoned 0 entry
        return typeids.size() - idAndRange.first_interface_typeid + 1;
    }

    public int getNumberOfBytesInInterfaceBitsArray() {
        int numInterfaces = getNumberOfInterfacesInTypeIds();
        return (numInterfaces + Factory.interfaces_per_byte - 1) / Factory.interfaces_per_byte;
    }

    byte[] getImplementedInterfaceBits(LoadedTypeDefinition cls) {
        byte[] setBits = new byte[getNumberOfBytesInInterfaceBitsArray()];
        cls.forEachInterfaceFullImplementedSet(i -> {
            IdAndRange idRange = typeids.get(i);
            if (idRange != null) {
                int index = idRange.implementedInterfaceByteIndex();
                setBits[index] |= idRange.implementedInterfaceBitMask();
            }
        });

        return setBits;
    }

    public int getInterfaceByteIndex(LoadedTypeDefinition cls) {
        Assert.assertTrue(cls.isInterface());
        IdAndRange idRange = typeids.get(cls);
        if (idRange == null) {
            // breakpoint
            throw new IllegalStateException();
        }
        return idRange.implementedInterfaceByteIndex();
    }

    public int getInterfaceBitMask(LoadedTypeDefinition cls) {
        Assert.assertTrue(cls.isInterface());
        IdAndRange idRange = typeids.get(cls);
        return idRange.implementedInterfaceBitMask();
    }

    List<Literal> convertByteArrayToValuesList(LiteralFactory literalFactory, byte[] array) {
        Literal[] literals = new Literal[array.length];
        for (int i = 0; i < array.length; i++) {
            literals[i] = literalFactory.literalOf(array[i]);
        }
        return List.of(literals);
    }

    void defineTypeIdStructAndGlobalArray(LoadedTypeDefinition jlo) {
        TypeSystem ts = ctxt.getTypeSystem();
        UnsignedIntegerType u8 = ts.getUnsignedInteger8Type();
        int typeIdSize = ts.getTypeIdSize();
        UnsignedIntegerType uTypeId;
        switch(typeIdSize) {
        case 1:
            uTypeId = ts.getUnsignedInteger8Type();
            break;
        case 2:
            uTypeId = ts.getUnsignedInteger16Type();
            break;
        case 4:
            uTypeId = ts.getUnsignedInteger32Type();
            break;
        case 8:
            uTypeId = ts.getUnsignedInteger64Type();
            break;
        default:
            throw Assert.impossibleSwitchCase("#getTypeIdSize() must be one of {1,2,4,8} - was: " + typeIdSize);
        }
        supersLog.debug("typeIdSize set to: " + uTypeId.toFriendlyString(new StringBuilder()).toString());
        // TODO: better validation of typeId size
        Assert.assertTrue(typeIdSize <= uTypeId.getMinBits());
        int numInterfaces = getNumberOfInterfacesInTypeIds();
        supersLog.debug("NumInterfaces=" + numInterfaces + " numBytes=" + getNumberOfBytesInInterfaceBitsArray());
        ArrayType interfaceBitsType = ts.getArrayType(u8, getNumberOfBytesInInterfaceBitsArray());
        // ts.getStructType(tag, name, size, align, memberResolver);
        // typedef struct typeids {
        //   uintXX_t tid;
        //   uintXX_t maxsubid;
        //   uintXX_t superTypeId;
        //   uint8_t interfaces[x];
        // } typeids;
        UnsignedIntegerType u32 = ts.getUnsignedInteger32Type();
        
        StructType typeIdStruct =  StructType.builder(ts)
            .setTag(StructType.Tag.STRUCT)
            .setName("typeIds")
            .setOverallAlignment(ts.getPointerAlignment())
            .addNextMember("typedId", uTypeId)
            .addNextMember("maxSubTypeId", uTypeId)
            .addNextMember("superTypeId", uTypeId)
            .addNextMember("interfaceBits", interfaceBitsType)
            .build();

        ArrayType typeIdsArrayType = ts.getArrayType(typeIdStruct, get_number_of_typeids());

        // create a GlobalVariable for shared access to the typeId array
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder(GLOBAL_TYPEID_ARRAY, BaseTypeDescriptor.V);
        builder.setType(typeIdsArrayType);
        builder.setEnclosingType(jlo);
        builder.setSignature(BaseTypeSignature.V);
        builder.setSection(ctxt.getImplicitSection());
        typeIdArrayGlobal = builder.build();
        typeIdStructType = typeIdStruct;
    }
    
    void emitTypeIdTable(LoadedTypeDefinition jlo) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        
        /* Set up the implementedInterface[] for primitives */
        List<Literal> primitivesInterfaceBits = new ArrayList<>();
        Literal zero = lf.literalOf(0);
        ArrayType interfaceBitsType = (ArrayType)typeIdStructType.getMember("interfaceBits").getType();
        for (int i = 0; i < interfaceBitsType.getElementCount(); i++) {
            primitivesInterfaceBits.add(zero);
        }
        
        List<StructType.Member> members = typeIdStructType.getMembers();
        Literal[] typeIdTable = new Literal[get_number_of_typeids()];

        /* Primitives don't support instanceOf but they are only implemented by themselves */
        for (int i = 0; i < 10; i++) {
            typeIdTable[i] = lf.literalOf(typeIdStructType, 
                Map.of(
                    members.get(0), lf.literalOf((UnsignedIntegerType)members.get(0).getType(), i),
                    members.get(1), lf.literalOf((UnsignedIntegerType)members.get(1).getType(), i),
                    members.get(2), lf.literalOf((UnsignedIntegerType)members.get(2).getType(), 0),  /* Set super for prims to posion */
                    members.get(3), lf.literalOf(interfaceBitsType, primitivesInterfaceBits)
                )
            );
        }
        for (Map.Entry<LoadedTypeDefinition, IdAndRange> e : typeids.entrySet()) {
            LoadedTypeDefinition vtd = e.getKey();
            IdAndRange idRange = e.getValue();
            int superTypeId = 0;
            if (vtd.hasSuperClass()) {
                IdAndRange superRange = typeids.get(vtd.getSuperClass());
                superTypeId = superRange.typeid;
            }
            typeIdTable[vtd.getTypeId()] = lf.literalOf(typeIdStructType, 
                Map.of(
                    members.get(0), lf.literalOf((UnsignedIntegerType)members.get(0).getType(), idRange.typeid),
                    members.get(1), lf.literalOf((UnsignedIntegerType)members.get(1).getType(), idRange.maximumSubtypeId),
                    members.get(2), lf.literalOf((UnsignedIntegerType)members.get(2).getType(), superTypeId),
                    members.get(3), lf.literalOf(interfaceBitsType, convertByteArrayToValuesList(lf, getImplementedInterfaceBits(vtd)))
                )
            );
        }
        Literal typeIdsValue = ctxt.getLiteralFactory().literalOf((ArrayType)typeIdArrayGlobal.getType(), List.of(typeIdTable));
        
        /* Write the data into Object's section */
        ModuleSection section = ctxt.getImplicitSection(jlo);
        section.addData(null, GLOBAL_TYPEID_ARRAY, typeIdsValue);
    }

    /**
     * Get the GlobalVariableElement reference to the `qbicc_typeid_array`.
     * 
     * As part of it getting it, ensure a reference to it has been recorded into
     * the ExecutableElement's section.
     * 
     * @param originalElement the original element (must not be {@code null})
     * @return the type ID global
     */
    public GlobalVariableElement getAndRegisterGlobalTypeIdArray(ExecutableElement originalElement) {
        Assert.assertNotNull(typeIdArrayGlobal);
        if (!typeIdArrayGlobal.getEnclosingType().equals(originalElement.getEnclosingType())) {
            ProgramModule programModule = ctxt.getOrAddProgramModule(originalElement.getEnclosingType());
            programModule.declareData(null, typeIdArrayGlobal.getName(), typeIdArrayGlobal.getType());
        }
        return typeIdArrayGlobal;
    }

    /**
     * Get the StructType for the GlobalTypeIdArray (`qbicc_typeid_array`)
     * global variable elements.
     * 
     * See #emitTypeIdTable for the definition of the typeid array elements.
     * 
     * @return  The StructType describing the struct.
     */
    public StructType getGlobalTypeIdStructType() {
        Assert.assertNotNull(typeIdStructType);
        return typeIdStructType;
    }

    public int get_number_of_typeids() {
        Assert.assertTrue(idAndRange.typeid_index == typeids.size());
        supersLog.debug("get_highest_typeid == " + typeids.size());
        return typeids.size();
    }
}

