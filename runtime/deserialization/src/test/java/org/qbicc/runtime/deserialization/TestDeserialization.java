package org.qbicc.runtime.deserialization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class TestDeserialization {

    static MockObjectDeserializer mock = new MockObjectDeserializer();
    static ByteOrder endianness = DeserializationBuffer.LE ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;

    static class Test1 {
        int a;
        Object b;
        Test1() {}
    }

    static final class Test2 extends Test1 {
        Object c;
        Test2() {}
    }


    void writeStringL1(String str, ByteBuffer out) {
        if (str.length() < 20) { // artificially small for testing. Actually support up to 255.
            out.put(SerializationConstants.STRING_SMALL_L1);
            out.put((byte)str.getBytes(StandardCharsets.ISO_8859_1).length);
        } else {
            out.put(SerializationConstants.STRING_LARGE_L1);
            out.putInt(str.getBytes(StandardCharsets.ISO_8859_1).length);
        }
        out.put(str.getBytes(StandardCharsets.ISO_8859_1));
    }

    void writeStringU16(String str, ByteBuffer out) {
        if (str.length() < 20) { // artificially small for testing. Actually support up to 255.
            out.put(SerializationConstants.STRING_SMALL_U16);
            out.put((byte)(str.getBytes(StandardCharsets.UTF_16BE).length/2));
        } else {
            out.put(SerializationConstants.STRING_LARGE_U16);
            out.putInt(str.getBytes(StandardCharsets.UTF_16BE).length/2);
        }
        out.put(str.getBytes(StandardCharsets.UTF_16BE));
    }

    byte[] toArray(ByteBuffer buf) {
        byte[] ans = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, ans, 0, buf.position());
        return ans;
    }

    @Test
    public void testStrings() {
        ByteBuffer out = ByteBuffer.wrap(new byte[256]).order(endianness);

        String hello = "Hello World!";
        String ciao = "See you later alligator";
        writeStringL1(hello, out);
        writeStringU16(ciao, out);

        Deserializer ds = new Deserializer(toArray(out), 2, mock);
        ObjectGraph graph = ds.readAll();

        assertEquals(graph.getObject(0), hello);
        assertEquals(graph.getObject(1), ciao);
    }

    @Test
    public void testCell() {
        ByteBuffer out = ByteBuffer.wrap(new byte[256]).order(endianness);

        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST1);
        out.putInt(42);
        out.put(SerializationConstants.NULL);

        Deserializer ds = new Deserializer(toArray(out), 1, mock);
        ObjectGraph graph = ds.readAll();

        Test1 obj = (Test1) graph.getObject(0);
        assertEquals(42, obj.a);
        assertNull(obj.b);
    }

    @Test
    void testLinkedList() {
        ByteBuffer out = ByteBuffer.wrap(new byte[256]).order(endianness);

        // Linked list containing first 3 primes
        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST1);
        out.putInt(2);
        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST1);
        out.putInt(3);
        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST1);
        out.putInt(5);
        out.put(SerializationConstants.NULL);

        Deserializer ds = new Deserializer(toArray(out), 3, mock);
        ObjectGraph graph = ds.readAll();

        assertEquals(2, ((Test1) graph.getObject(0)).a);
        assertEquals(graph.getObject(1), ((Test1) graph.getObject(0)).b);
        assertEquals(3, ((Test1) graph.getObject(1)).a);
        assertEquals(graph.getObject(2), ((Test1) graph.getObject(1)).b);
        assertEquals(5, ((Test1) graph.getObject(2)).a);
        assertNull(((Test1) graph.getObject(2)).b);
    }

    @Test
    void testCyclicList() {
        ByteBuffer out = ByteBuffer.wrap(new byte[256]).order(endianness);

        // Cyclic linked list containing first 3 primes
        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST1);
        out.putInt(2);
        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST1);
        out.putInt(3);
        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST1);
        out.putInt(5);
        out.put((byte)(SerializationConstants.TINY_REF_TAG_BIT | 3));

        Deserializer ds = new Deserializer(toArray(out), 3, mock);
        ObjectGraph graph = ds.readAll();

        assertEquals(2, ((Test1) graph.getObject(0)).a);
        assertEquals(graph.getObject(1), ((Test1) graph.getObject(0)).b);
        assertEquals(3, ((Test1) graph.getObject(1)).a);
        assertEquals(graph.getObject(2), ((Test1) graph.getObject(1)).b);
        assertEquals(5, ((Test1) graph.getObject(2)).a);
        assertEquals(graph.getObject(0), ((Test1) graph.getObject(2)).b);
    }

    @Test
    void testSubclass() {
        ByteBuffer out = ByteBuffer.wrap(new byte[256]).order(endianness);

        // Cyclic linked list containing first 3 primes;  first & third cells are subclass that add self-pointer and String to the "c" field
        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST2);
        out.putInt(2);
        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST1);
        out.putInt(3);
        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST2);
        out.putInt(5);
        out.put((byte)(SerializationConstants.TINY_REF_TAG_BIT | 3));
        String hello = "Hello World!";
        writeStringU16(hello, out);

        // Now, finally back to the "c" field of the first cell using a SMALL instead of TINY just for testing..
        out.put(SerializationConstants.BACKREF_SMALL);
        out.putShort((short)4);

        Deserializer ds = new Deserializer(toArray(out), 4, mock);
        ObjectGraph graph = ds.readAll();

        assertEquals(2, ((Test1) graph.getObject(0)).a);
        assertEquals(graph.getObject(1), ((Test1) graph.getObject(0)).b);
        assertEquals(graph.getObject(0), ((Test2) graph.getObject(0)).c);
        assertEquals(3, ((Test1) graph.getObject(1)).a);
        assertEquals(graph.getObject(2), ((Test1) graph.getObject(1)).b);
        assertEquals(5, ((Test1) graph.getObject(2)).a);
        assertEquals(graph.getObject(0), ((Test1) graph.getObject(2)).b);
        assertEquals(hello, ((Test2) graph.getObject(2)).c);
    }

    @Test
    void testObjectArray() {
        ByteBuffer out = ByteBuffer.wrap(new byte[256]).order(endianness);

        out.put(SerializationConstants.ARRAY_SMALL_OBJECT);
        out.put((byte)4);
        writeStringL1("Eureka; I think we got it", out);
        out.put((byte)(SerializationConstants.TINY_REF_TAG_BIT | 2));
        out.put(SerializationConstants.OBJECT);
        out.putShort((short)MockObjectDeserializer.TYPEID_TEST1);
        out.putInt(3);
        out.put((byte)(SerializationConstants.TINY_REF_TAG_BIT | 1));
        writeStringU16("QED", out);

        Deserializer ds = new Deserializer(toArray(out), 4, mock);
        ObjectGraph graph = ds.readAll();

        Object[] spine = (Object[])graph.getObject(0);
        assertEquals(spine.length, 4);
        assertEquals("Eureka; I think we got it", spine[0]);
        assertEquals(spine, spine[1]);
        assertEquals(spine[2], ((Test1)spine[2]).b);
        assertEquals("QED", spine[3]);
    }
}
