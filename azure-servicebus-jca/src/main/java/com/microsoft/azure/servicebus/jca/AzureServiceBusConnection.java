package com.microsoft.azure.servicebus.jca;

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

/**
 * Application-level Connection handle for Azure Service Bus JCA integration.
 * 
 * This class provides the JMS Connection interface to applications while
 * delegating actual operations to the underlying managed connection.
 */
public class AzureServiceBusConnection implements Connection, QueueConnection {
    
    private static final Logger logger = LoggerFactory.getLogger(AzureServiceBusConnection.class);
    
    private AzureServiceBusManagedConnection managedConnection;
    private volatile boolean closed = false;
    private volatile boolean valid = true;
    
    /**
     * Creates a new connection handle.
     * 
     * @param managedConnection the underlying managed connection
     */
    public AzureServiceBusConnection(AzureServiceBusManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
        logger.debug("Created new connection handle");
    }
    
    /**
     * Creates a session for this connection.
     * 
     * @param transacted whether the session is transacted (not supported)
     * @param acknowledgeMode the acknowledgment mode
     * @return a new session
     * @throws JMSException if session creation fails
     */
    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        logger.debug("Creating session (transacted={}, acknowledgeMode={})", transacted, acknowledgeMode);
        
        validateConnection();
        
        if (transacted) {
            throw new JMSException("Transacted sessions are not supported. Use message outbox pattern with deduplication IDs.");
        }
        
        return managedConnection.createSession(false, acknowledgeMode);
    }
    
    /**
     * Creates a session with the specified session mode.
     * 
     * @param sessionMode the session mode
     * @return a new session
     * @throws JMSException if session creation fails
     */
    @Override
    public Session createSession(int sessionMode) throws JMSException {
        logger.debug("Creating session with mode: {}", sessionMode);
        return createSession(false, sessionMode);
    }
    
    /**
     * Creates a session for this connection (JMS 2.0 method).
     * 
     * @return a new session with AUTO_ACKNOWLEDGE mode
     * @throws JMSException if session creation fails
     */
    @Override
    public Session createSession() throws JMSException {
        logger.debug("Creating session with default settings");
        return createSession(false, Session.AUTO_ACKNOWLEDGE);
    }
    
    /**
     * Creates a queue session for this connection.
     * 
     * @param transacted whether the session is transacted (not supported)
     * @param acknowledgeMode the acknowledgment mode
     * @return a new queue session
     * @throws JMSException if session creation fails
     */
    @Override
    public QueueSession createQueueSession(boolean transacted, int acknowledgeMode) throws JMSException {
        logger.debug("Creating queue session (transacted={}, acknowledgeMode={})", transacted, acknowledgeMode);
        
        Session session = createSession(transacted, acknowledgeMode);
        
        if (!(session instanceof QueueSession)) {
            throw new JMSException("Created session does not support queue operations");
        }
        
        return (QueueSession) session;
    }
    
    /**
     * Gets the client identifier for this connection.
     * 
     * @return the client identifier (always null for Azure Service Bus)
     * @throws JMSException if the operation fails
     */
    @Override
    public String getClientID() throws JMSException {
        validateConnection();
        return null; // Azure Service Bus doesn't use client identifiers
    }
    
    /**
     * Sets the client identifier for this connection.
     * 
     * Note: Client identifiers are not supported by Azure Service Bus.
     * 
     * @param clientID the client identifier (ignored)
     * @throws JMSException always (not supported)
     */
    @Override
    public void setClientID(String clientID) throws JMSException {
        validateConnection();
        throw new JMSException("Client identifiers are not supported by Azure Service Bus");
    }
    
    /**
     * Gets the metadata for this connection.
     * 
     * @return connection metadata
     * @throws JMSException if metadata cannot be retrieved
     */
    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        validateConnection();
        return new AzureServiceBusConnectionMetaData();
    }
    
    /**
     * Gets the exception listener for this connection.
     * 
     * @return the exception listener (always null in this implementation)
     * @throws JMSException if the operation fails
     */
    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        validateConnection();
        return null; // Exception handling is managed by the JCA container
    }
    
    /**
     * Sets the exception listener for this connection.
     * 
     * @param listener the exception listener (ignored)
     * @throws JMSException if the operation fails
     */
    @Override
    public void setExceptionListener(ExceptionListener listener) throws JMSException {
        validateConnection();
        logger.debug("Exception listener ignored - exception handling managed by JCA container");
    }
    
    /**
     * Starts message delivery for this connection.
     * 
     * @throws JMSException if the operation fails
     */
    @Override
    public void start() throws JMSException {
        logger.debug("Starting connection");
        validateConnection();
        // Message delivery is automatically started for Azure Service Bus
    }
    
    /**
     * Stops message delivery for this connection.
     * 
     * @throws JMSException if the operation fails
     */
    @Override
    public void stop() throws JMSException {
        logger.debug("Stopping connection");
        validateConnection();
        // Message delivery control is managed by individual sessions
    }
    
    /**
     * Closes this connection handle.
     * This does not close the physical connection, which is managed by the JCA container.
     * 
     * @throws JMSException if the operation fails
     */
    @Override
    public void close() throws JMSException {
        logger.debug("Closing connection handle");
        
        if (closed) {
            return;
        }
        
        try {
            closed = true;
            
            if (managedConnection != null) {
                managedConnection.removeConnectionHandle(this);
            }
            
            logger.debug("Connection handle closed successfully");
            
        } catch (Exception e) {
            logger.error("Error closing connection handle", e);
            throw new JMSException("Failed to close connection: " + e.getMessage());
        }
    }
    
    // Unsupported operations for queue-only implementation
    
    @Override
    public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector,
                                                      ServerSessionPool sessionPool, int maxMessages)
            throws JMSException {
        throw new JMSException("Connection consumers are not supported");
    }
    
    @Override
    public ConnectionConsumer createConnectionConsumer(Queue queue, String messageSelector,
                                                      ServerSessionPool sessionPool, int maxMessages)
            throws JMSException {
        throw new JMSException("Connection consumers are not supported");
    }
    
    @Override
    public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String subscriptionName,
                                                           String messageSelector, ServerSessionPool sessionPool,
                                                           int maxMessages) throws JMSException {
        throw new JMSException("Topics and shared consumers are not supported in queue-only implementation");
    }
    
    @Override
    public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic, String subscriptionName,
                                                                   String messageSelector, ServerSessionPool sessionPool,
                                                                   int maxMessages) throws JMSException {
        throw new JMSException("Topics and durable consumers are not supported in queue-only implementation");
    }
    
    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
                                                            String messageSelector, ServerSessionPool sessionPool,
                                                            int maxMessages) throws JMSException {
        throw new JMSException("Topics and durable consumers are not supported in queue-only implementation");
    }
    
    // Package-private methods for JCA integration
    
    /**
     * Sets the managed connection for this handle.
     * Called during connection association.
     * 
     * @param managedConnection the managed connection
     */
    void setManagedConnection(AzureServiceBusManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
    }
    
    /**
     * Invalidates this connection handle.
     * Called during connection cleanup.
     */
    void invalidate() {
        logger.debug("Invalidating connection handle");
        valid = false;
        closed = true;
        managedConnection = null;
    }
    
    /**
     * Checks if this connection handle is valid and not closed.
     * 
     * @throws JMSException if the connection is invalid or closed
     */
    private void validateConnection() throws JMSException {
        if (!valid) {
            throw new JMSException("Connection handle has been invalidated");
        }
        if (closed) {
            throw new JMSException("Connection has been closed");
        }
        if (managedConnection == null) {
            throw new JMSException("No managed connection available");
        }
        if (managedConnection.isDestroyed()) {
            throw new JMSException("Underlying managed connection has been destroyed");
        }
    }
    
    /**
     * Gets the underlying managed connection.
     * 
     * @return the managed connection
     */
    AzureServiceBusManagedConnection getManagedConnection() {
        return managedConnection;
    }
    
    /**
     * Checks if this connection handle is closed.
     * 
     * @return true if closed, false otherwise
     */
    boolean isClosed() {
        return closed;
    }
    
    /**
     * Checks if this connection handle is valid.
     * 
     * @return true if valid, false otherwise
     */
    boolean isValid() {
        return valid;
    }
}