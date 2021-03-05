package cc.quarkus.qcc.plugin.instanceofcheckcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.literal.ArrayLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.plugin.instanceofcheckcast.SupersDisplayTables.IdAndRange.Factory;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.GlobalVariableElement;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.generic.BaseTypeSignature;
import io.smallrye.common.constraint.Assert;

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

    static final String GLOBAL_TYPEID_ARRAY = "qcc_typeid_array";
    private GlobalVariableElement typeIdArrayGlobal;
    CompoundType typeIdStructType;

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
     * [poison]
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

            private int typeid_index = 0; // avoid using 0;

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

    public ValidatedTypeDefinition[] getSupersDisplay(ValidatedTypeDefinition cls) {
        if (cls.getSuperClass() == null) {
            // java/lang/Object case
            return supers.computeIfAbsent(cls, theCls -> new ValidatedTypeDefinition[] { theCls });
        } else if (cls.isInterface()) {
            // Interfaces only have Object as their superclass
            // TODO: Should the interface be in the display? no for the Click paper
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
                ValidatedTypeDefinition vtd = es.getKey();
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

    void assignTypeID(ValidatedTypeDefinition cls) {
        IdAndRange myID = typeids.computeIfAbsent(cls, theCls -> idAndRange.nextID());
        log.debug("[" + myID.typeid + "] Class: " + cls.getInternalName());
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
                typeids.computeIfAbsent(interface_i, theInterface -> idAndRange.nextInterfaceID());
                // assign IDs to interfaces implemented by this interface
                assignInterfaceID(interface_i);
            }
        }
    }

    void updateJLORange(ValidatedTypeDefinition jlo) {
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
            ValidatedTypeDefinition vtd = es.getKey();
            IdAndRange idRange = es.getValue();
            vtd.assignTypeId(idRange.typeid);
            vtd.assignMaximumSubtypeId(idRange.maximumSubtypeId);
        });
    }

    int getNumberOfInterfacesInTypeIds() {
        // + 10 to handle poisioned 0 entry and the 8 prims and void
        return typeids.size() - idAndRange.first_interface_typeid + 10;
    }

    public int getNumberOfBytesInInterfaceBitsArray() {
        int numInterfaces = getNumberOfInterfacesInTypeIds();
        return (numInterfaces + Factory.interfaces_per_byte - 1) / Factory.interfaces_per_byte;
    }

    byte[] getImplementedInterfaceBits(ValidatedTypeDefinition cls) {
        byte[] setBits = new byte[getNumberOfBytesInInterfaceBitsArray()];
        // supersLog.debug("Setting interface bits for: " + cls.getInternalName() + " byteArray size=" + setBits.length);
        for (ValidatedTypeDefinition i : cls.getInterfaces()) {
            IdAndRange idRange = typeids.get(i);
            if (idRange != null) {
                int index = idRange.implementedInterfaceByteIndex();
                // int originalValue = setBits[index];
                setBits[index] |= idRange.implementedInterfaceBitMask();
                // supersLog.debug("Applying interface: " + i.getInternalName() + " setBits["+index+"] mask:" + Integer.toUnsignedString(idRange.implementedInterfaceBitMask(), 2));
                // supersLog.debug("was: " + Integer.toUnsignedString(originalValue, 2));
                // supersLog.debug("now: " + Integer.toUnsignedString(setBits[index], 2));
            }
        }

        return setBits;
    }

    public int getInterfaceByteIndex(ValidatedTypeDefinition cls) {
        Assert.assertTrue(cls.isInterface());
        IdAndRange idRange = typeids.get(cls);
        return idRange.implementedInterfaceByteIndex();
    }

    public int getInterfaceBitMask(ValidatedTypeDefinition cls) {
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

    void emitTypeIdTable(ValidatedTypeDefinition jlo) {
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
        // ts.getCompoundType(tag, name, size, align, memberResolver);
        // typedef struct typeids {
        //   uintXX_t tid;
        //   uintXX_t maxsubid;
        //   uint8_t interfaces[x];
        // } typeids;
        CompoundType.Member[] members = new CompoundType.Member[] {
            ts.getCompoundTypeMember("typeId", uTypeId, 0, uTypeId.getAlign()),
            ts.getCompoundTypeMember("maxSubTypeId", uTypeId, (int)uTypeId.getSize(), uTypeId.getAlign()),
            ts.getCompoundTypeMember("interfaceBits", interfaceBitsType, (int)uTypeId.getSize() * 2, interfaceBitsType.getAlign())
        };
        int memberSize = (int)(uTypeId.getSize() * 2 + interfaceBitsType.getSize());
        CompoundType typeIdStruct = ts.getCompoundType(
            CompoundType.Tag.STRUCT, 
            "typeIds", 
            memberSize /* size */,
            ts.getPointerAlignment(), 
            () -> List.of(members));
        
        Section section = ctxt.getImplicitSection(jlo);
        Literal[] typeIdTable = new Literal[typeids.size() + 10]; // invalid zero + 8 prims + void
        LiteralFactory literalFactory = ctxt.getLiteralFactory();
        
        /* Set up the implementedInterface[] for primitives */
        List<Literal> primitivesInterfaceBits = new ArrayList<>();
        Literal zero = literalFactory.literalOf(0);
        for (int i = 0; i < interfaceBitsType.getElementCount(); i++) {
            primitivesInterfaceBits.add(zero);
        }

        /* Primitives don't support instanceOf but they are only implemented by themselves */
        for (int i = 0; i < 10; i++) {
            typeIdTable[i] = literalFactory.literalOf(typeIdStruct, 
                Map.of(
                    members[0], literalFactory.literalOf(uTypeId, i),
                    members[1], literalFactory.literalOf(uTypeId, i),
                    members[2], literalFactory.literalOf(interfaceBitsType, primitivesInterfaceBits)
                )
            );
        }
        for (Map.Entry<ValidatedTypeDefinition, IdAndRange> e : typeids.entrySet()) {
            ValidatedTypeDefinition vtd = e.getKey();
            IdAndRange idRange = e.getValue();
            typeIdTable[vtd.getTypeId()] = literalFactory.literalOf(typeIdStruct, 
                Map.of(
                    members[0], literalFactory.literalOf(uTypeId, idRange.typeid),
                    members[1], literalFactory.literalOf(uTypeId, idRange.maximumSubtypeId),
                    members[2], literalFactory.literalOf(interfaceBitsType, convertByteArrayToValuesList(literalFactory, getImplementedInterfaceBits(vtd)))
                )
            );
        }
        ArrayType typeIdsArrayType = ctxt.getTypeSystem().getArrayType(typeIdStruct, typeIdTable.length);
        ArrayLiteral typeIdsValue = ctxt.getLiteralFactory().literalOf(typeIdsArrayType, List.of(typeIdTable));
        section.addData(null, GLOBAL_TYPEID_ARRAY, typeIdsValue);

        // create a GlobalVariable for shared access to the typeId array
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder();
        builder.setName(GLOBAL_TYPEID_ARRAY);
        builder.setType(typeIdsArrayType);
        builder.setEnclosingType(jlo);
        // void for now, but this is cheating terribly
        builder.setDescriptor(BaseTypeDescriptor.V);
        builder.setSignature(BaseTypeSignature.V);
        typeIdArrayGlobal = builder.build();
        typeIdStructType = typeIdStruct;
    }

    /**
     * Get the GlobalVariableElement reference to the `qcc_typeid_array`.
     * 
     * As part of it getting it, ensure a reference to it has been recorded into
     * the ExectuableElement's section.
     * 
     * @param typeIdGlobal - the `qcc_typeid_array` global
     */
    public GlobalVariableElement getAndRegisterGlobalTypeIdArray(ExecutableElement originalElement) {
        Assert.assertNotNull(typeIdArrayGlobal);
        if (!typeIdArrayGlobal.getEnclosingType().equals(originalElement.getEnclosingType())) {
            Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
            section.declareData(null, typeIdArrayGlobal.getName(), typeIdArrayGlobal.getType(List.of()));
        }
        return typeIdArrayGlobal;
    }
}

