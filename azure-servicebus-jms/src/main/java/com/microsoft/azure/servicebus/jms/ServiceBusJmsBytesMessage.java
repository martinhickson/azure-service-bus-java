// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.MessageBodyType;
import com.microsoft.azure.servicebus.Utils;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Azure Service Bus JMS BytesMessage implementation.
 */
public class ServiceBusJmsBytesMessage extends ServiceBusJmsMessage implements BytesMessage {
    
    private ByteArrayOutputStream writeBuffer;
    private DataOutputStream dataOut;
    private ByteArrayInputStream readBuffer;
    private DataInputStream dataIn;
    private boolean readMode = false;
    
    public ServiceBusJmsBytesMessage(ServiceBusJmsSession session) {
        super(session);
        initializeForWrite();
    }
    
    public ServiceBusJmsBytesMessage(ServiceBusJmsSession session, IMessage serviceBusMessage) {
        super(session, serviceBusMessage);
        initializeForRead(extractBytesFromServiceBusMessage(serviceBusMessage));
    }
    
    @Override
    public long getBodyLength() throws JMSException {
        if (!readMode) {
            throw new MessageNotReadableException("Message is in write mode");
        }
        return readBuffer != null ? readBuffer.available() : 0;
    }
    
    @Override
    public boolean readBoolean() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readBoolean();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public byte readByte() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readByte();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public int readUnsignedByte() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readUnsignedByte();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public short readShort() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readShort();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public int readUnsignedShort() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readUnsignedShort();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public char readChar() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readChar();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public int readInt() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readInt();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public long readLong() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readLong();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public float readFloat() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readFloat();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public double readDouble() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readDouble();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public String readUTF() throws JMSException {
        checkReadMode();
        try {
            return dataIn.readUTF();
        } catch (IOException e) {
            throw new MessageEOFException("End of message");
        }
    }
    
    @Override
    public int readBytes(byte[] value) throws JMSException {
        return readBytes(value, value.length);
    }
    
    @Override
    public int readBytes(byte[] value, int length) throws JMSException {
        checkReadMode();
        try {
            return dataIn.read(value, 0, length);
        } catch (IOException e) {
            throw new MessageFormatException("Error reading bytes: " + e.getMessage());
        }
    }
    
    @Override
    public void writeBoolean(boolean value) throws JMSException {
        checkWriteMode();
        try {
            dataOut.writeBoolean(value);
        } catch (IOException e) {
            throw new JMSException("Error writing boolean: " + e.getMessage());
        }
    }
    
    @Override
    public void writeByte(byte value) throws JMSException {
        checkWriteMode();
        try {
            dataOut.writeByte(value);
        } catch (IOException e) {
            throw new JMSException("Error writing byte: " + e.getMessage());
        }
    }
    
    @Override
    public void writeShort(short value) throws JMSException {
        checkWriteMode();
        try {
            dataOut.writeShort(value);
        } catch (IOException e) {
            throw new JMSException("Error writing short: " + e.getMessage());
        }
    }
    
    @Override
    public void writeChar(char value) throws JMSException {
        checkWriteMode();
        try {
            dataOut.writeChar(value);
        } catch (IOException e) {
            throw new JMSException("Error writing char: " + e.getMessage());
        }
    }
    
    @Override
    public void writeInt(int value) throws JMSException {
        checkWriteMode();
        try {
            dataOut.writeInt(value);
        } catch (IOException e) {
            throw new JMSException("Error writing int: " + e.getMessage());
        }
    }
    
    @Override
    public void writeLong(long value) throws JMSException {
        checkWriteMode();
        try {
            dataOut.writeLong(value);
        } catch (IOException e) {
            throw new JMSException("Error writing long: " + e.getMessage());
        }
    }
    
    @Override
    public void writeFloat(float value) throws JMSException {
        checkWriteMode();
        try {
            dataOut.writeFloat(value);
        } catch (IOException e) {
            throw new JMSException("Error writing float: " + e.getMessage());
        }
    }
    
    @Override
    public void writeDouble(double value) throws JMSException {
        checkWriteMode();
        try {
            dataOut.writeDouble(value);
        } catch (IOException e) {
            throw new JMSException("Error writing double: " + e.getMessage());
        }
    }
    
    @Override
    public void writeUTF(String value) throws JMSException {
        checkWriteMode();
        try {
            dataOut.writeUTF(value);
        } catch (IOException e) {
            throw new JMSException("Error writing UTF string: " + e.getMessage());
        }
    }
    
    @Override
    public void writeBytes(byte[] value) throws JMSException {
        writeBytes(value, 0, value.length);
    }
    
    @Override
    public void writeBytes(byte[] value, int offset, int length) throws JMSException {
        checkWriteMode();
        try {
            dataOut.write(value, offset, length);
        } catch (IOException e) {
            throw new JMSException("Error writing bytes: " + e.getMessage());
        }
    }
    
    @Override
    public void writeObject(Object value) throws JMSException {
        if (value instanceof Boolean) {
            writeBoolean((Boolean) value);
        } else if (value instanceof Byte) {
            writeByte((Byte) value);
        } else if (value instanceof Short) {
            writeShort((Short) value);
        } else if (value instanceof Character) {
            writeChar((Character) value);
        } else if (value instanceof Integer) {
            writeInt((Integer) value);
        } else if (value instanceof Long) {
            writeLong((Long) value);
        } else if (value instanceof Float) {
            writeFloat((Float) value);
        } else if (value instanceof Double) {
            writeDouble((Double) value);
        } else if (value instanceof String) {
            writeUTF((String) value);
        } else if (value instanceof byte[]) {
            writeBytes((byte[]) value);
        } else {
            throw new MessageFormatException("Unsupported object type: " + value.getClass().getName());
        }
    }
    
    @Override
    public void reset() throws JMSException {
        if (!readMode) {
            // Switch from write mode to read mode
            try {
                dataOut.flush();
                byte[] data = writeBuffer.toByteArray();
                updateServiceBusMessage(data);
                initializeForRead(data);
            } catch (IOException e) {
                throw new JMSException("Error switching to read mode: " + e.getMessage());
            }
        } else {
            // Reset read position
            if (readBuffer != null) {
                readBuffer.reset();
                dataIn = new DataInputStream(readBuffer);
            }
        }
    }
    
    @Override
    public void clearBody() throws JMSException {
        initializeForWrite();
        serviceBusMessage.setMessageBody(Utils.fromBinary(new byte[0]));
    }
    
    // JMS 2.0 method
    @Override
    public <T> T getBody(Class<T> c) throws JMSException {
        if (c == null) {
            throw new MessageFormatException("Class cannot be null");
        }
        if (byte[].class.isAssignableFrom(c)) {
            if (!readMode) {
                try {
                    dataOut.flush();
                    byte[] data = writeBuffer.toByteArray();
                    return c.cast(data);
                } catch (Exception e) {
                    throw new JMSException("Error getting body: " + e.getMessage());
                }
            } else {
                // In read mode, return the original binary data
                return c.cast(extractBytesFromServiceBusMessage(serviceBusMessage));
            }
        }
        throw new MessageFormatException("BytesMessage body cannot be assigned to " + c.getName());
    }
    
    @Override
    public boolean isBodyAssignableTo(Class c) throws JMSException {
        if (c == null) {
            return false;
        }
        return byte[].class.isAssignableFrom(c);
    }
    
    private void initializeForWrite() {
        readMode = false;
        writeBuffer = new ByteArrayOutputStream();
        dataOut = new DataOutputStream(writeBuffer);
        readBuffer = null;
        dataIn = null;
    }
    
    private void initializeForRead(byte[] data) {
        readMode = true;
        writeBuffer = null;
        dataOut = null;
        readBuffer = new ByteArrayInputStream(data);
        dataIn = new DataInputStream(readBuffer);
    }
    
    private void checkReadMode() throws JMSException {
        if (!readMode) {
            throw new MessageNotReadableException("Message is in write mode");
        }
    }
    
    private void checkWriteMode() throws JMSException {
        if (readMode) {
            throw new MessageNotWriteableException("Message is in read mode");
        }
    }
    
    private void updateServiceBusMessage(byte[] data) {
        serviceBusMessage.setMessageBody(Utils.fromBinary(data));
    }
    
    private byte[] extractBytesFromServiceBusMessage(IMessage serviceBusMessage) {
        try {
            MessageBody body = serviceBusMessage.getMessageBody();
            if (body == null) {
                return new byte[0];
            }
            
            if (body.getBodyType() == MessageBodyType.BINARY) {
                return body.getBinaryData().get(0);
            }
            
            throw new RuntimeException("Message body is not binary");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract bytes from Service Bus message", e);
        }
    }
}