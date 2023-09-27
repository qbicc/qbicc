package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * A thing that can be directly serialized to a WASM stream.
 */
public interface WasmSerializable {
    /**
     * Serialize this item.
     *
     * @param wos the output stream (must not be {@code null})
     * @param encoder the encoder (must not be {@code null})
     * @throws IOException if an error occurs while writing
     */
    void writeTo(WasmOutputStream wos, Encoder encoder) throws IOException;
}
