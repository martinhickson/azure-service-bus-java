// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.Utils;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageFormatException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base Azure Service Bus JMS Message implementation.
 * Wraps Azure Service Bus Message for JMS compatibility.
 */
public class ServiceBusJmsMessage implements javax.jms.Message {
    
    protected final Message serviceBusMessage;
    protected final ServiceBusJmsSession session;
    
    // JMS standard properties
    private String jmsMessageID;
    private long jmsTimestamp;
    private String jmsCorrelationID;
    private Destination jmsReplyTo;
    private Destination jmsDestination;
    private int jmsDeliveryMode = DeliveryMode.PERSISTENT;
    private boolean jmsRedelivered;
    private String jmsType;
    private long jmsExpiration;
    private int jmsPriority = javax.jms.Message.DEFAULT_PRIORITY;
    
    // Custom properties
    private final Map<String, Object> properties = new HashMap<>();
    
    protected ServiceBusJmsMessage(ServiceBusJmsSession session) {
        this.session = session;
        this.serviceBusMessage = new Message();
        this.jmsTimestamp = System.currentTimeMillis();
        this.jmsMessageID = "ID:" + UUID.randomUUID().toString();
        this.serviceBusMessage.setMessageId(this.jmsMessageID);
    }
    
    protected ServiceBusJmsMessage(ServiceBusJmsSession session, IMessage serviceBusMessage) {
        this.session = session;
        this.serviceBusMessage = (Message) serviceBusMessage;
        this.jmsMessageID = serviceBusMessage.getMessageId();
        this.jmsTimestamp = serviceBusMessage.getEnqueuedTimeUtc() != null ? 
            serviceBusMessage.getEnqueuedTimeUtc().toEpochMilli() : 
            System.currentTimeMillis();
        this.jmsCorrelationID = serviceBusMessage.getCorrelationId();
        this.jmsRedelivered = serviceBusMessage.getDeliveryCount() > 1;
        
        // Load custom properties
        if (serviceBusMessage.getProperties() != null) {
            properties.putAll(serviceBusMessage.getProperties());
        }
    }
    
    @Override
    public String getJMSMessageID() throws JMSException {
        return jmsMessageID;
    }
    
    @Override
    public void setJMSMessageID(String id) throws JMSException {
        this.jmsMessageID = id;
        this.serviceBusMessage.setMessageId(id);
    }
    
    @Override
    public long getJMSTimestamp() throws JMSException {
        return jmsTimestamp;
    }
    
    @Override
    public void setJMSTimestamp(long timestamp) throws JMSException {
        this.jmsTimestamp = timestamp;
    }
    
    @Override
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        return jmsCorrelationID != null ? jmsCorrelationID.getBytes() : null;
    }
    
    @Override
    public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
        this.jmsCorrelationID = correlationID != null ? new String(correlationID) : null;
        this.serviceBusMessage.setCorrelationId(this.jmsCorrelationID);
    }
    
    @Override
    public String getJMSCorrelationID() throws JMSException {
        return jmsCorrelationID;
    }
    
    @Override
    public void setJMSCorrelationID(String correlationID) throws JMSException {
        this.jmsCorrelationID = correlationID;
        this.serviceBusMessage.setCorrelationId(correlationID);
    }
    
    @Override
    public Destination getJMSReplyTo() throws JMSException {
        return jmsReplyTo;
    }
    
    @Override
    public void setJMSReplyTo(Destination replyTo) throws JMSException {
        this.jmsReplyTo = replyTo;
        if (replyTo instanceof ServiceBusJmsQueue) {
            serviceBusMessage.setReplyTo(((ServiceBusJmsQueue) replyTo).getQueueName());
        }
    }
    
    @Override
    public Destination getJMSDestination() throws JMSException {
        return jmsDestination;
    }
    
    @Override
    public void setJMSDestination(Destination destination) throws JMSException {
        this.jmsDestination = destination;
    }
    
    @Override
    public int getJMSDeliveryMode() throws JMSException {
        return jmsDeliveryMode;
    }
    
    @Override
    public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
        this.jmsDeliveryMode = deliveryMode;
    }
    
    @Override
    public boolean getJMSRedelivered() throws JMSException {
        return jmsRedelivered;
    }
    
    @Override
    public void setJMSRedelivered(boolean redelivered) throws JMSException {
        this.jmsRedelivered = redelivered;
    }
    
    @Override
    public String getJMSType() throws JMSException {
        return jmsType;
    }
    
    @Override
    public void setJMSType(String type) throws JMSException {
        this.jmsType = type;
        this.serviceBusMessage.setLabel(type);
    }
    
    @Override
    public long getJMSExpiration() throws JMSException {
        return jmsExpiration;
    }
    
    @Override
    public void setJMSExpiration(long expiration) throws JMSException {
        this.jmsExpiration = expiration;
        if (expiration > 0) {
            long timeToLive = expiration - System.currentTimeMillis();
            if (timeToLive > 0) {
                serviceBusMessage.setTimeToLive(java.time.Duration.ofMillis(timeToLive));
            }
        }
    }
    
    @Override
    public int getJMSPriority() throws JMSException {
        return jmsPriority;
    }
    
    @Override
    public void setJMSPriority(int priority) throws JMSException {
        this.jmsPriority = priority;
    }
    
    // JMS 2.0 delivery time methods
    private long jmsDeliveryTime = 0;
    
    public long getJMSDeliveryTime() throws JMSException {
        return jmsDeliveryTime;
    }
    
    public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
        this.jmsDeliveryTime = deliveryTime;
        // Azure Service Bus doesn't support scheduled delivery through JMS
        // This would need to be handled differently if required
    }
    
    @Override
    public void clearProperties() throws JMSException {
        properties.clear();
        serviceBusMessage.setProperties(new HashMap<>());
    }
    
    @Override
    public boolean propertyExists(String name) throws JMSException {
        return properties.containsKey(name);
    }
    
    @Override
    public boolean getBooleanProperty(String name) throws JMSException {
        Object value = properties.get(name);
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        throw new MessageFormatException("Property " + name + " is not a boolean");
    }
    
    @Override
    public byte getByteProperty(String name) throws JMSException {
        Object value = properties.get(name);
        if (value == null) throw new NumberFormatException("Property " + name + " is null");
        if (value instanceof Byte) return (Byte) value;
        throw new MessageFormatException("Property " + name + " is not a byte");
    }
    
    @Override
    public short getShortProperty(String name) throws JMSException {
        Object value = properties.get(name);
        if (value == null) throw new NumberFormatException("Property " + name + " is null");
        if (value instanceof Short) return (Short) value;
        if (value instanceof Byte) return (Byte) value;
        throw new MessageFormatException("Property " + name + " is not a short");
    }
    
    @Override
    public int getIntProperty(String name) throws JMSException {
        Object value = properties.get(name);
        if (value == null) throw new NumberFormatException("Property " + name + " is null");
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Short) return (Short) value;
        if (value instanceof Byte) return (Byte) value;
        throw new MessageFormatException("Property " + name + " is not an int");
    }
    
    @Override
    public long getLongProperty(String name) throws JMSException {
        Object value = properties.get(name);
        if (value == null) throw new NumberFormatException("Property " + name + " is null");
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Short) return (Short) value;
        if (value instanceof Byte) return (Byte) value;
        throw new MessageFormatException("Property " + name + " is not a long");
    }
    
    @Override
    public float getFloatProperty(String name) throws JMSException {
        Object value = properties.get(name);
        if (value == null) throw new NumberFormatException("Property " + name + " is null");
        if (value instanceof Float) return (Float) value;
        throw new MessageFormatException("Property " + name + " is not a float");
    }
    
    @Override
    public double getDoubleProperty(String name) throws JMSException {
        Object value = properties.get(name);
        if (value == null) throw new NumberFormatException("Property " + name + " is null");
        if (value instanceof Double) return (Double) value;
        if (value instanceof Float) return (Float) value;
        throw new MessageFormatException("Property " + name + " is not a double");
    }
    
    @Override
    public String getStringProperty(String name) throws JMSException {
        Object value = properties.get(name);
        return value != null ? value.toString() : null;
    }
    
    @Override
    public Object getObjectProperty(String name) throws JMSException {
        return properties.get(name);
    }
    
    @Override
    public Enumeration getPropertyNames() throws JMSException {
        return Collections.enumeration(properties.keySet());
    }
    
    @Override
    public void setBooleanProperty(String name, boolean value) throws JMSException {
        setObjectProperty(name, value);
    }
    
    @Override
    public void setByteProperty(String name, byte value) throws JMSException {
        setObjectProperty(name, value);
    }
    
    @Override
    public void setShortProperty(String name, short value) throws JMSException {
        setObjectProperty(name, value);
    }
    
    @Override
    public void setIntProperty(String name, int value) throws JMSException {
        setObjectProperty(name, value);
    }
    
    @Override
    public void setLongProperty(String name, long value) throws JMSException {
        setObjectProperty(name, value);
    }
    
    @Override
    public void setFloatProperty(String name, float value) throws JMSException {
        setObjectProperty(name, value);
    }
    
    @Override
    public void setDoubleProperty(String name, double value) throws JMSException {
        setObjectProperty(name, value);
    }
    
    @Override
    public void setStringProperty(String name, String value) throws JMSException {
        setObjectProperty(name, value);
    }
    
    @Override
    public void setObjectProperty(String name, Object value) throws JMSException {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
        
        // Update Service Bus message properties
        Map<String, String> sbProperties = new HashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            sbProperties.put(entry.getKey(), entry.getValue().toString());
        }
        serviceBusMessage.setProperties(sbProperties);
    }
    
    @Override
    public void acknowledge() throws JMSException {
        if (session != null) {
            session.acknowledgeMessage(this);
        }
    }
    
    @Override
    public void clearBody() throws JMSException {
        // Implemented by subclasses
    }
    
    // JMS 2.0 method
    @Override
    public boolean isBodyAssignableTo(Class c) throws JMSException {
        // Basic implementation - subclasses can override for specific behavior
        if (c == null) {
            return false;
        }
        // Generic message body assignment check
        return Object.class.isAssignableFrom(c);
    }
    
    // JMS 2.0 method
    @Override
    public <T> T getBody(Class<T> c) throws JMSException {
        // Basic implementation - subclasses should override for specific behavior
        if (c == null) {
            throw new MessageFormatException("Class cannot be null");
        }
        throw new MessageFormatException("getBody not supported for generic message type");
    }
    
    // Package-private method to get the underlying Service Bus message
    Message getServiceBusMessage() {
        return serviceBusMessage;
    }
}