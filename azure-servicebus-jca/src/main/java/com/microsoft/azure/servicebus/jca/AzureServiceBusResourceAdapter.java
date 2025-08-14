package com.microsoft.azure.servicebus.jca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;

/**
 * Azure Service Bus Resource Adapter for JCA 1.7 integration with WildFly 26.
 * 
 * This class implements the main ResourceAdapter interface and manages the lifecycle
 * of the Azure Service Bus connector within the application server environment.
 */
@Connector(
    displayName = "Azure Service Bus Resource Adapter",
    vendorName = "Microsoft Azure",
    eisType = "Azure Service Bus",
    version = "1.0.0"
)
public class AzureServiceBusResourceAdapter implements ResourceAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(AzureServiceBusResourceAdapter.class);
    
    private BootstrapContext bootstrapContext;
    private boolean started = false;
    
    /**
     * Lifecycle method called when the resource adapter is deployed to the application server.
     * 
     * @param ctx the bootstrap context provided by the application server
     * @throws ResourceAdapterInternalException if initialization fails
     */
    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        logger.info("Starting Azure Service Bus Resource Adapter");
        
        if (ctx == null) {
            throw new ResourceAdapterInternalException("BootstrapContext cannot be null");
        }
        
        this.bootstrapContext = ctx;
        
        try {
            // Initialize any shared resources needed by the adapter
            // Note: The actual Azure Service Bus connections are managed per ManagedConnection
            logger.info("Azure Service Bus Resource Adapter initialized successfully");
            this.started = true;
            
        } catch (Exception e) {
            logger.error("Failed to start Azure Service Bus Resource Adapter", e);
            throw new ResourceAdapterInternalException("Failed to start resource adapter", e);
        }
    }
    
    /**
     * Lifecycle method called when the resource adapter is undeployed from the application server.
     */
    @Override
    public void stop() {
        logger.info("Stopping Azure Service Bus Resource Adapter");
        
        try {
            // Clean up any shared resources
            this.started = false;
            this.bootstrapContext = null;
            
            logger.info("Azure Service Bus Resource Adapter stopped successfully");
            
        } catch (Exception e) {
            logger.error("Error during resource adapter shutdown", e);
        }
    }
    
    /**
     * Called by the application server to activate a message endpoint for inbound messaging.
     * 
     * Note: This implementation is for outbound messaging (queue sending) only.
     * Inbound messaging (message-driven beans) is not supported in this version.
     * 
     * @param endpointFactory the message endpoint factory
     * @param spec the activation specification
     * @throws ResourceException if endpoint activation fails
     */
    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) 
            throws ResourceException {
        logger.debug("Endpoint activation requested for Azure Service Bus Resource Adapter");
        
        // For future implementation of inbound messaging (MDB support)
        throw new NotSupportedException(
            "Inbound messaging not supported in this version. " +
            "This resource adapter is designed for outbound queue operations only."
        );
    }
    
    /**
     * Called by the application server to deactivate a message endpoint.
     * 
     * @param endpointFactory the message endpoint factory
     * @param spec the activation specification
     */
    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        logger.debug("Endpoint deactivation requested for Azure Service Bus Resource Adapter");
        
        // Nothing to do for outbound-only implementation
    }
    
    /**
     * Called during crash recovery to return an array of XAResource instances for transaction recovery.
     * 
     * Note: This implementation does not support XA transactions as per the design requirements
     * (message outbox pattern with deduplication IDs).
     * 
     * @param specs array of activation specifications
     * @return empty array since XA transactions are not supported
     * @throws ResourceException if recovery fails
     */
    @Override
    public javax.transaction.xa.XAResource[] getXAResources(ActivationSpec[] specs) 
            throws ResourceException {
        logger.debug("XA resource recovery requested - returning empty array (no XA support)");
        
        // Return empty array - no XA transaction support
        // Application should use message outbox pattern with deduplication IDs
        return new javax.transaction.xa.XAResource[0];
    }
    
    // Package-private methods for internal use
    
    /**
     * Get the bootstrap context provided during startup.
     * 
     * @return the bootstrap context, or null if not started
     */
    BootstrapContext getBootstrapContext() {
        return bootstrapContext;
    }
    
    /**
     * Check if the resource adapter has been started.
     * 
     * @return true if started, false otherwise
     */
    boolean isStarted() {
        return started;
    }
}