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
import org.qbicc.object.Function;
import org.qbicc.object.Section;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayTables.IdAndRange.Factory;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.Primitive;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InitializerElement;
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
    private CompoundType typeIdStructType;

    static final String GLOBAL_CLINIT_STATES_STRUCT = "qbicc_clinit_states";
    private GlobalVariableElement clinitStatesGlobal;

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

    void assignTypeIdToPrimitives() {
        Primitive.forEach(type -> type.setTypeId(idAndRange.nextID().typeid));
    }

    void assignTypeID(LoadedTypeDefinition cls) {
        IdAndRange myID = typeids.computeIfAbsent(cls, theCls -> idAndRange.nextID());
        log.debug("[" + myID.typeid + "] Class: " + cls.getInternalName());
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
        // + 10 to handle poisioned 0 entry and the 8 prims and void
        return typeids.size() - idAndRange.first_interface_typeid + 10;
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

    /**
     * Flags:
     * 1 - has clinit method
     * 2 - declares default methods
     * 4 - has default methods
     */
    Literal calculateTypeIdFlags(final UnsignedIntegerType type, LoadedTypeDefinition ltd) {
        int flags = 0;
        InitializerElement initializer = ltd.getInitializer();
        if (initializer != null && initializer.hasMethodBody()) {
            flags |= 1;
        }
        if (ltd.declaresDefaultMethods()) {
            flags |= 2;
        }
        if (ltd.hasDefaultMethods()) {
            flags |= 4;
        }
        
        LiteralFactory lf = ctxt.getLiteralFactory();
        supersLog.debug("[[Flags] ID["+ ltd.getTypeId() + "] Flags = " + Integer.toBinaryString(flags) + " : " + ltd.getInternalName() + "]");
        return lf.literalOf(type, flags);
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
        // ts.getCompoundType(tag, name, size, align, memberResolver);
        // typedef struct typeids {
        //   uintXX_t tid;
        //   uintXX_t maxsubid;
        //   uintXX_t superTypeId;
        //   uint8_t interfaces[x];
        //   u32_t flags;
        // } typeids;
        UnsignedIntegerType u32 = ts.getUnsignedInteger32Type();
        
        CompoundType typeIdStruct =  CompoundType.builder(ts)
            .setTag(CompoundType.Tag.STRUCT)
            .setName("typeIds")
            .setOverallAlignment(ts.getPointerAlignment())
            .addNextMember("typedId", uTypeId)
            .addNextMember("maxSubTypeId", uTypeId)
            .addNextMember("superTypeId", uTypeId)
            .addNextMember("interfaceBits", interfaceBitsType)
            .addNextMember("flags", u32)
            .build();

        ArrayType typeIdsArrayType = ts.getArrayType(typeIdStruct, get_number_of_typeids());

        // create a GlobalVariable for shared access to the typeId array
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder(GLOBAL_TYPEID_ARRAY, BaseTypeDescriptor.V);
        builder.setType(typeIdsArrayType);
        builder.setEnclosingType(jlo);
        builder.setSignature(BaseTypeSignature.V);
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
        
        List<CompoundType.Member> members = typeIdStructType.getMembers();
        Literal[] typeIdTable = new Literal[get_number_of_typeids()];

        /* Primitives don't support instanceOf but they are only implemented by themselves */
        for (int i = 0; i < 10; i++) {
            typeIdTable[i] = lf.literalOf(typeIdStructType, 
                Map.of(
                    members.get(0), lf.literalOf((UnsignedIntegerType)members.get(0).getType(), i),
                    members.get(1), lf.literalOf((UnsignedIntegerType)members.get(1).getType(), i),
                    members.get(2), lf.literalOf((UnsignedIntegerType)members.get(2).getType(), 0),  /* Set super for prims to posion */
                    members.get(3), lf.literalOf(interfaceBitsType, primitivesInterfaceBits),
                    members.get(4), lf.literalOf((UnsignedIntegerType)members.get(4).getType(), 0)  /* no flags for prims */
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
                    members.get(3), lf.literalOf(interfaceBitsType, convertByteArrayToValuesList(lf, getImplementedInterfaceBits(vtd))),
                    members.get(4), calculateTypeIdFlags((UnsignedIntegerType)members.get(4).getType(), vtd)
                )
            );
        }
        Literal typeIdsValue = ctxt.getLiteralFactory().literalOf((ArrayType)typeIdArrayGlobal.getType(), List.of(typeIdTable));
        
        /* Write the data into Object's section */
        Section section = ctxt.getImplicitSection(jlo);
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
            Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
            section.declareData(null, typeIdArrayGlobal.getName(), typeIdArrayGlobal.getType());
        }
        return typeIdArrayGlobal;
    }

    /**
     * Get the CompoundType for the GlobalTypeIdArray (`qbicc_typeid_array`)
     * global variable elements.
     * 
     * See #emitTypeIdTable for the definition of the typeid array elements.
     * 
     * @return  The CompoundType describing the struct.
     */
    public CompoundType getGlobalTypeIdStructType() {
        Assert.assertNotNull(typeIdStructType);
        return typeIdStructType;
    }

    public int get_number_of_typeids() {
        Assert.assertTrue(idAndRange.typeid_index == (typeids.size() + 10));
        supersLog.debug("get_highest_typeid == " + (typeids.size() + 10));
        return typeids.size() + 10; // invalid zero + 8 prims + void
    }

    /**
     * Get the GlobalVariableElement reference to the `qbicc_clinit_states`.
     * 
     * As part of it getting it, ensure a reference to it has been recorded into
     * the ExecutableElement's section.
     * 
     * @param originalElement the original element (must not be {@code null})
     * @return the clinitStates global
     */
    public GlobalVariableElement getAndRegisterGlobalClinitStateStruct(ExecutableElement originalElement) {
        Assert.assertNotNull(clinitStatesGlobal);
        if (!clinitStatesGlobal.getEnclosingType().equals(originalElement.getEnclosingType())) {
            Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
            section.declareData(null, clinitStatesGlobal.getName(), clinitStatesGlobal.getType());
        }
        return clinitStatesGlobal;
    }

    public void defineClinitStatesGlobal(LoadedTypeDefinition jlo) {
        // Structure will be laid out as two inline arrays:
        // i8[] initStatus
        // Function[] clinit_function_ptrs

        TypeSystem ts = ctxt.getTypeSystem();
        final int numElements = get_number_of_typeids();

        // Sleazy way to get the function type for an Initializer
        FunctionType clinit_function_type = null;
        for (LoadedTypeDefinition ltd : typeids.keySet()) {
            InitializerElement ie = ltd.getInitializer();
            if (ie != null) {
                clinit_function_type = ctxt.getFunctionTypeForElement(ie);
                break;
            }
        }

        ArrayType init_state_t = ts.getArrayType(ts.getUnsignedInteger8Type(), numElements);
        ArrayType class_initializers_t = ts.getArrayType(clinit_function_type.getPointer(), numElements);
        CompoundType clinit_state_t =  CompoundType.builder(ts)
            .setTag(CompoundType.Tag.STRUCT)
            .setName("qbicc_clinit_state")
            .setOverallAlignment(ts.getPointerAlignment())
            .addNextMember("init_state", init_state_t)
            .addNextMember("class_initializers", class_initializers_t)
            .build();

        // create a GlobalVariable for shared access to the clinitStates struct
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder(GLOBAL_CLINIT_STATES_STRUCT, BaseTypeDescriptor.V);
        builder.setType(clinit_state_t);
        builder.setEnclosingType(jlo);
        builder.setSignature(BaseTypeSignature.V);
        clinitStatesGlobal = builder.build();
    }

    public void emitClinitStateTable(LoadedTypeDefinition jlo) {
        // Structure will be laid out as two inline arrays:
        // i8[] initStatus
        // Function[] clinit_function_ptrs
        final int numElements = get_number_of_typeids();

        CompoundType clinit_state_t = (CompoundType)clinitStatesGlobal.getType();

        Section section = ctxt.getImplicitSection(jlo);
        LiteralFactory lf = ctxt.getLiteralFactory();
        List<Literal> init_state_literals = new ArrayList<>();
        List<Literal> class_initializers_literals = new ArrayList<>();
        Literal uninitialized = lf.literalOf(0);
        Literal initialized = lf.literalOf(1);
        Literal nullInitializer = lf.zeroInitializerLiteralOfType(clinit_state_t.getMember("class_initializers").getType());
        ReachabilityInfo info = ReachabilityInfo.get(ctxt);
         // poison
        init_state_literals.add(uninitialized);
        class_initializers_literals.add(nullInitializer);
        // primitives
        for (int i = 1; i < 10; i++) {
            init_state_literals.add(initialized);
            class_initializers_literals.add(nullInitializer);
        }
        // real types
        typeids.entrySet().stream()
            .sorted((a, b) -> a.getValue().typeid - b.getValue().typeid)
            .forEach(es -> {            
                LoadedTypeDefinition ltd = es.getKey();
                Literal init_state = initialized;
                Literal initializer = nullInitializer;
                if (!isAlreadyInitialized(ltd)) {
                    init_state = uninitialized;
                    if (info.isInitializedType(ltd)) {
                        InitializerElement ie = ltd.getInitializer();
                        if (ie != null && ie.hasMethodBody() && ctxt.wasEnqueued(ie)) {
                            FunctionType funType = ctxt.getFunctionTypeForElement(ie);
                            Function impl = ctxt.getExactFunction(ie);
                            if (!ie.getEnclosingType().load().equals(jlo)) {
                                section.declareFunction(ie, impl.getName(), funType);
                            }
                            initializer = ctxt.getLiteralFactory().literalOf(impl);
                        }
                    }
                }
                init_state_literals.add(init_state);
                class_initializers_literals.add(initializer);
            }
        );
        Assert.assertTrue(init_state_literals.size() == numElements);
  
        ArrayType init_states = (ArrayType)clinit_state_t.getMember("init_state").getType();
        ArrayType class_initializers = (ArrayType)clinit_state_t.getMember("class_initializers").getType();

        Literal clinit_states = lf.literalOf(clinit_state_t, 
            Map.of(
                clinit_state_t.getMember(0), lf.literalOf(init_states, init_state_literals),
                clinit_state_t.getMember(1), lf.literalOf(class_initializers, class_initializers_literals)
            )
        );
        
        /* Write the data into Object's section */
        section.addData(null, GLOBAL_CLINIT_STATES_STRUCT, clinit_states);
    }

    // TODO: implement this for real
    boolean isAlreadyInitialized(LoadedTypeDefinition ltd) {
        if (ltd.internalNameEquals("org/qbicc/runtime/main/VMHelpers$ClinitState")) { 
            return true;
        }
        if (ltd.internalNameEquals("org/qbicc/runtime/main/ObjectModel")) { 
            return true;
        }
        return false;
    }
}

