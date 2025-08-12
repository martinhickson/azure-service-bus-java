// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jca;

import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import javax.jms.Session;
import javax.jms.JMSException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Azure Service Bus Managed Connection implementation.
 * 
 * This class represents the physical connection to Azure Service Bus and manages
 * the lifecycle of JMS connections within the JCA container.
 */
public class AzureServiceBusManagedConnection implements ManagedConnection {
    
    private static final Logger logger = LoggerFactory.getLogger(AzureServiceBusManagedConnection.class);
    
    private final ServiceBusJmsConnection jmsConnection;
    private final AzureServiceBusManagedConnectionFactory mcf;
    private final List<ConnectionEventListener> listeners;
    private final Set<AzureServiceBusConnection> connectionHandles;
    
    private boolean destroyed = false;
    
    /**
     * Creates a new managed connection wrapping a JMS connection.
     * 
     * @param jmsConnection the underlying JMS connection
     * @param mcf the managed connection factory that created this connection
     */
    public AzureServiceBusManagedConnection(ServiceBusJmsConnection jmsConnection,
                                           AzureServiceBusManagedConnectionFactory mcf) {
        this.jmsConnection = jmsConnection;
        this.mcf = mcf;
        this.listeners = new ArrayList<>();
        this.connectionHandles = new HashSet<>();
        
        logger.debug("Created new managed connection");
    }
    
    /**
     * Creates a connection handle for application use.
     * 
     * @param subject the security subject (not used)
     * @param cri connection request info (not used)
     * @return a connection handle for the application
     * @throws ResourceException if connection creation fails
     */
    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        logger.debug("Creating connection handle");
        
        if (destroyed) {
            throw new ResourceException("Managed connection has been destroyed");
        }
        
        try {
            AzureServiceBusConnection handle = new AzureServiceBusConnection(this);
            connectionHandles.add(handle);
            
            logger.debug("Created connection handle, total handles: {}", connectionHandles.size());
            return handle;
            
        } catch (Exception e) {
            logger.error("Failed to create connection handle", e);
            throw new ResourceException("Failed to create connection handle", e);
        }
    }
    
    /**
     * Physically closes the managed connection and releases all resources.
     * 
     * @throws ResourceException if destruction fails
     */
    @Override
    public void destroy() throws ResourceException {
        logger.debug("Destroying managed connection");
        
        if (destroyed) {
            logger.debug("Managed connection already destroyed");
            return;
        }
        
        try {
            // Close all connection handles
            cleanup();
            
            // Close the underlying JMS connection
            if (jmsConnection != null) {
                jmsConnection.close();
            }
            
            destroyed = true;
            logger.debug("Managed connection destroyed successfully");
            
        } catch (JMSException e) {
            logger.error("Error destroying managed connection", e);
            throw new ResourceException("Failed to destroy managed connection", e);
        }
    }
    
    /**
     * Cleans up all connection handles but keeps the physical connection open.
     * 
     * @throws ResourceException if cleanup fails
     */
    @Override
    public void cleanup() throws ResourceException {
        logger.debug("Cleaning up managed connection handles");
        
        try {
            // Invalidate all connection handles
            for (AzureServiceBusConnection handle : new HashSet<>(connectionHandles)) {
                handle.invalidate();
            }
            connectionHandles.clear();
            
            logger.debug("Cleaned up {} connection handles", connectionHandles.size());
            
        } catch (Exception e) {
            logger.error("Error during connection cleanup", e);
            throw new ResourceException("Failed to cleanup connection handles", e);
        }
    }
    
    /**
     * Associates this managed connection with a new set of connection handles.
     * 
     * @param connection the connection handle to associate
     * @throws ResourceException if association fails
     */
    @Override
    public void associateConnection(Object connection) throws ResourceException {
        logger.debug("Associating connection handle with managed connection");
        
        if (destroyed) {
            throw new ResourceException("Managed connection has been destroyed");
        }
        
        if (!(connection instanceof AzureServiceBusConnection)) {
            throw new ResourceException("Invalid connection type: " + connection.getClass().getName());
        }
        
        AzureServiceBusConnection handle = (AzureServiceBusConnection) connection;
        handle.setManagedConnection(this);
        connectionHandles.add(handle);
        
        logger.debug("Associated connection handle, total handles: {}", connectionHandles.size());
    }
    
    /**
     * Adds a connection event listener.
     * 
     * @param listener the listener to add
     */
    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            logger.debug("Added connection event listener, total listeners: {}", listeners.size());
        }
    }
    
    /**
     * Removes a connection event listener.
     * 
     * @param listener the listener to remove
     */
    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        if (listener != null && listeners.remove(listener)) {
            logger.debug("Removed connection event listener, total listeners: {}", listeners.size());
        }
    }
    
    /**
     * Returns an XAResource for transaction management.
     * 
     * Note: This implementation does not support XA transactions as per design requirements.
     * 
     * @return null (no XA support)
     * @throws ResourceException if XA resources are requested
     */
    @Override
    public XAResource getXAResource() throws ResourceException {
        // No XA transaction support - applications should use message outbox pattern
        throw new NotSupportedException("XA transactions are not supported. Use message outbox pattern with deduplication IDs.");
    }
    
    /**
     * Returns a LocalTransaction for local transaction management.
     * 
     * Note: This implementation does not support local transactions.
     * 
     * @return null (no local transaction support)
     * @throws ResourceException if local transactions are requested
     */
    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        // No local transaction support
        throw new NotSupportedException("Local transactions are not supported. Use message outbox pattern with deduplication IDs.");
    }
    
    /**
     * Returns metadata about this managed connection.
     * 
     * @return connection metadata
     * @throws ResourceException if metadata cannot be retrieved
     */
    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new AzureServiceBusManagedConnectionMetaData();
    }
    
    /**
     * Sets the log writer (not implemented - using SLF4J).
     */
    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        // Using SLF4J instead of PrintWriter
    }
    
    /**
     * Gets the log writer (not implemented - using SLF4J).
     */
    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        // Using SLF4J instead of PrintWriter
        return null;
    }
    
    // Package-private methods for use by connection handles
    
    /**
     * Creates a JMS session using the underlying connection.
     * 
     * @param transacted whether the session should be transacted
     * @param acknowledgeMode the acknowledgment mode
     * @return a JMS session
     * @throws JMSException if session creation fails
     */
    Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        if (destroyed) {
            throw new JMSException("Managed connection has been destroyed");
        }
        
        return jmsConnection.createSession(transacted, acknowledgeMode);
    }
    
    /**
     * Removes a connection handle from this managed connection.
     * Called when the handle is closed.
     * 
     * @param handle the handle to remove
     */
    void removeConnectionHandle(AzureServiceBusConnection handle) {
        if (connectionHandles.remove(handle)) {
            logger.debug("Removed connection handle, remaining handles: {}", connectionHandles.size());
        }
    }
    
    /**
     * Gets the managed connection factory that created this connection.
     * 
     * @return the managed connection factory
     */
    AzureServiceBusManagedConnectionFactory getManagedConnectionFactory() {
        return mcf;
    }
    
    /**
     * Gets the underlying JMS connection.
     * 
     * @return the JMS connection
     */
    ServiceBusJmsConnection getJmsConnection() {
        return jmsConnection;
    }
    
    /**
     * Checks if this managed connection has been destroyed.
     * 
     * @return true if destroyed, false otherwise
     */
    boolean isDestroyed() {
        return destroyed;
    }
    
    /**
     * Gets the current connection event listeners.
     * 
     * @return a copy of the listeners list
     */
    List<ConnectionEventListener> getListeners() {
        return new ArrayList<>(listeners);
    }
}