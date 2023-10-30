package org.qbicc.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jboss.logging.Logger;
import org.qbicc.context.DiagnosticContext;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.type.TypeSystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

public class PlatformTypeSystemLoader {

    public enum ReferenceType {
        POINTER(void_p, "pointer"),
        INT64(int64_t, "int64"),
        INT32(int32_t, "int32");

        private final CProbe.Type type;
        private final String jsonField;

        ReferenceType(CProbe.Type type, String jsonField) {
            this.type = type;
            this.jsonField = jsonField;
        }
    }

    private static final Logger log = Logger.getLogger("org.qbicc.main.PlatformTypeSystemLoader");

    private static final CProbe.Type char_t = CProbe.Type.builder().setName("char").build();
    private static final CProbe.Type int8_t = CProbe.Type.builder().setName("int8_t").build();
    private static final CProbe.Type int16_t = CProbe.Type.builder().setName("int16_t").build();
    private static final CProbe.Type int32_t = CProbe.Type.builder().setName("int32_t").build();
    private static final CProbe.Type int64_t = CProbe.Type.builder().setName("int64_t").build();
    private static final CProbe.Type float_t = CProbe.Type.builder().setName("float").build();
    private static final CProbe.Type double_t = CProbe.Type.builder().setName("double").build();
    private static final CProbe.Type _Bool = CProbe.Type.builder().setName("_Bool").build();
    private static final CProbe.Type void_p = CProbe.Type.builder().setName("void *").build();
    private static final CProbe.Type max_align_t = CProbe.Type.builder().setName("max_align_t").build();

    private final Platform platform;
    private final CToolChain toolChain;
    private final ObjectFileProvider objectFileProvider;
    private final DiagnosticContext initialContext;
    private final ReferenceType referenceType;
    private final boolean smallTypeIds;


    public PlatformTypeSystemLoader(
        Platform platform,
        CToolChain toolChain,
        ObjectFileProvider objectFileProvider,
        DiagnosticContext initialContext,
        ReferenceType referenceType,
        boolean smallTypeIds) {
        this.platform = platform;
        this.toolChain = toolChain;
        this.objectFileProvider = objectFileProvider;
        this.initialContext = initialContext;
        this.smallTypeIds = smallTypeIds;
        this.referenceType = referenceType;
    }

    private static CProbe makeProbe() {
        CProbe.Builder probeBuilder = CProbe.builder();
        probeBuilder.include("<stdint.h>");
        probeBuilder.include("<limits.h>");
        // size and signedness of char
        probeBuilder.probeType(char_t);
        // int sizes
        probeBuilder.probeType(int8_t);
        probeBuilder.probeType(int16_t);
        probeBuilder.probeType(int32_t);
        probeBuilder.probeType(int64_t);
        // float sizes
        probeBuilder.probeType(float_t);
        probeBuilder.probeType(double_t);
        // bool
        probeBuilder.probeType(_Bool);
        // pointer
        probeBuilder.probeType(void_p);
        // number of bits in char
        probeBuilder.probeConstant("CHAR_BIT");
        // max alignment
        probeBuilder.probeType(max_align_t);
        // execute
        return probeBuilder.build();
    }

    TypeSystem load() throws IOException {
        TypeSystem typeSystem = this.fromYaml();
        if (typeSystem == null) {
            if (toolChain == null) {
                log.errorf("Cannot determine target type information for platform %s", platform);
                return null;
            }
            log.warnf("Failed to load type info for platform %s, using probe.", platform);
            // no such manifest: use a probe
            typeSystem = this.fromProbe();
        }
        return typeSystem;
    }

    TypeSystem fromProbe() throws IOException {
        CProbe probe = makeProbe();
        CProbe.Result probeResult = probe.run(toolChain, objectFileProvider, initialContext);
        if (probeResult == null) {
            initialContext.error("Type system probe compiler execution failed");
        } else {
            long charSize = probeResult.getTypeInfo(char_t).getSize();
            if (charSize != 1) {
                initialContext.error("Unexpected size of `char`: %d", Long.valueOf(charSize));
            }
            TypeSystem.Builder tsBuilder = TypeSystem.builder();
            tsBuilder.setBoolSize((int) probeResult.getTypeInfo(_Bool).getSize());
            tsBuilder.setBoolAlignment((int) probeResult.getTypeInfo(_Bool).getAlign());
            tsBuilder.setByteBits(probeResult.getConstantInfo("CHAR_BIT").getValueAsInt());
            tsBuilder.setSignedChar(probeResult.getTypeInfo(char_t).isSigned());
            tsBuilder.setInt8Size((int) probeResult.getTypeInfo(int8_t).getSize());
            tsBuilder.setInt8Alignment((int) probeResult.getTypeInfo(int8_t).getAlign());
            tsBuilder.setInt16Size((int) probeResult.getTypeInfo(int16_t).getSize());
            tsBuilder.setInt16Alignment((int) probeResult.getTypeInfo(int16_t).getAlign());
            tsBuilder.setInt32Size((int) probeResult.getTypeInfo(int32_t).getSize());
            tsBuilder.setInt32Alignment((int) probeResult.getTypeInfo(int32_t).getAlign());
            tsBuilder.setInt64Size((int) probeResult.getTypeInfo(int64_t).getSize());
            tsBuilder.setInt64Alignment((int) probeResult.getTypeInfo(int64_t).getAlign());
            tsBuilder.setFloat32Size((int) probeResult.getTypeInfo(float_t).getSize());
            tsBuilder.setFloat32Alignment((int) probeResult.getTypeInfo(float_t).getAlign());
            tsBuilder.setFloat64Size((int) probeResult.getTypeInfo(double_t).getSize());
            tsBuilder.setFloat64Alignment((int) probeResult.getTypeInfo(double_t).getAlign());
            tsBuilder.setPointerSize((int) probeResult.getTypeInfo(void_p).getSize());
            tsBuilder.setPointerAlignment((int) probeResult.getTypeInfo(void_p).getAlign());
            tsBuilder.setMaxAlignment((int) probeResult.getTypeInfo(max_align_t).getAlign());
            // todo: function alignment probe
            tsBuilder.setReferenceSize((int) probeResult.getTypeInfo(referenceType.type).getSize());
            tsBuilder.setReferenceAlignment((int) probeResult.getTypeInfo(referenceType.type).getAlign());
            CProbe.Type type_id_type = smallTypeIds ? int16_t : int32_t;
            tsBuilder.setTypeIdSize((int) probeResult.getTypeInfo(type_id_type).getSize());
            tsBuilder.setTypeIdAlignment((int) probeResult.getTypeInfo(type_id_type).getAlign());
            tsBuilder.setEndianness(probeResult.getByteOrder());
            // todo: compressed refs
            tsBuilder.setReferenceSize(tsBuilder.getPointerSize());
            tsBuilder.setReferenceAlignment(tsBuilder.getPointerAlignment());

            return tsBuilder.build();
        }
        return null;
    }

    TypeSystem fromYaml() throws IOException {
        String searchName = platform.cpu() + "-" + platform.os() + "-" + platform.abi();
        String fpath = "bundles/" + searchName + "/platform-type-info.yaml";
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(fpath);
        if (stream == null) {
            searchName = platform.cpu() + "-" + platform.os();
            fpath = "bundles/" + searchName + "/platform-type-info.yaml";
            stream = this.getClass().getClassLoader().getResourceAsStream(fpath);
        }
        if (stream == null) return null;
        JsonNode jsonNode = new ObjectMapper(new YAMLFactory()).readTree(stream);
        JsonNode platformTypeInfo = jsonNode.get("platform-type-info");

        TypeSystem.Builder tsBuilder = TypeSystem.builder();

        JsonNode boolean_t = platformTypeInfo.get("boolean");
        tsBuilder.setBoolSize(boolean_t.get("size").asInt());
        tsBuilder.setBoolAlignment(boolean_t.get("align").asInt());

        tsBuilder.setByteBits(platformTypeInfo.get("byte-bits").asInt());

        tsBuilder.setSignedChar(platformTypeInfo.get("signed-char").asBoolean());

        JsonNode int8_t = platformTypeInfo.get("int8");
        tsBuilder.setInt8Size(int8_t.get("size").asInt());
        tsBuilder.setInt8Alignment(int8_t.get("align").asInt());

        JsonNode int16_t = platformTypeInfo.get("int16");
        tsBuilder.setInt16Size(int16_t.get("size").asInt());
        tsBuilder.setInt16Alignment(int16_t.get("align").asInt());

        JsonNode int32_t = platformTypeInfo.get("int32");
        tsBuilder.setInt32Size(int32_t.get("size").asInt());
        tsBuilder.setInt32Alignment(int32_t.get("align").asInt());

        JsonNode int64_t = platformTypeInfo.get("int64");
        tsBuilder.setInt64Size(int64_t.get("size").asInt());
        tsBuilder.setInt64Alignment(int64_t.get("align").asInt());

        JsonNode float32_t = platformTypeInfo.get("float32");
        tsBuilder.setFloat32Size(float32_t.get("size").asInt());
        tsBuilder.setFloat32Alignment(float32_t.get("align").asInt());

        JsonNode float64_t = platformTypeInfo.get("float64");
        tsBuilder.setFloat64Size(float64_t.get("size").asInt());
        tsBuilder.setFloat64Alignment(float64_t.get("align").asInt());

        JsonNode pointer_t = platformTypeInfo.get("pointer");
        tsBuilder.setPointerSize(pointer_t.get("size").asInt());
        tsBuilder.setPointerAlignment(pointer_t.get("align").asInt());

        tsBuilder.setMaxAlignment(platformTypeInfo.get("max-alignment").asInt());

        JsonNode reference_t = platformTypeInfo.get(referenceType.jsonField);
        tsBuilder.setReferenceSize(reference_t.get("size").asInt());
        tsBuilder.setReferenceAlignment(reference_t.get("align").asInt());

        JsonNode type_id_t = smallTypeIds? int16_t : int32_t;
        tsBuilder.setTypeIdSize(type_id_t.get("size").asInt());
        tsBuilder.setTypeIdAlignment(type_id_t.get("align").asInt());

        tsBuilder.setEndianness(endianness(platformTypeInfo.get("endian").asText()));

        tsBuilder.setReferenceSize(tsBuilder.getPointerSize());
        tsBuilder.setReferenceAlignment(tsBuilder.getPointerAlignment());

        return tsBuilder.build();
    }

    ByteOrder endianness(String endianness) {
        return switch (endianness) {
            case "little" -> ByteOrder.LITTLE_ENDIAN;
            case "big" -> ByteOrder.BIG_ENDIAN;
            default -> throw new IllegalArgumentException(endianness);
        };
    }
}

