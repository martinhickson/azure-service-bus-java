// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.jca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.JMSContext;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import java.io.Serializable;

/**
 * Application-level Connection Factory for Azure Service Bus JCA integration.
 * 
 * This ConnectionFactory is used by applications to obtain connections to Azure Service Bus.
 * It works with the JCA ConnectionManager to provide connection pooling and management.
 */
public class AzureServiceBusConnectionFactory 
        implements ConnectionFactory, QueueConnectionFactory, Serializable, Referenceable {
    
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AzureServiceBusConnectionFactory.class);
    
    private final AzureServiceBusManagedConnectionFactory mcf;
    private final ConnectionManager connectionManager;
    private Reference reference;
    
    /**
     * Creates a connection factory for managed environments.
     * 
     * @param mcf the managed connection factory
     * @param cm the connection manager provided by the application server
     */
    public AzureServiceBusConnectionFactory(AzureServiceBusManagedConnectionFactory mcf, 
                                           ConnectionManager cm) {
        this.mcf = mcf;
        this.connectionManager = cm;
        logger.debug("Created managed AzureServiceBusConnectionFactory");
    }
    
    /**
     * Creates a connection to Azure Service Bus.
     * 
     * @return a connection to Azure Service Bus
     * @throws JMSException if connection creation fails
     */
    @Override
    public Connection createConnection() throws JMSException {
        logger.debug("Creating connection");
        return createConnection(null, null);
    }
    
    /**
     * Creates a connection to Azure Service Bus with specified credentials.
     * 
     * Note: Username/password authentication is not supported.
     * Authentication is configured through the resource adapter configuration.
     * 
     * @param username ignored (authentication configured via resource adapter)
     * @param password ignored (authentication configured via resource adapter)
     * @return a connection to Azure Service Bus
     * @throws JMSException if connection creation fails
     */
    @Override
    public Connection createConnection(String username, String password) throws JMSException {
        logger.debug("Creating connection (username/password ignored - using resource adapter configuration)");
        
        try {
            // Use the JCA ConnectionManager to get a managed connection
            Object connection = connectionManager.allocateConnection(mcf, null);
            
            if (!(connection instanceof AzureServiceBusConnection)) {
                throw new JMSException("Invalid connection type returned by ConnectionManager: " + 
                                     connection.getClass().getName());
            }
            
            logger.debug("Successfully created connection");
            return (Connection) connection;
            
        } catch (ResourceException e) {
            logger.error("Failed to create connection", e);
            throw new JMSException("Failed to create connection: " + e.getMessage());
        }
    }
    
    /**
     * Creates a queue connection to Azure Service Bus.
     * 
     * @return a queue connection to Azure Service Bus
     * @throws JMSException if connection creation fails
     */
    @Override
    public QueueConnection createQueueConnection() throws JMSException {
        logger.debug("Creating queue connection");
        return createQueueConnection(null, null);
    }
    
    /**
     * Creates a queue connection to Azure Service Bus with specified credentials.
     * 
     * Note: Username/password authentication is not supported.
     * Authentication is configured through the resource adapter configuration.
     * 
     * @param username ignored (authentication configured via resource adapter)
     * @param password ignored (authentication configured via resource adapter)
     * @return a queue connection to Azure Service Bus
     * @throws JMSException if connection creation fails
     */
    @Override
    public QueueConnection createQueueConnection(String username, String password) throws JMSException {
        logger.debug("Creating queue connection (username/password ignored - using resource adapter configuration)");
        
        Connection connection = createConnection(username, password);
        
        if (!(connection instanceof QueueConnection)) {
            throw new JMSException("Connection does not support queue operations");
        }
        
        return (QueueConnection) connection;
    }
    
    // JMS 2.0 methods
    
    /**
     * Creates a JMS context with the specified session mode.
     * 
     * Note: JMSContext is not supported in this queue-only implementation.
     * 
     * @param sessionMode the session mode
     * @return never returns (throws exception)
     * @throws JMSException always (not supported)
     */
    @Override
    public JMSContext createContext(int sessionMode) {
        throw new UnsupportedOperationException("JMSContext is not supported in queue-only implementation. Use Connection.createSession() instead.");
    }
    
    /**
     * Creates a JMS context with the specified user credentials and session mode.
     * 
     * Note: JMSContext is not supported in this queue-only implementation.
     * 
     * @param userName ignored (authentication configured via resource adapter)
     * @param password ignored (authentication configured via resource adapter)  
     * @param sessionMode the session mode
     * @return never returns (throws exception)
     * @throws JMSException always (not supported)
     */
    @Override
    public JMSContext createContext(String userName, String password, int sessionMode) {
        throw new UnsupportedOperationException("JMSContext is not supported in queue-only implementation. Use Connection.createSession() instead.");
    }
    
    /**
     * Creates a JMS context with default settings.
     * 
     * Note: JMSContext is not supported in this queue-only implementation.
     * 
     * @return never returns (throws exception)
     * @throws JMSException always (not supported)
     */
    @Override
    public JMSContext createContext() {
        throw new UnsupportedOperationException("JMSContext is not supported in queue-only implementation. Use Connection.createSession() instead.");
    }
    
    /**
     * Creates a JMS context with the specified user credentials.
     * 
     * Note: JMSContext is not supported in this queue-only implementation.
     * 
     * @param userName ignored (authentication configured via resource adapter)
     * @param password ignored (authentication configured via resource adapter)  
     * @return never returns (throws exception)
     * @throws JMSException always (not supported)
     */
    @Override
    public JMSContext createContext(String userName, String password) {
        throw new UnsupportedOperationException("JMSContext is not supported in queue-only implementation. Use Connection.createSession() instead.");
    }
    
    /**
     * Gets the JNDI reference for this connection factory.
     * 
     * @return the JNDI reference
     * @throws NamingException if the reference cannot be created
     */
    @Override
    public Reference getReference() throws NamingException {
        return reference;
    }
    
    /**
     * Sets the JNDI reference for this connection factory.
     * 
     * @param reference the JNDI reference
     */
    public void setReference(Reference reference) {
        this.reference = reference;
    }
    
    /**
     * Gets the managed connection factory.
     * 
     * @return the managed connection factory
     */
    public AzureServiceBusManagedConnectionFactory getManagedConnectionFactory() {
        return mcf;
    }
    
    /**
     * Gets the connection manager.
     * 
     * @return the connection manager
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }
    
    @Override
    public String toString() {
        return "AzureServiceBusConnectionFactory{" +
               "mcf=" + mcf +
               ", connectionManager=" + connectionManager +
               '}';
    }
}