// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jms;

import com.microsoft.azure.servicebus.ClientSettings;
import com.microsoft.azure.servicebus.primitives.MessagingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Azure Service Bus JMS Connection implementation.
 * Wraps Azure Service Bus MessagingFactory for connection management.
 */
public class ServiceBusJmsConnection implements QueueConnection {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceBusJmsConnection.class);
    
    private final URI namespaceUri;
    private final ClientSettings clientSettings;
    private final Properties connectionProperties;
    private final List<ServiceBusJmsSession> sessions;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    
    private MessagingFactory messagingFactory;
    private ExceptionListener exceptionListener;
    private String clientId;
    
    public ServiceBusJmsConnection(URI namespaceUri, ClientSettings clientSettings, Properties connectionProperties) {
        this.namespaceUri = namespaceUri;
        this.clientSettings = clientSettings;
        this.connectionProperties = connectionProperties;
        this.sessions = new CopyOnWriteArrayList<>();
    }
    
    @Override
    public Session createSession() throws JMSException {
        return createSession(false, Session.AUTO_ACKNOWLEDGE);
    }
    
    @Override
    public Session createSession(int sessionMode) throws JMSException {
        return createSession(false, sessionMode);
    }
    
    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        return createQueueSession(transacted, acknowledgeMode);
    }
    
    @Override
    public QueueSession createQueueSession(boolean transacted, int acknowledgeMode) throws JMSException {
        validateNotClosed();
        
        if (transacted) {
            throw new JMSException("Transacted sessions are not supported");
        }
        
        ensureMessagingFactory();
        
        ServiceBusJmsSession session = new ServiceBusJmsSession(
            this, messagingFactory, acknowledgeMode);
        sessions.add(session);
        return session;
    }
    
    @Override
    public String getClientID() throws JMSException {
        validateNotClosed();
        return clientId;
    }
    
    @Override
    public void setClientID(String clientID) throws JMSException {
        validateNotClosed();
        if (started.get()) {
            throw new JMSException("Cannot set client ID after connection has been started");
        }
        this.clientId = clientID;
    }
    
    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        validateNotClosed();
        return new ServiceBusJmsConnectionMetaData();
    }
    
    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        validateNotClosed();
        return exceptionListener;
    }
    
    @Override
    public void setExceptionListener(ExceptionListener listener) throws JMSException {
        validateNotClosed();
        this.exceptionListener = listener;
    }
    
    @Override
    public void start() throws JMSException {
        validateNotClosed();
        started.set(true);
        logger.debug("Connection started");
    }
    
    @Override
    public void stop() throws JMSException {
        validateNotClosed();
        started.set(false);
        logger.debug("Connection stopped");
    }
    
    @Override
    public void close() throws JMSException {
        if (closed.compareAndSet(false, true)) {
            logger.debug("Closing connection");
            
            // Close all sessions
            for (ServiceBusJmsSession session : sessions) {
                try {
                    session.close();
                } catch (Exception e) {
                    logger.warn("Error closing session", e);
                }
            }
            sessions.clear();
            
            // Close messaging factory
            if (messagingFactory != null) {
                try {
                    messagingFactory.close();
                } catch (Exception e) {
                    logger.warn("Error closing messaging factory", e);
                }
            }
        }
    }
    
    // Unsupported operations for queue-only implementation
    @Override
    public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, 
                                                      ServerSessionPool sessionPool, int maxMessages) 
                                                      throws JMSException {
        throw new JMSException("ConnectionConsumer not supported");
    }
    
    @Override
    public ConnectionConsumer createConnectionConsumer(Queue queue, String messageSelector, 
                                                      ServerSessionPool sessionPool, int maxMessages) 
                                                      throws JMSException {
        throw new JMSException("ConnectionConsumer not supported");
    }
    
    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, 
                                                            String messageSelector, ServerSessionPool sessionPool, 
                                                            int maxMessages) throws JMSException {
        throw new JMSException("Durable subscriptions not supported");
    }
    
    // JMS 2.0 method
    @Override
    public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic, String subscriptionName, 
                                                                   String messageSelector, ServerSessionPool sessionPool, 
                                                                   int maxMessages) throws JMSException {
        throw new JMSException("Shared durable connection consumers not supported in queue-only implementation");
    }
    
    @Override
    public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String subscriptionName, 
                                                           String messageSelector, ServerSessionPool sessionPool, 
                                                           int maxMessages) throws JMSException {
        throw new JMSException("Shared connection consumers not supported in queue-only implementation");
    }
    
    // Package-private methods for session management
    void removeSession(ServiceBusJmsSession session) {
        sessions.remove(session);
    }
    
    boolean isStarted() {
        return started.get() && !closed.get();
    }
    
    boolean isClosed() {
        return closed.get();
    }
    
    MessagingFactory getMessagingFactory() throws JMSException {
        ensureMessagingFactory();
        return messagingFactory;
    }
    
    private void ensureMessagingFactory() throws JMSException {
        if (messagingFactory == null) {
            synchronized (this) {
                if (messagingFactory == null) {
                    try {
                        // MessagingFactory.createFromNamespaceEndpointURI returns MessagingFactory directly (synchronous)
                        messagingFactory = MessagingFactory.createFromNamespaceEndpointURI(
                            namespaceUri, clientSettings);
                    } catch (Exception e) {
                        throw new JMSException("Failed to create messaging factory: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    private void validateNotClosed() throws JMSException {
        if (closed.get()) {
            throw new JMSException("Connection is closed");
        }
    }
}