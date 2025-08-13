// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.IMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.Queue;
import javax.jms.QueueSender;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Azure Service Bus JMS QueueSender implementation.
 * Wraps Azure Service Bus IMessageSender.
 */
public class ServiceBusJmsQueueSender implements QueueSender {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceBusJmsQueueSender.class);
    
    private final ServiceBusJmsSession session;
    private final Queue queue;
    private final IMessageSender messageSender;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    // Default delivery properties
    private int deliveryMode = DeliveryMode.PERSISTENT;
    private int priority = Message.DEFAULT_PRIORITY;
    private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;
    private boolean disableMessageID = false;
    private boolean disableMessageTimestamp = false;
    
    public ServiceBusJmsQueueSender(ServiceBusJmsSession session, Queue queue, IMessageSender messageSender) {
        this.session = session;
        this.queue = queue;
        this.messageSender = messageSender;
    }
    
    @Override
    public Queue getQueue() throws JMSException {
        validateNotClosed();
        return queue;
    }
    
    @Override
    public void send(Message message) throws JMSException {
        send(queue, message, deliveryMode, priority, timeToLive);
    }
    
    @Override
    public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        send(queue, message, deliveryMode, priority, timeToLive);
    }
    
    @Override
    public void send(Queue queue, Message message) throws JMSException {
        send(queue, message, deliveryMode, priority, timeToLive);
    }
    
    @Override
    public void send(Queue queue, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        validateNotClosed();
        
        if (message == null) {
            throw new JMSException("Message cannot be null");
        }
        
        if (queue == null) {
            throw new JMSException("Queue cannot be null");
        }
        
        if (!(message instanceof ServiceBusJmsMessage)) {
            throw new MessageFormatException("Message must be a ServiceBusJmsMessage");
        }
        
        // Validate priority range per JMS specification (0-9)
        if (priority < 0 || priority > 9) {
            throw new JMSException("Priority must be between 0 and 9, was: " + priority);
        }
        
        ServiceBusJmsMessage jmsMessage = (ServiceBusJmsMessage) message;
        com.microsoft.azure.servicebus.Message serviceBusMessage = jmsMessage.getServiceBusMessage();
        
        // Set JMS header properties
        if (!disableMessageID && (jmsMessage.getJMSMessageID() == null || jmsMessage.getJMSMessageID().isEmpty())) {
            String messageId = "ID:" + UUID.randomUUID().toString();
            jmsMessage.setJMSMessageID(messageId);
        }
        
        if (!disableMessageTimestamp) {
            jmsMessage.setJMSTimestamp(System.currentTimeMillis());
        }
        
        jmsMessage.setJMSDeliveryMode(deliveryMode);
        jmsMessage.setJMSPriority(priority);
        
        if (timeToLive > 0) {
            long expiration = System.currentTimeMillis() + timeToLive;
            jmsMessage.setJMSExpiration(expiration);
        } else {
            jmsMessage.setJMSExpiration(0);
        }
        
        jmsMessage.setJMSDestination(queue);
        
        try {
            logger.debug("Sending message to queue: {}", queue.getQueueName());
            messageSender.send(serviceBusMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JMSException("Send operation interrupted");
        } catch (Exception e) {
            throw new JMSException("Failed to send message: " + e.getMessage());
        }
    }
    
    @Override
    public void send(Destination destination, Message message) throws JMSException {
        if (destination instanceof Queue) {
            send((Queue) destination, message);
        } else {
            throw new JMSException("Only Queue destinations are supported");
        }
    }
    
    @Override
    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        if (destination instanceof Queue) {
            send((Queue) destination, message, deliveryMode, priority, timeToLive);
        } else {
            throw new JMSException("Only Queue destinations are supported");
        }
    }
    
    // JMS 2.0 methods with CompletionListener - not supported in queue-only implementation
    @Override
    public void send(Message message, javax.jms.CompletionListener completionListener) throws JMSException {
        throw new UnsupportedOperationException("CompletionListener not supported in queue-only implementation");
    }
    
    @Override
    public void send(Message message, int deliveryMode, int priority, long timeToLive, 
                     javax.jms.CompletionListener completionListener) throws JMSException {
        throw new UnsupportedOperationException("CompletionListener not supported in queue-only implementation");
    }
    
    @Override
    public void send(Destination destination, Message message, 
                     javax.jms.CompletionListener completionListener) throws JMSException {
        throw new UnsupportedOperationException("CompletionListener not supported in queue-only implementation");
    }
    
    @Override
    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive, 
                     javax.jms.CompletionListener completionListener) throws JMSException {
        throw new UnsupportedOperationException("CompletionListener not supported in queue-only implementation");
    }
    
    @Override
    public void close() throws JMSException {
        if (closed.compareAndSet(false, true)) {
            logger.debug("Closing queue sender");
            try {
                messageSender.close();
            } catch (Exception e) {
                logger.warn("Error closing message sender", e);
            }
        }
    }
    
    @Override
    public int getDeliveryMode() throws JMSException {
        validateNotClosed();
        return deliveryMode;
    }
    
    @Override
    public void setDeliveryMode(int deliveryMode) throws JMSException {
        validateNotClosed();
        this.deliveryMode = deliveryMode;
    }
    
    @Override
    public boolean getDisableMessageID() throws JMSException {
        validateNotClosed();
        return disableMessageID;
    }
    
    @Override
    public void setDisableMessageID(boolean value) throws JMSException {
        validateNotClosed();
        this.disableMessageID = value;
    }
    
    @Override
    public boolean getDisableMessageTimestamp() throws JMSException {
        validateNotClosed();
        return disableMessageTimestamp;
    }
    
    @Override
    public void setDisableMessageTimestamp(boolean value) throws JMSException {
        validateNotClosed();
        this.disableMessageTimestamp = value;
    }
    
    @Override
    public Destination getDestination() throws JMSException {
        return getQueue();
    }
    
    @Override
    public int getPriority() throws JMSException {
        validateNotClosed();
        return priority;
    }
    
    @Override
    public void setPriority(int priority) throws JMSException {
        validateNotClosed();
        // Normalize priority to valid range per JMS specification (0-9)
        if (priority < 0) {
            logger.warn("Priority {} is below minimum, normalizing to 0", priority);
            this.priority = 0;
        } else if (priority > 9) {
            logger.warn("Priority {} is above maximum, normalizing to 9", priority);
            this.priority = 9;
        } else {
            this.priority = priority;
        }
    }
    
    @Override
    public long getTimeToLive() throws JMSException {
        validateNotClosed();
        return timeToLive;
    }
    
    @Override
    public void setTimeToLive(long timeToLive) throws JMSException {
        validateNotClosed();
        this.timeToLive = timeToLive;
    }
    
    // JMS 2.0 method
    @Override  
    public long getDeliveryDelay() throws JMSException {
        validateNotClosed();
        return 0; // Azure Service Bus doesn't support delivery delay
    }
    
    @Override
    public void setDeliveryDelay(long deliveryDelay) throws JMSException {
        validateNotClosed();
        if (deliveryDelay != 0) {
            throw new UnsupportedOperationException("Delivery delay not supported by Azure Service Bus");
        }
    }
    
    /**
     * Returns true if this sender is closed.
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed.get();
    }
    
    private void validateNotClosed() throws JMSException {
        if (closed.get()) {
            throw new JMSException("QueueSender is closed");
        }
        if (session.isClosed()) {
            throw new JMSException("Session is closed");
        }
    }
}