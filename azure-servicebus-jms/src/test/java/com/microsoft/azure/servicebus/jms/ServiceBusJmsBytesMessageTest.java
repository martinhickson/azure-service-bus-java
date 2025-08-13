// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import javax.jms.Session;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ServiceBusJmsBytesMessage.
 * 
 * This class tests the binary message operations that were previously untested,
 * including read/write operations, type conversions, buffer management, and edge cases.
 */
public class ServiceBusJmsBytesMessageTest {

    private ServiceBusJmsBytesMessage bytesMessage;

    @Mock
    private ServiceBusJmsSession mockSession;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        // Mock session to not be closed
        when(mockSession.isClosed()).thenReturn(false);
        
        bytesMessage = new ServiceBusJmsBytesMessage(mockSession);
    }

    // ========== Basic Write/Read Operations Tests ==========

    @Test
    public void testWriteBoolean_ThenReadBoolean() throws Exception {
        // Given
        boolean testValue = true;

        // When
        bytesMessage.writeBoolean(testValue);
        bytesMessage.reset(); // Switch to read mode

        // Then
        assertThat(bytesMessage.readBoolean()).isEqualTo(testValue);
    }

    @Test
    public void testWriteByte_ThenReadByte() throws Exception {
        // Given
        byte testValue = 42;

        // When
        bytesMessage.writeByte(testValue);
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readByte()).isEqualTo(testValue);
    }

    @Test
    public void testWriteShort_ThenReadShort() throws Exception {
        // Given
        short testValue = 1234;

        // When
        bytesMessage.writeShort(testValue);
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readShort()).isEqualTo(testValue);
    }

    @Test
    public void testWriteChar_ThenReadChar() throws Exception {
        // Given
        char testValue = 'A';

        // When
        bytesMessage.writeChar(testValue);
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readChar()).isEqualTo(testValue);
    }

    @Test
    public void testWriteInt_ThenReadInt() throws Exception {
        // Given
        int testValue = 123456;

        // When
        bytesMessage.writeInt(testValue);
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readInt()).isEqualTo(testValue);
    }

    @Test
    public void testWriteLong_ThenReadLong() throws Exception {
        // Given
        long testValue = 123456789L;

        // When
        bytesMessage.writeLong(testValue);
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readLong()).isEqualTo(testValue);
    }

    @Test
    public void testWriteFloat_ThenReadFloat() throws Exception {
        // Given
        float testValue = 3.14159f;

        // When
        bytesMessage.writeFloat(testValue);
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readFloat()).isCloseTo(testValue, within(0.0001f));
    }

    @Test
    public void testWriteDouble_ThenReadDouble() throws Exception {
        // Given
        double testValue = 3.141592653589793;

        // When
        bytesMessage.writeDouble(testValue);
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readDouble()).isCloseTo(testValue, within(0.000000001));
    }

    @Test
    public void testWriteUTF_ThenReadUTF() throws Exception {
        // Given
        String testValue = "Hello Azure Service Bus! 🚀";

        // When
        bytesMessage.writeUTF(testValue);
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readUTF()).isEqualTo(testValue);
    }

    @Test
    public void testWriteBytes_ThenReadBytes() throws Exception {
        // Given
        byte[] testData = "Hello World!".getBytes();
        byte[] readBuffer = new byte[testData.length];

        // When
        bytesMessage.writeBytes(testData);
        bytesMessage.reset();

        // Then
        int bytesRead = bytesMessage.readBytes(readBuffer);
        assertThat(bytesRead).isEqualTo(testData.length);
        assertThat(readBuffer).isEqualTo(testData);
    }

    @Test
    public void testWriteBytes_WithOffsetAndLength() throws Exception {
        // Given
        byte[] sourceData = "Hello World!".getBytes();
        byte[] readBuffer = new byte[5];
        int offset = 6; // "World" starts at index 6
        int length = 5; // "World" length

        // When
        bytesMessage.writeBytes(sourceData, offset, length);
        bytesMessage.reset();

        // Then
        int bytesRead = bytesMessage.readBytes(readBuffer);
        assertThat(bytesRead).isEqualTo(5);
        assertThat(new String(readBuffer)).isEqualTo("World");
    }

    // ========== State Management Tests ==========

    @Test
    public void testReset_SwitchesToReadMode() throws Exception {
        // Given - write some data
        bytesMessage.writeInt(123);

        // When
        bytesMessage.reset();

        // Then - should be able to read but not write
        assertThat(bytesMessage.readInt()).isEqualTo(123);
    }

    @Test
    public void testClearBody_SwitchesToWriteMode() throws Exception {
        // Given - write and switch to read mode
        bytesMessage.writeInt(123);
        bytesMessage.reset();

        // When
        bytesMessage.clearBody();

        // Then - should be able to write again
        bytesMessage.writeInt(456);
        bytesMessage.reset();
        assertThat(bytesMessage.readInt()).isEqualTo(456);
    }

    @Test
    public void testGetBodyLength_AfterWriting() throws Exception {
        // Given
        bytesMessage.writeInt(123);
        bytesMessage.writeUTF("Hello");
        bytesMessage.reset(); // Switch to read mode

        // When
        long length = bytesMessage.getBodyLength();

        // Then - should include int (4 bytes) + UTF string length
        assertThat(length).isGreaterThan(4); // At least the int size
    }

    @Test
    public void testGetBodyLength_EmptyMessage() throws Exception {
        // Switch to read mode first
        bytesMessage.reset();
        
        // When
        long length = bytesMessage.getBodyLength();

        // Then
        assertThat(length).isEqualTo(0);
    }

    // ========== Error Condition Tests ==========

    @Test
    public void testReadBoolean_InWriteMode_ThrowsException() throws Exception {
        // Given - message is in write mode by default

        // When & Then
        assertThatThrownBy(() -> bytesMessage.readBoolean())
                .isInstanceOf(MessageNotReadableException.class);
    }

    @Test
    public void testWriteInt_InReadMode_ThrowsException() throws Exception {
        // Given - switch to read mode
        bytesMessage.reset();

        // When & Then
        assertThatThrownBy(() -> bytesMessage.writeInt(123))
                .isInstanceOf(MessageNotWriteableException.class);
    }

    @Test
    public void testReadBeyondEnd_ThrowsEOFException() throws Exception {
        // Given - write one byte and switch to read
        bytesMessage.writeByte((byte) 42);
        bytesMessage.reset();

        // When - read the byte, then try to read another
        bytesMessage.readByte();

        // Then - second read should throw EOF exception
        assertThatThrownBy(() -> bytesMessage.readByte())
                .isInstanceOf(MessageEOFException.class);
    }

    @Test
    public void testReadBytes_IntoNullBuffer_ThrowsException() throws Exception {
        // Given
        bytesMessage.writeBytes("test".getBytes());
        bytesMessage.reset();

        // When & Then
        assertThatThrownBy(() -> bytesMessage.readBytes(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testReadBytes_WithNegativeLength_ThrowsException() throws Exception {
        // Given
        bytesMessage.writeBytes("test".getBytes());
        bytesMessage.reset();
        byte[] buffer = new byte[10];

        // When & Then
        assertThatThrownBy(() -> bytesMessage.readBytes(buffer, -1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    // ========== Multiple Data Types Test ==========

    @Test
    public void testComplexDataMix_RoundTrip() throws Exception {
        // Given - write various data types
        bytesMessage.writeBoolean(true);
        bytesMessage.writeByte((byte) 42);
        bytesMessage.writeShort((short) 1234);
        bytesMessage.writeChar('A');
        bytesMessage.writeInt(123456);
        bytesMessage.writeLong(123456789L);
        bytesMessage.writeFloat(3.14f);
        bytesMessage.writeDouble(2.71828);
        bytesMessage.writeUTF("Hello World");
        bytesMessage.writeBytes("ByteData".getBytes());

        // When - switch to read mode
        bytesMessage.reset();

        // Then - read in same order
        assertThat(bytesMessage.readBoolean()).isTrue();
        assertThat(bytesMessage.readByte()).isEqualTo((byte) 42);
        assertThat(bytesMessage.readShort()).isEqualTo((short) 1234);
        assertThat(bytesMessage.readChar()).isEqualTo('A');
        assertThat(bytesMessage.readInt()).isEqualTo(123456);
        assertThat(bytesMessage.readLong()).isEqualTo(123456789L);
        assertThat(bytesMessage.readFloat()).isCloseTo(3.14f, within(0.01f));
        assertThat(bytesMessage.readDouble()).isCloseTo(2.71828, within(0.00001));
        assertThat(bytesMessage.readUTF()).isEqualTo("Hello World");
        
        byte[] byteBuffer = new byte[8];
        int bytesRead = bytesMessage.readBytes(byteBuffer);
        assertThat(bytesRead).isEqualTo(8);
        assertThat(new String(byteBuffer)).isEqualTo("ByteData");
    }

    // ========== Edge Cases and Boundaries ==========

    @Test
    public void testLargeByteArray_1KB() throws Exception {
        // Given
        byte[] largeData = new byte[1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        // When
        bytesMessage.writeBytes(largeData);
        bytesMessage.reset();

        // Then
        byte[] readData = new byte[1024];
        int totalRead = 0;
        int chunkSize = 100;
        
        while (totalRead < largeData.length) {
            byte[] chunk = new byte[chunkSize];
            int read = bytesMessage.readBytes(chunk);
            if (read == -1) break;
            
            System.arraycopy(chunk, 0, readData, totalRead, read);
            totalRead += read;
        }

        assertThat(totalRead).isEqualTo(largeData.length);
        assertThat(readData).isEqualTo(largeData);
    }

    @Test
    public void testEmptyUTFString() throws Exception {
        // Given
        String emptyString = "";

        // When
        bytesMessage.writeUTF(emptyString);
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readUTF()).isEqualTo(emptyString);
    }

    @Test
    public void testUnicodeCharacters() throws Exception {
        // Given
        String unicodeText = "Hello 世界 🌍 Мир";

        // When
        bytesMessage.writeUTF(unicodeText);
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readUTF()).isEqualTo(unicodeText);
    }

    @Test
    public void testZeroValues() throws Exception {
        // Given - write zero values
        bytesMessage.writeBoolean(false);
        bytesMessage.writeByte((byte) 0);
        bytesMessage.writeShort((short) 0);
        bytesMessage.writeChar('\0');
        bytesMessage.writeInt(0);
        bytesMessage.writeLong(0L);
        bytesMessage.writeFloat(0.0f);
        bytesMessage.writeDouble(0.0);

        // When
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readBoolean()).isFalse();
        assertThat(bytesMessage.readByte()).isEqualTo((byte) 0);
        assertThat(bytesMessage.readShort()).isEqualTo((short) 0);
        assertThat(bytesMessage.readChar()).isEqualTo('\0');
        assertThat(bytesMessage.readInt()).isEqualTo(0);
        assertThat(bytesMessage.readLong()).isEqualTo(0L);
        assertThat(bytesMessage.readFloat()).isEqualTo(0.0f);
        assertThat(bytesMessage.readDouble()).isEqualTo(0.0);
    }

    @Test
    public void testExtremeValues() throws Exception {
        // Given - write extreme values
        bytesMessage.writeByte(Byte.MAX_VALUE);
        bytesMessage.writeByte(Byte.MIN_VALUE);
        bytesMessage.writeShort(Short.MAX_VALUE);
        bytesMessage.writeShort(Short.MIN_VALUE);
        bytesMessage.writeInt(Integer.MAX_VALUE);
        bytesMessage.writeInt(Integer.MIN_VALUE);
        bytesMessage.writeLong(Long.MAX_VALUE);
        bytesMessage.writeLong(Long.MIN_VALUE);
        bytesMessage.writeFloat(Float.MAX_VALUE);
        bytesMessage.writeFloat(Float.MIN_VALUE);
        bytesMessage.writeDouble(Double.MAX_VALUE);
        bytesMessage.writeDouble(Double.MIN_VALUE);

        // When
        bytesMessage.reset();

        // Then
        assertThat(bytesMessage.readByte()).isEqualTo(Byte.MAX_VALUE);
        assertThat(bytesMessage.readByte()).isEqualTo(Byte.MIN_VALUE);
        assertThat(bytesMessage.readShort()).isEqualTo(Short.MAX_VALUE);
        assertThat(bytesMessage.readShort()).isEqualTo(Short.MIN_VALUE);
        assertThat(bytesMessage.readInt()).isEqualTo(Integer.MAX_VALUE);
        assertThat(bytesMessage.readInt()).isEqualTo(Integer.MIN_VALUE);
        assertThat(bytesMessage.readLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(bytesMessage.readLong()).isEqualTo(Long.MIN_VALUE);
        assertThat(bytesMessage.readFloat()).isEqualTo(Float.MAX_VALUE);
        assertThat(bytesMessage.readFloat()).isEqualTo(Float.MIN_VALUE);
        assertThat(bytesMessage.readDouble()).isEqualTo(Double.MAX_VALUE);
        assertThat(bytesMessage.readDouble()).isEqualTo(Double.MIN_VALUE);
    }

    // ========== JMS 2.0 Compatibility Tests ==========

    @Test
    public void testGetBody_ByteArrayClass() throws Exception {
        // Given
        byte[] testData = "Hello World".getBytes();
        bytesMessage.writeBytes(testData);

        // When
        byte[] result = bytesMessage.getBody(byte[].class);

        // Then
        assertThat(result).isEqualTo(testData);
    }

    @Test
    public void testGetBody_InvalidClass_ThrowsMessageFormatException() throws Exception {
        // Given
        bytesMessage.writeBytes("test".getBytes());

        // When & Then
        assertThatThrownBy(() -> bytesMessage.getBody(String.class))
                .isInstanceOf(MessageFormatException.class)
                .hasMessageContaining("BytesMessage body cannot be assigned to");
    }

    @Test
    public void testIsBodyAssignableTo_ByteArray() throws Exception {
        // Given
        bytesMessage.writeBytes("test".getBytes());

        // When & Then
        assertThat(bytesMessage.isBodyAssignableTo(byte[].class)).isTrue();
        assertThat(bytesMessage.isBodyAssignableTo(String.class)).isFalse();
        assertThat(bytesMessage.isBodyAssignableTo(null)).isFalse();
    }

    // ========== Multiple Reset/Clear Cycles ==========

    @Test
    public void testMultipleResetClearCycles() throws Exception {
        // Cycle 1
        bytesMessage.writeInt(123);
        bytesMessage.reset();
        assertThat(bytesMessage.readInt()).isEqualTo(123);

        // Cycle 2 - clear and rewrite
        bytesMessage.clearBody();
        bytesMessage.writeInt(456);
        bytesMessage.reset();
        assertThat(bytesMessage.readInt()).isEqualTo(456);

        // Cycle 3 - clear and rewrite different data
        bytesMessage.clearBody();
        bytesMessage.writeUTF("Hello");
        bytesMessage.reset();
        assertThat(bytesMessage.readUTF()).isEqualTo("Hello");
    }
}