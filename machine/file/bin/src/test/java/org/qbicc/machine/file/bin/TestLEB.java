package org.qbicc.machine.file.bin;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for LEB encoding.
 */
public final class TestLEB {

    @Test
    public void testEdgeCases() throws IOException {
        int[] testValues = new int[] {
            0, 1, 63, 64, 127, 128, 129, 254, 255, 256, 257, 510, 511, 512, 513, 16383, 16384, 16385,
            -1, -29, -127, -128, -129, -254, -512
        };
        try (BinaryOutput output = BinaryOutput.temporary(input -> {
            for (int testValue : testValues) {
                Assertions.assertEquals(testValue, input.uleb32());
            }
        })) {
            for (int testValue : testValues) {
                output.uleb(testValue);
            }
        }
        try (BinaryOutput output = BinaryOutput.temporary(input -> {
            for (int testValue : testValues) {
                Assertions.assertEquals(testValue, input.sleb32());
            }
        })) {
            for (int testValue : testValues) {
                output.sleb(testValue);
            }
        }
        try (BinaryOutput output = BinaryOutput.temporary(input -> {
            for (int testValue : testValues) {
                Assertions.assertEquals(testValue, input.uleb64());
            }
        })) {
            for (int testValue : testValues) {
                output.uleb((long)testValue);
            }
        }
        try (BinaryOutput output = BinaryOutput.temporary(input -> {
            for (int testValue : testValues) {
                Assertions.assertEquals(testValue, input.sleb64());
            }
        })) {
            for (int testValue : testValues) {
                output.sleb((long)testValue);
            }
        }
        try (BinaryOutput output = BinaryOutput.temporary(input -> {
            for (int testValue : testValues) {
                Assertions.assertEquals(new I128(testValue, 0), input.uleb128());
            }
        })) {
            for (int testValue : testValues) {
                output.uleb(new I128(testValue, 0));
            }
        }
        try (BinaryOutput output = BinaryOutput.temporary(input -> {
            for (int testValue : testValues) {
                Assertions.assertEquals(new I128(testValue), input.sleb128());
            }
        })) {
            for (int testValue : testValues) {
                output.sleb(new I128(testValue));
            }
        }
    }
}
