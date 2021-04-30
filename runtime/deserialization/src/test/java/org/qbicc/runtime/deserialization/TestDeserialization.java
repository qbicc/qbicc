package org.qbicc.runtime.deserialization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TestDeserialization {

    static MockObjectDeserializer mock = new MockObjectDeserializer();

    static class Test1 {
        int a;
        Object b;
        Test1() {}
    }

    static final class Test2 extends Test1 {
        Object c;
        Test2() {}
    }


    void writeStringL1(String str, DataOutputStream out) throws IOException {
        if (str.length() < 20) { // artificially small for testing. Actually support up to 255.
            out.write(SerializationConstants.STRING_SMALL_L1);
            out.writeByte(str.getBytes(StandardCharsets.ISO_8859_1).length);
        } else {
            out.write(SerializationConstants.STRING_LARGE_L1);
            out.writeInt(str.getBytes(StandardCharsets.ISO_8859_1).length);
        }
        out.write(str.getBytes(StandardCharsets.ISO_8859_1));
    }

    void writeStringU16(String str, DataOutputStream out) throws IOException {
        if (str.length() < 20) { // artificially small for testing. Actually support up to 255.
            out.write(SerializationConstants.STRING_SMALL_U16);
            out.writeByte(str.getBytes(StandardCharsets.UTF_16).length/2);
        } else {
            out.write(SerializationConstants.STRING_LARGE_U16);
            out.writeInt(str.getBytes(StandardCharsets.UTF_16).length/2);
        }
        out.write(str.getBytes(StandardCharsets.UTF_16));
    }

    @Test
    public void testStrings() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(os);

        String hello = "Hello World!";
        String ciao = "See you later alligator";
        writeStringL1(hello, out);
        writeStringU16(ciao, out);

        out.flush();
        Deserializer ds = new Deserializer(ByteBuffer.wrap(os.toByteArray()), mock);
        ObjectGraph graph = ds.readAll();

        assertEquals(graph.getObject(0), hello);
        assertEquals(graph.getObject(1), ciao);
    }

    @Test
    public void testCell() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(os);

        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST1);
        out.writeInt(42);
        out.writeByte(SerializationConstants.NULL);

        out.flush();
        Deserializer ds = new Deserializer(ByteBuffer.wrap(os.toByteArray()), mock);
        ObjectGraph graph = ds.readAll();

        Test1 obj = (Test1) graph.getObject(0);
        assertEquals(42, obj.a);
        assertNull(obj.b);
    }

    @Test
    void testLinkedList() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(os);

        // Linked list containing first 3 primes
        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST1);
        out.writeInt(2);
        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST1);
        out.writeInt(3);
        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST1);
        out.writeInt(5);
        out.writeByte(SerializationConstants.NULL);

        out.flush();
        Deserializer ds = new Deserializer(ByteBuffer.wrap(os.toByteArray()), mock);
        ObjectGraph graph = ds.readAll();

        assertEquals(2, ((Test1) graph.getObject(0)).a);
        assertEquals(graph.getObject(1), ((Test1) graph.getObject(0)).b);
        assertEquals(3, ((Test1) graph.getObject(1)).a);
        assertEquals(graph.getObject(2), ((Test1) graph.getObject(1)).b);
        assertEquals(5, ((Test1) graph.getObject(2)).a);
        assertNull(((Test1) graph.getObject(2)).b);
    }

    @Test
    void testCyclicList() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(os);

        // Cyclic linked list containing first 3 primes
        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST1);
        out.writeInt(2);
        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST1);
        out.writeInt(3);
        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST1);
        out.writeInt(5);
        out.writeByte(SerializationConstants.TINY_REF_TAG_BIT | 3);

        out.flush();
        Deserializer ds = new Deserializer(ByteBuffer.wrap(os.toByteArray()), mock);
        ObjectGraph graph = ds.readAll();

        assertEquals(2, ((Test1) graph.getObject(0)).a);
        assertEquals(graph.getObject(1), ((Test1) graph.getObject(0)).b);
        assertEquals(3, ((Test1) graph.getObject(1)).a);
        assertEquals(graph.getObject(2), ((Test1) graph.getObject(1)).b);
        assertEquals(5, ((Test1) graph.getObject(2)).a);
        assertEquals(graph.getObject(0), ((Test1) graph.getObject(2)).b);
    }

    @Test
    void testSubclass() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(os);

        // Cyclic linked list containing first 3 primes;  first & third cells are subclass that add self-pointer and String to the "c" field
        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST2);
        out.writeInt(2);
        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST1);
        out.writeInt(3);
        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST2);
        out.writeInt(5);
        out.writeByte(SerializationConstants.TINY_REF_TAG_BIT | 3);
        String hello = "Hello World!";
        writeStringU16(hello, out);

        // Now, finally back to the "c" field of the first cell using a SMALL instead of TINY just for testing..
        out.writeByte(SerializationConstants.BACKREF_SMALL);
        out.writeShort(4);

        out.flush();
        Deserializer ds = new Deserializer(ByteBuffer.wrap(os.toByteArray()), mock);
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
    void testObjectArray() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(os);

        out.writeByte(SerializationConstants.ARRAY_SMALL_OBJECT);
        out.writeByte(4);
        writeStringL1("Eureka; I think we got it", out);
        out.writeByte(SerializationConstants.TINY_REF_TAG_BIT | 2);
        out.writeByte(SerializationConstants.OBJECT);
        out.writeShort(MockObjectDeserializer.TYPEID_TEST1);
        out.writeInt(3);
        out.writeByte(SerializationConstants.TINY_REF_TAG_BIT | 1);
        writeStringU16("QED", out);

        out.flush();
        Deserializer ds = new Deserializer(ByteBuffer.wrap(os.toByteArray()), mock);
        ObjectGraph graph = ds.readAll();

        Object[] spine = (Object[])graph.getObject(0);
        assertEquals(spine.length, 4);
        assertEquals("Eureka; I think we got it", spine[0]);
        assertEquals(spine, spine[1]);
        assertEquals(spine[2], ((Test1)spine[2]).b);
        assertEquals("QED", spine[3]);
    }
}
