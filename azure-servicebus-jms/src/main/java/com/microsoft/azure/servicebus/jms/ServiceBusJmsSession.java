// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.MessagingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Azure Service Bus JMS Session implementation.
 * Manages message producers and consumers for a connection.
 */
public class ServiceBusJmsSession implements QueueSession {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceBusJmsSession.class);
    
    private final ServiceBusJmsConnection connection;
    private final MessagingFactory messagingFactory;
    private final int acknowledgeMode;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean transacted = new AtomicBoolean(false);
    
    // Cache for senders and receivers
    private final Map<String, IMessageSender> senders = new ConcurrentHashMap<>();
    private final Map<String, IMessageReceiver> receivers = new ConcurrentHashMap<>();
    
    public ServiceBusJmsSession(ServiceBusJmsConnection connection, 
                               MessagingFactory messagingFactory, 
                               int acknowledgeMode) {
        this.connection = connection;
        this.messagingFactory = messagingFactory;
        this.acknowledgeMode = acknowledgeMode;
    }
    
    @Override
    public Queue createQueue(String queueName) throws JMSException {
        validateNotClosed();
        return new ServiceBusJmsQueue(queueName);
    }
    
    @Override
    public QueueSender createSender(Queue queue) throws JMSException {
        validateNotClosed();
        if (queue == null) {
            throw new JMSException("Queue cannot be null");
        }
        
        String queueName = queue.getQueueName();
        IMessageSender sender = senders.computeIfAbsent(queueName, name -> {
            try {
                return ClientFactory.createMessageSenderFromEntityPathAsync(messagingFactory, name).get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create sender for queue: " + name, e);
            }
        });
        
        return new ServiceBusJmsQueueSender(this, queue, sender);
    }
    
    @Override
    public QueueReceiver createReceiver(Queue queue) throws JMSException {
        return createReceiver(queue, null);
    }
    
    @Override
    public QueueReceiver createReceiver(Queue queue, String messageSelector) throws JMSException {
        validateNotClosed();
        if (queue == null) {
            throw new JMSException("Queue cannot be null");
        }
        
        if (messageSelector != null && !messageSelector.trim().isEmpty()) {
            logger.warn("Message selectors are not supported by Azure Service Bus");
        }
        
        String queueName = queue.getQueueName();
        IMessageReceiver receiver = receivers.computeIfAbsent(queueName, name -> {
            try {
                ReceiveMode receiveMode = acknowledgeMode == Session.CLIENT_ACKNOWLEDGE ? 
                    ReceiveMode.PEEKLOCK : ReceiveMode.RECEIVEANDDELETE;
                // Use the correct ClientFactory method signature with MessagingFactory
                return ClientFactory.createMessageReceiverFromEntityPathAsync(
                    messagingFactory, name, com.microsoft.azure.servicebus.primitives.MessagingEntityType.QUEUE, receiveMode).get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create receiver for queue: " + name, e);
            }
        });
        
        return new ServiceBusJmsQueueReceiver(this, queue, receiver);
    }
    
    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        return createBrowser(queue, null);
    }
    
    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        validateNotClosed();
        if (queue == null) {
            throw new JMSException("Queue cannot be null");
        }
        
        if (messageSelector != null && !messageSelector.trim().isEmpty()) {
            logger.warn("Message selectors are not supported by Azure Service Bus");
        }
        
        // QueueBrowser implementation would go here
        throw new JMSException("QueueBrowser not yet implemented");
    }
    
    @Override
    public TextMessage createTextMessage() throws JMSException {
        validateNotClosed();
        return new ServiceBusJmsTextMessage(this);
    }
    
    @Override
    public TextMessage createTextMessage(String text) throws JMSException {
        validateNotClosed();
        return new ServiceBusJmsTextMessage(this, text);
    }
    
    @Override
    public BytesMessage createBytesMessage() throws JMSException {
        validateNotClosed();
        return new ServiceBusJmsBytesMessage(this);
    }
    
    @Override
    public MapMessage createMapMessage() throws JMSException {
        validateNotClosed();
        throw new JMSException("MapMessage not yet implemented");
    }
    
    @Override
    public ObjectMessage createObjectMessage() throws JMSException {
        validateNotClosed();
        throw new JMSException("ObjectMessage not yet implemented");
    }
    
    @Override
    public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
        validateNotClosed();
        throw new JMSException("ObjectMessage not yet implemented");
    }
    
    @Override
    public StreamMessage createStreamMessage() throws JMSException {
        validateNotClosed();
        throw new JMSException("StreamMessage not yet implemented");
    }
    
    @Override
    public Message createMessage() throws JMSException {
        validateNotClosed();
        return new ServiceBusJmsMessage(this);
    }
    
    @Override
    public boolean getTransacted() throws JMSException {
        validateNotClosed();
        return transacted.get();
    }
    
    @Override
    public int getAcknowledgeMode() throws JMSException {
        validateNotClosed();
        return acknowledgeMode;
    }
    
    @Override
    public void commit() throws JMSException {
        validateNotClosed();
        if (!transacted.get()) {
            throw new JMSException("Session is not transacted");
        }
        // Transaction support not implemented
        throw new JMSException("Transactions not supported");
    }
    
    @Override
    public void rollback() throws JMSException {
        validateNotClosed();
        if (!transacted.get()) {
            throw new JMSException("Session is not transacted");
        }
        // Transaction support not implemented
        throw new JMSException("Transactions not supported");
    }
    
    @Override
    public void close() throws JMSException {
        if (closed.compareAndSet(false, true)) {
            logger.debug("Closing session");
            
            // Close all receivers
            for (IMessageReceiver receiver : receivers.values()) {
                try {
                    receiver.close();
                } catch (Exception e) {
                    logger.warn("Error closing receiver", e);
                }
            }
            receivers.clear();
            
            // Close all senders
            for (IMessageSender sender : senders.values()) {
                try {
                    sender.close();
                } catch (Exception e) {
                    logger.warn("Error closing sender", e);
                }
            }
            senders.clear();
            
            // Remove from connection
            connection.removeSession(this);
        }
    }
    
    @Override
    public void recover() throws JMSException {
        validateNotClosed();
        if (transacted.get()) {
            throw new JMSException("Cannot recover transacted session");
        }
        // Recovery not implemented
        logger.warn("Session recovery not implemented");
    }
    
    @Override
    public MessageListener getMessageListener() throws JMSException {
        throw new JMSException("Session-level message listeners not supported");
    }
    
    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        throw new JMSException("Session-level message listeners not supported");
    }
    
    @Override
    public void run() {
        // Not implemented for this version
    }
    
    // Unsupported operations for queue-only implementation
    @Override
    public Topic createTopic(String topicName) throws JMSException {
        throw new JMSException("Topics not supported");
    }
    
    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        throw new JMSException("Topics not supported");
    }
    
    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        throw new JMSException("Topics not supported");
    }
    
    @Override
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        if (destination instanceof Queue) {
            return createReceiver((Queue) destination);
        }
        throw new JMSException("Only Queue destinations are supported");
    }
    
    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
        if (destination instanceof Queue) {
            return createReceiver((Queue) destination, messageSelector);
        }
        throw new JMSException("Only Queue destinations are supported");
    }
    
    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) throws JMSException {
        return createConsumer(destination, messageSelector);
    }
    
    @Override
    public MessageProducer createProducer(Destination destination) throws JMSException {
        if (destination instanceof Queue) {
            return createSender((Queue) destination);
        }
        throw new JMSException("Only Queue destinations are supported");
    }
    
    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        throw new JMSException("Temporary queues not supported");
    }
    
    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        throw new JMSException("Topics not supported");
    }
    
    @Override
    public void unsubscribe(String name) throws JMSException {
        throw new JMSException("Topics not supported");
    }
    
    // JMS 2.0 methods - not supported in queue-only implementation
    @Override
    public MessageConsumer createSharedDurableConsumer(Topic topic, String name) throws JMSException {
        throw new JMSException("Shared durable consumers not supported in queue-only implementation");
    }
    
    @Override
    public MessageConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) throws JMSException {
        throw new JMSException("Shared durable consumers not supported in queue-only implementation");
    }
    
    @Override
    public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) throws JMSException {
        throw new JMSException("Shared consumers not supported in queue-only implementation");
    }
    
    @Override
    public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) throws JMSException {
        throw new JMSException("Shared consumers not supported in queue-only implementation");
    }
    
    @Override
    public MessageConsumer createDurableConsumer(Topic topic, String name) throws JMSException {
        throw new JMSException("Durable consumers not supported in queue-only implementation");
    }
    
    @Override
    public MessageConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        throw new JMSException("Durable consumers not supported in queue-only implementation");
    }
    
    // Package-private methods for message acknowledgment
    void acknowledgeMessage(ServiceBusJmsMessage message) throws JMSException {
        if (acknowledgeMode != Session.CLIENT_ACKNOWLEDGE) {
            return; // Nothing to do for AUTO_ACKNOWLEDGE
        }
        
        // Acknowledgment would be implemented here using the message's delivery tag
        logger.warn("Message acknowledgment not yet implemented");
    }
    
    boolean isClosed() {
        return closed.get();
    }
    
    private void validateNotClosed() throws JMSException {
        if (closed.get()) {
            throw new JMSException("Session is closed");
        }
        if (connection.isClosed()) {
            throw new JMSException("Connection is closed");
        }
    }
}