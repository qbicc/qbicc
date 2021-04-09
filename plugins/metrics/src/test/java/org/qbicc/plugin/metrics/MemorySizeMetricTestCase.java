package org.qbicc.plugin.metrics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class MemorySizeMetricTestCase {
    @Test
    public void testFormatting() {
        MemorySizeMetric test = new MemorySizeMetric("test", null);
        assertEquals("0B", test.getFormattedValue());
        test.add(1024);
        assertEquals("1K", test.getFormattedValue());
        test.add(100);
        assertEquals("1.1K", test.getFormattedValue());
        test.add(10);
        assertEquals("1.11K", test.getFormattedValue());
        test.add(10000);
        assertEquals("10.894K", test.getFormattedValue());
        test.add(1 << 30);
        assertEquals("1G", test.getFormattedValue());
    }
}
